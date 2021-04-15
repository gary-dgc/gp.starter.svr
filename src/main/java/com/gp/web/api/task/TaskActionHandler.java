/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.task;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.NodeIdKey;
import com.gp.common.Operations;
import com.gp.common.ServiceContext;
import com.gp.common.Tasks.State;
import com.gp.exception.BaseException;
import com.gp.svc.CommonService;
import com.gp.svc.task.TaskActionService;
import com.gp.svc.task.TaskService;
import com.gp.validate.ArgsValidator;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;

import io.undertow.server.HttpServerExchange;

public class TaskActionHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(TaskAttachHandler.class);
	
	private TaskService taskService;	
	private TaskActionService taskActionService;
	private CommonService commonService;
	
	public TaskActionHandler() {
		
		taskService = BindScanner.instance().getBean(TaskService.class);	
		taskActionService = BindScanner.instance().getBean(TaskActionService.class);
		commonService = BindScanner.instance().getBean(CommonService.class);
	}

	@WebApi(path="task-progress")
	public void handleTaskProgress(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_UPD);
		ArgsValidator.newValidator(params)
			.requireOne("task_id", "task_code")
			.require("progress")
			.validate(true);
		
		svcctx.addOperationPredicates(params);
		
		long taskid = Filters.filterLong(params, "task_id");		
		InfoId taskKey = IdKeys.getInfoId(NodeIdKey.TASK, taskid);
		if(!IdKeys.isValidId(taskKey)) {
			String taskcode = Filters.filterString(params, "task_code");
			taskKey = commonService.queryInfoId(taskcode);
		}
		int progress = Filters.filterInt(params, "progress");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.task.info"));
		
		taskActionService.changeTaskProgress(svcctx, taskKey, progress);
		
		this.sendResult(exchange, result);
		
	}
	
	@WebApi(path="task-state")
	public void handleTaskStateSwitch(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_UPD);
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
		String stateStr = Filters.filterString(params, "state");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.task.info"));
		
		Optional<State> opt = Enums.getIfPresent(State.class, stateStr);
		
		boolean ok = taskActionService.changeTaskState(svcctx, taskKey, opt.get());
		
		this.sendResult(exchange, result);
	}
	
	
}
