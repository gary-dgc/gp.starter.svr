/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.master;

import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.gp.bind.BindScanner;
import com.gp.common.*;
import com.gp.common.Sources.State;
import com.gp.dao.info.SourceInfo;
import com.gp.dao.info.SysOptionInfo;
import com.gp.exception.BaseException;
import com.gp.info.BaseIdKey;
import com.gp.info.DataBuilder;
import com.gp.info.Principal;
import com.gp.paging.PageQuery;
import com.gp.svc.SystemService;
import com.gp.svc.master.SourceService;
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

public class SourceHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(SourceHandler.class);
	
	private SourceService sourceService;
	private SystemService systemService;
	
	public SourceHandler() {
		sourceService = BindScanner.instance().getBean(SourceService.class);
		systemService = BindScanner.instance().getBean(SystemService.class);
		
	}

	@WebApi(path="source-info")
	public void handleGetInstance(HttpServerExchange exchange)throws BaseException {
		
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);
		svcctx.setOperation(Operations.SRC_INF);
		svcctx.addOperationPredicates(paramap);
		ArgsValidator.newValidator(paramap)
				.require("source_id")
				.validate(true);

		Long instanceId = Filters.filterLong(paramap, "source_id");
		
		ActionResult result = null;

		if(!GeneralConsts.LOCAL_SOURCE.equals(instanceId) && instanceId <= 0){
		
			result = ActionResult.error("parameter [source_id] is not valid.");
			this.sendResult(exchange, result);
			return;
		}

		InfoId id = IdKeys.getInfoId(BaseIdKey.SOURCE, instanceId);
		svcctx.setOperationObject(id);
		
		List<SysOptionInfo> opts = systemService.getOptions(new String[] {"NETWORK"}, null, null);
		
		SourceInfo instinfo = sourceService.getSource( id);
		
		DataBuilder data = new DataBuilder();
		data.set("source_id", instinfo.getId().toString());
		data.set(instinfo, "abbr", "description", "email", "state", 
				"entity_gid", "entity_name", "node_gid", "short_name", "source_name");
		
		data.set("admin", admin-> {
			admin.set(instinfo, "username", "full_name", "email", "avatar_url");
			admin.set("user_id", instinfo.getAdminUid().toString());
		});
		
		for(SysOptionInfo opt : opts) {
			switch(opt.getOptKey()) {
				case "public.access":
					data.set("public_url", opt.getOptValue());
					break;
				case "file.access":
					data.set("binary_url", opt.getOptValue());
					break;
				case "service.access":
					data.set("service_url", opt.getOptValue());
					break;
				default:
					break;
			}
		}
				
		result = ActionResult.success(getMessage(exchange, "mesg.find.instance"));
		result.setData(data);
	
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="source-change-state", operation = "src:upd")
	public void handleChangeSourceState(HttpServerExchange exchange)throws BaseException {

		ActionResult result = new ActionResult();
		Map<String,Object> paramap = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.SRC_UPD);
		svcctx.addOperationPredicates(paramap);
		
		Long instanceId = Filters.filterLong(paramap, "source_id");
		String stateStr = Filters.filterAll(paramap, "source_state");
		if(!GeneralConsts.LOCAL_SOURCE.equals(instanceId) && instanceId <= 0){
			
			result = ActionResult.error("parameter [source_id] is not valid.");
			this.sendResult(exchange, result);
			return;
		}

		InfoId id = IdKeys.getInfoId(BaseIdKey.SOURCE, instanceId);
		svcctx.setOperationObject(id);
		svcctx.addOperationPredicates(paramap);
			
		sourceService.changeSourceState(svcctx, id, State.valueOf(stateStr));
		
		result = ActionResult.success(getMessage(exchange, "mesg.change.instance.state"));
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="source-save")
	public void handleSaveInstance( HttpServerExchange exchange )throws BaseException{

		ActionResult result = new ActionResult();
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.SRC_UPD);
		// read request parameters
		Map<String, Object> params = this.getRequestBody(exchange);
	
		InfoId id = IdKeys.getInfoId(BaseIdKey.SOURCE, Filters.filterLong(params, "source_id"));
		
		Principal princ = this.getPrincipal(exchange);
					
		SourceInfo instinfo = new SourceInfo();
		instinfo.setInfoId(id);
		instinfo.setAbbr(Filters.filterString(params, "abbr"));
		instinfo.setAdminUid(Filters.filterLong(params, "admin_uid"));
		instinfo.setDescription(Filters.filterString(params, "description"));
		instinfo.setEmail(Filters.filterString(params, "email"));
		instinfo.setEntityGid(Filters.filterString(params, "entity_gid"));
		instinfo.setEntityName(Filters.filterString(params, "entity_name"));
		instinfo.setNodeGid(Filters.filterString(params, "node_gid"));
		instinfo.setShortName(Filters.filterString(params, "short_name"));
		instinfo.setSourceName(Filters.filterString(params, "source_name"));
		instinfo.setState(Filters.filterString(params, "state"));

		svcctx.setOperationObject(instinfo.getInfoId());
		svcctx.addOperationPredicates(instinfo);
		svcctx.addOperationPredicate("public_url", Filters.filterString(params, "public_url"));
		svcctx.addOperationPredicate("service_url", Filters.filterString(params, "service_url"));
		svcctx.addOperationPredicate("binary_url", Filters.filterString(params, "binary_url"));
		
		sourceService.saveSource(svcctx, instinfo);
		
		result = ActionResult.success(getMessage(exchange, "mesg.save.instance"));
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="sources-query")
	public void handleSearchInstance( HttpServerExchange exchange )throws BaseException {
				
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.SRC_FND);
		svcctx.addOperationPredicates(paramap);
		
		String name = Filters.filterAll(paramap, "keyword");
		PageQuery pquery = Filters.filterPageQuery(paramap);
		String typeStr = Filters.filterAll(paramap, "type");
		
		Optional<Sources.Type> opt = Enums.getIfPresent(Sources.Type.class, Strings.nullToEmpty(typeStr));
		Sources.Type type = opt.isPresent() ? opt.get() : null;
	
		ActionResult result = null;
	
		// query accounts information
		List<SourceInfo> instances = sourceService.getSources(type, Strings.nullToEmpty(name), pquery );
		List<Map<String, Object>> list = instances.stream().map((instinfo) -> {
			
			DataBuilder data = new DataBuilder();
			data.set("source_id", instinfo.getId().toString());
			data.set(instinfo, "abbr", "description", "email", "state", 
					"entity_gid", "entity_name", "node_gid", "short_name", "source_name");
			
			data.set("admin_uid", instinfo.getAdminUid().toString());
	
			return data.build();
			
		}).collect(Collectors.toList());
				
		result = ActionResult.success(getMessage(exchange, "mesg.find.instance"));
		result.setData(pquery == null ? null : pquery.getPagination(), list);
		
		this.sendResult(exchange, result);	
	}
}
