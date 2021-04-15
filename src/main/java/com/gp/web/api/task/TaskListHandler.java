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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.InfoId;
import com.gp.common.NodeIdKey;
import com.gp.common.Operations;
import com.gp.common.ServiceContext;
import com.gp.dao.info.TaskListInfo;
import com.gp.exception.BaseException;
import com.gp.info.DataBuilder;
import com.gp.svc.CommonService;
import com.gp.svc.task.TaskListService;
import com.gp.validate.ArgsValidator;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import com.gp.web.api.wgroup.WGroupExtHandler;

import io.undertow.server.HttpServerExchange;

public class TaskListHandler extends BaseApiSupport {

	static Logger LOGGER = LoggerFactory.getLogger(WGroupExtHandler.class);
	
	private CommonService commonService;
	private TaskListService taskListTmplService;
	
	public TaskListHandler() {
		commonService = BindScanner.instance().getBean(CommonService.class);
		taskListTmplService = BindScanner.instance().getBean(TaskListService.class);		
	}

	@WebApi(path="task-lists-query")
	public void handleTaskListsQuery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_FND);		
		svcctx.addOperationPredicates(params);
		
		String wgrpCode = Filters.filterString(params, "wgroup_code");
				
		InfoId wgrpKey = Filters.filterInfoId(params, "wgroup_id", NodeIdKey.WORKGROUP);
		if(!Strings.isNullOrEmpty(wgrpCode)) {
			wgrpKey = commonService.queryInfoId(wgrpCode);
		}
		List<TaskListInfo> infos = taskListTmplService.getTaskLists(wgrpKey);
		
		List<Object> data = infos.stream().map(info -> {
			DataBuilder builder = new DataBuilder();
			builder.set("list_id", info.getId().toString());
			builder.set(info, "list_name", "description");

			return builder.build();
		}).collect(Collectors.toList());
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.cates"));

		result.setData(data);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-list-add")
	public void handleTaskListAdd(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_FND);		
		svcctx.addOperationPredicates(params);
	
		ArgsValidator.newValidator(params)
			.require("list_name")
			.validate(true);
		
		String wgrpCode = Filters.filterString(params, "wgroup_code");
		
		InfoId wgrpKey = Filters.filterInfoId(params, "wgroup_id", NodeIdKey.WORKGROUP);
		if(!Strings.isNullOrEmpty(wgrpCode)) {
			wgrpKey = commonService.queryInfoId(wgrpCode);
		}
		String catename = Filters.filterString(params, "list_name");
		String descr = Filters.filterString(params, "description");
		TaskListInfo cate = new TaskListInfo();
			
		cate.setWorkgroupId(wgrpKey.getId());
		cate.setListName(catename);
		cate.setDescription(descr);
		
		InfoId cKey = taskListTmplService.newTaskList(svcctx, cate);
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.new.cate"));
		
		Map<String, Object> data = Maps.newHashMap();
		data.put("list_id", cKey.getId().toString());
		result.setData(data);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-list-save")
	public void handleTaskListSave(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_FND);		
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.requireId("list_id")
			.require("list_name")
			.validate(true);
		
		InfoId cateKey = Filters.filterInfoId(params, "list_id", NodeIdKey.TASK_LIST);
		String catename = Filters.filterString(params, "list_name");
		String descr = Filters.filterString(params, "description");
		TaskListInfo cate = new TaskListInfo();
		
		cate.setInfoId(cateKey);
		cate.setListName(catename);
		cate.setDescription(descr);
		
		taskListTmplService.updateTaskList(svcctx, cate);
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.save.cate"));
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path = "task-list-remove")
	public void handleTaskListRemove(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_FND);		
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.requireId("list_id")
			.validate(true);
		
		InfoId cateKey = Filters.filterInfoId(params, "list_id", NodeIdKey.TASK_LIST);
		Boolean force = Filters.filterBoolean(params, "force");
		
		taskListTmplService.removeTaskList(cateKey, force);
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.save.cate"));
		
		this.sendResult(exchange, result);
	}	
	
}
