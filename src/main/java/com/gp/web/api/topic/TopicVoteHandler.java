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

import com.google.common.collect.Maps;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.NodeIdKey;
import com.gp.common.Operations;
import com.gp.common.ServiceContext;
import com.gp.dao.info.TopicChoiceInfo;
import com.gp.exception.BaseException;
import com.gp.svc.CommonService;
import com.gp.svc.topic.TopicQueryService;
import com.gp.svc.topic.TopicVoteService;
import com.gp.validate.ArgsValidator;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import io.undertow.server.HttpServerExchange;

public class TopicVoteHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(TopicVoteHandler.class);
	
	private CommonService commonService;
	private TopicVoteService topicVoteService;
	private TopicQueryService topicQueryService;	
	
	public TopicVoteHandler() {
		commonService = BindScanner.instance().getBean(CommonService.class);
		topicVoteService = BindScanner.instance().getBean(TopicVoteService.class);
		topicQueryService = BindScanner.instance().getBean(TopicQueryService.class);
	}

	@WebApi(path="topic-vote")
	public void handleTopicVote(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_ADD_ATC);
		svcctx.addOperationPredicates(params);
		
		InfoId voterId = svcctx.getPrincipal().getUserId();
		// check the parameter map
		ArgsValidator.newValidator(params)
			.requireId("choice_id")
			.validate(true);
				
		long choiceid = Filters.filterLong(params, "choice_id");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.topic.vote"));
		
		topicVoteService.addChoiceVote(svcctx, IdKeys.getInfoId(NodeIdKey.TOPIC_CHOICE, choiceid), voterId);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="topic-votes-query")
	public void handleTopicChoicesQuery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_ADD_ATC);
		svcctx.addOperationPredicates(params);
		
		// check the parameter map
		ArgsValidator.newValidator(params)
			.requireId("topic_id")
			.validate(true);
				
		long topicid = Filters.filterLong(params, "topic_id");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.topic.vote"));
		
		List<TopicChoiceInfo> clist = topicQueryService.getTopicChoices(IdKeys.getInfoId(NodeIdKey.TOPIC, topicid));
		List<Object> opts = clist.stream().map(cinfo -> {
			Map<String, Object> c = Maps.newHashMap();
			c.put("choice_id", cinfo.getId().toString());
			c.put("opt_val", cinfo.getOptVal());
			c.put("opt_label", cinfo.getOptLabel());
			c.put("vote_count", cinfo.getVoteCount());
			
			return c;
		}).collect(Collectors.toList());
	
		result.setData(opts);
		
		this.sendResult(exchange, result);
	}

	@WebApi(path="topic-like")
	public void handleTopicThumbup(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_LIK);
		svcctx.addOperationPredicates(params);
		
		InfoId voterId = svcctx.getPrincipal().getUserId();
		// check the parameter map
		ArgsValidator.newValidator(params)
			.requireId("topic_id")
			.validate(true);
		
		InfoId topicKey = Filters.filterInfoId(params, "topic_id", NodeIdKey.TOPIC);
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.topic.vote"));
		topicVoteService.addTopicOrAnswerLike(svcctx, topicKey, voterId);
		
		Map<String, Object> data = Maps.newHashMap();
		params = Maps.newHashMap();
		params.put("target_id", topicKey.getId());
		
		commonService.queryRows(NodeIdKey.TOPIC_STAT, params, (rs) -> {
			data.put("upvote_cnt", rs.getInt("upvote_cnt"));
			data.put("downvote_cnt", rs.getInt("downvote_cnt"));
		});
		
		// set object id to context
		svcctx.setOperationObject(topicKey);
		
		result.setData(data);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="topic-dislike")
	public void handleTopicThumbdown(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_DLK);
		svcctx.addOperationPredicates(params);
		
		InfoId voterId = svcctx.getPrincipal().getUserId();
		// check the parameter map
		ArgsValidator.newValidator(params)
			.requireId("topic_id")
			.validate(true);
		
		InfoId topicKey = Filters.filterInfoId(params, "topic_id", NodeIdKey.TOPIC);
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.topic.vote"));
		topicVoteService.addTopicOrAnswerDislike(svcctx, topicKey, voterId);
		Map<String, Object> data = Maps.newHashMap();		
		params = Maps.newHashMap();
		params.put("target_id", topicKey.getId());
		
		commonService.queryRows(NodeIdKey.TOPIC_STAT, params, (rs) -> {
			data.put("upvote_cnt", rs.getInt("upvote_cnt"));
			data.put("downvote_cnt", rs.getInt("downvote_cnt"));
		});
		
		// set object id to context
		svcctx.setOperationObject(topicKey);
		
		result.setData(data);
		this.sendResult(exchange, result);
	}
}
