## ES6.3.2 副本失败处理

副本的失败处理对理解ES的数据副本模型很有帮助。在[ES6.3.2 index操作源码流程](https://www.cnblogs.com/hapjin/p/10577427.html)的总结中提到：ES的写操作会先写主分片，然后主分片再将操作同步到副本分片。本文给出ES中的源码片断，分析副本执行操作失败时，ES是如何处理的。

副本执行源码：`replicasProxy.performOn`实现了副本操作，执行正常结束回调onResponse()，异常回调onFailure()

```java
replicasProxy.performOn(shard, replicaRequest, globalCheckpoint, new ActionListener<ReplicaResponse>() {
            @Override
            public void onResponse(ReplicaResponse response) {
                successfulShards.incrementAndGet();
                try {
                    primary.updateLocalCheckpointForShard(shard.allocationId().getId(), response.localCheckpoint());//执行成功回调更新检查点
                    primary.updateGlobalCheckpointForShard(shard.allocationId().getId(), response.globalCheckpoint());
                } catch (final AlreadyClosedException e) {
                    // okay, the index was deleted or this shard was never activated after a relocation; fall through and finish normally
                } catch (final Exception e) {
                    // fail the primary but fall through and let the rest of operation processing complete
                    final String message = String.format(Locale.ROOT, "primary failed updating local checkpoint for replica %s", shard);
                    primary.failShard(message, e);
                }
                decPendingAndFinishIfNeeded();//不管是正常的onResponse还是异常的onFailure,都会调用这个方法,代表已经完成了一个操作,pendingActions减1
            }

            @Override
            public void onFailure(Exception replicaException) {
                logger.trace(() -> new ParameterizedMessage(
                    "[{}] failure while performing [{}] on replica {}, request [{}]",
                    shard.shardId(), opType, shard, replicaRequest), replicaException);
                // Only report "critical" exceptions - TODO: Reach out to the master node to get the latest shard state then report.
                if (TransportActions.isShardNotAvailableException(replicaException) == false) {
                    RestStatus restStatus = ExceptionsHelper.status(replicaException);
                    shardReplicaFailures.add(new ReplicationResponse.ShardInfo.Failure(
                        shard.shardId(), shard.currentNodeId(), replicaException, restStatus, false));
                }
                String message = String.format(Locale.ROOT, "failed to perform %s on replica %s", opType, shard);
                //---> failShardIfNeeded 具体执行何种操作要看 replicasProxy的真正实现类:如果是WriteActionReplicasProxy则会报告shard错误
                replicasProxy.failShardIfNeeded(shard, message,
                    replicaException, ReplicationOperation.this::decPendingAndFinishIfNeeded,
                    ReplicationOperation.this::onPrimaryDemoted, throwable -> decPendingAndFinishIfNeeded());
            }
        });
    }
```



### 执行正常结束回调onResponse()

`successfulShards.incrementAndGet();`，在返回的结果里面，_shards 字段里面就能看到 successful 数值。

更新 local checkpoint 和 global checkpoint：如果检查点更新失败，触发：replica shard engine 关闭。

```java
/**
     * Fails the shard and marks the shard store as corrupted if
     * <code>e</code> is caused by index corruption
     *
     * org.elasticsearch.index.shard.IndexShard#failShard
     */
    public void failShard(String reason, @Nullable Exception e) {
        // fail the engine. This will cause this shard to also be removed from the node's index service.
        getEngine().failEngine(reason, e);
    }
```

```
fail engine due to some error. the engine will also be closed.
The underlying store is marked corrupted iff failure is caused by index corruption

```

关于检查点，可参考这篇文章：[elasticsearch-sequence-ids-6-0](https://www.elastic.co/blog/elasticsearch-sequence-ids-6-0)

### 异常结束回调 onFailure()

```java
replicasProxy.failShardIfNeeded(shard, message,
                    replicaException, ReplicationOperation.this::decPendingAndFinishIfNeeded,
                    ReplicationOperation.this::onPrimaryDemoted, throwable -> decPendingAndFinishIfNeeded());
```

failShardIfNeeded **可以**做2件事情，具体是如何执行得看failShardIfNeeded的实现类。

1. onPrimaryDemoted

   通知master primary stale（过时）了。index操作首先在primary shard执行成功了，然后同步给replica，但是replica发现此primary shard 的 primary term  比它知道的该索引的primary term 还小，于是replica就认为此primary shard是一个已经过时了的primary shard，因此就回调onFailure()拒绝执行，并执行onPrimaryDemoted通知master节点。

   ```java
   private void onPrimaryDemoted(Exception demotionFailure) {
           String primaryFail = String.format(Locale.ROOT,
               "primary shard [%s] was demoted while failing replica shard",
               primary.routingEntry());
           // we are no longer the primary, fail ourselves and start over
           primary.failShard(primaryFail, demotionFailure);
           finishAsFailed(new RetryOnPrimaryException(primary.routingEntry().shardId(), primaryFail, demotionFailure));
       }
   ```

   ​

2. decPendingAndFinishIfNeeded

   计数。一个请求会由ReplicationGroup中的 多个分片执行，这些分片是否都已经执行完成了？就由pendingActions计数。不管是执行正常结束onResponse还是异常结束onFailure都会调用这个方法。

   ```java
   private void decPendingAndFinishIfNeeded() {
           assert pendingActions.get() > 0 : "pending action count goes below 0 for request [" + request + "]";
           if (pendingActions.decrementAndGet() == 0) {//当所有的shard都处理完这个请求,client收到ACK(里面允许一些replica执行失败), 或者是收到一个请求超时的响应
               finish();
           }
       }
   ```

   对于发起index操作的Client而言，该 index 操作会由primary shard 执行，也会由若干个replica执行。因此，pendingActions统计到底有多少个分片(既包括主分片也包括副本分片)执行**完成**(在某些副本分片上执行失败也算执行完成)了。

   Client要么收到一个执行成功的ACK（默认情况下，只要primary shard执行成功，若存在 replica执行失败，Client也会收到一个执行成功的ACK，只不过 返回的ACK里面 _shards参数下的 failed 不为0而已），如下：

   >{
   >  "_index": "user",
   >  "_type": "profile",
   >  "_id": "10",
   >  "_version": 1,
   >  "result": "created",
   >  "_shards": {
   >​    "total": 3,
   >​    "successful": 1,
   >​    "failed": 0
   >  },
   >  "_seq_no": 0,
   >  "_primary_term": 1
   >}

   要么收到一个超时ACK，如下：（[这篇文章](https://www.cnblogs.com/hapjin/p/9821073.html)提到了如何产生一个超时的ACK）

   >{
   >  "statusCode": 504,
   >  "error": "Gateway Time-out",
   >  "message": "Client request timeout"
   >}

failShardIfNeeded方法一共有2个具体实现，看类图：



TransportReplicationAction.ReplicasProxy#failShardIfNeeded （默认实现）

```java
@Override
        public void failShardIfNeeded(ShardRouting replica, String message, Exception exception,Runnable onSuccess, Consumer<Exception> onPrimaryDemoted, Consumer<Exception> onIgnoredFailure) {
            // This does not need to fail the shard. The idea is that this
            // is a non-write operation (something like a refresh or a global
            // checkpoint sync) and therefore the replica should still be
            // "alive" if it were to fail.
            onSuccess.run();
        }
```



TransportResyncReplicationAction.ResyncActionReplicasProxy#failShardIfNeeded(副本resync操作的实现)

```java
/**
     * A proxy for primary-replica resync operations which are performed on replicas when a new primary is promoted.
     * Replica shards fail to execute resync operations will be failed but won't be marked as stale.
     * This avoids marking shards as stale during cluster restart but enforces primary-replica resync mandatory.
     */
    class ResyncActionReplicasProxy extends ReplicasProxy {
        @Override
        public void failShardIfNeeded(ShardRouting replica, String message, Exception exception, Runnable onSuccess,
                                      Consumer<Exception> onPrimaryDemoted, Consumer<Exception> onIgnoredFailure) {
            shardStateAction.remoteShardFailed(replica.shardId(), replica.allocationId().getId(), primaryTerm, false, message, exception,
                createShardActionListener(onSuccess, onPrimaryDemoted, onIgnoredFailure));
        }
    }
```



TransportWriteAction.WriteActionReplicasProxy#failShardIfNeeded(index 写操作的实现)

 ```java
/**
 * A proxy for <b>write</b> operations that need to be performed on the
 * replicas, where a failure to execute the operation should fail
 * the replica shard and/or mark the replica as stale.
 *
 * This extends {@code TransportReplicationAction.ReplicasProxy} to do the
 * failing and stale-ing.
 */
class WriteActionReplicasProxy extends ReplicasProxy {

    @Override
    public void failShardIfNeeded(ShardRouting replica, String message, Exception exception,Runnable onSuccess, Consumer<Exception> onPrimaryDemoted, Consumer<Exception> onIgnoredFailure) {
        if (TransportActions.isShardNotAvailableException(exception) == false) {
            logger.warn(new ParameterizedMessage("[{}] {}", replica.shardId(), message), exception);}
        shardStateAction.remoteShardFailed(replica.shardId(), replica.allocationId().getId(), primaryTerm, true, message, exception,
            createShardActionListener(onSuccess, onPrimaryDemoted, onIgnoredFailure));
    }
 ```



