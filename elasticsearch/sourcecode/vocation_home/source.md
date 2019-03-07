#  ES源码阅读

`org.apache.lucene.util.SetOnce`实现**一次写，多次读**的数据结构，线程安全。

参考点：泛型、内部类、AtomicBoolean实现仅一次语义

`java.util.concurrent.ConcurrentLinkedQueue`阻塞队列和非阻塞队列的实现。提到了非阻塞算法：http://www.cs.rochester.edu/u/michael/PODC96.html

`UnicastZenPing`

- `org.elasticsearch.discovery.zen.UnicastZenPing#resolveHostsLists`发现节点，创建DiscoveryNode

- `org.elasticsearch.discovery.zen.UnicastZenPing#ping(java.util.function.Consumer<org.elasticsearch.discovery.zen.ZenPing.PingCollection>, org.elasticsearch.common.unit.TimeValue)`

  ```
  //有三轮pings请求发送
  Sends three rounds of pings notifying the specified

  //第一轮
  A batch of pings is sent（因为有很多discovery nodes）
  //第二轮
  then another batch of pings is sent at half the specified TimeValue
  //第三轮
  and then another batch of pings is sent at the specified TimeValue


  //超时设置
  The pings that are sent carry a timeout of 1.25 times the specified TimeValue
  ```

  ```java
  threadPool.generic().execute(pingSender);
  threadPool.schedule(TimeValue.timeValueMillis(scheduleDuration.millis() / 3), ThreadPool.Names.GENERIC, pingSender);
  threadPool.schedule(TimeValue.timeValueMillis(scheduleDuration.millis() / 3 * 2), ThreadPool.Names.GENERIC, pingSender);
  ```

  ​

- `org.elasticsearch.common.util.concurrent.KeyedLock`锁的实现。锁的持有者计数、多个锁管理与销毁

`ElasticsearchUncaughtExceptionHandler`ES异常处理器。

`JoinThreadControl`master选举线程。在主线程中创建Runnable任务，然后由generic线程池提交执行。

`org.elasticsearch.discovery.zen.NodeJoinController`选举过程实现，比如发起选举、等待足够投票以完成选举、选举之后发布集群状态。



选举涉及到的类：

- `org.elasticsearch.discovery.zen.ZenDiscovery`发现节点
- `org/elasticsearch/discovery/zen/ZenPing.java:40`用来发现节点的ping服务



`org.apache.lucene.util.CloseableThreadLocal`ThreadLocal类有缺陷，改进了ThreadLocal

`org.elasticsearch.common.util.concurrent.ThreadContext#PREFIX`什么是线程上下文？线程上下文线程执行所需要、执行过程中产生的一些信息而已。这些信息只针对这个线程有用而已。至于保存什么信息作为线程上下文，视应用的具体需求而定。

ES集群错误检测：
`org.elasticsearch.discovery.zen.MasterFaultDetection`检测master是否失效

`org.elasticsearch.discovery.zen.NodesFaultDetection`



