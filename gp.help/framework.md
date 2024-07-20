# Groupress Framework 

之前提到过GP框架目的在于构建系统产品的使用场景，而不是针对常见的管理后台型应用，因为这类应用通过常见的框架即可实现。
而GP框架更多是关注于如何灵活清晰地构建复杂的逻辑功能，比如财务系统的月结，库存的收，发货等长流程的业务。因此框架尽量在纵向层级
保持清晰，总体上框架由应用（Application）-接口（Interface）-服务（Service）三部分构成。

## 应用核心

应用核心主要负责应用的启动控制处理和认证机制实现，以及GP框架核心功能（内部消息分发）的处理代码，和Bean绑定机制实现等。
其代码主要包含在gp-core和gp-common包中

## 应用接口

应用接口主要负责对请求的分发处理，框架采用异步IO进行请求处理，用以获得更好的并发性能。此部分代码主要在
gp-core中。

## 应用服务

应用服务主要指传统开发中的服务层和DAO层，在逻辑层面DAO可以看作表，基于表上构建一系列的服务层对象。框架在服务层设计
上提出了ActionService和LinkerService概念，用于对离散的服务实现提供更加灵活的业务构建。

# 开发包说明

## 框架采用POM的多模块方式实现，其中POM结构如下：

    /gp-root             // 顶层包定义
    |--/gp-core          // Web核心处理
        |--/gp-svc-base  // 服务层Service处理
            |--gp-common // 通用功能代码

    |--/gp-jgroups       // 应用间同步消息处理
        |--/gp-svc-base  
        |--gp-common

    |--/gp-netty         // Netty实现的嵌入微服务器，以及Webdav服务器
        |--/gp-core      
        |--/gp-svc-base  
            |--gp-common

    |--/gp-sync-base      // 应用模块间同步处理 
        |--/gp-core      
        |--/gp-svc-base  
            |--gp-common


## 外部依赖说明（gp-root.pom中声明全部依赖版本信息）

### 项目编译环境参数如下：JDK11
    
    <maven.compiler.source>11</maven.compiler.source>
    <maven.compiler.target>11</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <java.version>11</java.version>

### 其他依赖包版本信息

    <slf4j.version>1.7.25</slf4j.version>
    <log4j.version>2.11.1</log4j.version>
    <logback.version>1.5.6</logback.version>

    <guava.version>32.0.0-jre</guava.version>
    <disruptor.version>3.4.2</disruptor.version>
    <jackson.version>2.13.4.1</jackson.version>
    <jackson.dataformat.version>2.13.4</jackson.dataformat.version>
    <mbassador.version>1.3.2</mbassador.version>
    <snakeyaml.version>2.0</snakeyaml.version>
    <asm.version>8.0.1</asm.version>
    <buddy.agent.version>1.11.20</buddy.agent.version>

    <mysql.version>8.0.28</mysql.version>
    <postgresql.version>42.6.1</postgresql.version>
    <hikaricp.version>4.0.3</hikaricp.version>

    <jedis.version>2.9.0</jedis.version>
    <arsick.version>0.4.0</arsick.version>
    <version.jdbi>3.45.2</version.jdbi>
    <jose4j.version>0.9.6</jose4j.version>
    <caffeine.version>2.6.2</caffeine.version>

    <glassfish.version>3.0.1-b08</glassfish.version>
    <aspectj.version>1.9.5</aspectj.version>
    <redission.version>3.17.6</redission.version>
    <minio.version>7.1.0</minio.version>
    <rocketmq.version>4.9.2</rocketmq.version>

    <light-4j.version>2.1.34</light-4j.version>
    <jstl.version>1.2</jstl.version>
    <standard.version>1.1.2</standard.version>
    <jgroup.version>4.1.1.Final</jgroup.version>
    <netty.version>4.1.108.Final</netty.version>
    <undertow.version>2.3.15.Final</undertow.version>
    <junit.version>4.13.2</junit.version>

    <freemarker.version>2.3.30</freemarker.version>
    <version.json-schema-validator>1.0.29</version.json-schema-validator>

