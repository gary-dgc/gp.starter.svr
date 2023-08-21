/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api;

import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.svc.dev.DebugService;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import com.gp.web.sse.EventSourceManager;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

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
