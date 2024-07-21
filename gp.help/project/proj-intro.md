# 应用依赖说明及模块命名规范

GP框架目前已经发布到[Central Repository](https://central.sonatype.com/search?q=groupress)，pom信息如下：

    <parent>
        <groupId>com.groupress</groupId>
        <artifactId>gp-root</artifactId>
        <version>0.4.1</version>
    </parent>

在上述POM信息中需要注意以下几点：
1. Group Id：com.groupress
2. GP框架中全部组件POM的Artifact Id使用kebab-case命名法，使用“-”号连接
3. GP框架提供一个根POM：gp-root

## 项目POM的管理

在实际的工作中推荐如下做法：
1. 新建目录作为项目开发目录，并通过工具或手动编辑创建pom.xml文件
2. pom文件中设定gp-root为parent POM信息，这种方式可以使项目自动引入框架相关的开发包

``` 
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- 设定项目信息，可以进行独立的版本号管理 -->
    <groupId>com.starter</groupId>
    <artifactId>gp-starter</artifactId>
    <version>0.4.0</version>
    <!-- 指定打包方式 -->
    <packaging>pom</packaging>

    <!-- 指定框架的版本 -->
    <parent>
        <groupId>com.groupress</groupId>
        <artifactId>gp-root</artifactId>
        <version>0.4.1</version>
    </parent>

    <properties>
        <versions.maven-version>2.4</versions.maven-version>
        
        <!-- 设定框架版本，主要用于在module中控制框架版本信息-->
        <gpress.version>0.4.1</gpress.version>
    </properties>

    <!-- 指定项目的子模块 -->
    <modules>
        <module>gp.app.api</module>
        <module>gp.app.svc</module>
    </modules>

    <build>
        <plugins>
            <!-- Optional：禁用签名处理组件 -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-gpg-plugin</artifactId>
                <version>1.5</version>
                <executions>
                    <execution>
                        <id>sign-artifacts</id>
                        <phase>none</phase> <!-- disable plugin from gp-root: verify -->
                        <goals>
                            <goal>sign</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

3. 在项目目录中根据需要创建子模块目录，数据库应用推荐创建两模块分别对应ApI处理和数据库的服务层开发，二者之间API依赖于服务层项目。

``` 
  /com.starter:gp-starter
  |--/com.starter:gp.starter.api
     |--/com.starter.gp.starter.svc
```
采用上述依赖设计的原因在于避免在开发中的循环依赖问题，历史经验告诉我们：越是认为不会发生的事儿，往往总是发生。在开发中曾有人将HttpRequest
对象作为参数用来调用服务的方法。从结构管理方面这样的做法对产品级的系统开发只有好处，没有坏处，如果有人认为这会导致开发上的不便，那么只能说
他的设计能力有待提高。

4. 在子模块的目录中分别创建相应的POM.xml
    1. 服务组件模块POM
        ``` 
        <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
            <modelVersion>4.0.0</modelVersion>
            <!-- 可以不必指定版本号和GroupId -->
            <artifactId>gp-svc-starter</artifactId>
        
            <packaging>jar</packaging>
        
            <!-- 指定parent pom及版本号 -->
            <parent>
                <groupId>com.starter</groupId>
                <artifactId>gp-starter</artifactId>
                <version>0.4.0</version>
            </parent>
        
            <properties>
        
            </properties>
        
            <dependencies>
                <!-- 指定GP框架依赖包：${gpress.version}由父pom提供 -->
                <dependency>
                    <groupId>com.groupress</groupId>
                    <artifactId>gp-common</artifactId>
                    <version>${gpress.version}</version>
                </dependency>
                <dependency>
                    <groupId>com.groupress</groupId>
                    <artifactId>gp-svc-base</artifactId>
                    <version>${gpress.version}</version>
                </dependency>
                <!-- 指定数据库驱动包：${mysql.version}由GP框架提供 -->
                <dependency>
                    <groupId>mysql</groupId>
                    <artifactId>mysql-connector-java</artifactId>
                    <version>${mysql.version}</version>
                </dependency>
                <dependency>
                    <groupId>com.github.ben-manes.caffeine</groupId>
                    <artifactId>caffeine</artifactId>
                    <version>${caffeine.version}</version>
                </dependency>
                <dependency>
                    <groupId>redis.clients</groupId>
                    <artifactId>jedis</artifactId>
                    <version>${jedis.version}</version>
                    <scope>provided</scope>
                </dependency>
                <dependency>
                    <groupId>junit</groupId>
                    <artifactId>junit</artifactId>
                    <version>${junit.version}</version>
                    <scope>test</scope>
                </dependency>
                <dependency>
                    <groupId>org.freemarker</groupId>
                    <artifactId>freemarker</artifactId>
                    <version>${freemarker.version}</version>
                    <scope>test</scope>
                </dependency>
                <dependency>
                    <groupId>ch.qos.logback</groupId>
                    <artifactId>logback-classic</artifactId>
                    <version>${logback.version}</version>
                    <scope>test</scope>
                </dependency>
            </dependencies>
            <build>
                <resources>
                    <resource>
                        <directory>src/test/java</directory>
                        <includes>                      
                            <include>**/*.ftl</include>
                        </includes>
                    </resource>
                </resources>
                <pluginManagement>
                    <plugins>
                    </plugins>
                </pluginManagement>
                <plugins>
                    <!-- 指定打包插件 -->
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-resources-plugin</artifactId>
                        <version>2.5</version>
                        <configuration>
                            <encoding>${project.build.sourceEncoding}</encoding>
                        </configuration>
                    </plugin>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-compiler-plugin</artifactId>
                        <version>3.8.1</version>
                        <configuration>
                            <source>${java.version}</source>
                            <target>${java.version}</target>
                            <encoding>${project.build.sourceEncoding}</encoding>
                        </configuration>
                    </plugin>
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-surefire-plugin</artifactId>
                        <version>2.10</version>
                        <configuration>
                            <includes>
                                <include>**/HScanTestxxx.java</include>
                            </includes>
                        </configuration>
                    </plugin>
                </plugins>
        
            </build>
        </project>
        ```
       
       2. API服务组件模块POM
``` 
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
	<modelVersion>4.0.0</modelVersion>

	<artifactId>gp.starter.api</artifactId>

	<packaging>jar</packaging>

	<parent>
		<groupId>com.starter</groupId>
		<artifactId>gp-starter</artifactId>
		<version>0.4.0</version>
	</parent>
	
	<properties>
		<start.main.class>com.gp.config.AppApiServer</start.main.class>
	</properties>
	<dependencies>
	    <!-- 指定服务层依赖 -->
		<dependency>
			<groupId>com.gpress.starter</groupId>
			<artifactId>gp-svc-starter</artifactId>
			<version>${project.version}</version>
		</dependency>
		<!-- 指定GP框架依赖包：${gpress.version}由父pom提供 -->
		<dependency>
			<groupId>com.groupress</groupId>
			<artifactId>gp-core</artifactId>
			<version>${gpress.version}</version>
		</dependency>
		......
		<dependency>
			<groupId>ch.qos.logback</groupId>
			<artifactId>logback-classic</artifactId>
			<version>${logback.version}</version>
		</dependency>
		<dependency>
			<groupId>junit</groupId>
			<artifactId>junit</artifactId>
			<version>${junit.version}</version>
			<scope>test</scope>
		</dependency>

    </dependencies>
	<build>
		<finalName>${project.artifactId}-${project.version}-${env}</finalName>
		<!-- 资源及参数过滤控制 -->
		<filters> <!-- specify env filter -->
			<filter>src/main/filters/env-${env}.properties</filter>
		</filters>
		<!-- 资源打包 -->
		<resources>
			<resource> <!-- specify resource path -->
				<directory>src/main/resources</directory>
				<filtering>true</filtering>
			</resource>
		</resources>
		<pluginManagement>
			<plugins>
			    <!-- 应用打包处理 -->
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
		</pluginManagement>
		<plugins>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-compiler-plugin</artifactId>
				<version>3.8.1</version>
				<configuration>
					<source>${java.version}</source>
					<target>${java.version}</target>
					<encoding>${project.build.sourceEncoding}</encoding>
				</configuration>
			</plugin>
			<plugin>
				<groupId>org.codehaus.mojo</groupId>
				<artifactId>versions-maven-plugin</artifactId>
				<version>${versions.maven-version}</version>
			</plugin>
			<!-- 应用打包插件：jar包合并 -->
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-shade-plugin</artifactId>
				<version>3.2.4</version>
				<configuration>
					<createDependencyReducedPom>false</createDependencyReducedPom>
				</configuration>
				<executions>
					<execution>
						<phase>package</phase>
						<goals>
							<goal>shade</goal>
						</goals>
						<configuration>
							<transformers>
								<transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer" />
							</transformers>
						</configuration>
					</execution>
				</executions>
			</plugin>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-surefire-plugin</artifactId>
				<version>2.14</version>
				<dependencies>
					<dependency>
						<groupId>org.apache.maven.surefire</groupId>
						<artifactId>surefire-junit47</artifactId>
						<version>2.14</version>
					</dependency>
				</dependencies>
			</plugin>
			<!-- 应用打包插件 -->
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-jar-plugin</artifactId>
				<version>3.2.0</version>
				<configuration>
					<archive>
						<manifest>
							<mainClass>${start.main.class}</mainClass>
						</manifest>
					</archive>
				</configuration>
			</plugin>
			<!-- 过滤中资源处理控制 -->
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-resources-plugin</artifactId>
				<version>3.2.0</version>
				<configuration>
					<encoding>UTF-8</encoding>
					<nonFilteredFileExtensions>
						<!-- ignore filtering file format -->
						<nonFilteredFileExtension>xlsx</nonFilteredFileExtension>
						<nonFilteredFileExtension>crt</nonFilteredFileExtension>
						<nonFilteredFileExtension>keystore</nonFilteredFileExtension>
						<nonFilteredFileExtension>truststore</nonFilteredFileExtension>
						<nonFilteredFileExtension>json</nonFilteredFileExtension>
					</nonFilteredFileExtensions>
				</configuration>
			</plugin>
		</plugins>
	</build>
	<!-- 为不同的环境准备的参数控制 -->
	<profiles>
		<!-- 开发环境 -->
		<profile>
			<id>dev</id>
			<properties>
				<env>dev</env>
			</properties>
			<activation>
				<activeByDefault>true</activeByDefault><!-- 默认配置 -->
			</activation>
		</profile>
		<!-- 生产环境 -->
		<profile>
			<id>prod</id>
			<properties>
				<env>prod</env>
			</properties>
		</profile>
	</profiles>
</project>
```

5. 在应用模块中准备filter信息，如：src/main/filters/env-dev.properties中可以配置开发环境参数