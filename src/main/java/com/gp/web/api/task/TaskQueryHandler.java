/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.task;

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
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.LocalDates;
import com.gp.common.NodeIdKey;
import com.gp.common.Operations;
import com.gp.common.ServiceContext;
import com.gp.common.Tasks.Priority;
import com.gp.dao.info.TaskCheckInfo;
import com.gp.dao.info.TaskInfo;
import com.gp.exception.BaseException;
import com.gp.exception.ServiceException;
import com.gp.info.DataBuilder;
import com.gp.info.Principal;
import com.gp.pagination.PageQuery;
import com.gp.svc.CommonService;
import com.gp.svc.task.TaskCheckService;
import com.gp.svc.task.TaskQueryService;
import com.gp.validate.ArgsValidator;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import com.gp.web.api.ServiceApiHelper;

import io.undertow.server.HttpServerExchange;

public class TaskQueryHandler  extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(TaskQueryHandler.class);
	
	private TaskQueryService taskQueryService;
	private TaskCheckService taskCheckService;
	private CommonService commonService;
	
	public TaskQueryHandler() {
		taskQueryService = BindScanner.instance().getBean(TaskQueryService.class);
		taskCheckService = BindScanner.instance().getBean(TaskCheckService.class);	
		commonService = BindScanner.instance().getBean(CommonService.class);
	}

	@WebApi(path="tasks-query")
	public void handleTasksQuery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_FND);		
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.requireOne("owner_gid", "assignee_gid", "collab_gid")
			.validate(true);
		
		String keyword = Filters.filterString(params, "keyword");
		List<String> features = Filters.filterList(params, "features", String.class);
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
				
		InfoId ownerId = commonService.queryInfoId(ownGid);
		InfoId assignId = commonService.queryInfoId(asnGid);
		InfoId collabId = commonService.queryInfoId(collGid);
		@SuppressWarnings("unchecked")
		Collection<String> state = (Collection<String>)params.get("states");
		
		List<TaskInfo> infos = taskQueryService.getTasks(svcctx, null, listKey, ownerId, assignId, collabId, keyword, marks, priorities, features, state, dueDate, pquery);
	
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
			builder.set("plan_time", info.getPlanTime() == null ? info.getPlanTime().getTime() : 0);
			builder.set("due_time", info.getDueTime() == null ? info.getDueTime().getTime() : 0);
			
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

	@WebApi(path="task-subs-query")
	public void handleTaskSubsQuery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_FND);
		
		svcctx.addOperationPredicates(params);
		long taskid = Filters.filterLong(params, "task_id");
		String taskcode = Filters.filterString(params, "task_code");
		String keyword = Filters.filterString(params, "keyword");
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.fnd.task"));
		
		InfoId taskId = IdKeys.getInfoId(NodeIdKey.TASK, taskid);
		if(!IdKeys.isValidId(taskId)) {
			taskId = commonService.queryInfoId(taskcode);
		}
		
		@SuppressWarnings("unchecked")
		Collection<String> state = (Collection<String>)params.get("state");
		
		List<TaskInfo> infos = taskQueryService.getTaskSubs(svcctx, taskId, keyword, state, null);
	
		List<Object> list = new ArrayList<Object>();
		for(TaskInfo info: infos) {
			
			DataBuilder builder = new DataBuilder();
			builder.set("task_id", info.getId().toString());
			builder.set("task_code", info.getTraceCode());
			
			builder.set("workgroup_id", info.getWorkgroupId().toString());
			builder.set("flow_id", info.getFlowId().toString());
			builder.set("task_chronical_id", info.getTaskChronicalId().toString());
			if(null != info.getTaskPid()) {
				builder.set("task_pid", info.getTaskPid().toString());
			}
			builder.set("plan_time", info.getPlanTime() == null ? info.getPlanTime().getTime() : 0);
			builder.set("due_time", info.getDueTime() == null ? info.getDueTime().getTime() : 0);
			
			builder.set("start_time", info.getStartTime() != null ? info.getStartTime().getTime() : 0);
			builder.set("end_time", info.getEndTime() != null ? info.getEndTime().getTime() : 0);
			
			
			builder.set(info, "title",  "content", "excerpt",
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
		
		result.setData(list);
	
		this.sendResult(exchange, result);
		
	}
		
	@WebApi(path="task-checks-query")
	public void handleTaskChecksQuery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.CHK_FND);		
		svcctx.addOperationPredicates(params);
		
		InfoId targetKey = Filters.filterInfoId(params, "target_id", NodeIdKey.TASK);
		PageQuery pquery = Filters.filterPageQuery(params);
		
		List<TaskCheckInfo> infos = null;
		
		if(IdKeys.isValidId(targetKey)) {
			
			infos = taskCheckService.getTaskChecks(svcctx, targetKey);
		}else {
			
			String keyword = Filters.filterString(params, "keyword");
			List<String> features = Filters.filterList(params, "features", String.class);
			
			List<String> marks = Filters.filterList(params, "marks", String.class);
			String ownGid = Filters.filterString(params, "owner_gid");
			String asnGid = Filters.filterString(params, "assignee_gid");
			
			Principal princ = svcctx.getPrincipal();
			String dateStr = Filters.filterString(params, "due_time");
			Date dueDate = Strings.isNullOrEmpty(dateStr) ? null: LocalDates.parseMediumDate(princ.getTimeZone(), dateStr, princ.getLocale());
			
			long wgid = Filters.filterLong(params, "wgroup_id");
			String wcode = Filters.filterString(params, "wgroup_code");
								
			InfoId ownerId = commonService.queryInfoId(ownGid);
			InfoId assignId = commonService.queryInfoId(asnGid);
			InfoId gid = IdKeys.getInfoId(NodeIdKey.WORKGROUP, wgid);
			if(!IdKeys.isValidId(gid)) {
				gid = commonService.queryInfoId(wcode);
			}
			@SuppressWarnings("unchecked")
			Collection<String> states = (Collection<String>)params.get("states");
			
			if(!IdKeys.isValidId(ownerId) && !IdKeys.isValidId(assignId) && !IdKeys.isValidId(gid)) {
				throw new ServiceException("excp.miss.param" );
			}
			infos = taskCheckService.getTaskChecks(svcctx, gid, ownerId, assignId, keyword, marks, features, states, dueDate, pquery);
		}
		
		List<Object> data = infos.stream().map(info -> {
			DataBuilder builder = new DataBuilder();
			builder.set("check_id", info.getId().toString());
			builder.set("check_code", info.getTraceCode());
			builder.set("target_id", info.getTargetId().toString());
			
			builder.set(info, "title", "content", "excerpt", "state", "mark", "task_progress");
			builder.set("check_order", info.getCheckOrder());
			
			builder.set("plan_time", info.getPlanTime() != null ? info.getPlanTime().getTime() : 0);
			builder.set("due_time", info.getDueTime() != null ? info.getDueTime().getTime() : 0);
			
			builder.set("start_time", info.getStartTime() != null ? info.getStartTime().getTime() : 0);
			builder.set("end_time", info.getEndTime() != null ? info.getEndTime().getTime() : 0);
			
			builder.set("create_time", info.getCreateTime().getTime());
			
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
			
			return builder.build();
		}).collect(Collectors.toList());
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.checks"));

		result.setData(pquery == null ? null : pquery.getPagination(), data);
		
		this.sendResult(exchange, result);
	}
	
}
