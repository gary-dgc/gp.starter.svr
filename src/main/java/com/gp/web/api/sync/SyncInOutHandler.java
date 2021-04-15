/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.sync;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.InfoId;
import com.gp.common.NodeIdKey;
import com.gp.common.Synchronizes.PushState;
import com.gp.dao.info.SyncMsgInInfo;
import com.gp.dao.info.SyncMsgOutInfo;
import com.gp.dao.info.SyncPullInfo;
import com.gp.dao.info.SyncPushInfo;
import com.gp.info.DataBuilder;
import com.gp.pagination.PageQuery;
import com.gp.sql.BaseBuilder.SortOrder;
import com.gp.svc.sync.SyncInOutService;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;

import io.undertow.server.HttpServerExchange;

public class SyncInOutHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(SyncInOutHandler.class);
	
	private SyncInOutService syncOutService;
	
	static SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy/MM/dd");
	
	public SyncInOutHandler() {
		
		syncOutService = BindScanner.instance().getBean(SyncInOutService.class);
	
	}

	@WebApi(path="sync-outs-query", auditable=false, traceable=false)
	public void handleSyncOutQuery(HttpServerExchange exchange) throws Exception {
		
		ActionResult result = null;
		Map<String, Object> params = this.getRequestBody(exchange);
		
		PageQuery pquery = Filters.filterPageQuery(params);
		String scope = Filters.filterAll(params, "dest_scope");
		String gid = Filters.filterString(params, "dest_gid");
		String cmd = Filters.filterString(params, "sync_cmd");
		String object = Filters.filterString(params, "object");
		String dtFrom = Filters.filterString(params, "date_from");
		String dtTo = Filters.filterString(params, "date_to");
		
		Date auditFrom = null;
		Date auditTo = null;
		try {
			auditFrom = Strings.isNullOrEmpty(dtFrom)? null : DATE_FMT.parse(dtFrom);
			auditTo = Strings.isNullOrEmpty(dtTo)? null : DATE_FMT.parse(dtTo);
		} catch (ParseException e) {
			
			result = ActionResult.failure("excp.illegal.param");
			this.sendResult(exchange, result);
		}
		
		if(null != pquery) {
			pquery.setOrderBy("msg_id");
			pquery.setOrder(SortOrder.DESC.name());
		}
		List<SyncMsgOutInfo> infos = syncOutService.getSyncOutMesgs(scope, gid, cmd, object, auditFrom, auditTo, pquery);
		
		List<Object> data = infos.stream().map((info) -> {
			
			DataBuilder builder = new DataBuilder();
			builder.set("msg_id", info.getId().toString());
			
			builder.set(info, "trace_code", "dest_scope", "dest_gid", "object_code", "object");
			builder.set(info, "sync_cmd", "payload", "state", "result", "operator_gid");
			
			builder.set("oper_id", String.valueOf(info.getOperId()));
			builder.set("push_id", String.valueOf(info.getPushId()));
			
			builder.set("oper_time", info.getOperTime().getTime());
			return builder.build();
			
		}).collect(Collectors.toList());
		
		result = ActionResult.success("mesg.find.outs");
		result.setData(pquery == null ? null : pquery.getPagination(), data);
		
		this.sendResult(exchange, result);
	}

	@WebApi(path="sync-push-info", auditable=false, traceable=false)
	public void handleSyncPushInfo(HttpServerExchange exchange) throws Exception {
		
		ActionResult result = null;
		Map<String, Object> params = this.getRequestBody(exchange);
		InfoId pushKey = Filters.filterInfoId(params, "push_id", NodeIdKey.SYNC_PUSH);
		
		SyncPushInfo info = syncOutService.getSyncPush(pushKey);
		
		Map<String, Object> data = info.toMap();
		
		data.put("push_time", info.getPushTime().getTime());
		
		result = ActionResult.success("mesg.find.push");
		result.setData(data);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="sync-push-reset", auditable=false, traceable=false)
	public void handleSyncPushReset(HttpServerExchange exchange) throws Exception {
		
		ActionResult result = null;
		Map<String, Object> params = this.getRequestBody(exchange);
		InfoId pushKey = Filters.filterInfoId(params, "push_id", NodeIdKey.SYNC_PUSH);
		PushState state = Filters.filterEnum(params, "state", PushState.class);
		
		syncOutService.resetSyncPush(pushKey, state);
	
		result = ActionResult.success("mesg.reset.push");
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="sync-ins-query", auditable=false, traceable=false)
	public void handleSyncInQuery(HttpServerExchange exchange) throws Exception {
		
		ActionResult result = null;
		Map<String, Object> params = this.getRequestBody(exchange);
		
		PageQuery pquery = Filters.filterPageQuery(params);
		String scope = Filters.filterAll(params, "origin_scope");
		String gid = Filters.filterString(params, "origin_gid");
		String cmd = Filters.filterString(params, "sync_cmd");
		String dtFrom = Filters.filterString(params, "date_from");
		String dtTo = Filters.filterString(params, "date_to");
		
		Date auditFrom = null;
		Date auditTo = null;
		try {
			auditFrom = Strings.isNullOrEmpty(dtFrom)? null : DATE_FMT.parse(dtFrom);
			auditTo = Strings.isNullOrEmpty(dtTo)? null : DATE_FMT.parse(dtTo);
		} catch (ParseException e) {
			
			result = ActionResult.failure("excp.illegal.param");
			this.sendResult(exchange, result);
		}
		
		if(null != pquery) {
			pquery.setOrderBy("msg_id");
			pquery.setOrder(SortOrder.DESC.name());
		}
		List<SyncMsgInInfo> infos = syncOutService.getSyncInMesgs(scope, gid, cmd, auditFrom, auditTo, pquery);
		
		List<Object> data = infos.stream().map((info) -> {
			
			DataBuilder builder = new DataBuilder();
			builder.set("msg_id", info.getId().toString());
			
			builder.set(info, "trace_code", "origin_scope", "origin_gid", "object_code");
			builder.set(info, "sync_cmd", "payload", "state", "result", "operator_gid");
			
			builder.set("pull_id", String.valueOf(info.getPullId()));
			
			builder.set("oper_time", info.getOperTime().getTime());
			
			return builder.build();
			
		}).collect(Collectors.toList());
		
		result = ActionResult.success("mesg.find.ins");
		result.setData(pquery == null ? null : pquery.getPagination(), data);
		
		this.sendResult(exchange, result);
	}

	@WebApi(path="sync-pull-info", auditable=false, traceable=false)
	public void handleSyncPullInfo(HttpServerExchange exchange) throws Exception {
		
		ActionResult result = null;
		Map<String, Object> params = this.getRequestBody(exchange);
		InfoId pushKey = Filters.filterInfoId(params, "pull_id", NodeIdKey.SYNC_PULL);
		
		SyncPullInfo info = syncOutService.getSyncPull(pushKey);
		
		Map<String, Object> data = info.toMap();
		
		data.put("pull_time", info.getPullTime().getTime());
		
		result = ActionResult.success("mesg.find.push");
		result.setData(data);
		
		this.sendResult(exchange, result);
	}
}
