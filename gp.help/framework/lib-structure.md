# 开发包结构详细说明

## gp-common开发包

common包是框架的基础包，该包用于组织管理框架中非业务的功能代码。该包重要的外部依赖有如下：
* gurava 项目没有采用常见apache-commons等，而是选择guava作为工具基础包
* jackson-databind 用于进行JSON数据处理
* mbassador 用于进行消息的pub-sub处理
* org.ow2.asm 用于进行Java的字节码处理
* byte-buddy-agent 用于进行字节码代码的JVM agent绑定

重要开发包说明：

    /com.gp
    |--/asm  JVMagent绑定代码
    |--/bean Bean处理代码，通过ASM动态生成Bean代理进行Bean的读写处理，取代反射机制的处理方式
    |--/bind Bean绑定处理代码，通过@BindComponent和@BindAutowired实现Bean的依赖注入处理，实现类似Spring的Bean绑定功能
    |--/cache 缓存处理实现代码
    |--/eventbus 消息总线处理代码
    |--/exec 线程执行相关的处理代码，如果执行时间采集等等
    |--/info Bean对象基本构建对象类等
    |--/launcher 应用启动处理代码，如启动生命期绑定事件等等
    |--/paging 分页处理实现代码
    |--/validate 参数校验实现代码


## gp-svc-base 开发包

gp-svc-base包主要封装数据库访问的基础开发类库包括DAO和Service基础类等。因为该包逻辑设计主要用于对
服务相关基础代码进行管理
* 数据库连接痴使用hikariCP包
* 数据库访问处理使用jdbi包
* OSS处理以minio为基本包
* MQ处理以rocketmq为基本包

重要开发包说明：

    /com.gp
    |--/common 定义基本的常量信息
    |--/dao 定义DAO相关的基础封装代码，包括有BaseDAO和ExtendDAO等
    |--/db 定义数据库事物处理代码，包括ASM的事物封装机制实现代码
    |--/filter 动态过滤组合条件处理代码
    |--/info 访问处理相关如何客户端访问信息等等
    |--/mq 消息处理代码
    |--/oss OSS处理代码
    |--/sql SQL语句DSL封装代码
    |--/svc 服务层封装代码
    |--/util 工具类代码

## gp-core 开发包

go-core包主要封装在light4j基础上的web相关处理代码。该模块中利用java的lambda机制
对API的相关处理逻辑进行分解处理。

重要开发包说明：

    /gp-core
    |--audit 审计相关代码，包括核心消息处理，同步消息处理，操作消息处理
    |--auth 认证相关封装代码
    |--core 应用核心处理引擎代码
    |--web Web的API服务基础封装代码

