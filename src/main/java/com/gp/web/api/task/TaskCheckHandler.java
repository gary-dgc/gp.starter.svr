/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.task;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.LocalDates;
import com.gp.common.NodeIdKey;
import com.gp.common.Operations;
import com.gp.common.ServiceContext;
import com.gp.common.Tasks;
import com.gp.common.Tasks.State;
import com.gp.dao.info.TaskCheckInfo;
import com.gp.dao.info.TaskTmplInfo;
import com.gp.exception.BaseException;
import com.gp.exception.WebException;
import com.gp.info.DataBuilder;
import com.gp.info.Principal;
import com.gp.info.TraceableInfo;
import com.gp.svc.CommonService;
import com.gp.svc.task.TaskCheckService;
import com.gp.validate.ArgsValidator;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.FilterHandler;
import com.gp.web.anno.WebApi;
import com.gp.web.api.ServiceApiHelper;
import com.gp.web.api.wgroup.WGroupExtHandler;

import io.undertow.server.HttpServerExchange;

public class TaskCheckHandler extends BaseApiSupport {

	static Logger LOGGER = LoggerFactory.getLogger(WGroupExtHandler.class);
	
	private CommonService commonService;
	private TaskCheckService taskCheckService;
	
	public TaskCheckHandler() {
		commonService = BindScanner.instance().getBean(CommonService.class);
		taskCheckService = BindScanner.instance().getBean(TaskCheckService.class);		
	}

	@WebApi(path="task-tmpls-query")
	public void handleTaskTmplsQuery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_FND);		
		svcctx.addOperationPredicates(params);
		
		String wgrpCode = Filters.filterString(params, "wgroup_code");
				
		InfoId wgrpKey = Filters.filterInfoId(params, "wgroup_id", NodeIdKey.WORKGROUP);
		if(!Strings.isNullOrEmpty(wgrpCode)) {
			wgrpKey = commonService.queryInfoId(wgrpCode);
		}
		List<TaskTmplInfo> infos = taskCheckService.getTaskTemplates(wgrpKey);
		
		List<Object> data = infos.stream().map(info -> {
			DataBuilder builder = new DataBuilder();
			builder.set("tmpl_id", info.getId().toString());
			builder.set(info, "tmpl_name", "description",  "progress_calc");
			Long fid = info.getFlowId();
			if(null != fid) {
				builder.set("flow_id", fid.toString());
			}else {
				builder.set("flow_id", "");
			}
			return builder.build();
		}).collect(Collectors.toList());
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.cates"));

		result.setData(data);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-tmpl-add")
	public void handleTaskTmplAdd(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_NEW);		
		svcctx.addOperationPredicates(params);
	
		ArgsValidator.newValidator(params)
			.require("tmpl_name")
			.validate(true);
		
		String wgrpCode = Filters.filterString(params, "wgroup_code");
		
		InfoId wgrpKey = Filters.filterInfoId(params, "wgroup_id", NodeIdKey.WORKGROUP);
		if(!Strings.isNullOrEmpty(wgrpCode)) {
			wgrpKey = commonService.queryInfoId(wgrpCode);
		}
		
		TaskTmplInfo info = new TaskTmplInfo();
			
		info.setWorkgroupId(wgrpKey.getId());
		info.setTmplName(Filters.filterString(params, "tmpl_name"));
		info.setDescription(Filters.filterString(params, "description"));
		info.setProgressCalc(Filters.filterString(params, "progress_calc"));
		info.setFlowId(Filters.filterLong(params, "flow_id"));
		
		InfoId cKey = taskCheckService.newTaskTemplate(svcctx, info);
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.new.task.tmpl"));
		
		Map<String, Object> data = Maps.newHashMap();
		data.put("tmpl_id", cKey.getId().toString());
		result.setData(data);
		
		// add object key to service context
		svcctx.setOperationObject(info.getInfoId());
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-tmpl-save")
	public void handleTaskTmplSave(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_UPD);		
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.requireId("tmpl_id")
			.require("tmpl_name")
			.validate(true);
		
		InfoId cateKey = Filters.filterInfoId(params, "tmpl_id", NodeIdKey.TASK_TMPL);
	
		TaskTmplInfo info = new TaskTmplInfo();
		
		info.setInfoId(cateKey);
		info.setTmplName(Filters.filterString(params, "tmpl_name"));
		info.setDescription(Filters.filterString(params, "description"));
		info.setProgressCalc(Filters.filterString(params, "progress_calc"));
		info.setFlowId(Filters.filterLong(params, "flow_id"));
		
		Collection<String> keys = Filters.filterKeys(params, 
				"tmpl_name", "description", "progress_calc", "flow_id");
		info.setPropertyFilter(keys);
		
		taskCheckService.updateTaskTemplate(svcctx, info);
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.save.cate"));
		
		// add object key to service context
		svcctx.setOperationObject(info.getInfoId());
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path = "task-tmpl-remove")
	public void handleTaskTmplRemove(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_RMV);		
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.requireId("tmpl_id")
			.validate(true);
		
		InfoId cateKey = Filters.filterInfoId(params, "tmpl_id", NodeIdKey.TASK_TMPL);
	
		taskCheckService.removeTaskTemplate(cateKey);
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.save.cate"));
		
		// add object key to service context
		svcctx.setOperationObject(cateKey);
				
		this.sendResult(exchange, result);
	}	
	
	@WebApi(path="task-check-info")
	public void handleTaskCheckInfo(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.CHK_FND);
		
		svcctx.addOperationPredicates(params);
		long checkid = Filters.filterLong(params, "check_id");
		String checkcode = Filters.filterString(params, "check_code");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.fnd.task"));
		
		InfoId checkKey = null;
		if(checkid > 0) {
			checkKey = IdKeys.getInfoId(NodeIdKey.TASK_CHECK, checkid);
		}else {
			checkKey = commonService.queryInfoId(checkcode);
		}
		TaskCheckInfo info = taskCheckService.getTaskCheck(svcctx, checkKey);
		if(null != info) {
			
			DataBuilder builder = new DataBuilder();
			builder.set("check_id", info.getId().toString());
			builder.set("check_code", info.getTraceCode());
			builder.set("target_id", info.getTargetId().toString());
			
			builder.set(info, "title", "content", "excerpt",  "state", "mark", "task_progress");
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
			result.setData(builder.build());
		}
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-check-add")
	public void handleTaskCheckAdd(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.CHK_NEW);		
		svcctx.addOperationPredicates(params);
	
		ArgsValidator.newValidator(params)
			.require("title")
			.requireId("target_id")
			.requireOne("wgroup_id", "wgroup_code")
			.validate(true);
		
		String wgrpCode = Filters.filterString(params, "wgroup_code");
		
		InfoId wgrpKey = Filters.filterInfoId(params, "wgroup_id", NodeIdKey.WORKGROUP);
		if(!Strings.isNullOrEmpty(wgrpCode)) {
			wgrpKey = commonService.queryInfoId(wgrpCode);
		}
		
		TaskCheckInfo info = new TaskCheckInfo();
			
		info.setWorkgroupId(wgrpKey.getId());
		info.setTargetId(Filters.filterLong(params, "target_id"));
		info.setTitle(Filters.filterString(params, "title"));
		info.setContent(Filters.filterString(params, "content"));
		info.setExcerpt(Filters.filterString(params, "excerpt"));
		info.setTaskProgress(Filters.filterInt(params, "task_progress"));
		info.setState(Tasks.State.PENDING.name());
		info.setAssigneeUid(Filters.filterLong(params, "assignee_uid"));
		
		Principal princ = svcctx.getPrincipal();
		String scheduleStr = Filters.filterString(params, "plan_time");
		if(!Strings.isNullOrEmpty(scheduleStr)) {
			info.setPlanTime(LocalDates.parseMediumDate(princ.getTimeZone(), scheduleStr, princ.getLocale()));
		}
		String dateStr = Filters.filterString(params, "due_time");
		if(!Strings.isNullOrEmpty(dateStr)) {
			info.setDueTime(LocalDates.parseMediumDate(princ.getTimeZone(), dateStr, princ.getLocale()));
		}
		
		info.setOwnerUid(princ.getUserId().getId());
		info.setCreateTime(LocalDates.now());
		
		InfoId cKey = taskCheckService.newTaskCheck(svcctx, info);
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.new.check"));
		
		Map<String, Object> data = Maps.newHashMap();
		data.put("check_id", cKey.getId().toString());
		result.setData(data);
		
		// Set object key to service context
		svcctx.setOperationObject(info.getInfoId());
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-check-save")
	public void handleTaskCheckSave(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.CHK_UPD);		
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.requireId("check_id")
			.validate(true);
		
		InfoId cateKey = Filters.filterInfoId(params, "check_id", NodeIdKey.TASK_CHECK);
	
		TaskCheckInfo info = new TaskCheckInfo();
		info.setInfoId(cateKey);
		info.setWorkgroupId(Filters.filterLong(params, "workgroup_id"));
		info.setTargetId(Filters.filterLong(params, "target_id"));
		info.setTitle(Filters.filterString(params, "title"));
		info.setContent(Filters.filterString(params, "content"));
		info.setTaskProgress(Filters.filterInt(params, "task_progress"));
		info.setState(Filters.filterString(params, "state"));
		
		Principal princ = svcctx.getPrincipal();
		String scheduleStr = Filters.filterString(params, "plan_time");
		if(!Strings.isNullOrEmpty(scheduleStr)) {
			info.setPlanTime(LocalDates.parseMediumDate(princ.getTimeZone(), scheduleStr, princ.getLocale()));
		}
		String dateStr = Filters.filterString(params, "due_time");
		if(!Strings.isNullOrEmpty(dateStr)) {
			info.setDueTime(LocalDates.parseMediumDate(princ.getTimeZone(), dateStr, princ.getLocale()));
		}
		String startStr = Filters.filterString(params, "start_time");
		if(!Strings.isNullOrEmpty(startStr)) {
			info.setStartTime(LocalDates.parseMediumDate(princ.getTimeZone(), startStr, princ.getLocale()));
		}
		String endStr = Filters.filterString(params, "end_time");
		if(!Strings.isNullOrEmpty(endStr)) {
			info.setEndTime(LocalDates.parseMediumDate(princ.getTimeZone(), endStr, princ.getLocale()));
		}
		info.setOwnerUid(Filters.filterLong(params, "owner_uid"));
		info.setAssigneeUid(Filters.filterLong(params, "assignee_uid"));
		
		Collection<String> keys = Filters.filterKeys(params, 
				"workgroup_id", "target_id", "title", "content", "task_progress",
				"state", "plan_time", "due_time", "start_time", "end_time",
				"owner_uid", "assignee_uid");
		
		info.setPropertyFilter(keys);
		
		taskCheckService.updateTaskCheck(svcctx, info);
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.save.check"));
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-check-update")
	public void handleTaskCheckUpdate(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.CHK_UPD);		
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.requireId("check_id")
			.validate(true);
		
		InfoId checkKey = Filters.filterInfoId(params, "check_id", NodeIdKey.TASK_CHECK);
	
		Set<String> keys = Sets.newHashSet();
		TaskCheckInfo info = new TaskCheckInfo();
		info.setInfoId(checkKey);
		if(params.containsKey("mark")) {
			info.setProperty("mark", Filters.filterString(params, "mark"));
		}
		
		if(params.containsKey("target_id")) {
			keys.add("target_id");
			info.setTargetId(Filters.filterLong(params, "target_id"));
		}
		
		if(params.containsKey("title")) {
			keys.add("title");
			info.setTitle(Filters.filterString(params, "title"));
		}
		
		if(params.containsKey("content")) {
			keys.add("content");
			info.setContent(Filters.filterString(params, "content"));
			
			keys.add("excerpt");
			info.setExcerpt(Filters.filterString(params, "excerpt"));
		}
		
		if(params.containsKey("task_progress")) {
			keys.add("task_progress");
			info.setTaskProgress(Filters.filterInt(params, "task_progress"));
		}
		
		if(params.containsKey("state")) {
			keys.add("state");
			info.setState(Filters.filterString(params, "state"));
		}
		
		Principal princ = svcctx.getPrincipal();
		if(params.containsKey("due_time")) {
			keys.add("due_time");
			String dateStr = Filters.filterString(params, "due_time");
			info.setDueTime(LocalDates.parseMediumDate(princ.getTimeZone(), dateStr, princ.getLocale()));
		}
		if(params.containsKey("plan_time")) {
			keys.add("plan_time");
			String dateStr = Filters.filterString(params, "plan_time");
			info.setPlanTime(LocalDates.parseMediumDate(princ.getTimeZone(), dateStr, princ.getLocale()));
		}
		
		if(params.containsKey("start_time")) {
			keys.add("start_time");
			String dateStr = Filters.filterString(params, "start_time");
			info.setStartTime(LocalDates.parseMediumDate(princ.getTimeZone(), dateStr, princ.getLocale()));
		}
		if(params.containsKey("end_time")) {
			keys.add("end_time");
			String dateStr = Filters.filterString(params, "end_time");
			info.setEndTime(LocalDates.parseMediumDate(princ.getTimeZone(), dateStr, princ.getLocale()));
		}
		
		if(params.containsKey("owner_uid")) {
			keys.add("owner_uid");
			info.setOwnerUid(Filters.filterLong(params, "owner_uid"));
		}
		if(params.containsKey("assignee_uid")) {
			keys.add("assignee_uid");
			info.setAssigneeUid(Filters.filterLong(params, "assignee_uid"));
		}
		
		info.setPropertyFilter(keys);
		
		taskCheckService.updateTaskCheck(svcctx, info);
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.save.check"));
		svcctx.setOperationObject(info.getInfoId());
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path = "task-check-remove")
	public void handleTaskCheckRemove(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.CHK_RMV);		
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.requireId("check_id")
			.validate(true);
		
		InfoId cateKey = Filters.filterInfoId(params, "check_id", NodeIdKey.TASK_CHECK);
	
		// prepare the service context data
		Long taskpid = commonService.queryColumn(cateKey, "target_id", Long.class);
		svcctx.addOperationPredicate("task_id", taskpid);
		svcctx.setOperationObject(cateKey);
		
		taskCheckService.removeTaskCheck(cateKey);
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.remove.check"));
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-check-reorder")
	@SuppressWarnings("unchecked")
	public void handleTaskPhaseReorder(HttpServerExchange exchange) throws Exception {
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.reorder"));
		ServiceContext svcctx = this.getServiceContext(exchange);
		
		List<Object> paramlist = (List<Object>)exchange.getAttachment(FilterHandler.REQUEST_BODY);
		
		if(paramlist.isEmpty()) {
			
			throw new WebException("excp.illegal.param");
		}
		
		Map<Long, Integer> phases = Maps.newHashMap();
		paramlist.forEach(el -> {
			Map<String, Object> item = (Map<String, Object>) el;
			Long phaseId = NumberUtils.toLong((String)item.get("check_id"));
			Integer order = (Integer)item.get("check_order");
			
			phases.put(phaseId, order);
		});
		
		if(!phases.isEmpty()) {
			taskCheckService.updateTaskChecksOrder(svcctx, phases);
		}
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-check-summary")
	public void handleTaskSummaryInfo(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_FND);
		
		ArgsValidator.newValidator(params)
			.requireId("check_id")
			.validate(true);
		
		svcctx.addOperationPredicates(params);
		InfoId checkId = Filters.filterInfoId(params, "check_id", NodeIdKey.TASK);
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.task.info"));
		
		TraceableInfo info = taskCheckService.getTaskCheckSummary(checkId);
		
		result.setData(info.toMap("attach", "check_id"));
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-check-state")
	public void handleTaskStateSwitch(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_UPD);
		ArgsValidator.newValidator(params)
			.requireOne("check_id", "check_code")
			.validate(true);
		
		svcctx.addOperationPredicates(params);
		
		long taskid = Filters.filterLong(params, "check_id");		
		InfoId checkId = IdKeys.getInfoId(NodeIdKey.TASK_CHECK, taskid);
		if(!IdKeys.isValidId(checkId)) {
			String taskcode = Filters.filterString(params, "check_code");
			checkId = commonService.queryInfoId(taskcode);
		}
		String stateStr = Filters.filterString(params, "state");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.task.info"));
		
		Optional<State> opt = Enums.getIfPresent(State.class, stateStr);
		
		taskCheckService.changeCheckState(svcctx, checkId, opt.get());
		
		this.sendResult(exchange, result);
	}
}
