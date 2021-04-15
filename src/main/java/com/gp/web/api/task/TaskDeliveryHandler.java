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
import com.gp.common.Binaries.BinaryMode;
import com.gp.common.Filters;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.NodeIdKey;
import com.gp.common.Operations;
import com.gp.common.ServiceContext;
import com.gp.dao.info.TaskDeliveryInfo;
import com.gp.exception.BaseException;
import com.gp.exception.WebException;
import com.gp.info.DataBuilder;
import com.gp.svc.CommonService;
import com.gp.svc.task.TaskService;
import com.gp.validate.ArgsValidator;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import com.gp.web.api.ServiceApiHelper;

import io.undertow.server.HttpServerExchange;

public class TaskDeliveryHandler extends BaseApiSupport{

	static final int MAX_LEN = 256;
	
	static Logger LOGGER = LoggerFactory.getLogger(TaskDeliveryHandler.class);
	
	private TaskService taskService;
	private CommonService commonService;
	
	public TaskDeliveryHandler() {
		
		taskService = BindScanner.instance().getBean(TaskService.class);
		commonService = BindScanner.instance().getBean(CommonService.class);
				
	}

	@WebApi(path="task-delivery-add")
	public void handleTaskAddDelivery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_ADD_DLV);
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.requireId("task_id", "file_id")
			.validate(true);
		
		long taskid = Filters.filterLong(params, "task_id");
		long fileid = Filters.filterLong(params, "file_id");
		String delivery = Filters.filterString(params, "delivery_name");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.add.attach"));
	
		InfoId fId = IdKeys.getInfoId(NodeIdKey.CAB_FILE, fileid);
		Map<String, Object> colMap = commonService.queryColumns(fId, "file_name", "format");
		if(null == colMap) {
			throw new WebException("excp.none.file");
		}
		TaskDeliveryInfo deliv = new TaskDeliveryInfo();
		deliv.setTaskId(taskid);
		deliv.setFileId(fileid);
		
		if(Strings.isNullOrEmpty(delivery)) {
			deliv.setDeliveryName((String)colMap.get("file_name"));
		}else {
			deliv.setDeliveryName(delivery);
		}
		
		deliv.setFormat((String)colMap.get("format"));
		
		taskService.newDelivery(svcctx, deliv);
		
		Map<String, Object> data = Maps.newHashMap();
		data.put("delivery_id", deliv.getId().toString());
		result.setData(data);
		
		// set object id into context
		svcctx.setOperationObject(deliv.getInfoId());
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-delivery-remove")
	public void handleTaskRemoveDelivery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_RMV_DLV);
		ArgsValidator.newValidator(params)
			.requireId("delivery_id")
			.validate(true);
		
		svcctx.addOperationPredicates(params);
		long delivid = Filters.filterLong(params, "delivery_id");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.remove.attach"));
	
		InfoId delivId = IdKeys.getInfoId(NodeIdKey.TASK_DELIVERY, delivid);
		
		// set object id into context
		Long taskid = commonService.queryColumn(delivId, "task_id", Long.class);
		svcctx.addOperationPredicate("task_id", taskid);
		svcctx.setOperationObject(delivId);
		
		taskService.removeDelivery(svcctx, delivId);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-deliveries-query")
	public void handleTaskDeliveryQuery(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_FND_DLV);
		ArgsValidator.newValidator(params)
			.requireOne("task_id", "task_code")
			.validate(true);
		
		svcctx.addOperationPredicates(params);
		long taskid = Filters.filterLong(params, "task_id");
		String taskCd = Filters.filterString(params, "task_code");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.attach"));
				
		InfoId taskId = IdKeys.getInfoId(NodeIdKey.TASK, taskid);		
		if(taskid <= 0){
			
			taskId = commonService.queryInfoId(taskCd);
		}
		
		List<TaskDeliveryInfo> attachlist = taskService.getDeliveries(svcctx, taskId, null);
		
		List<Map<String,Object>> rows = attachlist.stream().map(info -> {
			
			DataBuilder builder = new DataBuilder();
			builder.set("delivery_id", info.getId().toString());
			builder.set("task_id", info.getTaskId().toString());
			builder.set("file_id", info.getFileId().toString());
			
			InfoId fileId = IdKeys.getInfoId(NodeIdKey.CAB_FILE, info.getFileId());
			String attachUrl = ServiceApiHelper.instance().absoluteBinaryUrl(BinaryMode.FILE, fileId, info.getFormat());
			builder.set("attach_url", attachUrl);
			
			builder.set(info, "delivery_name", "format");
			
			return builder.build();
		}).collect(Collectors.toList());
		
		result.setData(rows);
		
		this.sendResult(exchange, result);
	}

}
