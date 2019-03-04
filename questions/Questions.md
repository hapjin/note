## Questions



### JVM 和 GC

1. 什么时候触发GC？

   JVM堆内存结构：新生代和老年代。新生代有Eden区和两个Survivor区，当Eden区和其中一个Survivor区无法容纳新对象时，触发Minor GC。

   新生代的对象有年龄，当年龄到达15时晋升到老年代。老年代没有足够空间容纳晋升的对象时，触发Major GC。

   这种触发GC的方法叫做Allocation Failure（分配失败）

   还有一种触发GC的方式是：GC Locker：

   另外，触发GC事件与特定的垃圾回收器有关，以CMS为例：有一个参数`XX:CMSInitiatingOccupancyFraction=75`当老年代使用量超过75%时触发GC。CMS也会预判当新生代对象不能顺利晋升到老年代时，会提前触发老年代GC。

2. 什么时候触发OOM？

   对于Parallel垃圾收集器（相对于The Mostly Concurrent Collectors而言）当使用了98%的时间却回收了不到2%的堆内存时，抛出OOM Error。

   ​

3. ​



### 多线程和线程池

1. 为什么使用线程池？

   线程复用。用线程池来管理线程比手工new Thread 再调用start启动线程的好处实在是太多了……

2. 线程池的实现方式？

   自定义线程池从继承ThreadPoolExecutor开始，指定参数：core pool size，max pool size，工作队列(SynchronousQueue、LinkedBlockquque/LinkedTransferQueue、ArrayBlockingQueue)、线程工厂、拒绝策略。

   pool size决定线程的数量，这个可根据任务类型来确定。线程数量公式(感觉也是基于Little's Law)$N_{cpu}*U_{cpu}*(1+\frac{W}{C})$。或者，CPU密集型作业$N_{cpu}+1​$，IO密集型作业$2*N_{cpu}+1$。

   定义工作队列需要确定队列的长度，单从任务的角度来考虑：可基于Little's Law确定，$任务队列长度=任务的到达速率*任务的响应时间$。任务的响应时间分成2部分：队列中的等待时间、执行时间。

   而在实际的系统中，不仅仅需要考虑 任务的提交/到达速度，也需要考虑服务器的处理能力。而服务器的处理能力，又影响了任务的响应时间。比如10000QPS的服务器，处理每个任务耗时0.1s，任何时刻该服务器只能承担10000*0.1=1000个任务。

3. ​





### Bloom Filter

优势：

1. 空间优势（相比于HashMap），采用**多个**哈希函数将元素是否存在映射成位操作（有点类似时间换空间--只能判断元素是否存在，不能根据key获取value）
2. 判断海量数据。解决 HashMap 大数据量的冲突严重问题
3. 非常适用于判断，某个元素不在某个大集合中

算法原理：

1. 两个待指定的参数：待写入的数据量n、误报的概率p

2. 一个默认参数：murmur3_128 哈希策略

3. 计算Array数组位数（底层是个long类型数组而不是BitSet）：n是数据量、p是误报率。

   $$m=\frac{-n*log(p)}{log2*log2}$$

   底层数组长度m  远大于 数据量n

4. 计算哈希函数的个数：n是数据量，m是上一步计算出来的位数

   $$k=max(1,\frac{m}{n}*log2)$$

5. 对于一个元素e，多少位会被置为1？k哈希函数个数，m是数组的长度

   k个位。对每个哈希函数，$index=hash_i(e)，0 \leq index\leq m-1$，将对应位置为1：Array[index]=1

   ​

   ​

6. xxx



### skip list（跳跃表）

1. JDK包：java.util.concurrent.ConcurrentSkipListMap 和 Redis的 sorted list都采用了skip list。skip list与红黑树的各个操作的时间复杂度都一样：插入、删除、查找都是：O(logN)，但红黑树有复杂的平衡操作，此外skip list更适合于并发数据结构的实现。

   >The reason is that there are no known efficient lock-free insertion and deletion algorithms for search trees.

2. 链式结构，已排序的数据分布在多层链表中，当插入节点时以一个概率值决定新节点是否晋升到高层链表中，因此有节点冗余，是一种以空间换时间的数据结构。

3. 基本特征

   - Skip List由若干层链表组成，每层链表都是有序的。
   - 每个元素有四个基本的指针（前、后、上、下）如果一个元素出现在第 i 层，所有比 i 小的层都会包含该元素。
   - 第 i 层的元素通过一个向下的指针 指向 下一层 相同值的元素。头指针最高层的第一个元素，尾指针指向最高层最后一个元素。

4. 查找操作

   从头节点(顶层)开始，按照右指针直到右节点的值大于待查找的值。判断是否还有更低层次的链表，若有则移动到当前节点的下一层，直到最底层。如果最底层节点匹配，查找成功，否则查找失败。

5. 插入操作

   先按查找操作找到待插入的位置，更新链表节点指针插入节点。再以随机概率决定新加入节点的是否晋升到上一层链表（看论文细节）



### Elasticsearch

熟悉各个操作的**底层实现原理**、清楚该操作的基本**执行流程**。

1. Elasticsearch 数据副本模型

2. Elasticsearch master节点选举

3. Elasticsearch 索引(index)创建原理

4. Elasticsearch分片分配原理

   分片如何分配在各个节点上？分配决策由主节点完成。主要有2个问题：哪些分片分配给哪些节点？哪个分片作为主分片，哪些作为副本？

   ​

5. Elasticsearch文档路由原理

   文档如何分配给分片？根据document id 和 routing 参数计算 shard id 的过程。

   >shard_num = hash(_routing) % num_primary_shards

   默认情况下文档id就是路由参数_routing，这个公式直接将文档路由（采用murmur3哈希函数）到某个具体的分片上。

   当采用自定义路由参数时，为了避免数据分布不均匀，引入了routing_partition_size参数，先将文档路由到一组分片上，然后再从这组分片里面选择一个分片存储文档。

   >shard_num = (hash(_routing) + hash(_id) % routing_partition_size) % num_primary_shards

   在源码中：num_primary_shards其实是 routingNumShards（参数路由的分片数量）“并不是”定义索引时指定的分片数量。这是为了支持shrink操作。

   ​

6. Elasticsearch写操作

   - docId 是如何自动生成的？
   - doc--->in memroy buffer--->refresh segment--->commit disk。写操作首先将文档保存到内存，默认1s refresh 成为segment，segment是可被搜索的，然后是默认30min flush到磁盘。

7. ElasticSearch 获取文档原理（GET操作）

   GET 操作的会引发refresh吗？GET操作的一致性，Index了一篇文档，什么时候能被GET到？(测试一下，把refresh 设置得大一些，看看GET能否GET到？)

   写blog记录理解org.apache.lucene.util.SetOnce，如何实现只允许一次修改，多次读取的场景？

8. ElasticSearch查询原理（Search操作）



### Kafka









### 机器学习

1. 线性回归与逻辑回归的区别？
2. 朴素贝叶斯假设？
3. ​











### 有用的参考：

[Google Guava之BloomFilter源码分析及基于Redis的重构](http://www.fullstackyang.com/bu-long-guo-lu-qi-google-guavalei-ku-yuan-ma-fen-xi-ji-ji-yu-redis-bitmapsde-zhong-gou/)

[Java GC Causes Distilled](https://dzone.com/articles/java-gc-causes-distilled)

[抛出OOM Error 的8个症状](https://plumbr.io/outofmemoryerror)【已下载PDF版本】

[跳跃表Skip List的原理和实现(Java)](https://blog.csdn.net/derrantcm/article/details/79063312) 有图解

[SkipList的那点事儿](https://sylvanassun.github.io/2017/12/31/2017-12-31-skip_list/)实现参考

《skiplist a probabilistic alternative to balanced trees》









