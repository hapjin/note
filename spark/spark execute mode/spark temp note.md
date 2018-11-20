### spark temp note

https://spark.apache.org/docs/latest/spark-standalone.html#launching-spark-applications

spark standalone cluster 支持2种作业的执行模式，client mode 和 cluster mode

>In `client` mode, the driver is launched in the same process as the client that submits the application. In `cluster` mode, however, the driver is launched from one of the Worker processes inside the cluster, and the client process exits as soon as it fulfills its responsibility of submitting the application without waiting for the application to finish.

https://github.com/hapjin/note.git