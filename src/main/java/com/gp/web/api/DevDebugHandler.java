/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.KeyValuePair;
import com.gp.common.NodeIdKey;
import com.gp.common.ServiceContext;
import com.gp.common.Synchronizes.SyncOrigin;
import com.gp.core.NodeApiAgent;
import com.gp.exception.BaseException;
import com.gp.svc.dev.DemoService;
import com.gp.util.JsonUtils;
import com.gp.validate.ArgsValidator;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import com.gp.web.sse.EventSourceManager;
import com.gp.web.util.WebUtils;

import io.undertow.server.HttpServerExchange;

public class DevDebugHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(DevDebugHandler.class);
	
	private DemoService demoService;
	
	public DevDebugHandler() {
		demoService = BindScanner.instance().getBean(DemoService.class);
		
		//this.setPathMapping(WebUtils.getOpenApiUri("sync-debug"), Methods.GET, this::handleSyncDebug);
		//this.setPathMapping(WebUtils.getAuthApiUri("demo-calc"), Methods.POST, this::handleDemoCalc);
		//this.setPathMapping(WebUtils.getOpenApiUri("demo-text"), Methods.GET, this::handleDemoText);
		//this.setPathMapping(WebUtils.getOpenApiUri("test-audit"), Methods.GET, this::handleAuditDebug);
		//this.setPathMapping(WebUtils.getOpenApiUri("test-excep"), Methods.GET, this::handleExcepDebug);
		
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
	
	@WebApi(path="sync-debug", method="GET", open=true)
	public void handleSyncDebug(HttpServerExchange exchange) throws Exception {
		
		LOGGER.debug("Test sending post sync origns");
		SyncOrigin[] originAry = new SyncOrigin[] {
			SyncOrigin.GLOBAL
		};
		
		String postData = JsonUtils.toJson(originAry);
		NodeApiAgent.instance().sendSyncPost(WebUtils.getAuthApiUri("sync-trigger"), postData);
		
		ActionResult result = ActionResult.success("invoke sync trigger");
		
		this.sendResult(exchange, result);
	}

	@WebApi(path="trans-debug", method="GET", open=true)
	public void handleTransDebug(HttpServerExchange exchange) throws Exception {
		demoService.testTrans();
	}
	
	@WebApi(path="trans1-debug", method="GET", open=true)
	public void handleTrans1Debug(HttpServerExchange exchange) throws Exception {
		demoService.testTrans1();
	}
	
	@WebApi(path="trans2-debug", method="GET", open=true)
	public void handleTrans2Debug(HttpServerExchange exchange) throws Exception {
		demoService.testTransWithReadOnly(false);
	}
	
	@WebApi(path="trans3-debug", method="GET", open=true)
	public void handleTrans3Debug(HttpServerExchange exchange) throws Exception {
		demoService.testTransWithReadOnly(true);
	}
	
	@WebApi(path="test-audit", operation="adr:new", open=true)
	public void handleAuditDebug(HttpServerExchange exchange) throws Exception {
		
		ActionResult result = ActionResult.success("invoke audtit test");
		ServiceContext svcctx = this.getServiceContext(exchange);
		Map<String, Object> params = this.getRequestBody(exchange);
		svcctx.addOperationPredicates(params);
		
		this.sendResult(exchange, result);
	}
	

	@WebApi(path="test-excep", operation="adr:new", open=true)
	public void handleExcepDebug(HttpServerExchange exchange) throws Exception {
		
		ServiceContext svcctx = this.getServiceContext(exchange);
		Map<String, Object> params = this.getRequestBody(exchange);
		svcctx.addOperationPredicates(params);
		BaseException de = new BaseException("demo excep");
		
		Map d = Maps.newHashMap();
		d.put("sk", "ss3");
		d.put("sk1", "222");
		
		de.setExtra(d);
		
		throw de;
	}

	@WebApi(path="demo-calc", operation="adr:new")
	public void handleDemoCalc(HttpServerExchange exchange) throws Exception {
	
		Map<String, Object> paramap = this.getRequestBody(exchange);
		
		ArgsValidator.newValidator(paramap)
			.require("var").validate(true);
		
		ServiceContext svcctx = this.getServiceContext(exchange);
		svcctx.addOperationPredicates(paramap);
		
		int var = Filters.filterInt(paramap, "var");
		
		ActionResult result = null;
	
		int i = demoService.demoCalc(var);
					
		result = ActionResult.success("this is ok message");
		result.setData(i);
	
		this.sendResult(exchange, result);
	}


	@WebApi(path="demo-text", operation="adr:new", open=true)
	public void handleDemoText(HttpServerExchange exchange) throws Exception {
		
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);
		svcctx.addOperationPredicates(paramap);
		
		String var = Filters.filterString(paramap, "var");
		var = var == null ? "dft": var;
		ActionResult result = null;
	
		String itxt = demoService.demoText(var);
					
		result = ActionResult.success("this is ok message");
		result.setData(itxt);
	
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="demo-suggests", operation="ukn:op", open=true)
	public void handleDemoSuggests(HttpServerExchange exchange) throws Exception {
		
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);
		svcctx.addOperationPredicates(paramap);
		
		String query = Filters.filterString(paramap, "query");
		ActionResult result = null;
	
		List<KeyValuePair<String, String>> data = Lists.newArrayList();
		query = Strings.isNullOrEmpty(query) ? "uname-" : query + "-";
		
		for(int i = 0; i< 8; i++) {
			KeyValuePair<String, String> item = KeyValuePair.newPair("uid_"+i, query+i);
			data.add(item);
		}
		
		result = ActionResult.success("this is ok message");
		if(query.length() > 20) {
			data.clear();
		}
		result.setData(data);
	
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="demo-args-valid", operation="adr:new", open=true, method="GET")
	public void handleDemoArgsValid(HttpServerExchange exchange) throws Exception {
		
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);
		svcctx.addOperationPredicates(paramap);
		
		ArgsValidator.newValidator(paramap)
			.require("var", "var1")
			.range(3, 4, "intv1")
			.validate(true);
		
		String var = Filters.filterString(paramap, "var");
		var = var == null ? "dft": var;
		ActionResult result = null;
	
		String itxt = demoService.demoText(var);
					
		result = ActionResult.success("this is ok message");
		result.setData(itxt);
	
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="demo-param", operation="adr:new", open=true, method="GET")
	public void handleDemoParam(HttpServerExchange exchange) throws Exception {
		
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);
		svcctx.addOperationPredicates(paramap);
		
		ActionResult result = null;
	
		InfoId pkey = IdKeys.newInfoId(NodeIdKey.BAN_WORD);
		demoService.testInfoIdParam(pkey);
					
		result = ActionResult.success("this is ok message");
			
		this.sendResult(exchange, result);
	}
}
