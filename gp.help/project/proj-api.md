# 如何实现接口服务

服务接口的开发设计中需要考虑以下几点：

1. 确定请求是否需要进行鉴权处理

2. 确定API请求的方法：GET，POST，PUT等等

3. 确定API参数的校验规则和要求

4. 确定ApI关联的业务，明确Handler类中需要完成哪些API的声明定义

## API声明处理

``` 

public class DevDebugHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(DevDebugHandler.class);

	private DebugService demoService;

	public DevDebugHandler() {
		demoService = BindScanner.instance().getBean(DebugService.class);

	}

	@WebApi(path="sse-debug", open=true)
	public void handleServerSendDebug(HttpServerExchange exchange) throws Exception {
		
		LOGGER.debug("Test server send event ");
		
		ActionResult result = ActionResult.success("invoke sse trigger");
			
		Map<String, Object> paramap = this.getRequestBody(exchange);
		String message = Filters.filterString(paramap, "message");
		
		EventSourceManager.instance().broadcast(message);
		//EventSourceManager.instance().broadcast(message, "open");
		this.sendResult(exchange, result);
	}

}
```