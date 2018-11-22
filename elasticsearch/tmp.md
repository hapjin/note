[elasticsearch] 大概明白了为什么ElasticSearch采用CMS垃圾回收器了，CMS的特点是响应时间优先，以最短回收停顿时间为目标。初始标记和重新标记阶段会发生STW，但是操作简单执行速度很快，而在耗时的并发标记和并发清除阶段，GC线程和用户线程一起并发执行的，最短响应停顿时间能保证用户的搜索请求能及时得到响应。由于ElasticSearch需要使用大内存，JVM使用内存越大，回收垃圾时间也越长。由于CMS采用Mark-sweep算法回收内存，当Full GC后会产生大量的内存碎片，大量碎片导致内存整理，而内存整理是STW的，如果某节点发生STW时间过长，其他节点就Ping不通这个节点了，默认3s ping超时之后，就会认为这个节点失败，可能就要进行重新选主或者分片迁移了……这样就越来越糟糕了

https://www.elastic.co/guide/en/elasticsearch/reference/current/modules-discovery-zen.html

https://www.elastic.co/blog/found-understanding-memory-pressure-indicator