# 应用启动器介绍

服务端类型应用的启动过程是一系列动作的就绪准备过程，为更好的对这个过程进行管理，GP框架构建了应用启动内核的基本结构。
这个启动结构包括启动周期状态和状态监听器，状态监听器的注册利用java的SPI机制进行注册管理。

# 启动内核

## 启动状态及启动状态监听：

* 启动状态包括：

        /**
         * Unknown life state.
         */
        UNKNOWN,
        /**
           * Startup life state.
           */
        STARTUP,
        /**
           * Shutdown life state.
           */
        SHUTDOWN

* 启动状态监听器LifecycleListener，用来装配不同状态下的执行代码

* 生命周期执行器Lifecycle

提供对状态监听器的绑定注册和取消注册、事件驱动功能。

## 应用启动引擎及启动状态初始化绑定代码

### 初始化类CoreInitializer

初始化类支持SPI方式的注册，在应用中可以通过/META-INFO/services/com.gp.launcher.CoreInitializer中进行相关初始化类
的绑定。该类相当于开放了一个定制化代码的入口，应用通过重载该类的initial()方法，将应用的不同状态的相关处理代码进行与启动状态
的关联。

```
// 启动绑定代码
public class WebInitializer extends CoreInitializer{

	public WebInitializer() throws BaseException {
		super("WebInitializer", 10);// 指定名称，执行顺序
	}

	@Override
	public void initial() {
		
		// 绑定启动事件
		this.bindLifeEvent(LifeState.STARTUP, (state) -> {
			
			LOGGER.debug("Start: setup caches");
		});
		
		// 绑定停止事件
		this.bindLifeEvent(LifeState.SHUTDOWN, (state) -> {
			
			LOGGER.debug("stop: clear caches");
		});
	}
}

// 上述类的实例可以获得LifecycleListener实例

WebInitializer winit = ....;

LifecycleListener listener = winit.getLifecycleHooker();
```

### CoreEngine用于核心启动引擎类

该类是单实例对象，在内部维护一个Lifecycle对象。在加载完成后自动对/META-INFO/services/com.gp.launcher.CoreInitializer中
指定的CoreInitializer实例进行注册处理，处理过程说明如下：

1. 以静态方式在类加载后执行setup函数
2. 依次加载com.gp.launcher.CoreInitializer的初始化对象
   1. 遍历CoreInitializer的初始化对象
   2. 从每个对象中获得LifecycleListener（getLifecycleHooker()）
   3. 将LifecycleListener监听器注册到CoreEngine的Lifecycle对象中
3. CoreEngine完成启动准备，等待启动信号。

## 应用系统的启动方式

GP框架下提供两种启动方式：被动启动方式、主动启动方式。

### 被动启动方式（light-4j）

被动启动方式，是指在基于完整应用框架的基础上（如：springboot，light-4j）构建的启动过程，这种启动方式主要是利用既有底层框架
的启动事件，通过启停事件分别对应CoreEngine的startup/shutdown方法，完成GP框架的启动。

```
// 关联启动处理
public class AppStartupHook implements StartupHookProvider{

	@Override
	public void onStartup() {

		CoreEngine.startup();
	}
}

// 关联停止处理
public class AppShutdownHook implements ShutdownHookProvider{

	@Override
	public void onShutdown() {
		
		CoreEngine.shutdown();
	}

}
```

### 主动启动方式（WebMiniServer / Webdav Server）

主动启动方式指先通过CoreEngine的startup/shutdown方法，依次执行lifecycle listener的执行代码。这些执行代码包括绑定端口，
初始化数据库连接等处理步骤。

* 主动式启动代码

``` 
public class ConvertServer extends AppRunner {

	private static Logger LOGGER = LoggerFactory.getLogger(ConvertServer.class);
	
    /**
	 * 程序主入口
	 **/
	public static void main(String[] args) {
		
		// parse the core arguments
		CoreArgument coreArg = readArgument(args);
		if(coreArg.isBlind()) {
			coreArg.setFlag(FLAG_HELP);
		}
		initial(args);
		CoreLauncher launcher = ConvertLauncher.instance();
		boolean daemonOn = launcher.tryDaemonOn();
		
		if(coreArg.isCommand(FLAG_START) && daemonOn) {
		
			String resp = launcher.tryPerform(coreArg);
			
			LOGGER.info(resp);
			
		}else if(coreArg.isCommand(FLAG_STOP)) {
			
			String resp = launcher.tryPerform(coreArg);
			LOGGER.info(resp);
			
		}else if(coreArg.isCommand(FLAG_HELP)) {
			
			String resp = launcher.tryPerform(coreArg);
			LOGGER.info(resp);
		}
   
    }

    // 解析命令行参数
	private static CoreArgument readArgument(String[] args) {
		
		CoreArgument coreArg = null;
		
		List<CoreArgument> coreArgs;
		try {
			coreArgs = new OptionParser().parseArguments(args);
			
			if(coreArgs.size() == 0) {
				// no args default is START command
				coreArg = new CoreArgument(FLAG_START);
			} else {
				// return the first command
				coreArg = coreArgs.get(0);
			}
		} catch (BaseException e) {
			// exception default a help command
			coreArg = new CoreArgument(FLAG_HELP);
		}
		
		return coreArg;
	}
	
	// 初始化命令行参数
	private static void initial(String[] args) {
		CoreLauncher launcher = ConvertLauncher.instance();
				
		// success start prepare the START & STOP handler
		launcher.registerCommand(FLAG_START, (CoreArgument arg)->{
			if(arg.isDaemon()) {
				
				return "GPress Convert Server already started";
			}else {

				try {
					ConvertServer.initialAgent();
				} catch (Exception e) {
					// ignore;
				}

				launcher.engineOn();
				return  "GPress Convert Server start" ;
			}
		});

		launcher.registerCommand(FLAG_STOP, (CoreArgument arg) -> {
			if(arg.isDaemon()) {
				
				launcher.engineOff();
		
				return "GPress Convert Server stop" ;
			}else {
				
				return "GPress Convert Server not start yet";
			}
		});
	}
}

```

* 守护模式应用启动的处理

```
public class ConvertLauncher extends CoreLauncher{

	static Logger LOGGER = LoggerFactory.getLogger(ConvertLauncher.class);

	private static ConvertLauncher instance;
	
	public static final String FLAG_START = OptionParser.FLAG_START;
	public static final String FLAG_STOP  = "stop";
	public static final String FLAG_HELP  = "help";
	
	/**
	 * Hidden default constructor 
	 **/
	private ConvertLauncher() {
		this.initialize();	
	}
	
	/**
	 * Get the singleton instance 
	 **/
	public static ConvertLauncher instance() {
		
		if(null == instance) {
			instance = new ConvertLauncher();
		}
		
		return instance;
	}
	
	@Override
	public void initialize() {
		super.initialize();
		this.registerCommand(FLAG_HELP, (CoreArgument arg) -> {
			return "this is cmd: "+ arg;
		});
	}
	
	@Override
	public void engineOn() {
		LOGGER.debug("ContextInitFinishListener:CoreStarter starting");
	
		CoreEngine.startup();
		LOGGER.debug("CoreEngine startup");
		
	}

	@Override
	public void engineOff() {
		LOGGER.debug("ServletContextListener:CoreStarter destroying");
	
		CoreEngine.shutdown();
		LOGGER.debug("CoreEngine shutdown");
		
	}

} 
```