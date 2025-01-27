/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.debug;

import com.gp.svc.dev.DebugService;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DevDebugHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(DevDebugHandler.class);

	DebugService debugService;

	public DevDebugHandler() {
		debugService = getBean(DebugService.class);
	}

	@WebApi(path="debug-test", open=true, method = "GET")
	public void handleUpdateTrace(HttpServerExchange exchange) throws Exception {

		LOGGER.debug("Update Trace Code");

		ActionResult result = ActionResult.success("success update trace code");

		this.sendResult(exchange, result);
	}

	@WebApi(path="debug-demo", open=true, method = "GET")
	public void handleDemo(HttpServerExchange exchange) throws Exception {

		LOGGER.debug("Update Trace Code");

		ActionResult result = ActionResult.success("success update trace code");

		abort(exchange, "excp.unexist", "用户信息");

		this.sendResult(exchange, result);
	}

}
