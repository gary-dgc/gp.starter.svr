/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.master;

import com.gp.bind.BindScanner;
import com.gp.common.*;
import com.gp.dao.info.EndpointInfo;
import com.gp.exception.BaseException;
import com.gp.info.DataBuilder;
import com.gp.info.InfoCopier;
import com.gp.info.Principal;
import com.gp.paging.PageQuery;
import com.gp.svc.master.EndpointService;
import com.gp.validate.ArgsValidator;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class EndpointHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(EndpointHandler.class);

	private EndpointService endpointService;
	
	public EndpointHandler() {
		endpointService = BindScanner.instance().getBean(EndpointService.class);
		
	}

	@WebApi(path="endpoints-query")
	public void handleEndpointsQuery(HttpServerExchange exchange) {
		
		ActionResult result = ActionResult.success("mesg.find.endpoints");
		ServiceContext svcctx = this.getServiceContext(exchange);
		
		Map<String, Object> params = this.getRequestBody(exchange);
		String module = Filters.filterString(params, "module");
		String name = Filters.filterString(params, "name");
		
		PageQuery pquery = Filters.filterPageQuery(params);
		svcctx.addOperationPredicates(params);
		
		List<EndpointInfo> infos = endpointService.getEndpoints(module, name, pquery);
		
		List<Object> data = infos.stream().map((info) -> {
			
			DataBuilder builder = new DataBuilder();
			builder.set("endpoint_id", info.getId().toString());
			
			builder.set(info, "endpoint_name", "module", "type", "endpoint_abbr", "access_path");
			builder.set(info, "description");
						
			return builder.build();
			
		}).collect(Collectors.toList());
		
		result.setData(pquery == null ? null : pquery.getPagination(), data);
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="endpoint-add")
	public void handleEndpointAdd(HttpServerExchange exchange) throws BaseException {
		
		ActionResult result = null;
		ServiceContext svcctx = this.getServiceContext(exchange);
		Principal principal = this.getPrincipal(exchange);
		Map<String, Object> params = this.getRequestBody(exchange);
		
		EndpointInfo info = new EndpointInfo();
		InfoCopier.copyToInfo(params, info);

		endpointService.addEndpoint(svcctx, info);

		result = ActionResult.success(getMessage(exchange, "mesg.new.endpoint"));

		this.sendResult(exchange, result);
	}
	
	@WebApi(path="endpoint-save")
	public void handleEndpointSave(HttpServerExchange exchange) throws BaseException {
		ActionResult result = null;
		Map<String, Object> params = this.getRequestBody(exchange);
		ArgsValidator.newValidator(params)
			.requireId("endpoint_id")
			.validate(true);
		
		ServiceContext svcctx = this.getServiceContext(exchange);
		
		Long endpointId = Filters.filterLong(params, "endpoint_id");
		EndpointInfo info = new EndpointInfo();
		InfoCopier.copyToInfo(params, info);
		
		InfoId epKey = IdKeys.getInfoId(AppIdKey.ENDPOINT, endpointId);
		info.setInfoId(epKey);
		
		endpointService.updateEndpoint(svcctx, info);

		result = ActionResult.success(getMessage(exchange, "mesg.save.endpoint"));

		this.sendResult(exchange, result);
	}
	
	@WebApi(path="endpoint-remove")
	public void handleEndpointRemove(HttpServerExchange exchange) throws BaseException {
		ActionResult result = null;
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ArgsValidator.newValidator(params)
			.requireId("endpoint_id")
			.validate(true);
		
		Long endpointId = Filters.filterLong(params, "endpoint_id");
		InfoId epKey = IdKeys.getInfoId(AppIdKey.ENDPOINT, endpointId);
	
		endpointService.removeEndpoint(epKey);

		result = ActionResult.success(getMessage(exchange, "mesg.remove.endpoint"));

		this.sendResult(exchange, result);
	}
}
