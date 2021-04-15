/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.base;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.KeyValuePair;
import com.gp.common.Operations;
import com.gp.common.ServiceContext;
import com.gp.dao.info.SysOptionInfo;
import com.gp.exception.BaseException;
import com.gp.info.DataBuilder;
import com.gp.pagination.PageQuery;
import com.gp.svc.SystemService;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;

import io.undertow.server.HttpServerExchange;

public class SysOptionHandler extends BaseApiSupport {

	private SystemService systemservice;
	
	public SysOptionHandler() {
		
		systemservice = BindScanner.instance().getBean(SystemService.class);
		
	}

	@WebApi(path="options-query")
	public void handleSysOptionsQuery(HttpServerExchange exchange)throws BaseException {
		
		// the model and view
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);
		svcctx.setOperation(Operations.OPT_FND);
		svcctx.addOperationPredicates(paramap);
		
		String group = Filters.filterAll(paramap, "group");
		String keywords = Filters.filterString(paramap, "keyword");
		PageQuery pquery = Filters.filterPageQuery(paramap);
		
		if(!Objects.isNull(pquery)) {
			if(pquery.getOrderBy().indexOf("group") > 0){
				pquery.setOrderBy("opt_group");
			}
			if(pquery.getOrderBy().indexOf("option") > 0){
				pquery.setOrderBy("opt_key");
			}
		}
		ActionResult result = null;

		svcctx.addOperationPredicates(paramap);
		String[] grp = new String[0];
		if(!Strings.isNullOrEmpty(group)) {
			Iterable<String> groups = Splitter.on(",").split(group);
			grp = Iterables.toArray(groups, String.class);
		}
		// query accounts information
		List<SysOptionInfo> opts = systemservice.getOptions( grp, keywords, pquery);
		List<Map<String, Object>> rows = opts.stream().map((opt)->{
			
			DataBuilder builder = new DataBuilder();
			
			builder.set("opt_id", opt.getId().toString());
			builder.set(opt, "opt_group", "opt_key", "opt_value", "description");
						
			return builder.build();
		}).collect(Collectors.toList());
		
		result = ActionResult.success(getMessage(exchange, "mesg.find.sysopts"));
		result.setData(pquery == null ? null : pquery.getPagination(), rows);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="option-save")
	public void handleSaveSystemOption(HttpServerExchange exchange)throws BaseException {
		
		ActionResult result = new ActionResult();
		
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.OPT_UPD);
		svcctx.addOperationPredicates(paramap);
		
		String optkey = Filters.filterString(paramap, "opt_key");
		String optvalue = Filters.filterString(paramap, "opt_value");
		
		svcctx.addOperationPredicates(KeyValuePair.newPair(optkey, optvalue));
		systemservice.updateOption(svcctx, optkey, optvalue);
		
		result = ActionResult.success(getMessage(exchange, "mesg.save.sysopt"));

		this.sendResult(exchange, result);
	}
	
	@WebApi(path="option-groups")
	public void handleGetSystemOptionGroups(HttpServerExchange exchange)throws BaseException{
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.sysopt.group"));
		
		this.getServiceContext(exchange, Operations.OPT_FND_GRP);
		
		List<String> grst = systemservice.getOptionGroups();
		List<Object> groups = grst.stream().map((group)->{
			String label = Character.toUpperCase(group.charAt(0)) + group.substring(1).toLowerCase();
			return KeyValuePair.newPair(group, label);
		}).collect(Collectors.toList());
	
		groups.add(KeyValuePair.newPair("ALL", "All"));
		
		result.setData(groups);

		this.sendResult(exchange, result);
	}
}
