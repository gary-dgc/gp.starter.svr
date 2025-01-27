/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.gp.common.Filters;
import com.gp.common.GeneralConfig;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import com.gp.web.util.WebUtils;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SystemHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(SystemHandler.class);

		
	public SystemHandler() {

		this.setPathMapping(WebUtils.getOpenApiUri("system-info"), Methods.POST, this::handelSystemInfo);

	}

	
	/**
	 * Detect the system information, it's used when 
	 * GUI Admin console page start, tip for connect to Global server.
	 *  
	 **/
	@WebApi(path="system-info", open=true)
	public void handelSystemInfo (HttpServerExchange exchange) throws Exception {

		Map<String, Object> data = Maps.newHashMap();
		data.put("system", GeneralConfig.getStringByKeys("system", "app"));
		data.put("instance", GeneralConfig.getStringByKeys("system", "instance"));
		data.put("version", GeneralConfig.getStringByKeys("system", "version"));

		ActionResult result = ActionResult.success("Success get the system information");
		result.setData(data);
		
		this.sendResult(exchange, result);
		
	}

}
