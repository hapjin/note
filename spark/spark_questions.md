1. spark应用程序的执行模式(executor mode)？三种

   - cluster mode

     提交JAR包给Cluster Manager，比如YARN(spark-submit --master xxx --deploy-mode cluster)，由YARN管理的机器上创建 spark driver process和 executor process。spark driver进程所在的机器也是由集群资源管理器(既可以是standalone、也可以是YARN、Mesos)来管理的

   - client mode

     单独的一台Client机器接收spark JAR包，spark JAR包提交到这台机器上(spark-submit --master xxx --deploy-mode client)，这台机器并不由集群资源管理器管理，spark driver process在这台client机器上运行，而 executor process则运行在集群资源管理器管理的机器上。

   - local mode

     整个spark 应用程序运行在一台机器上。即spark driver process和executor process都在一台机器上。

   ​

2. spark 应用程序的生命周期、执行过程？

   宏观上看：假设以cluster node模式为例：

   - 提交：client 提交 spark app jar，执行这个提交的代码是在本地机器上，它与cluster manager通信，将作业提交给cluster manager 请求为创建spark driver process分配资源。因此，提交程序退出。
   - 启动：创建spark driver process（spark session），cluster manager为之分配资源。spark session再请求为各个executor process分配资源。这时就是一个"spark cluster application"开始运行了
   - 执行：driver process 为各个 executor process分配任务，executor process向driver process报告task执行状态。
   - 完成：driver、executor 退出，在集群资源管理上能查到任务执行结果。

   spark作业的执行步骤？

   - 写 DataFrame、Dataset、Spark SQL API 程序
   - 代码校验后转化成 logic plan
   - logic plan 转化成 physical plan （ catalyst 优化）physical plan 就会生成一系列的RDD和transformations
   - 选择一个cost最小的physical plan，执行。

   从spark内部看 spark 作业的生命周期

   - ​

3. spark应用程序的组成？

   spark应用程序由driver process和executor process组成。driver process由spark session创建，比如将spark代码打成jar包提交后，driver process负责执行main方法，并将代码的执行逻辑(分布式处理任务)交由executor process执行。

4. spark集群部署模式？三种

   这是指spark节点(机器)运行在何种模式下。这三种模式都可以是多个spark节点(机器)组成的一个集群。都需要spark Master节点的高可用性。

   - standalone mode

     由spark自己来管理集群资源(cpu、内存...)，只能执行spark作业。

   - YARN

     集群的资源管理交由YARN，client将spark程序提交给YARN，作业运行过程中所需的资源(cpu、内存)由YARN来分配、回收。在这种模式下，比如还安装了Hadoop，那么也可以将MapReduce作业提交给YARN，执行MR作业。

   - Mesos

     和YARN类似，只不过是另一种集群资源管理器。

5. spark master 高可用性如何保证？spark master HA (二种方式)

   - zookeeper

     启动多个master实例，其中一个是Leader，其他的运行在standby 模式。这些master实例都"注册"到zookeeper中，由zookpeer的高可用性保证spark master的高可用性。

     当前的leader挂了后，zookeeper会从若干个standby 中选择一个作为新的master，恢复旧的master的状态信息。

   - local file system

6. spark master节点 slave节点的作用对比？

   master 节点、worker节点(slave节点)是从节点角度出发的定义。spark driver process、executor process是从spark app jar 应用程序角度的定义。

   ​

7. RDD、DataFram、DataSet 异同

   共同点：Partition 分布（分布式执行）、惰性执行、创建之后不可变(Transformation)

   不同点：RDD是low level AP中使用的数据结构，它更偏向底层，其他2个是high level API，提供了更多的抽象。比如说RDD中的一条记录可能是一个python对象、java对象（这种底层的对象），处理这些对象时，相同的操作(比如求和)需要编写不同的代码，重复发明轮子。

   RDD是无结构的，一行数据不知道是何种类型，没有模式信息(字段类型、字段名称)；DataFrame 和 Dataset 是结构化数据集合，是有schema信息的。但是，DataFrame是无类型的(运行期检查)，第一行记录是Row对象(Row对象其实是个byte 数组)，Dataset是强类型的，即每一行记录有一个明确的类型(编译期检查、String 对象)。DataFrame和Dataset都会编译成RDD运行。

   DataFrame、Dataset的schmea信息能够让Spark更好地优化数据的存储。因为知道数据类型，能够更好地压缩。

   Python  RDD执行效率非常低，它的每个记录是python对象，运行python RDD 就相当于运行python udf，数据需要来回地在python 进程和jvm进程(spark是scala写的，运行在jvm之上)之间序列化

8. spark算子 Transformation 和 action 区别？哪些算子是Transformation，哪些算子Actions?

   在spark中核心数据结构是不可变的，比如DataFrame。但是数据处理过程中需要从一种形式变成另一种形式，这就是Transforamtion（数据处理逻辑），它生成一个新的DataFrame

   Action 则是触发执行。Transforamtion只是定义好处理逻辑，具有延迟执行的特征，Action会触发Transformation执行，计算结果。(bring data out of the RDD world of spark into some other storage system)

   常见的Action有：reduce、take、collect、count、show

   常见的Transformation 有：filter、mapping

   Transforamation分成2种：窄依赖 和 宽依赖

   - 窄依赖：一个partition输入对应一个partition输出。对于窄依赖转换，spark会采用pipelining操作，一系列的转换是在内存中完成的。（filter操作是窄转换）
   - 宽依赖：一个partition输入对应若干个partition输出。对于宽依赖，又称为shuffle，它意味各个partitiion中的记录(record) 打乱后重新组织，**shuffle操作会伴随中间结果写入disk**。(aggregation、sort 是宽转换)

   ​

9. Cache 和 checkpoint 的作用及异同？

   cache：将RDD保存在内存里面。需要重复计算并且不太大的rdd，有必要cache。cache的目的是避免重复计算。

   checkpoint：将RDD持久化到磁盘。chain太长的rdd、计算时间很长的rdd 有必要checkpoint。checkpoint的目的是避免丢失(数据可靠性)

   缓存的代价：1，缓存数据需要序列化及反序列化。2，缓存耗费存储空间。

10. groupBy、reduce、groupByKey、reduceByKey

11. spark job、stage、task 以及pipeline

    一个spark job包含多个stages，每个stage由多个tasks组成。当transforamtion中包含shuffle(physical repartitioning of the data)时，会生成新的stage。range函数是一个shuffle函数，默认生成8个partition，由spark sql 执行shuffle时默认生成200个partition

    partition的数量不要超过executor process的数量。stage里面的各个tasks是并行执行的。每个task由一块数据(one partition)以及定义在这块数据上的transformation组成，每个task由一个executor process执行。因此，partition的数量影响并行度，比如一个DataFrame分割成更多的partition，就有更好的并行度。

    pipeline：把多个 transformation（stage）组合成一个stage(不能包含有shuffle的操作)，从而避免了每执行一个transforamtion就要写disk(比如MR快的原因)。比如map-->filter-->map-->filter，上一个map的结果直接传递给filter，不需要write disk。 

12. xxx