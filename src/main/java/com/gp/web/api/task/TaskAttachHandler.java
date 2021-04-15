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

import com.gp.bind.BindScanner;
import com.gp.common.Binaries.BinaryMode;
import com.gp.common.Filters;
import com.gp.common.GeneralConstants;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.NodeIdKey;
import com.gp.common.Operations;
import com.gp.common.ServiceContext;
import com.gp.dao.info.TaskAttachInfo;
import com.gp.exception.BaseException;
import com.gp.info.BaseIdKey;
import com.gp.info.DataBuilder;
import com.gp.svc.task.TaskService;
import com.gp.validate.ArgsValidator;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import com.gp.web.api.ServiceApiHelper;

import io.undertow.server.HttpServerExchange;

public class TaskAttachHandler extends BaseApiSupport {

	static Logger LOGGER = LoggerFactory.getLogger(TaskAttachHandler.class);
	
	private TaskService taskService;
	
	public TaskAttachHandler() {
		taskService = BindScanner.instance().getBean(TaskService.class);
		
	}

	@WebApi(path="task-attach-add")
	public void handleTaskAddAttach(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_ADD_ATC);
		svcctx.addOperationPredicates(params);
		
		// check the parameter map
		ArgsValidator.newValidator(params)
			.requireId("task_id", "binary_id")
			.require("attach_name")
			.validate(true);
		
		long taskid = Filters.filterLong(params, "task_id");
		long binid = Filters.filterLong(params, "binary_id");
		String attachName = Filters.filterString(params, "attach_name");
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.add.attach"));
		
		TaskAttachInfo attach = new TaskAttachInfo();
		attach.setSourceId(GeneralConstants.LOCAL_SOURCE);
		attach.setAttachName(attachName);
		attach.setTargetId(taskid);
		attach.setBinaryId(binid);
		
		taskService.newAttach(svcctx, attach);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-attach-remove")
	public void handleTaskRemoveAttach(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_UPD_ATC);
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.requireId("attach_id")
			.validate(true);
		
		long attachid = Filters.filterLong(params, "attach_id");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.remove.attach"));
				
		if(attachid <= 0){
			result = ActionResult.failure("miss parameters");
			this.sendResult(exchange, result);
			return;
		}
		
		InfoId attachId = IdKeys.getInfoId(NodeIdKey.TASK_ATTACH, attachid);
		taskService.removeAttach(svcctx, attachId);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-attachs-query")
	public void handleTaskAttachQuery(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_FND);
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.requireId("task_id")
			.validate(true);
		
		long taskid = Filters.filterLong(params, "task_id");
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.attach"));
		
		if(taskid <= 0){
			result = ActionResult.failure("miss parameters");
			this.sendResult(exchange, result);
			return;
		}
		
		List<TaskAttachInfo> attachlist = taskService.getAttachs(IdKeys.getInfoId(NodeIdKey.TASK, taskid));
		
		List<Map<String,Object>> rows = attachlist.stream().map(info -> {
			
			DataBuilder builder = new DataBuilder();
			builder.set("attach_id", info.getId().toString());
			builder.set("task_id", info.getTargetId().toString());
			builder.set("binary_id", info.getBinaryId().toString());
			
			InfoId binId = IdKeys.getInfoId(BaseIdKey.BINARY, info.getBinaryId());
			String attachUrl = ServiceApiHelper.instance().absoluteBinaryUrl(BinaryMode.ATTACH, binId, info.getFormat());
			builder.set("attach_url", attachUrl);
			
			builder.set(info, "attach_name", "format");
			
			return builder.build();
		}).collect(Collectors.toList());
		
		result.setData(rows);
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-attach-info")
	public void handleTaskAttachInfo(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);
		svcctx.setOperation(Operations.TSK_FND);
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.requireId("attach_id")
			.validate(true);
		
		long attachid = Filters.filterLong(params, "attach_id");
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.attach"));
		
		TaskAttachInfo info = taskService.getAttach(IdKeys.getInfoId(NodeIdKey.TASK_ATTACH, attachid));
		
		DataBuilder builder = new DataBuilder();
		builder.set("attach_id", info.getId().toString());
		builder.set("task_id", info.getTargetId().toString());
		builder.set("binary_id", info.getBinaryId().toString());
		
		InfoId binId = IdKeys.getInfoId(BaseIdKey.BINARY, info.getBinaryId());
		String attachUrl = ServiceApiHelper.instance().absoluteBinaryUrl(BinaryMode.ATTACH, binId, info.getFormat());
		builder.set("attach_url", attachUrl);
		
		builder.set(info, "attach_name", "format");
			
		result.setData(builder.build());
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="check-attach-add")
	public void handleCheckAddAttach(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_ADD_ATC);
		svcctx.addOperationPredicates(params);
		
		// check the parameter map
		ArgsValidator.newValidator(params)
			.requireId("check_id", "binary_id")
			.require("attach_name")
			.validate(true);
		
		long taskid = Filters.filterLong(params, "check_id");
		long binid = Filters.filterLong(params, "binary_id");
		String attachName = Filters.filterString(params, "attach_name");
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.add.attach"));
		
		TaskAttachInfo attach = new TaskAttachInfo();
		attach.setSourceId(GeneralConstants.LOCAL_SOURCE);
		attach.setAttachName(attachName);
		attach.setTargetId(taskid);
		attach.setBinaryId(binid);
		
		taskService.newAttach(svcctx, attach);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="check-attach-remove")
	public void handleCheckRemoveAttach(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_UPD_ATC);
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.requireId("attach_id")
			.validate(true);
		
		long attachid = Filters.filterLong(params, "attach_id");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.remove.attach"));
				
		if(attachid <= 0){
			result = ActionResult.failure("miss parameters");
			this.sendResult(exchange, result);
			return;
		}
		
		InfoId attachId = IdKeys.getInfoId(NodeIdKey.TASK_ATTACH, attachid);
		taskService.removeAttach(svcctx, attachId);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="check-attachs-query")
	public void handleCheckAttachQuery(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_FND);
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.requireId("check_id")
			.validate(true);
		
		long taskid = Filters.filterLong(params, "check_id");
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.attach"));
		
		if(taskid <= 0){
			result = ActionResult.failure("miss parameters");
			this.sendResult(exchange, result);
			return;
		}
		
		List<TaskAttachInfo> attachlist = taskService.getAttachs(IdKeys.getInfoId(NodeIdKey.TASK, taskid));
		
		List<Map<String,Object>> rows = attachlist.stream().map(info -> {
			
			DataBuilder builder = new DataBuilder();
			builder.set("attach_id", info.getId().toString());
			builder.set("check_id", info.getTargetId().toString());
			builder.set("binary_id", info.getBinaryId().toString());
			
			InfoId binId = IdKeys.getInfoId(BaseIdKey.BINARY, info.getBinaryId());
			String attachUrl = ServiceApiHelper.instance().absoluteBinaryUrl(BinaryMode.ATTACH, binId, info.getFormat());
			builder.set("attach_url", attachUrl);
			
			builder.set(info, "attach_name", "format");
			
			return builder.build();
		}).collect(Collectors.toList());
		
		result.setData(rows);
		this.sendResult(exchange, result);
	}
}
