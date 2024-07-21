# 应用启动过程说明

## 应用启动过程说明（com.gp.api模块为例）

1. 程序启动入口：com/gp/config/AppApiServer.main()
2. 进入后打印banner信息：showBanner()
3. 执行initialAgent()，在JVM中加入agent处理绑定，用来进行BeanBind的类依赖树分析
4. 启动light-4j服务器
   1. com.networknt.server.Server.init()初始化执行后，发出系统启动事件
   2. 服务启动事件触发 AppStartupHook.onStartup()执行
     ```
       Singleton service factory configuration/IoC injection
       singletons:
       # HandlerProvider implementation
       - com.networknt.handler.HandlerProvider:
          - com.gp.config.AppHandlerProvider
        
       # StartupHookProvider implementations, there are one to many and they are called in the same sequence defined.
       - com.networknt.server.StartupHookProvider:
         - com.gp.config.AppStartupHook
        
       # ShutdownHookProvider implementations, there are one to many and they are called in the same sequence defined.
       - com.networknt.server.ShutdownHookProvider:
          - com.gp.config.AppShutdownHook
        
          # MiddlewareHandler implementations, the calling sequence is as defined in the request/response chain.
          - com.networknt.handler.MiddlewareHandler:
           # Exception Global exception handler that needs to be called first to wrap all middleware handlers and business handlers
          - com.gp.web.ExceTrapHandler
     ```
   3. 加载CoreEngine类后，执行对SPI文件com.gp.launch.CoreInitializer的检查，加载全部的LifecycleListener监听器
     ```
     # META-INF/services/com.gp.launch.CoreInitializer
     com.gp.core.WebInitializer # the initializer for Web component
     
     // WebInitializer对象创建时将自动执行initial()方法
     ```
   4. 触发CoreEngine.startup()
      1. 根据优先级遍历全部LifecycleListener
      2. 执行LifeState.STARTUP关联的处理逻辑
         ```
          // WebInitializer中准备的启动逻辑代码
          public void initial() {

   	        this.bindLifeEvent(LifeState.STARTUP, (state) -> {
   		
   		       // 初始化数据源和连接池
   		       initialDataSource();
   		
   		       // 初始化DAO和Service Bean
   		       initialService();
   		
   		       // 初始化引擎代理
   		       CoreDelegate coreFacade = new CoreDelegate();
   		       CoreEngine.initial(coreFacade);

   	         });
          }
         ```
   5. CoreEngine.startup()启动过程完成
   
5. light-4j启动过程结束

## 应用停止过程

1. 服务启动事件触发 AppStartupHook.onShutdown()执行
2. 触发CoreEngine.shutdown()
   1. 根据优先级遍历全部LifecycleListener
   2. 执行LifeState.SHUTDOWN关联的处理逻辑
3. CoreEngine.shutdown()停止过程完成
4. light-4j停止过程结束

*其他*

* 如果想使用自己的banner数据，可以创建自己的banner放在resources目录中