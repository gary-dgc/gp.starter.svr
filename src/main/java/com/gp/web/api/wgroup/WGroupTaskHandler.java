/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.wgroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.LocalDates;
import com.gp.common.NodeIdKey;
import com.gp.common.Operations;
import com.gp.common.ServiceContext;
import com.gp.common.Tasks.Priority;
import com.gp.dao.info.TaskInfo;
import com.gp.dao.info.WorkgroupInfo;
import com.gp.exception.BaseException;
import com.gp.info.DataBuilder;
import com.gp.info.Principal;
import com.gp.pagination.PageQuery;
import com.gp.svc.CommonService;
import com.gp.svc.task.TaskExtraService;
import com.gp.svc.task.TaskQueryService;
import com.gp.validate.ArgsValidator;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import com.gp.web.api.ServiceApiHelper;

import io.undertow.server.HttpServerExchange;

public class WGroupTaskHandler extends BaseApiSupport{

static Logger LOGGER = LoggerFactory.getLogger(WGroupTaskHandler.class);
	
	private TaskQueryService taskQueryService;
	private TaskExtraService taskExtraService;
	private CommonService commonService;
	
	public WGroupTaskHandler() {
		taskQueryService = BindScanner.instance().getBean(TaskQueryService.class);
		taskExtraService = BindScanner.instance().getBean(TaskExtraService.class);
		commonService = BindScanner.instance().getBean(CommonService.class);
	}

	/**
	 * Get workgroup tasks
	 * 
	 * @param features [ALL, PLAIN, FLOW, BIND, DELIV, ASSIGN, UNASSIGN]
	 **/
	@WebApi(path="wgroup-tasks-query")
	public void handleTasksQuery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_FND);		
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.requireOne("wgroup_id","wgroup_code")
			.validate(true);
		
		long wgid = Filters.filterLong(params, "wgroup_id");
		String wcode = Filters.filterString(params, "wgroup_code");
		String keyword = Filters.filterString(params, "keyword");
		
		List<String> features = Filters.filterList(params, "features", String.class);
		features.remove(Filters.ALL);
		List<String> _priorities = Filters.filterList(params, "priorities", String.class);
		List<Integer> priorities = _priorities == null ? null : _priorities.stream().map(p -> {
			Optional<Priority> _p = Enums.getIfPresent(Priority.class, p);
			if(_p.isPresent()) {
				return _p.get().ordinal();
			}
			return -1;
		}).filter(p -> p > 0).collect(Collectors.toList());
		
		InfoId listKey = Filters.filterInfoId(params, "list_id", NodeIdKey.TASK_LIST);
		
		List<String> marks = Filters.filterList(params, "marks", String.class);
		String ownGid = Filters.filterString(params, "owner_gid");
		String asnGid = Filters.filterString(params, "assignee_gid");
		String collGid = Filters.filterString(params, "collab_gid");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.fnd.task"));
		
		Principal princ = svcctx.getPrincipal();
		String dateStr = Filters.filterString(params, "due_time");
		Date dueDate = Strings.isNullOrEmpty(dateStr) ? null: LocalDates.parseMediumDate(princ.getTimeZone(), dateStr, princ.getLocale());
		
		PageQuery pquery = Filters.filterPageQuery(params);
		
		InfoId gid = IdKeys.getInfoId(NodeIdKey.WORKGROUP, wgid);
		if(!IdKeys.isValidId(gid)) {
			gid = commonService.queryInfoId(wcode);
		}
		
		InfoId ownerId = commonService.queryInfoId(ownGid);
		InfoId assignId = commonService.queryInfoId(asnGid);
		InfoId collabId = commonService.queryInfoId(collGid);
		@SuppressWarnings("unchecked")
		Collection<String> state = (Collection<String>)params.get("states");
		
		List<TaskInfo> infos = taskQueryService.getTasks(svcctx, gid, listKey, ownerId, assignId, collabId, keyword, marks, priorities, features, state, dueDate, pquery);
	
		List<Object> list = new ArrayList<Object>();
		for(TaskInfo info: infos) {
			
			DataBuilder builder = new DataBuilder();
			builder.set("task_id", info.getId().toString());
			builder.set("task_code", info.getTraceCode());
			
			builder.set("list_id", info.getListId() == null ? "": info.getListId().toString());
			builder.set("list_name", info.getProperty("list_name"));
			
			builder.set("workgroup_id", info.getWorkgroupId().toString());
			builder.set("flow_id", info.getFlowId().toString());
			builder.set("task_chronical_id", info.getTaskChronicalId().toString());
			if(null != info.getTaskPid()) {
				builder.set("task_pid", info.getTaskPid().toString());
			}

			builder.set("create_time", info.getCreateTime().getTime());
			
			builder.set("plan_time", info.getPlanTime() != null ? info.getPlanTime().getTime() : 0);
			builder.set("due_time", info.getDueTime() != null ? info.getDueTime().getTime() : 0);
			
			builder.set("start_time", info.getStartTime() != null ? info.getStartTime().getTime() : 0);
			builder.set("end_time", info.getEndTime() != null ? info.getEndTime().getTime() : 0);
						
			builder.set(info, "title", "content", "excerpt", "is_constrained",
					"state", "task_opinion", "precedence", "mark", "progress", "priority");
			
			builder.set("reminder_type", info.getReminderType());
			
			builder.set("owner_uid", info.getOwnerUid().toString());
			builder.set("owner", sbuilder -> {
				sbuilder.set("user_gid", info.getProperty("owner_gid"));
				sbuilder.set("username", info.getProperty("owner_username"));
				
				sbuilder.set("full_name", info.getProperty("owner_full_name"));
				sbuilder.set("nickname", info.getProperty("owner_nickname"));
				String avatarUrl = info.getProperty("owner_avatar_url", String.class);
				avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
				sbuilder.set("avatar_url", avatarUrl);
			});
			
			builder.set("assignee_uid", info.getAssigneeUid().toString());
			builder.set("assignee", sbuilder -> {
				sbuilder.set("user_gid", info.getProperty("assign_gid"));
				sbuilder.set("username", info.getProperty("assign_username"));
				
				sbuilder.set("full_name", info.getProperty("assign_full_name"));
				sbuilder.set("nickname", info.getProperty("assign_nickname"));
				String avatarUrl = info.getProperty("assign_avatar_url", String.class);
				avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
				sbuilder.set("avatar_url", avatarUrl);
			});
			
			builder.set("workgroup", sbuilder -> {
				sbuilder.set("workgroup_code", info.getProperty("workgroup_code"));
				sbuilder.set("workgroup_name", info.getProperty("workgroup_name"));
				sbuilder.set("category", info.getProperty("category"));
				Long cabId = info.getProperty("cabinet_id", Long.class);
				sbuilder.set("cabinet_id", cabId == null ? "": cabId.toString());
			});
			
			list.add( builder.build());
		}
		
		result.setData(pquery == null ? null : pquery.getPagination(), list);
	
		this.sendResult(exchange, result);
		
	}
	

	@WebApi(path="wgroup-task-assigns-query")
	public void handleTaskAvailAssigns(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_FND);
		
		svcctx.addOperationPredicates(params);
		long taskid = Filters.filterLong(params, "task_id");
		String keyword = Filters.filterString(params, "keyword");
		String category = Filters.filterString(params, "category");
		@SuppressWarnings("unchecked")
		Collection<String> state = (Collection<String>)params.get("state");
		PageQuery pquery = Filters.filterPageQuery(params);
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.task.info"));
		
		if(taskid <= 0 ){
			result = ActionResult.failure("miss parameters");
			this.sendResult(exchange, result);
			return ;
		}
		InfoId taskId = IdKeys.getInfoId(NodeIdKey.TASK, taskid);
		Principal princ = svcctx.getPrincipal();
		
		List<WorkgroupInfo> infos = taskExtraService.getAvailableAssigns(princ.getUserId(), taskId, keyword, category, state, pquery);
		List<Object> data = Lists.newArrayList();
		
		data = infos.stream().map((info)->{
			DataBuilder builder = new DataBuilder();
			
			builder.set("workgroup_id", info.getId().toString());
			builder.set("workgroup_code", info.getTraceCode());
			
			builder.set("admin_uid", info.getAdminUid().toString());
			builder.set("manger_uid", info.getManagerUid().toString());
			builder.set("workgroup_name", info.getWorkgroupName());
			
			builder.set(info, "description","state", "publish_scope", "visible_scope");
			builder.set("category", info.getCategory());
			
			
			builder.set("admin", abuilder -> {
				abuilder.set("user_id", info.getAdminUid().toString());
				abuilder.set("user_gid", info.getProperty("admin_gid", String.class));
				abuilder.set("username", info.getProperty("admin_username", String.class));
				abuilder.set("full_name", info.getProperty("admin_full_name", String.class));
			});
			
			builder.set("manager", abuilder -> {
				abuilder.set("user_id", info.getManagerUid().toString());
				abuilder.set("user_gid", info.getProperty("manager_gid", String.class));
				abuilder.set("username", info.getProperty("manager_username", String.class));
				abuilder.set("full_name", info.getProperty("manager_full_name", String.class));
			});
			
			String avatarUrl = ServiceApiHelper.absoluteBinaryUrl(info.getAvatarUrl());
			builder.set("avatar_url", avatarUrl);
			
			builder.set("create_time", info.getCreateTime().getTime());
				
			return builder.build();
			
		}).collect(Collectors.toList());
		
		result = ActionResult.success(getMessage(exchange, "mesg.find.wgroup"));
		result.setData(pquery == null ? null : pquery.getPagination(), data);
 		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="wgroup-task-assign")
	public void handleTaskAssignWorkgroup(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_FND);
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.task.info"));
		
		long taskid = Filters.filterLong(params, "task_id");
		long wgrpid = Filters.filterLong(params, "wgroup_id");
		
		if(taskid <= 0 || wgrpid <= 0){
			result = ActionResult.failure("miss parameters");
			this.sendResult(exchange, result);
			return ;
		}
		InfoId taskId = IdKeys.getInfoId(NodeIdKey.TASK, taskid);
		InfoId wgroupId = IdKeys.getInfoId(NodeIdKey.WORKGROUP, wgrpid);
		
		taskExtraService.assignWorkgroup(svcctx, taskId, wgroupId);
		result = ActionResult.success(getMessage(exchange, "mesg.assign.wgroup"));
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="wgroup-task-unassign")
	public void handleTaskUnassignWorkgroup(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_FND);
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.task.info"));
	
		long taskid = Filters.filterLong(params, "task_id");
		
		if(taskid <= 0){
			result = ActionResult.failure("miss parameters");
			this.sendResult(exchange, result);
			return ;
		}
		InfoId taskId = IdKeys.getInfoId(NodeIdKey.TASK, taskid);
		
		taskExtraService.unassignWorkgroup(svcctx, taskId);
		result = ActionResult.success(getMessage(exchange, "mesg.assign.wgroup"));
		
		this.sendResult(exchange, result);
	}
	
	/**
	 * Get workgroup tasks
	 * 
	 * @param features [ALL, PLAIN, FLOW, BIND, DELIV, ASSIGN, UNASSIGN]
	 **/
	@WebApi(path="phase-tasks-query")
	public void handlePhaseTasksQuery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_FND);		
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.requireId("phase_id")
			.validate(true);
		
		long phaseid = Filters.filterLong(params, "phase_id");
		String keyword = Filters.filterString(params, "keyword");
		
		List<String> features = Filters.filterList(params, "features", String.class);
		features.remove(Filters.ALL);
		List<String> _priorities = Filters.filterList(params, "priorities", String.class);
		List<Integer> priorities = _priorities == null ? null : _priorities.stream().map(p -> {
			Optional<Priority> _p = Enums.getIfPresent(Priority.class, p);
			if(_p.isPresent()) {
				return _p.get().ordinal();
			}
			return -1;
		}).filter(p -> p > 0).collect(Collectors.toList());
		
		InfoId listKey = Filters.filterInfoId(params, "list_id", NodeIdKey.TASK_LIST);
		
		List<String> marks = Filters.filterList(params, "marks", String.class);
		String ownGid = Filters.filterString(params, "owner_gid");
		String asnGid = Filters.filterString(params, "assignee_gid");
		String collGid = Filters.filterString(params, "collab_gid");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.fnd.task"));
		
		Principal princ = svcctx.getPrincipal();
		String dateStr = Filters.filterString(params, "due_time");
		Date dueDate = Strings.isNullOrEmpty(dateStr) ? null: LocalDates.parseMediumDate(princ.getTimeZone(), dateStr, princ.getLocale());
		
		PageQuery pquery = Filters.filterPageQuery(params);
		
		InfoId phaseKey = IdKeys.getInfoId(NodeIdKey.TASK_PHASE, phaseid);
			
		InfoId ownerId = commonService.queryInfoId(ownGid);
		InfoId assignId = commonService.queryInfoId(asnGid);
		InfoId collabId = commonService.queryInfoId(collGid);
		@SuppressWarnings("unchecked")
		Collection<String> state = (Collection<String>)params.get("states");
		
		List<TaskInfo> infos = taskQueryService.getPhaseTasks(svcctx, phaseKey, listKey, ownerId, assignId, collabId, keyword, marks, priorities, features, state, dueDate, pquery);
	
		List<Object> list = new ArrayList<Object>();
		for(TaskInfo info: infos) {
			
			DataBuilder builder = new DataBuilder();
			builder.set("task_id", info.getId().toString());
			builder.set("task_code", info.getTraceCode());
			
			builder.set("list_id", info.getListId() == null ? "": info.getListId().toString());
			builder.set("list_name", info.getProperty("list_name"));
			
			builder.set("workgroup_id", info.getWorkgroupId().toString());
			builder.set("flow_id", info.getFlowId().toString());
			builder.set("task_chronical_id", info.getTaskChronicalId().toString());
			if(null != info.getTaskPid()) {
				builder.set("task_pid", info.getTaskPid().toString());
			}
			builder.set("create_time", info.getCreateTime().getTime());
			
			builder.set("plan_time", info.getPlanTime() != null ? info.getPlanTime().getTime() : 0);
			builder.set("due_time", info.getDueTime() != null ? info.getDueTime().getTime() : 0);
			
			builder.set("start_time", info.getStartTime() != null ? info.getStartTime().getTime() : 0);
			builder.set("end_time", info.getEndTime() != null ? info.getEndTime().getTime() : 0);
				
			
			builder.set(info, "title", "content", "excerpt", "is_constrained",
					"state", "task_opinion", "precedence", "mark", "progress", "priority");
			
			builder.set("reminder_type", info.getReminderType());
			
			builder.set("owner_uid", info.getOwnerUid().toString());
			builder.set("owner", sbuilder -> {
				sbuilder.set("user_gid", info.getProperty("owner_gid"));
				sbuilder.set("username", info.getProperty("owner_username"));
				
				sbuilder.set("full_name", info.getProperty("owner_full_name"));
				sbuilder.set("nickname", info.getProperty("owner_nickname"));
				String avatarUrl = info.getProperty("owner_avatar_url", String.class);
				avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
				sbuilder.set("avatar_url", avatarUrl);
			});
			
			builder.set("assignee_uid", info.getAssigneeUid().toString());
			builder.set("assignee", sbuilder -> {
				sbuilder.set("user_gid", info.getProperty("assign_gid"));
				sbuilder.set("username", info.getProperty("assign_username"));
				
				sbuilder.set("full_name", info.getProperty("assign_full_name"));
				sbuilder.set("nickname", info.getProperty("assign_nickname"));
				String avatarUrl = info.getProperty("assign_avatar_url", String.class);
				avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
				sbuilder.set("avatar_url", avatarUrl);
			});
			
			builder.set("workgroup", sbuilder -> {
				sbuilder.set("workgroup_code", info.getProperty("workgroup_code"));
				sbuilder.set("workgroup_name", info.getProperty("workgroup_name"));
				sbuilder.set("category", info.getProperty("category"));
				Long cabId = info.getProperty("cabinet_id", Long.class);
				sbuilder.set("cabinet_id", cabId == null ? "": cabId.toString());
			});
			
			list.add( builder.build());
		}
		
		result.setData(pquery == null ? null : pquery.getPagination(), list);
	
		this.sendResult(exchange, result);
		
	}
}
