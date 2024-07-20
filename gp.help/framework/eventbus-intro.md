# 异步事件的处理机制及设计实现

在应用框架中，消息机制是框架的重要组成部分，一方面有时应用内部的各个组件之间需要借助消息实现协同，另一方面应用的某些业务相关的消息
也需要对其进行针对性的处理，所以在消息处理是十分必要的。GP框架中，使用mbassador库构建消息总线，提供消息的pub-sub处理。
利用消息一个显而易见的好处是可以通过消息实现异步的处理。

## 消息总线的操作

``` 
// 声明监听器
DemoListener coreHandler = new DemoListener();

// 注册监听器到总线实例中
EventDispatcher.instance().register(coreHandler);

// 发布消息到总线中
EventPayload event = new EventPaylad();
EventDispatcher.instance().sendPayload(event);
```

### 消息总线监听

```
// 基于EventListener扩展定制的监听器
public class DemoListener extends EventListener{

    /**
     * Instantiates a new Core audit listener.
     */
    public DemoListener() {
		super(EventType.AUDIT);
	}
	
	@Override
	public boolean supportEvent(EventPayload payload) {
		
		// 指定消息匹配检查
		if(( !EventType.CORE.equals(payload.getEventType()) && !EventType.AUDIT.equals(payload.getEventType()))
				|| !(payload instanceof CoreEventload)) {
			return false;
		}
		
		return true;
	}
	
	@Override
	public void process(EventPayload payload) throws BaseException {

		CoreEventload coreload = (CoreEventload) payload;

        // 消息处理逻辑代码
		...
	
	}
}

```