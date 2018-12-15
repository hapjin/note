### Elasticsearch6.3.2启动过程源码阅读记录

网上有很多关于es的源码分析，觉得自己技术深度还不够，所以这些文章只是看源码过程中的一个笔记，谈不上分析。

整个启动过程以类名.方法名，按顺序依次描述如下：

1. `Elasticsearch.main`启动入口类，注册JVM关闭钩子用来清理资源。

2. `Command.mainwithoutErrorHandling`在es正式启动之前，加载一些命令：比如 `./elasticsearch -help`命令

   ```html
   starts elasticsearch
   Option                Description                                               
   ------                -----------                                               
   -E <KeyValuePair>     Configure a setting                                       
   -V, --version         Prints elasticsearch version information and exits        
   -d, --daemonize       Starts Elasticsearch in the background                    
   -h, --help            show help                                       
   ```

3. `EnvironmentAwareCommand.execute`加载配置参数

   ```java
           putSystemPropertyIfSettingIsMissing(settings, "path.data", "es.path.data");
           putSystemPropertyIfSettingIsMissing(settings, "path.home", "es.path.home");
           putSystemPropertyIfSettingIsMissing(settings, "path.logs", "es.path.logs");
   ```

   ​

4. `InternalSettingsPrepare.prePareEnvironment`解析ElasticSearch.yml中的配置参数

   >Prepares the settings by gathering all elasticsearch system properties, optionally loading the configuration settings,and then replacing all property placeholders.and then replacing all property placeholders.

   ![es_settings_yml](F:\note\github\note\elasticsearch\sourcecode\es_settings_yml.png)

5. `ElasticSearch.execute`执行初始化命令。另外在源码中还有看到一些有趣的注释，比如必须设置`java.io.tmpdir`，这个参数在 `config/jvm.options`文件中指定。

   ```java
   // a misconfigured java.io.tmpdir can cause hard-to-diagnose problems later, so reject it immediately
           try {
               env.validateTmpFile();
           } catch (IOException e) {
               throw new UserException(ExitCodes.CONFIG, e.getMessage());
           }
   ```

   ​

6. `Bootstrap.init`正式开始启动ElasticSearch

   `checkLucene()`检查相应的Lucene jar包。

   创建节点`node=new Node(environment){...}`

   ![init_node_info](F:\note\github\note\elasticsearch\sourcecode\init_node_info.png)

7. `Node.java 构造器`设置节点环境变量信息、构造插件服务(PluginService)，创建自定义的线程池，创建NodeClient，以及各种Module的加载

   ```java
   this.pluginsService=
   client = new NodeClient
   analysisModule
   settingsModule

   ```

   总之，Node.java的构造方法里面实现了创建一个ElasticSearch节点所必须的各种信息，想要了解ElasticSearch节点的内部结构，应该就得多看看这个方法里面的代码吧。

8. `ModulesBuilder.createInjector`

9. `Node.start`

10. xx

    ​

    ​





### 总结





最后放一张图：

![1544865737181](F:\note\typora\elasticsearch\source_code_analysis\启动过程\start_processing.png)

原文：