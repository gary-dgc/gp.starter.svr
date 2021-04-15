/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.GeneralConfig;
import com.gp.core.NodeApiAgent;
import com.gp.dao.info.SourceInfo;
import com.gp.svc.master.SourceService;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import com.gp.web.util.WebUtils;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Methods;

public class SystemHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(SystemHandler.class);
	
	private SourceService sourceService;
		
	public SystemHandler() {
		
		sourceService = BindScanner.instance().getBean(SourceService.class);
		
		this.setPathMapping(WebUtils.getOpenApiUri("system-info"), Methods.POST, this::handelSystemInfo);
		this.setPathMapping(WebUtils.getAuthApiUri("system-ping"), Methods.POST, this::handleSystemPing);
	}

	
	/**
	 * Detect the system information, it's used when 
	 * GUI Admin console page start, tip for connect to Global server.
	 *  
	 **/
	@WebApi(path="system-info", open=true)
	public void handelSystemInfo (HttpServerExchange exchange) throws Exception {
		
		SourceInfo local = sourceService.getLocalSource();
				
		Map<String, Object> data = Maps.newHashMap();
		data.put("system", GeneralConfig.getStringByKeys("system", "app"));
		data.put("instance", GeneralConfig.getStringByKeys("system", "instance"));
		data.put("version", GeneralConfig.getStringByKeys("system", "version"));
		data.put("state", local.getState());
		
		ActionResult result = ActionResult.success("Success get the system information");
		result.setData(data);
		
		this.sendResult(exchange, result);
		
	}
	
	/**
	 * Detect the system information, it's used when 
	 * GUI Admin console page start, tip for connect to Global server.
	 *  
	 **/
	@WebApi(path="system-ping", open=true)
	public void handleSystemPing (HttpServerExchange exchange) throws Exception {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		
		String node = Filters.filterString(params, "node");
		ActionResult result = ActionResult.failure("Fail ping target server: "+ node);
		
		if(Objects.equal(node, "sync")) {
			try {
				ActionResult _result = NodeApiAgent.instance().sendSyncGet("/opapi/ping", null);
				
				result.setMeta(_result.getMeta());
				result.setData(_result.getData());
			}catch(Exception ce) {
				
				result = ActionResult.failure("Sync node server is not available");
			}
			
		} else if(Objects.equal(node, "convert")) {
			try {
				ActionResult _result = NodeApiAgent.instance().sendConvertGet("/opapi/ping", null);
				
				result.setMeta(_result.getMeta());
				result.setData(_result.getData());
			}catch(Exception ce) {
				
				result = ActionResult.failure("convert node server is not available");
			}
		}
		
		this.sendResult(exchange, result);
		
	}
}
