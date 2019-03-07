```java
//modules 和 plugins 目录下的内容
this.pluginsService = new PluginsService(tmpSettings, environment.configFile(), environment.modulesFile(), environment.pluginsFile(), classpathPlugins);
```

ClassLoader、SPI、Bundle(a "bundle" is a group of plugins in a single classloader)

```java
        // load modules
        if (modulesDirectory != null) {
                Set<Bundle> modules = getModuleBundles(modulesDirectory);
                for (Bundle bundle : modules) {
                    modulesList.add(bundle.plugin);
                }
                seenBundles.addAll(modules);
        }

        // now, find all the ones that are in plugins/
        if (pluginsDirectory != null) {
              List<BundleCollection> plugins = findBundles(pluginsDirectory, "plugin");
                    for (final BundleCollection plugin : plugins) {
                        final Collection<Bundle> bundles = plugin.bundles();
                        for (final Bundle bundle : bundles) {
                            pluginsList.add(bundle.plugin);
                        }
                        seenBundles.addAll(bundles);
                        pluginsNames.add(plugin.name());
        }
```



```java
            try (DirectoryStream<Path> jarStream = Files.newDirectoryStream(dir, "*.jar")) {
                for (Path jar : jarStream) {
                    // normalize with toRealPath to get symlinks out of our hair
                    URL url = jar.toRealPath().toUri().toURL();
                    if (urls.add(url) == false) {
                        throw new IllegalStateException("duplicate codebase: " + url);
                    }
                }
            }
```







```java

        // create a child to load the plugin in this bundle
        ClassLoader parentLoader = PluginLoaderIndirection.createLoader(getClass().getClassLoader(), extendedLoaders);
        ClassLoader loader = URLClassLoader.newInstance(bundle.urls.toArray(new URL[0]), parentLoader);
```







```java

//从plugin service 中过滤出 与Analysis相关的plugin
AnalysisModule analysisModule = new AnalysisModule(this.environment, pluginsService.filterPlugins(AnalysisPlugin.class));
```







```java
        NamedRegistry<AnalysisProvider<CharFilterFactory>> charFilters = setupCharFilters(plugins);
        NamedRegistry<AnalysisProvider<TokenFilterFactory>> tokenFilters = setupTokenFilters(plugins, hunspellService);
        NamedRegistry<AnalysisProvider<TokenizerFactory>> tokenizers = setupTokenizers(plugins);
        NamedRegistry<AnalysisProvider<AnalyzerProvider<?>>> analyzers = setupAnalyzers(plugins);
        NamedRegistry<AnalysisProvider<AnalyzerProvider<?>>> normalizers = setupNormalizers(plugins);
```



```java
    private NamedRegistry<AnalysisProvider<AnalyzerProvider<?>>> setupAnalyzers(List<AnalysisPlugin> plugins) {
        NamedRegistry<AnalysisProvider<AnalyzerProvider<?>>> analyzers = new NamedRegistry<>("analyzer");
        analyzers.register("default", StandardAnalyzerProvider::new);
        analyzers.register("standard", StandardAnalyzerProvider::new);
```



```java
    public StandardAnalyzerProvider(IndexSettings indexSettings, Environment env, String name, Settings settings) {
     	//....
        standardAnalyzer = new StandardAnalyzer(stopWords);
        standardAnalyzer.setVersion(version);
    }
```



引用一段《》中关于 token、type、term、dictionary概念的解释：(这里的type和ElasticSearch索引中的type是不一样的，ElasticSearch索引中的type以后版本将不支持了)

>A token is an instance of a sequence of characters in some particular document that are grouped together as a useful semantic unit for processing. A type is the class of all tokens containing the same character sequence. A term is a (perhaps normalized) type that is included in the IR system's dictionary.
>
>For example, if the document to be indexed is to `sleep perchance to dream`, then there are 5 tokens, but only 4 types (since there are 2 instances of to). However, if to is omitted from the index (as a stop word) then there will be only 3 terms: sleep, perchance, and dream.





- token、type、term 区别：第二章22-23页
- p41 match_pharse 的底层查询实现原理，基于 term 的position
- ​