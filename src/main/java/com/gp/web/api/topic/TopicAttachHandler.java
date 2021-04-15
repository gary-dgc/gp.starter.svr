/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.topic;

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
import com.gp.dao.info.TopicAttachInfo;
import com.gp.exception.BaseException;
import com.gp.info.BaseIdKey;
import com.gp.info.DataBuilder;
import com.gp.svc.topic.TopicService;
import com.gp.validate.ArgsValidator;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import com.gp.web.api.ServiceApiHelper;

import io.undertow.server.HttpServerExchange;

public class TopicAttachHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(TopicAttachHandler.class);
	
	private TopicService topicService;
	
	public TopicAttachHandler() {
		
		topicService = BindScanner.instance().getBean(TopicService.class);
		
	}

	@WebApi(path="topic-attach-add")
	public void handleTopicAddAttach(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_ADD_ATC);
		svcctx.addOperationPredicates(params);
		
		// check the parameter map
		ArgsValidator.newValidator(params)
			.requireId("topic_id", "binary_id")
			.require("attach_name")
			.validate(true);
				
		long topicid = Filters.filterLong(params, "topic_id");
		long binid = Filters.filterLong(params, "binary_id");
		
		String attachName = Filters.filterString(params, "attach_name");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.attach.save"));
		
		if(topicid <= 0){
			result = ActionResult.failure("miss parameters");
			this.sendResult(exchange, result);
			return ;
		}
		
		TopicAttachInfo attach = new TopicAttachInfo();
		attach.setSourceId(GeneralConstants.LOCAL_SOURCE);
		attach.setAttachName(attachName);
		attach.setTargetId(topicid);
		attach.setBinaryId(binid);
		
		topicService.newAttach(svcctx, attach);
		
		this.sendResult(exchange, result);
	}

	@WebApi(path="topic-attach-remove")
	public void handleTopicRemoveAttach(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_RMV_ATC);
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.requireId("attach_id")
			.validate(true);
		
		long attachid = Filters.filterLong(params, "attach_id");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.attach.remove"));
		
		if(attachid <= 0){
			result = ActionResult.failure("miss parameters");
			
			this.sendResult(exchange, result);
			return ;
		}
		
		InfoId attachId = IdKeys.getInfoId(NodeIdKey.TOPIC_ATTACH, attachid);
		topicService.removeAttach(svcctx, attachId);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="topic-attachs-query")
	public void handleTopicAttachQuery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_FND_ATC);
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.requireId("topic_id")
			.validate(true);
		
		long topicid = Filters.filterLong(params, "topic_id");
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.attach.find"));
		
		List<TopicAttachInfo> attachlist = topicService.getAttachs(IdKeys.getInfoId(NodeIdKey.TASK, topicid));
		
		List<Map<String,Object>> rows = attachlist.stream().map(info -> {
			
			DataBuilder builder = new DataBuilder();
			builder.set("attach_id", info.getId().toString());
			builder.set("topic_id", info.getTargetId().toString());
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
	
	@WebApi(path="topic-attach-info")
	public void handleTopicAttachInfo(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_FND);
		
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.requireId("attach_id")
			.validate(true);
		
		long attachid = Filters.filterLong(params, "attach_id");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.attach.find"));
		
		if(attachid <= 0){
			result = ActionResult.failure("miss parameters");
			this.sendResult(exchange, result);
			return ;
		}
		
		TopicAttachInfo info = topicService.getAttach(IdKeys.getInfoId(NodeIdKey.TOPIC_ATTACH, attachid));
		
		DataBuilder builder = new DataBuilder();
		builder.set("attach_id", info.getId().toString());
		builder.set("topic_id", info.getTargetId().toString());
		builder.set("binary_id", info.getBinaryId().toString());
		
		InfoId binId = IdKeys.getInfoId(BaseIdKey.BINARY, info.getBinaryId());
		String attachUrl = ServiceApiHelper.instance().absoluteBinaryUrl(BinaryMode.ATTACH, binId, info.getFormat());
		builder.set("attach_url", attachUrl);
		
		builder.set(info, "attach_name", "format");
			
		result.setData(builder.build());
		
		this.sendResult(exchange, result);
	}
	

	@WebApi(path="answer-attach-add")
	public void handleAnswerAddAttach(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_ADD_ATC);
		svcctx.addOperationPredicates(params);
		
		// check the parameter map
		ArgsValidator.newValidator(params)
			.requireId("answer_id", "binary_id")
			.require("attach_name")
			.validate(true);
				
		long answerid = Filters.filterLong(params, "answer_id");
		long binid = Filters.filterLong(params, "binary_id");
		
		String attachName = Filters.filterString(params, "attach_name");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.attach.save"));
		
		if(answerid <= 0){
			result = ActionResult.failure("miss parameters");
			this.sendResult(exchange, result);
			return ;
		}
		
		TopicAttachInfo attach = new TopicAttachInfo();
		attach.setSourceId(GeneralConstants.LOCAL_SOURCE);
		attach.setAttachName(attachName);
		attach.setTargetId(answerid);
		attach.setBinaryId(binid);
		
		topicService.newAttach(svcctx, attach);
		
		this.sendResult(exchange, result);
	}

	@WebApi(path="answer-attach-remove")
	public void handleAnswerRemoveAttach(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_RMV_ATC);
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.requireId("attach_id")
			.validate(true);
		
		long attachid = Filters.filterLong(params, "attach_id");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.attach.remove"));
		
		if(attachid <= 0){
			result = ActionResult.failure("miss parameters");
			
			this.sendResult(exchange, result);
			return ;
		}
		
		InfoId attachId = IdKeys.getInfoId(NodeIdKey.TOPIC_ATTACH, attachid);
		topicService.removeAttach(svcctx, attachId);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="answer-attachs-query")
	public void handleAnswerAttachQuery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_FND_ATC);
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.requireId("answer_id")
			.validate(true);
		
		long answerid = Filters.filterLong(params, "answer_id");
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.attach.find"));
		
		List<TopicAttachInfo> attachlist = topicService.getAttachs(IdKeys.getInfoId(NodeIdKey.TOPIC_ANSWER, answerid));
		
		List<Map<String,Object>> rows = attachlist.stream().map(info -> {
			
			DataBuilder builder = new DataBuilder();
			builder.set("attach_id", info.getId().toString());
			builder.set("answer_id", info.getTargetId().toString());
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
