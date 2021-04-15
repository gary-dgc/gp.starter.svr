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
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Enums;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.primitives.Ints;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.LocalDates;
import com.gp.common.NodeIdKey;
import com.gp.common.Operations;
import com.gp.common.ServiceContext;
import com.gp.common.Tasks;
import com.gp.common.Tasks.Priority;
import com.gp.common.Tasks.ProgressCalc;
import com.gp.common.Tasks.State;
import com.gp.dao.info.TaskInfo;
import com.gp.dao.info.TaskUserInfo;
import com.gp.dao.info.WorkgroupInfo;
import com.gp.exception.BaseException;
import com.gp.exception.ServiceException;
import com.gp.info.BaseIdKey;
import com.gp.info.DataBuilder;
import com.gp.info.Principal;
import com.gp.info.TraceableInfo;
import com.gp.pagination.PageQuery;
import com.gp.svc.CommonService;
import com.gp.svc.task.TaskActionService;
import com.gp.svc.task.TaskExtraService;
import com.gp.svc.task.TaskService;
import com.gp.util.NumberUtils;
import com.gp.validate.ArgsValidator;
import com.gp.validate.ValidateMessage;
import com.gp.validate.ValidateUtils;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import com.gp.web.api.ServiceApiHelper;

import io.undertow.server.HttpServerExchange;

public class TaskHandler extends BaseApiSupport{

	static final int MAX_LEN = 256;
	
	static Logger LOGGER = LoggerFactory.getLogger(TaskHandler.class);
	
	private TaskService taskService;
	private TaskExtraService taskExtraService;
	private TaskActionService taskActionService;
	private CommonService commonService;

	
	public TaskHandler() {
		
		taskService = BindScanner.instance().getBean(TaskService.class);
		taskExtraService = BindScanner.instance().getBean(TaskExtraService.class);
		taskActionService = BindScanner.instance().getBean(TaskActionService.class);
		commonService = BindScanner.instance().getBean(CommonService.class);
	
	}
	
	@WebApi(path="task-info")
	public void handleTaskInfo(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_FND);
		ArgsValidator.newValidator(params)
			.requireOne("task_id", "task_code")
			.validate(true);
		
		svcctx.addOperationPredicates(params);
		long taskid = Filters.filterLong(params, "task_id");
		String taskcode = Filters.filterString(params, "task_code");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.fnd.task"));
		
		InfoId taskKey = null;
		if(taskid > 0) {
			taskKey = IdKeys.getInfoId(NodeIdKey.TASK, taskid);
		}else {
			taskKey = commonService.queryInfoId(taskcode);
		}
		TaskInfo info = taskService.getTask(svcctx, taskKey);
	
		if(null != info) {
			
			DataBuilder builder = new DataBuilder();
			builder.set("task_id", info.getId().toString());
			builder.set("task_code", info.getTraceCode());
			
			builder.set("workgroup_id", info.getWorkgroupId().toString());
			builder.set("flow_id", info.getFlowId().toString());
			builder.set("task_chronical_id", info.getTaskChronicalId().toString());
			if(null != info.getTaskPid()) {
				builder.set("task_pid", info.getTaskPid().toString());
			}
			builder.set("plan_time", info.getPlanTime() != null ? info.getPlanTime().getTime() : 0);
			builder.set("due_time", info.getDueTime() != null ? info.getDueTime().getTime() : 0);
			
			builder.set("start_time", info.getStartTime() != null ? info.getStartTime().getTime() : 0);
			builder.set("end_time", info.getEndTime() != null ? info.getEndTime().getTime() : 0);
			
			builder.set(info, "title", "content", "excerpt", "is_constrained", "progress_calc",
					"state", "task_opinion", "precedence", "mark", "progress", "priority",
					"reminder_before");
			
			List<String> type = Splitter.on(',').splitToList(info.getReminderType());
			builder.set("reminder_type", type);
			
			builder.set("list_id", info.getListId() != null ? info.getListId().toString() : "");
			
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
			result.setData(builder.build());
		}
		
		this.sendResult(exchange, result);
		
	}
	
	@WebApi(path="task-members-query")
	public void handleTaskUsersQuery(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_FND);
		ArgsValidator.newValidator(params)
			.requireOne("task_id", "task_code")
			.validate(true);
		svcctx.addOperationPredicates(params);
		
		long taskid = Filters.filterLong(params, "task_id");
		String taskcode = Filters.filterString(params, "task_code");
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.users"));
			
		InfoId taskKey = null;
		if(taskid > 0) {
			
			taskKey = IdKeys.getInfoId(NodeIdKey.TASK, taskid);
		} else {
			
			taskKey = commonService.queryInfoId(taskcode);
		}
		
		List<TaskUserInfo> infos = taskService.getTaskMembers(svcctx, taskKey);
		
		List<Object> data = infos.stream().map(info -> {
			DataBuilder builder = new DataBuilder();
			
			builder.set("member_uid", info.getAttendeeUid().toString());
			builder.set("user_gid", info.getProperty("assign_gid"));
			builder.set("username", info.getProperty("assign_username"));
			
			builder.set("full_name", info.getProperty("assign_full_name"));
			builder.set("nickname", info.getProperty("assign_nickname"));
			String avatarUrl = info.getProperty("assign_avatar_url", String.class);
			avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
			builder.set("avatar_url", avatarUrl);
			
			return builder.build();
			
		}).collect(Collectors.toList());
		
		result.setData(data);
		
		this.sendResult(exchange, result);
	}
	
	/**
	 * Create new task record, [task_pid, workgroup_id, owner_uid] are key predicates to trigger
	 * Operation statistics to work on workgroup, task and user
	 * 
	 **/
	@SuppressWarnings("unchecked")
	@WebApi(path="task-add")
	public void handleTaskAdd(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_NEW);
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.require("plan_time", "due_time", "title", "content", "excerpt")
			.validate(true);
		
		long wgid = Filters.filterLong(params, "wgroup_id");
		String wgroupCode = Filters.filterString(params, "wgroup_code");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.task.add"));

		if(wgid <= 0 && !Strings.isNullOrEmpty(wgroupCode)) {
			InfoId wgrpId = commonService.queryInfoId(wgroupCode);
			wgid = wgrpId.getId();
		}
		
		TaskInfo task = new TaskInfo();
		task.setWorkgroupId(wgid);
		task.setTaskPid(Filters.filterLong(params, "task_pid"));
		task.setTitle(Filters.filterString(params, "title"));
	
		task.setContent(Filters.filterString(params, "content"));
		task.setExcerpt(Filters.filterString(params, "excerpt"));
		
		// extra properties
		task.setFlowId(Filters.filterLong(params, "flow_id"));
		task.setListId(Filters.filterLong(params, "list_id"));
		
		String priorityStr = Filters.filterString(params, "priority");
		Optional<Priority> opt = Enums.getIfPresent(Priority.class, priorityStr)
				.or(Optional.of(Priority.NORMAL));
		
		task.setPriority(opt.get().ordinal());
		
		// prepare the reminder setting
		if(params.containsKey("reminder_type")) {
			Object types = params.get("reminder_type");
			if(types.getClass().equals(String.class)) {
				task.setReminderType((String)types);
			}else {
				Collection<String> coll = (Collection<String>)types;
				task.setReminderType(Joiner.on(',').join(coll));
			}
			
			task.setReminderBefore(Filters.filterInt(params, "reminder_before"));
		}
		
		// prepare the attachments
		if(params.containsKey("attachs") && params.get("attachs") != null) {
		
			List<Map<String, Object>> list = Lists.newArrayList();
			for(Map<String, String> attach: (Collection<Map<String, String>>)params.get("attachs")) {
				Map<String, Object> item = Maps.newHashMap();
				item.put("file_name", attach.get("file_name"));
				item.put("binary_id", NumberUtils.toLong(attach.get("binary_id")));
				
				list.add(item);
			}
			task.setProperty("attachs", list);
		}
		
		task.setState(Tasks.State.PENDING.name());
		task.setProgressCalc(Filters.filterString(params, "progress_calc", ProgressCalc.MANUAL.name()));
		
		Principal princ = svcctx.getPrincipal();
		
		String dateStr = Filters.filterString(params, "due_time");
		if(!Strings.isNullOrEmpty(dateStr)) {
			task.setDueTime(LocalDates.parseMediumDate(princ.getTimeZone(), dateStr, princ.getLocale()));
		}
		String scheduleStr = Filters.filterString(params, "plan_time");
		if(!Strings.isNullOrEmpty(scheduleStr)) {
			task.setPlanTime(LocalDates.parseMediumDate(princ.getTimeZone(), scheduleStr, princ.getLocale()));
		}
		task.setOwnerUid(princ.getUserId().getId());
		
		long assignUid = Filters.filterLong(params, "assignee_uid");
		task.setAssigneeUid(( assignUid <= 0 ) ? princ.getUserId().getId() : assignUid);
		
		if(params.containsKey("attendee_uids")) {
			Collection<String> collIds = (Collection<String>)params.get("attendee_uids");
			if(collIds != null) {
				List<InfoId> infoIds = collIds.stream().map(cid -> {
					return IdKeys.getInfoId(BaseIdKey.USER, NumberUtils.toLong(cid));
				}).collect(Collectors.toList());
				
				task.setProperty("attendee_uids", infoIds);
			}
		}
		
		task.setProperty("mark", Filters.filterString(params, "mark"));

		taskService.newTask(svcctx, task);
		
		svcctx.setOperationObject(task.getInfoId());
		svcctx.addOperationPredicate("workgroup_id", wgid);
		
		Map<String, Object> data = Maps.newHashMap();
		data.put("task_id", task.getId().toString());
		result.setData(data);
		
		this.sendResult(exchange, result);
		
	}
	
	@SuppressWarnings("unchecked")
	@WebApi(path="task-save")
	public void handleTaskSave(HttpServerExchange exchange) throws BaseException {

		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_UPD);
		
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.requireId("task_id")
			.validate(true);
		
		long taskid = Filters.filterLong(params, "task_id");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.task.save"));
				
		TaskInfo task = new TaskInfo();
		task.setInfoId(IdKeys.getInfoId(NodeIdKey.TASK, taskid));
		task.setTitle(Filters.filterString(params, "title"));
	
		task.setContent(Filters.filterString(params, "content"));
		task.setExcerpt(Filters.filterString(params, "excerpt"));
		
		String priorityStr = Filters.filterString(params, "priority");
		if(Ints.tryParse(priorityStr) == null) {
			Optional<Priority> opt = Enums.getIfPresent(Priority.class, priorityStr)
					.or(Optional.of(Priority.NORMAL));
			
			task.setPriority(opt.get().ordinal());
		}else {
			
			task.setPriority(Ints.tryParse(priorityStr));
		}
		if(params.containsKey("reminder_type")) {
			Object types = params.get("reminder_type");
			if(types.getClass().equals(String.class)) {
				task.setReminderType((String)types);
			}else {
				Collection<String> coll = (Collection<String>)types;
				task.setReminderType(Joiner.on(',').join(coll));
			}
			task.setReminderBefore(Filters.filterInt(params, "reminder_before"));
		}
	
		Principal princ = svcctx.getPrincipal();
		String scheduleStr = Filters.filterString(params, "plan_time");
		if(!Strings.isNullOrEmpty(scheduleStr)) {
			task.setPlanTime(LocalDates.parseMediumDate(princ.getTimeZone(), scheduleStr, princ.getLocale()));
		}
		String dateStr = Filters.filterString(params, "due_time");
		task.setDueTime(LocalDates.parseMediumDate(princ.getTimeZone(), dateStr, princ.getLocale()));
				
		task.setProperty("mark", Filters.filterString(params, "mark"));
		
		Set<String> keys = Sets.newHashSet("title", "content", "excerpt",
				"reminder_type", "reminder_before", "plan_time", "due_time");
		// check the validation of user information
		Set<ValidateMessage> vmsg = ValidateUtils.validateProperty(princ.getLocale(), task, keys.toArray(new String[0]));
		if(!Iterables.isEmpty(vmsg)){ // fail pass validation
			ServiceException svcexcp = new ServiceException(svcctx.getPrincipal().getLocale(), "excp.validate");
			svcexcp.addValidateMessages(vmsg);
			throw svcexcp;
		}
		
		task.setPropertyFilter(keys);
		taskService.updateTask(svcctx, task);
		
		svcctx.setOperationObject(task.getInfoId());
		
		this.sendResult(exchange, result);
		
	}
	
	@SuppressWarnings("unchecked")
	@WebApi(path="task-update")
	public void handleTaskUpdate(HttpServerExchange exchange) throws BaseException {

		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_UPD);
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.requireOne("task_id", "task_code")
			.validate(true);
		long taskid = Filters.filterLong(params, "task_id");
		
		InfoId taskKey = IdKeys.getInfoId(NodeIdKey.TASK, taskid);
		if(!IdKeys.isValidId(taskKey)) {
			String taskcode = Filters.filterString(params, "task_code");
			taskKey = commonService.queryInfoId(taskcode);
		}
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.task.save"));
			
		Set<String> keys = Sets.newHashSet();
		TaskInfo task = new TaskInfo();
		task.setInfoId(taskKey);
		
		if(params.containsKey("mark")) {
			task.setProperty("mark", Filters.filterString(params, "mark"));
		}
		if(params.containsKey("title")) {
			keys.add("title");
			task.setTitle(Filters.filterString(params, "title"));
		}
		
		if(params.containsKey("list_id")) {
			keys.add("list_id");
			task.setListId(Filters.filterLong(params, "list_id"));
		}
		if(params.containsKey("content")) {
			keys.add("content");
			task.setContent(Filters.filterString(params, "content"));
			
			keys.add("excerpt");
			task.setExcerpt(Filters.filterString(params, "excerpt"));
		}
		if(params.containsKey("reminder_type")) {
			keys.add("reminder_type");
			Object types = params.get("reminder_type");
			if(types.getClass().equals(String.class)) {
				task.setReminderType((String)types);
			}else {
				Collection<String> coll = (Collection<String>)types;
				task.setReminderType(Joiner.on(',').join(coll));
			}
		}
		
		if(params.containsKey("reminder_before")) {
			keys.add("reminder_before");
			task.setReminderBefore(Filters.filterInt(params, "reminder_before"));
		}
		
		if(params.containsKey("progress_calc")) {
			keys.add("progress_calc");
			task.setProgressCalc(Filters.filterString(params, "progress_calc"));
		}
		
		if(params.containsKey("priority")) {
			keys.add("priority");
			String priorityStr = Filters.filterString(params, "priority");
			if(Ints.tryParse(priorityStr) == null) {
				Optional<Priority> opt = Enums.getIfPresent(Priority.class, priorityStr)
						.or(Optional.of(Priority.NORMAL));
				
				task.setPriority(opt.get().ordinal());
			}else {
				
				task.setPriority(Ints.tryParse(priorityStr));
			}
		}
		
		if(params.containsKey("state")) {
			keys.add("state");
			task.setState(Filters.filterString(params, "state"));
		}
		
		// change task progress
		if(params.containsKey("progress")) {
			keys.remove("progress");
			
			taskActionService.changeTaskProgress(svcctx, taskKey, Filters.filterInt(params, "progress"));
		}
		Principal princ = svcctx.getPrincipal();
		if(params.containsKey("due_time")) {
			keys.add("due_time");
			String dateStr = Filters.filterString(params, "due_time");
			task.setDueTime(LocalDates.parseMediumDate(princ.getTimeZone(), dateStr, princ.getLocale()));
		}
		if(params.containsKey("plan_time")) {
			keys.add("plan_time");
			String dateStr = Filters.filterString(params, "plan_time");
			task.setPlanTime(LocalDates.parseMediumDate(princ.getTimeZone(), dateStr, princ.getLocale()));
		}
		
		if(params.containsKey("start_time")) {
			keys.add("start_time");
			String dateStr = Filters.filterString(params, "start_time");
			task.setStartTime(LocalDates.parseMediumDate(princ.getTimeZone(), dateStr, princ.getLocale()));
		}
		if(params.containsKey("end_time")) {
			keys.add("end_time");
			String dateStr = Filters.filterString(params, "end_time");
			task.setEndTime(LocalDates.parseMediumDate(princ.getTimeZone(), dateStr, princ.getLocale()));
		}
		
		if(params.containsKey("owner_uid")) {
			keys.add("owner_uid");
			task.setOwnerUid(Filters.filterLong(params, "owner_uid"));
		}
		if(params.containsKey("assignee_uid")) {
			keys.add("assignee_uid");
			task.setAssigneeUid(Filters.filterLong(params, "assignee_uid"));
		}
		if(params.containsKey("is_constrained")) {
			keys.add("is_constrained");
			task.setIsConstrained(Filters.filterBoolean(params, "is_constrained"));
		}
		
		task.setPropertyFilter(keys);
		
		taskService.updateTask(svcctx, task);
		svcctx.setOperationObject(task.getInfoId());
		
		this.sendResult(exchange, result);
		
	}

	@WebApi(path="task-remove")
	public void handleTaskRemove(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_FND);
		
		ArgsValidator.newValidator(params)
			.requireOne("task_id", "task_code")
			.validate(true);
		
		svcctx.addOperationPredicates(params);

		long taskid = Filters.filterLong(params, "task_id");
		
		InfoId taskKey = IdKeys.getInfoId(NodeIdKey.TASK, taskid);
		if(!IdKeys.isValidId(taskKey)) {
			String taskcode = Filters.filterString(params, "task_code");
			taskKey = commonService.queryInfoId(taskcode);
		}
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.task.info"));
		
		Long wgrpid = commonService.queryColumn(taskKey, "workgroup_id", Long.class);
		svcctx.addOperationPredicate("workgroup_id", wgrpid);
		Long ownerid = commonService.queryColumn(taskKey, "owner_uid", Long.class);
		svcctx.addOperationPredicate("owner_uid", ownerid);
		svcctx.setOperationObject(taskKey);
		
		taskService.removeTask(svcctx, taskKey);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-summary")
	public void handleTaskSummaryInfo(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_FND);
		ArgsValidator.newValidator(params)
			.requireOne("task_id", "task_code")
			.validate(true);
		svcctx.addOperationPredicates(params);
		
		long taskid = Filters.filterLong(params, "task_id");		
		InfoId taskKey = IdKeys.getInfoId(NodeIdKey.TASK, taskid);
		if(!IdKeys.isValidId(taskKey)) {
			String taskcode = Filters.filterString(params, "task_code");
			taskKey = commonService.queryInfoId(taskcode);
		}
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.task.info"));
		
		TraceableInfo info = taskExtraService.getTaskSummary(taskKey);
		
		result.setData(info.toMap("attach", "bind", "deliv", "flow", "task", "task_id"));
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-member-add")
	public void handleTaskCollabAdd(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_UPD);
		ArgsValidator.newValidator(params)
			.requireOne("task_id", "task_code")
			.require("member").list("member")
			.validate(true);
		svcctx.addOperationPredicates(params);
		long taskid = Filters.filterLong(params, "task_id");		
		InfoId taskKey = IdKeys.getInfoId(NodeIdKey.TASK, taskid);
		if(!IdKeys.isValidId(taskKey)) {
			String taskcode = Filters.filterString(params, "task_code");
			taskKey = commonService.queryInfoId(taskcode);
		}
		List<String> collabs = Filters.filterList(params, "member", String.class);
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.task.info"));
	
		for(String mbrId : collabs) {
			InfoId uid = IdKeys.getInfoId(BaseIdKey.USER, NumberUtils.toLong(mbrId));
			taskService.addTaskMember(svcctx, taskKey, uid);
		}
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-member-remove")
	public void handleTaskCollabRemove(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_UPD);
		ArgsValidator.newValidator(params)
			.requireOne("task_id", "task_code")
			.require("member").list("member")
			.validate(true);
		
		svcctx.addOperationPredicates(params);
		
		long taskid = Filters.filterLong(params, "task_id");		
		InfoId taskKey = IdKeys.getInfoId(NodeIdKey.TASK, taskid);
		if(!IdKeys.isValidId(taskKey)) {
			String taskcode = Filters.filterString(params, "task_code");
			taskKey = commonService.queryInfoId(taskcode);
		}
		List<String> collabs = Filters.filterList(params, "member", String.class);
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.task.info"));
		
		for(String mbrId : collabs) {
			InfoId uid = IdKeys.getInfoId(BaseIdKey.USER, NumberUtils.toLong(mbrId));
			taskService.removeTaskMember(taskKey, uid);
		}
		
		this.sendResult(exchange, result);
	}
	
}
