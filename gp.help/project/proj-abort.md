# 事件的捕捉和处理

GP框架采用Checked Exception进行中断处理，在Service或Api声明中，根据实际请假直接抛出Exception即终止程序代码。

## Exception分类

``` 
   / RawException               基本异常接口类，用于规范方法
   |-- / AbnormalException      非检查类异常
   |-- / BaseException          基本异常
       |-- / ServiceException   服务异常
       |-- / CoreException      核心异常
       |-- / SyncExcption       同步异常
       |-- / WebExceptin        API异常
```

## 异常的使用

### 在服务可以使用如下方式

``` 
	@JdbiTran
	public String demoMeth(ServiceContext context, String username, String password) throws ServiceException {

		List<UserInfo> list =  userdao.query(cond -> {
			cond.and("username = '" + username +"'");
		});
		UserInfo uinfo = Iterables.getFirst(list, null);

        // 发现异常，直接进行中断
		if (null == uinfo) {
			abort("excp.unexist", "用户信息");
		}

		// 在开发调试的情况，以下代码抛出异常，导致事务回滚，方便开发调试
		assertDebug(context);
		
		return null;

	}
```

### API中使用异常

``` 
	@WebApi(path="debug-demo", open=true)
	public void handleDemo(HttpServerExchange exchange) throws Exception {

		LOGGER.debug("Update Trace Code");

		ActionResult result = ActionResult.success("success update trace code");

		abort(exchange, "excp.unexist", "用户信息");
		
		this.sendResult(exchange, result);
	}
```