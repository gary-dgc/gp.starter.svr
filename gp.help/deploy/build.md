# 应用编译打包过程

因为项目中模块之间存在依赖关系，所以需要按照步骤进行编译，之后进行打包。

1. 在gp.app.svc项目中执行mvn install，将服务包安装到本地

2. 在gp.app.api项目中执行编译build

3. 在gp.app.api项目中执行打包操作： mvn package

打包后文件：gp.starter.api-0.4.0.jar

``` 
<plugins>
    <plugin>
      <groupId>org.codehaus.mojo</groupId>
      <artifactId>exec-maven-plugin</artifactId>
      <version>1.5.0</version>
        <configuration>
            <executable>java</executable>
            <arguments>
                <argument>-jar</argument>
                <argument>target/${project.artifactId}-${project.version}-${env}.jar</argument>
            </arguments>
        </configuration>
    </plugin>
</plugins>
```

