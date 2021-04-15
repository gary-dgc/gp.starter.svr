/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.base;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.IdKeys;
import com.gp.common.Identifier;
import com.gp.common.InfoId;
import com.gp.common.NodeIdKey;
import com.gp.common.ServiceContext;
import com.gp.dao.info.OperationInfo;
import com.gp.exception.BaseException;
import com.gp.pagination.PageQuery;
import com.gp.svc.CommonService;
import com.gp.svc.OperationService;
import com.gp.validate.ArgsValidator;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import com.gp.web.api.ServiceApiHelper;

import io.undertow.server.HttpServerExchange;

public class OperHandler extends BaseApiSupport {

	static Logger LOGGER = LoggerFactory.getLogger(OperHandler.class);
	
	private OperationService operService;
	private CommonService commonService;
	
	public OperHandler() {
		operService = BindScanner.instance().getBean(OperationService.class);
		commonService = BindScanner.instance().getBean(CommonService.class);		
	}

	@WebApi(path="wgroup-opers-query", operation="wgrp:opr")
	public void handleWGroupOpersQuery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);
		
		long groupId = Filters.filterLong(params, "wgroup_id");
		String groupCode = Filters.filterString(params, "wgroup_code");
				
		PageQuery pquery = Filters.filterPageQuery(params);
		
		List<Map<String, Object>> data = Lists.newArrayList();
		
		InfoId gid = IdKeys.getInfoId(NodeIdKey.WORKGROUP, groupId);
		if(!IdKeys.isValidId(gid)) {
			gid = commonService.queryInfoId(groupCode);
		}
		// amend the operation information
		svcctx.setOperationObject(gid);
		List<OperationInfo> opers = operService.getWorkgroupOperations(gid, pquery);
		
		opers.forEach(oper -> {
			Map<String, Object> row = oper.toMap("subject_uid", "subject", "subject_label", 
					"operation", "operation_label", "object", "object_label");
			row.put("oper_id", oper.getId().toString());
			row.put("subject_gid", oper.getProperty("user_gid"));
			
			String relativeUrl = ServiceApiHelper.absoluteBinaryUrl((String)oper.getProperty("avatar_url"));
			row.put("avatar_url", relativeUrl);
			
			Date operTime = oper.getOperationTime();
			if(null != operTime) {
				row.put("operation_time", operTime.getTime());
			}else {
				row.put("operation_time", "");
			}
			data.add(row);
		});
		
		ActionResult result = ActionResult.success("success get operation");
		if(null != pquery) {
			result.setData(pquery.getPagination(), data);
		}else {
			result.setData(data);
		}
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="node-opers-query", operation="wgrp:opr")
	public void handleNodeOpersQuery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
				
		PageQuery pquery = Filters.filterPageQuery(params);		
		List<Map<String, Object>> data = Lists.newArrayList();		
		List<OperationInfo> opers = operService.getNodeOperations(pquery);
		
		opers.forEach(oper -> {
			Map<String, Object> row = oper.toMap("subject_uid", "subject", "subject_label", 
					"operation", "operation_label", "object", "object_label");
			row.put("oper_id", oper.getId().toString());
			row.put("subject_gid", oper.getProperty("user_gid"));
			
			String relativeUrl = ServiceApiHelper.absoluteBinaryUrl((String)oper.getProperty("avatar_url"));
			row.put("avatar_url", relativeUrl);
			
			Date operTime = oper.getOperationTime();
			if(null != operTime) {
				row.put("operation_time", operTime.getTime());
			}else {
				row.put("operation_time", "");
			}
			data.add(row);
		});
		
		ActionResult result = ActionResult.success("success get operation");
		if(null != pquery) {
			result.setData(pquery.getPagination(), data);
		}else {
			result.setData(data);
		}
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="user-opers-query", operation="wgrp:opr")
	public void handleUserOpersQuery(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);
				
		PageQuery pquery = Filters.filterPageQuery(params);
		
		List<Map<String, Object>> data = Lists.newArrayList();
		String account = svcctx.getPrincipal().getUsername();
		
		List<OperationInfo> opers = operService.getAccountOperations(account, pquery);
		
		opers.forEach(oper -> {
			Map<String, Object> row = oper.toMap("subject_uid", "subject", "subject_label", 
					"operation", "operation_label", "object", "object_label");
			row.put("oper_id", oper.getId().toString());
			row.put("subject_gid", oper.getProperty("user_gid"));
			
			String relativeUrl = ServiceApiHelper.absoluteBinaryUrl((String)oper.getProperty("avatar_url"));
			row.put("avatar_url", relativeUrl);
			
			Date operTime = oper.getOperationTime();
			if(null != operTime) {
				row.put("operation_time", operTime.getTime());
			}else {
				row.put("operation_time", "");
			}
			data.add(row);
		});
		
		ActionResult result = ActionResult.success("success get operation");
		if(null != pquery) {
			result.setData(pquery.getPagination(), data);
		}else {
			result.setData(data);
		}
		
		this.sendResult(exchange, result);
		
	}
	
	@WebApi(path="object-opers-query", operation="wgrp:opr")
	public void handleObjectOpersQuery(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		
		ArgsValidator.newValidator(params)
			.require("schema", "id")
			.validate(true);
		
		PageQuery pquery = Filters.filterPageQuery(params);
		
		List<Map<String, Object>> data = Lists.newArrayList();
		Identifier KEY = IdKeys.valueOfIgnoreCase(Filters.filterString(params, "schema"));
		Long id = Filters.filterLong(params, "id");
		
		InfoId objectKey = new InfoId(KEY, id);
		List<OperationInfo> opers = operService.getObjectOperations(objectKey, pquery);
		
		opers.forEach(oper -> {
			Map<String, Object> row = oper.toMap("subject_uid", "subject", "subject_label", 
					"operation", "operation_label", "object", "object_label");
			row.put("oper_id", oper.getId().toString());
			row.put("subject_gid", oper.getProperty("user_gid"));
			
			String relativeUrl = ServiceApiHelper.absoluteBinaryUrl((String)oper.getProperty("avatar_url"));
			row.put("avatar_url", relativeUrl);
			
			Date operTime = oper.getOperationTime();
			if(null != operTime) {
				row.put("operation_time", operTime.getTime());
			}else {
				row.put("operation_time", "");
			}
			data.add(row);
		});
		
		ActionResult result = ActionResult.success("success get operation");
		if(null != pquery) {
			result.setData(pquery.getPagination(), data);
		}else {
			result.setData(data);
		}
		
		this.sendResult(exchange, result);
		
	}
}
