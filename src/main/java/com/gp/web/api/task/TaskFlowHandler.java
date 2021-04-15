/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.task;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.LocalDates;
import com.gp.common.NodeIdKey;
import com.gp.common.Operations;
import com.gp.common.ServiceContext;
import com.gp.dao.info.TaskFlowInfo;
import com.gp.dao.info.TaskPhaseInfo;
import com.gp.dao.info.TaskRouteInfo;
import com.gp.dao.info.TaskTraceInfo;
import com.gp.exception.BaseException;
import com.gp.exception.WebException;
import com.gp.info.DataBuilder;
import com.gp.info.Principal;
import com.gp.svc.CommonService;
import com.gp.svc.task.TaskFlowService;
import com.gp.validate.ArgsValidator;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.FilterHandler;
import com.gp.web.anno.WebApi;
import com.gp.web.api.ServiceApiHelper;

import io.undertow.server.HttpServerExchange;

public class TaskFlowHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(TaskFlowHandler.class);

	private TaskFlowService taskFlowService;
	private CommonService commonService;
	
	public TaskFlowHandler() {
		taskFlowService = BindScanner.instance().getBean(TaskFlowService.class);
		commonService = BindScanner.instance().getBean(CommonService.class);
	}

	@WebApi(path="task-flows-query")
	public void handleTaskFlowsQuery(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_FND_FLW);
		svcctx.addOperationPredicates(params);
		
		long wgrpid = Filters.filterLong(params, "wgroup_id");
		String wgrpCode = Filters.filterString(params, "wgroup_code");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.attach"));
		
		InfoId wgrpId = null;
		
		if(wgrpid <= 0){
			
			wgrpId = commonService.queryInfoId(wgrpCode);
		}else {
			
			wgrpId = IdKeys.getInfoId(NodeIdKey.WORKGROUP, wgrpid);
		}
		
		List<TaskFlowInfo> infos = taskFlowService.getTaskFlows(wgrpId);
		
		List<Object> data = infos.stream().map(info -> {
			
			DataBuilder builder = new DataBuilder();
			builder.set("flow_id", info.getId().toString());
		
			builder.set(info, "flow_name", "description");
			
			return builder.build();
		}).collect(Collectors.toList());
		
		result.setData(data);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-flow-add")
	public void handleTaskFlowAdd(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_ADD_FLW);
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.attach"));
		
		long wgrpid = Filters.filterLong(params, "wgroup_id");
		String wgrpCode = Filters.filterString(params, "wgroup_code");
		InfoId wgrpId = null;
		
		if(wgrpid <= 0){
			
			wgrpId = commonService.queryInfoId(wgrpCode);
			wgrpid = wgrpId.getId();
		}
		
		Principal princ = svcctx.getPrincipal();
		TaskFlowInfo finfo = new TaskFlowInfo();
		
		finfo.setWorkgroupId(wgrpid);
		finfo.setFlowName(Filters.filterString(params, "flow_name"));
		finfo.setDescription(Filters.filterString(params, "description"));
		
		finfo.setCreatorUid(princ.getUserId().getId());
		finfo.setCreateTime(LocalDates.now());
		
		InfoId fid = taskFlowService.addTaskFlow(svcctx, finfo);
		
		Map<String, Object> data = Maps.newHashMap();
		data.put("flow_id", fid.getId().toString());
		result.setData(data);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-flow-save")
	public void handleTaskFlowSave(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_UPD_FLW);
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.attach"));
	
		long flowid = Filters.filterLong(params, "flow_id");
		InfoId flowId = null;
		
		if(flowid <= 0){
			throw new WebException("excp.miss.param");
		}
		
		flowId = IdKeys.getInfoId(NodeIdKey.TASK_FLOW, flowid);
		TaskFlowInfo finfo = new TaskFlowInfo();
		finfo.setInfoId(flowId);
		finfo.setFlowName(Filters.filterString(params, "flow_name"));
		finfo.setDescription(Filters.filterString(params, "description"));
		
		int cnt = taskFlowService.updateTaskFlow(svcctx, finfo);
		
		if(cnt > 0) {
			Map<String, Object> data = Maps.newHashMap();
			data.put("flow_id", finfo.getId().toString());
			result.setData(data);
		}
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-flow-remove")
	public void handleTaskFlowRemove(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_RMV_FLW);
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.attach"));
		
		long flowid = Filters.filterLong(params, "flow_id");
		InfoId flowId = null;
		
		if(flowid <= 0){
			throw new WebException("excp.miss.param");
		}
		
		flowId = IdKeys.getInfoId(NodeIdKey.TASK_FLOW, flowid);
		
		int cnt = taskFlowService.removeTaskFlow(svcctx, flowId);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-phases-query")
	public void handleTaskPhasesQuery(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.requireOne("flow_id", "wgroup_id", "wgroup_code")
			.validate(true);
		
		long flowid = Filters.filterLong(params, "flow_id");
		long wgrpid = Filters.filterLong(params, "wgroup_id");
		String wgrpCode = Filters.filterString(params, "wgroup_code");
		boolean valid = Filters.filterBoolean(params, "valid");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.attach"));
	
		InfoId wgrpId = null;
		if(wgrpid <= 0){
			
			wgrpId = commonService.queryInfoId(wgrpCode);
		}else {
			
			wgrpId = IdKeys.getInfoId(NodeIdKey.WORKGROUP, wgrpid);
		}
		InfoId flowId = IdKeys.getInfoId(NodeIdKey.TASK_FLOW, flowid);
		
		List<TaskPhaseInfo> infos = null;
		
		if(IdKeys.isValidId(flowId)) {
			
			infos = taskFlowService.getTaskFlowPhases(flowId, valid);
		}else {
			
			infos = taskFlowService.getTaskPhases(wgrpId);
		}
		
		List<Object> data = infos.stream().map(info -> {
			
			DataBuilder builder = new DataBuilder();
			builder.set("phase_id", info.getId().toString());
			builder.set("workgroup_id", info.getWorkgroupId().toString());
			
			builder.set(info, "phase_name", "description");
			builder.set("is_default", info.getIsDefault());
			builder.set("phase_order", info.getPhaseOrder());
			
			Long routeId = (Long)info.getProperty("route_id");
			if( routeId != null && routeId > 0) {
				
				builder.set("route_id", routeId.toString());
				builder.set(info, "task_state", "task_progress");
				
				Long assignee = (Long) info.getProperty("assignee_uid", Long.class);
				if(assignee != null && assignee > 0) {
					
					builder.set("assignee_uid", assignee.toString());
					builder.set("assignee", (sbuilder) -> {
						
						sbuilder.set("user_gid", info.getProperty("assign_gid"));
						sbuilder.set("username", info.getProperty("assign_username"));
						
						sbuilder.set("full_name", info.getProperty("assign_full_name"));
						sbuilder.set("nickname", info.getProperty("assign_nickname"));
						
						String avatarUrl = info.getProperty("assign_avatar_url", String.class);
						avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
						sbuilder.set("avatar_url", avatarUrl);
					});
				}else {
					builder.set("assignee_uid", "");
				}
			}
			
			return builder.build();
			
		}).collect(Collectors.toList());
		
		result.setData(data);
		
		this.sendResult(exchange, result);
	}
	
	/**
	 * Query all the route of specified flow. but the task is optional id
	 * if task id is available, [is_active] is set on the result item
	 *   
	 **/
	@WebApi(path="task-routes-query")
	public void handleTaskRoutesQuery(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);
		
		svcctx.addOperationPredicates(params);
		
		long flowid = Filters.filterLong(params, "flow_id");
		long taskid = Filters.filterLong(params, "task_id");
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.attach"));
		
		InfoId flowId = IdKeys.getInfoId(NodeIdKey.TASK_FLOW, flowid);
		InfoId taskId = IdKeys.getInfoId(NodeIdKey.TASK, taskid);
		if(!IdKeys.isValidId(flowId)){
			throw new WebException("excp.miss.param");
		}
		
		List<TaskRouteInfo> infos = null;
		
		infos = taskFlowService.getTaskRoutes(flowId, taskId);
			
		List<Object> data = infos.stream().map(info -> {
			
			DataBuilder builder = new DataBuilder();
			builder.set("route_id", info.getId().toString());
			builder.set("phase_id", info.getPhaseId().toString());
			
			builder.set(info, "phase_name", "phase_order", "task_state", "task_progress");
					
			if(taskid > 0) {
				builder.set(info, "is_active");
			}
			
			Long assignee = (Long) info.getProperty("assignee_uid", Long.class);
			if(assignee != null && assignee > 0) {
				
				builder.set("assignee_uid", assignee.toString());
				builder.set("assignee", (sbuilder) -> {
					
					sbuilder.set("user_gid", info.getProperty("assign_gid"));
					sbuilder.set("username", info.getProperty("assign_username"));
					
					sbuilder.set("full_name", info.getProperty("assign_full_name"));
					sbuilder.set("nickname", info.getProperty("assign_nickname"));
					
					String avatarUrl = info.getProperty("assign_avatar_url", String.class);
					avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
					sbuilder.set("avatar_url", avatarUrl);
				});
			}else {
				builder.set("assignee_uid", "");
			}
			
			return builder.build();
			
		}).collect(Collectors.toList());
		
		result.setData(data);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-phase-add")
	public void handleTaskPhaseAdd(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.attach"));
		
		long wgrpid = Filters.filterLong(params, "wgroup_id");
		String wgrpCode = Filters.filterString(params, "wgroup_code");
		InfoId wgrpId = null;
		
		if(wgrpid <= 0){
			
			wgrpId = commonService.queryInfoId(wgrpCode);
			wgrpid = wgrpId.getId();
		}
		
		TaskPhaseInfo pinfo = new TaskPhaseInfo();
		
		pinfo.setWorkgroupId(wgrpid);
		pinfo.setPhaseName(Filters.filterString(params, "phase_name"));
		pinfo.setIsDefault(Filters.filterBoolean(params, "is_default"));
		pinfo.setDescription(Filters.filterString(params, "description"));
		
		InfoId fid = taskFlowService.addTaskPhase(svcctx, pinfo);
		
		Map<String, Object> data = Maps.newHashMap();
		data.put("phase_id", fid.getId().toString());
		result.setData(data);
		
		this.sendResult(exchange, result);
		
	}
	
	@WebApi(path="task-phase-save")
	public void handleTaskPhaseSave(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.attach"));
		
		long phaseid = Filters.filterLong(params, "phase_id");
		InfoId phaseId = null;
		
		if(phaseid <= 0){
			throw new WebException("excp.miss.param");
		}
		
		phaseId = IdKeys.getInfoId(NodeIdKey.TASK_PHASE, phaseid);
		TaskPhaseInfo pinfo = new TaskPhaseInfo();
		pinfo.setInfoId(phaseId);
		pinfo.setPhaseName(Filters.filterString(params, "phase_name"));
		pinfo.setDescription(Filters.filterString(params, "description"));
		pinfo.setPhaseOrder(Filters.filterInt(params, "phase_order"));
		pinfo.setIsDefault(Filters.filterBoolean(params, "is_default"));
		
		int cnt = taskFlowService.updateTaskPhase(svcctx, pinfo);
		
		if(cnt > 0) {
			Map<String, Object> data = Maps.newHashMap();
			data.put("phase_id", pinfo.getId().toString());
			result.setData(data);
		}
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-phase-remove")
	public void handleTaskPhaseRemove(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.attach"));
		
		long phaseid = Filters.filterLong(params, "phase_id");
		InfoId phaseId = null;
		
		if(phaseid <= 0){
			throw new WebException("excp.miss.param");
		}
		
		phaseId = IdKeys.getInfoId(NodeIdKey.TASK_PHASE, phaseid);
		
		int cnt = taskFlowService.removeTaskPhase(svcctx, phaseId);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-phase-reorder")
	@SuppressWarnings("unchecked")
	public void handleTaskPhaseReorder(HttpServerExchange exchange) throws Exception {
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.attach"));
		ServiceContext svcctx = this.getServiceContext(exchange);
		
		List<Object> paramlist = (List<Object>)exchange.getAttachment(FilterHandler.REQUEST_BODY);
		
		if(paramlist.isEmpty()) {
			
			throw new WebException("excp.illegal.param");
		}
		
		Map<Long, Integer> phases = Maps.newHashMap();
		paramlist.forEach(el -> {
			Map<String, Object> item = (Map<String, Object>) el;
			Long phaseId = NumberUtils.toLong((String)item.get("phase_id"));
			Integer order = (Integer)item.get("phase_order");
			
			phases.put(phaseId, order);
		});
		
		if(!phases.isEmpty()) {
			taskFlowService.updateTaskPhasesOrder(svcctx, phases);
		}
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-route-add")
	public void handleTaskRouteAdd(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.attach"));
		
		long flowid = Filters.filterLong(params, "flow_id");
		long phaseid = Filters.filterLong(params, "phase_id");
		
		if(phaseid <= 0 || flowid <= 0){
			throw new WebException("excp.miss.param");
		}
		
		TaskRouteInfo info = new TaskRouteInfo();
		
		info.setFlowId(flowid);
		info.setPhaseId(phaseid);
		info.setAssigneeUid(Filters.filterLong(params, "assignee_uid"));
		info.setTaskState(Filters.filterString(params, "task_state"));
		info.setTaskProgress(Filters.filterInt(params, "task_progress"));
		InfoId rid = taskFlowService.addTaskRoute(svcctx, info);
		
		Map<String, Object> data = Maps.newHashMap();
		data.put("route_id", rid.getId().toString());
		result.setData(data);
		
		this.sendResult(exchange, result);
	}

	@WebApi(path="task-route-save")
	public void handleTaskRouteSave(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.attach"));
		
		long routeid = Filters.filterLong(params, "route_id");
		InfoId routeId = null;
		
		if(routeid <= 0){
			throw new WebException("excp.miss.param");
		}
		
		routeId = IdKeys.getInfoId(NodeIdKey.TASK_ROUTE, routeid);
		TaskRouteInfo info = new TaskRouteInfo();
		info.setInfoId(routeId);
		
		info.setAssigneeUid(Filters.filterLong(params, "assignee_uid"));
		info.setTaskState(Filters.filterString(params, "task_state"));
		info.setTaskProgress(Filters.filterInt(params, "task_progress"));
		
		int cnt = taskFlowService.updateTaskRoute(svcctx, info);
		
		if(cnt > 0) {
			Map<String, Object> data = Maps.newHashMap();
			data.put("route_id", info.getId().toString());
			result.setData(data);
		}
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-route-remove")
	public void handleTaskRouteRemove(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.attach"));
		
		long routeid = Filters.filterLong(params, "route_id");
		InfoId routeId = null;
		
		if(routeid <= 0){
			throw new WebException("excp.miss.param");
		}
		
		routeId = IdKeys.getInfoId(NodeIdKey.TASK_ROUTE, routeid);
		
		int cnt = taskFlowService.removeTaskRoute(svcctx, routeId);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-assign-flow")
	public void handleTaskAssignFlow(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.attach"));
	
		long flowid = Filters.filterLong(params, "flow_id");
		long taskid = Filters.filterLong(params, "task_id");
		InfoId flowId = null;
		InfoId taskId = null;
		if(flowid <= 0 || taskid <= 0){
			throw new WebException("excp.miss.param");
		}
		flowId = IdKeys.getInfoId(NodeIdKey.TASK_FLOW, flowid);
		taskId = IdKeys.getInfoId(NodeIdKey.TASK, taskid);
		
		taskFlowService.assignTaskFlow(svcctx, taskId, flowId);
		
		this.sendResult(exchange, result);
	}
	

	@WebApi(path="task-unassign-flow")
	public void handleTaskUnassignFlow(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
	
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.attach"));
		
		long taskid = Filters.filterLong(params, "task_id");
		InfoId taskId = null;
		
		if(taskid <= 0){
			throw new WebException("excp.miss.param");
		}
		
		taskId = IdKeys.getInfoId(NodeIdKey.TASK, taskid);
		
		int cnt = commonService.updateColumn(taskId, "flow_id", "0");
		
		if(cnt == 0) {
			result = ActionResult.failure(getMessage(exchange, "excp.unassign.flow"));
		}
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-assign-phase")
	public void handleTaskAssignPhase(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.attach"));
		
		long phaseid = Filters.filterLong(params, "phase_id");
		long taskid = Filters.filterLong(params, "task_id");
		InfoId phaseId = null;
		InfoId taskId = null;
		if(phaseid <= 0 || taskid <= 0){
			throw new WebException("excp.miss.param");
		}
		phaseId = IdKeys.getInfoId(NodeIdKey.TASK_PHASE, phaseid);
		taskId = IdKeys.getInfoId(NodeIdKey.TASK, taskid);
		
		taskFlowService.migrateTaskPhase(svcctx, taskId, phaseId);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-traces-query")
	public void handleTaskTracesQuery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
	
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.attach"));
		
		long taskid = Filters.filterLong(params, "task_id");
		InfoId taskId = null;
		
		if(taskid <= 0){
			throw new WebException("excp.miss.param");
		}
		
		taskId = IdKeys.getInfoId(NodeIdKey.TASK, taskid);
		
		List<TaskTraceInfo> infos = taskFlowService.getTaskTraces(taskId);
		List<Object> data = infos.stream().map(info -> {
			
			DataBuilder builder = new DataBuilder();
			builder.set("trace_id", info.getId().toString());
			builder.set("flow_id", info.getFlowId().toString());
			builder.set("phase_id", info.getPhaseId().toString());
			builder.set(info, "phase_name", "phase_order", "is_active");
			builder.set("assignee_uid", info.getAssigneeUid().toString());
			builder.set("active_time", info.getActiveTime().getTime());
			
			builder.set("assignee", (sbuilder) -> {
				
				sbuilder.set("user_gid", info.getProperty("assign_gid"));
				sbuilder.set("username", info.getProperty("assign_username"));
				sbuilder.set("full_name", info.getProperty("assign_full_name"));
				sbuilder.set("nickname", info.getProperty("assign_nickname"));
				
				String avatarUrl = info.getProperty("assign_avatar_url", String.class);
				avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
				sbuilder.set("avatar_url", avatarUrl);
			});
			
			return builder.build();
		}).collect(Collectors.toList());
		
		result.setData(data);
		
		this.sendResult(exchange, result);
	}
}
