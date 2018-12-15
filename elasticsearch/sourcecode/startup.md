### es source code snippets

```java
        putSystemPropertyIfSettingIsMissing(settings, "path.data", "es.path.data");
        putSystemPropertyIfSettingIsMissing(settings, "path.home", "es.path.home");
        putSystemPropertyIfSettingIsMissing(settings, "path.logs", "es.path.logs");
```



```html
Prepares the settings by gathering all elasticsearch system properties, optionally loading the configuration settings,and then replacing all property placeholders.and then replacing all property placeholders.
```





```java
// a misconfigured java.io.tmpdir can cause hard-to-diagnose problems later, so reject it immediately
        try {
            env.validateTmpFile();
        } catch (IOException e) {
            throw new UserException(ExitCodes.CONFIG, e.getMessage());
        }
```



