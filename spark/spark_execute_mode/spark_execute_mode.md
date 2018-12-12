### Spark execute mode and deploy spark Applications

在这篇文章中提到了因未按官方推荐方式提交Spark jar 而导致Spark应用程序无法执行的问题。于是我就翻书找资料，于是就有了两个问题：

- spark execute mode 有哪些？
- how to deploy spark applications?

主要的参考资料是《Spark Definitive Guide》的第2章、15章和17章的内容。

当然，网上已经有很多关于这2个问题的文章，这里写下来重新理解一遍。

spark是一个计算引擎，在计算机集群上做并行数据处理。

如果你有1T数据，对于单机而言，不太可能一下子把这么多数据一下子加载到内存，单机1TB的内存的机器没见过；就算每次	1GB地把数据加载内存进行处理，那也是串行的方式。

而在分布式环境下，1TB的数据可以分成很多个Partition，将partition分散在不同机器上，每台机器处理自己机器上的partition，再有一个框架，负责协调不同机器上数据处理，这样的话就可以并行处理了，而spark就是一个这样的并行计算处理引擎，现实中的数据处理任务，不仅仅是分发数据到各个机器上，还有容错，有些任务还要合并结果，这些都由一个框架统一地给你做了，你呢只需写个代码打成jar包，它自动把jar包分发到各个机器上，执行程序、处理数据，最终把各台机器上的结果合并、输出。

有许多机器，这些机器上的资源比如CPU、内存、网络……需要管理，一个程序提交上来，给它开多少个进程，分配多少内存？于是就有了YARN或者Spark standalone cluster manager。

一般的数据处理任务，类似于SQL那样，比如找出所有用户中年纪最大的用户、每个用户充了多少钱……就可以用Spark SQL来处理。还有一些复杂点的任务，比如根据用户的个性签名和自我介绍，观察一下TA们的兴趣点在哪里？这些可以用Spark MLlib 来处理。

因此，咱就只需写一个spark应用程序，来完成上面所说的数据处理任务。

那什么是spark应用程序？spark application 由两部分组成：

- driver process

  类似于负责人，把数据处理任务分发给各个小弟去执行，时不时地问下小弟们任务完成得怎么样了？

- execute process

  执行具体任务的小弟

为什么是这样的两部分组成？这正是分布式处理的体现。

现在spark应用程序在集群的各个机器上执行了，要是有一个专门负责管理集群上执行的作业的东西就好了，有时还写了一个MapReduce程序，也想放到集群上跑。因此，就需要一个Cluster Manager的角色，而Hadoop YARN就很适合做Cluster Manager，另外还有两个常用的Cluster Manager分别是Spark Standalone和Apache Mesos

如下图：





### Spark execution mode

扯淡了这么多，终于扯到spark execution mode了。我觉得谈论spark execution mode前，最好提一下它是基于何种Cluster Manager的，spark application 有三种execution mode，看图最能体现出它们的区别了：

- cluster mode
- client mode
- local mode





https://spark.apache.org/docs/latest/spark-standalone.html#launching-spark-applications

spark standalone cluster 支持2种作业的执行模式，client mode 和 cluster mode

>In `client` mode, the driver is launched in the same process as the client that submits the application. In `cluster` mode, however, the driver is launched from one of the Worker processes inside the cluster, and the client process exits as soon as it fulfills its responsibility of submitting the application without waiting for the application to finish.

https://github.com/hapjin/note.git

参考spark示例中出现的bug分析。

注意 deploy mode 和 execution mdoe 的区别

spark 作业的执行模型：

- client mode

- cluster mode

- lcoal mode

  ​

spark deploy mode涉及到节点的角色：master节点（ResourceManger/NodeManager)

