# 应用环境及资源配置说明

项目的资源控制包括两部分：
1. 应用相关的运行参数设置，控制使用
2. 应用中运行程序外的非结构化数据资源

## 应用运行参数

对于环境级的参数建议将其通过maven的Filter功能进行控制，路径在/src/main/filters中，开发者可以根据自己需要进行调整。

对于其他固定参数配置在src的/resource中：
1. /resources/config中是light-4j使用的配置文件，具体可以查阅相关文件
2. /resources/config/datasource.yml是数据库连接配置文件
3. /resources/jwks是light4j使用的密钥文件
4. /resources/gpress.yml是应用私有的额外配置参数，如有开发中需要设置应用参数请在此文件中添加

    ``` 
    // yml配置
    system:
      version: "0.1"
      app: gpress.starter
    
    // 读取
    GeneralConfig.getStringByKey("system.version");
    
    // 或者
    GeneralConfig.getStringByKeys("system", "app");
    ```
   
5. /resources/logback.xml 日志输出控制
6. /META-INF/services/com.gp.launch.CoreInitializer 指定SPI加载类

*其他说明*
light-4j支持加密参数控制，系统会自动进行解密形式如下：

```
CRYPT:6770:D16B96D62286D0001BC4F268C501281C

// 加密的方式如下：
AesCryptor encryptor = new AesCryptor();
String enc = encryptor.encrypt("gp", "pwd001");
System.out.println(enc);
    
// 命令行
java -jar xxxx/gp-common.jar com.gp.util.AesCryptor gp pwd001

```

## 非结构化资源处理

因为启用了filter参数替换功能，所以在resources目录中存在有文件模版等内容时，需要在filter配置中添加控制

    <plugin>
		<groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-resources-plugin</artifactId>
		<version>3.2.0</version>
        <configuration>
            <encoding>UTF-8</encoding>
            <nonFilteredFileExtensions>
                <!-- ignore filtering 忽略哥格式处理 -->
                <nonFilteredFileExtension>xlsx</nonFilteredFileExtension>
                <nonFilteredFileExtension>crt</nonFilteredFileExtension>
                <nonFilteredFileExtension>keystore</nonFilteredFileExtension>
                <nonFilteredFileExtension>truststore</nonFilteredFileExtension>
                <nonFilteredFileExtension>json</nonFilteredFileExtension>
            </nonFilteredFileExtensions>
        </configuration>
    </plugin>

