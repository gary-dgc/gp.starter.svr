/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.global;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gp.bind.BindScanner;
import com.gp.core.CoreConstants;
import com.gp.core.NodeApiAgent;
import com.gp.exception.BaseException;
import com.gp.svc.CommonService;
import com.gp.svc.SystemService;
import com.gp.svc.user.UserService;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import io.undertow.server.HttpServerExchange;

public class ChannelApiHandler extends BaseApiSupport {

	static Logger LOGGER = LoggerFactory.getLogger(ChannelApiHandler.class);

	UserService userService;
	
	SystemService systemService;
	
	CommonService commonService;
	
	public ChannelApiHandler() {
		userService = BindScanner.instance().getBean(UserService.class);
		systemService = BindScanner.instance().getBean(SystemService.class);
		commonService = BindScanner.instance().getBean(CommonService.class);
		
	}
	
	@WebApi(path="channel-global-filter", operation="gbl:ivk")
	public void handleChannelsQuery(HttpServerExchange exchange) throws BaseException{
	
		Map<String, Object> data = this.getRequestBody(exchange);
		String nodeGid = systemService.getLocalNodeGid();
		data.put("node_gid", nodeGid);
		ActionResult result = NodeApiAgent.instance().sendGlobalPost( CoreConstants.OPEN_API_PATH + "/channels-query", data);
		
		this.sendResult(exchange, result);
	}
}
