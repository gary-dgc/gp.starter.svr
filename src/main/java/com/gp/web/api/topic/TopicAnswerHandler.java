/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.topic;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.NodeIdKey;
import com.gp.common.Operations;
import com.gp.common.ServiceContext;
import com.gp.common.Topics;
import com.gp.common.Topics.State;
import com.gp.dao.info.TopicAnswerInfo;
import com.gp.exception.BaseException;
import com.gp.info.DataBuilder;
import com.gp.info.Principal;
import com.gp.pagination.PageQuery;
import com.gp.svc.CommonService;
import com.gp.svc.topic.TopicAnswerService;
import com.gp.svc.topic.TopicVoteService;
import com.gp.util.NumberUtils;
import com.gp.validate.ArgsValidator;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import com.gp.web.api.ServiceApiHelper;

import io.undertow.server.HttpServerExchange;

public class TopicAnswerHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(TopicAnswerHandler.class);
	
	private TopicAnswerService topicAnswerService;
	private CommonService commonService;
	private TopicVoteService topicVoteService;
	
	public TopicAnswerHandler() {
		topicAnswerService = BindScanner.instance().getBean(TopicAnswerService.class);
		commonService = BindScanner.instance().getBean(CommonService.class);
		topicVoteService = BindScanner.instance().getBean(TopicVoteService.class);
	}
	
	@WebApi(path="answers-query")
	public void handleAnswersQuery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ANS_FND);
		svcctx.addOperationPredicates(params);
		
		String keyword = Filters.filterString(params, "keyword");
		
		List<String> states = Filters.filterList(params, "states", String.class);
		List<String> features = Filters.filterList(params, "features", String.class);
		List<String> marks = Filters.filterList(params, "marks", String.class);
		List<String> scopes = Filters.filterList(params, "scopes", String.class);
		
		PageQuery pquery = Filters.filterPageQuery(params);
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.answers"));
		
		List<TopicAnswerInfo> cmnts = topicAnswerService.getAnswers(svcctx, null, null, keyword, 
				marks, states, features, scopes, pquery);
		
		List<Map<String, Object>> rows = cmnts.stream().map(info -> {
			
			DataBuilder builder = new DataBuilder();
			
			builder.set("answer_id", info.getId().toString());
			builder.set("topic_id", info.getTopicId().toString());
			builder.set("workgroup_id", info.getWorkgroupId().toString());
			
			builder.set("answer_time", info.getAnswerTime().getTime());
			
			builder.set(info, "trace_code", "content", "excerpt", "state", "title", "thumb_url");
			
			Long favId = info.getProperty("favorite_id", Long.class);
			builder.set("fav", (favId != null && favId > 0L));
			
			builder.set("owner", sbuilder -> {
				sbuilder.set("user_gid", info.getProperty("owner_gid"));
				sbuilder.set("username", info.getProperty("owner_username"));
				
				sbuilder.set("full_name", info.getProperty("owner_full_name"));
				sbuilder.set("nickname", info.getProperty("owner_nickname"));
				String avatarUrl = info.getProperty("owner_avatar_url", String.class);
				avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
				sbuilder.set("avatar_url", avatarUrl);
			});
			
			builder.set("author", sbuilder -> {
				sbuilder.set("user_gid", info.getProperty("author_gid"));
				sbuilder.set("username", info.getProperty("author_username"));
				
				sbuilder.set("full_name", info.getProperty("author_full_name"));
				sbuilder.set("nickname", info.getProperty("author_nickname"));
				String avatarUrl = info.getProperty("author_avatar_url", String.class);
				avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
				sbuilder.set("avatar_url", avatarUrl);
			});
			
			builder.set("stat", sbuilder -> {
				sbuilder.set(info, "upvote_cnt", "downvote_cnt");				
			});
			return builder.build();
			
		}).collect(Collectors.toList());
		
		result.setData(pquery == null ? null : pquery.getPagination(), rows);
		
		this.sendResult(exchange, result);
	}

	@WebApi(path="topic-answers-query")
	public void handleTopicAnswersQuery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ANS_FND);
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.requireOne("topic_code", "topic_id")
			.validate(true);
		
		long topicid = Filters.filterLong(params, "topic_id");
		String topiccode = Filters.filterString(params, "topic_code");
		String keyword = Filters.filterString(params, "keyword");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.answers"));
		
		InfoId topicKey = topicid > 0 ? IdKeys.getInfoId(NodeIdKey.TOPIC, topicid) : commonService.queryInfoId(topiccode);
		
		List<TopicAnswerInfo> cmnts = topicAnswerService.getTopicAnswers(svcctx, topicKey, keyword, null, null);
		
		List<Map<String, Object>> rows = cmnts.stream().map(info -> {
			
			DataBuilder builder = new DataBuilder();
			
			builder.set("answer_id", info.getId().toString());
			builder.set("topic_id", info.getTopicId().toString());
			builder.set("workgroup_id", info.getWorkgroupId().toString());
			
			builder.set("answer_time", info.getAnswerTime().getTime());
			
			builder.set(info, "trace_code", "content", "excerpt", "state", "thumb_url");
			
			String title = info.getTitle();
			if(Strings.isNullOrEmpty(title)) {
				title = (String)info.getProperty("topic_title");
			}
			builder.set("title", title);
			
			Long favId = info.getProperty("favorite_id", Long.class);
			builder.set("fav", (favId != null && favId > 0L));
			
			builder.set("owner", sbuilder -> {
				sbuilder.set("user_gid", info.getProperty("owner_gid"));
				sbuilder.set("username", info.getProperty("owner_username"));
				
				sbuilder.set("full_name", info.getProperty("owner_full_name"));
				sbuilder.set("nickname", info.getProperty("owner_nickname"));
				String avatarUrl = info.getProperty("owner_avatar_url", String.class);
				avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
				sbuilder.set("avatar_url", avatarUrl);
			});
			
			builder.set("author", sbuilder -> {
				sbuilder.set("user_gid", info.getProperty("author_gid"));
				sbuilder.set("username", info.getProperty("author_username"));
				
				sbuilder.set("full_name", info.getProperty("author_full_name"));
				sbuilder.set("nickname", info.getProperty("author_nickname"));
				String avatarUrl = info.getProperty("author_avatar_url", String.class);
				avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
				sbuilder.set("avatar_url", avatarUrl);
			});
			
			builder.set("stat", sbuilder -> {
				sbuilder.set(info, "upvote_cnt", "downvote_cnt");				
			});
			return builder.build();
			
		}).collect(Collectors.toList());
		
		result.setData(rows);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="answer-info")
	public void handleAnswerInfo(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ANS_FND);
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.require("answer_id")
			.validate(true);
	
		long answerid = Filters.filterLong(params, "answer_id");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.answers"));
		
		TopicAnswerInfo info = topicAnswerService.getAnswer(svcctx, IdKeys.getInfoId(NodeIdKey.TOPIC_ANSWER, answerid));
		
		DataBuilder builder = new DataBuilder();
		
		builder.set("answer_id", info.getId().toString());
		builder.set("topic_id", info.getTopicId().toString());
		builder.set("workgroup_id", info.getWorkgroupId().toString());
		
		builder.set("answer_time", info.getAnswerTime().getTime());
		
		builder.set(info, "trace_code", "content", "excerpt", "state", "thumb_url", "comment_on");
		
		String title = info.getTitle();
		if(Strings.isNullOrEmpty(title)) {
			title = (String)info.getProperty("topic_title");
		}
		builder.set("title", title);
		
		Long favId = info.getProperty("favorite_id", Long.class);
		builder.set("fav", (favId != null && favId > 0L));
		
		builder.set("owner", sbuilder -> {
			sbuilder.set("user_gid", info.getProperty("owner_gid"));
			sbuilder.set("username", info.getProperty("owner_username"));
			
			sbuilder.set("full_name", info.getProperty("owner_full_name"));
			sbuilder.set("nickname", info.getProperty("owner_nickname"));
			String avatarUrl = info.getProperty("owner_avatar_url", String.class);
			avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
			sbuilder.set("avatar_url", avatarUrl);
		});
		
		builder.set("author", sbuilder -> {
			sbuilder.set("user_gid", info.getProperty("author_gid"));
			sbuilder.set("username", info.getProperty("author_username"));
			
			sbuilder.set("full_name", info.getProperty("author_full_name"));
			sbuilder.set("nickname", info.getProperty("author_nickname"));
			String avatarUrl = info.getProperty("author_avatar_url", String.class);
			avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
			sbuilder.set("avatar_url", avatarUrl);
		});
		
		builder.set("stat", sbuilder -> {
			sbuilder.set(info, "upvote_cnt", "downvote_cnt");				
		});
				
		result.setData(builder);
		
		this.sendResult(exchange, result);
	}
	
	@SuppressWarnings("unchecked")
	@WebApi(path="answer-add")
	public void handleTopicAddAnswer(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ANS_NEW);
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.require("content")
			.requireId("topic_id")
			.validate(true);
		
		long topicid = Filters.filterLong(params, "topic_id");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.topic.answer.save"));
		
		TopicAnswerInfo answer = new TopicAnswerInfo();
		answer.setTopicId(topicid);
		answer.setTitle(Filters.filterString(params, "title"));
		answer.setContent(Filters.filterString(params, "content"));
		answer.setExcerpt(Filters.filterString(params, "excerpt"));
		answer.setCommentOn(true);
		answer.setState(Topics.State.DRAFT.name());
		Principal princ = svcctx.getPrincipal();
		
		answer.setOwnerUid(princ.getUserId().getId());
		answer.setAuthorUid(princ.getUserId().getId());
		
		if(params.containsKey("attachs") && params.get("attachs") != null) {
			
			List<Map<String, Object>> list = Lists.newArrayList();
			for(Map<String, String> attach: (Collection<Map<String, String>>)params.get("attachs")) {
				Map<String, Object> item = Maps.newHashMap();
				item.put("file_name", attach.get("file_name"));
				item.put("binary_id", NumberUtils.toLong(attach.get("binary_id")));
				
				list.add(item);
			}
			answer.setProperty("attachs", list);
		}
				
		topicAnswerService.newAnswer(svcctx, answer);
		// set object id to context
		svcctx.setOperationObject(answer.getInfoId());
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="answer-save")
	public void handleTopicSaveAnswer(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ANS_UPD);
		
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.requireId("answer_id")
			.validate(true);
			
		long answerid = Filters.filterLong(params, "answer_id");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.save.answer"));
				
		TopicAnswerInfo answer = new TopicAnswerInfo();
		answer.setInfoId(IdKeys.getInfoId(NodeIdKey.TOPIC_ANSWER, answerid));
		answer.setTitle(Filters.filterString(params, "title"));
		answer.setContent(Filters.filterString(params, "content"));
		answer.setExcerpt(Filters.filterString(params, "excerpt"));
		answer.setState(Filters.filterString(params, "state"));
		answer.setCommentOn(Filters.filterBoolean(params, "comment_on"));
		Principal princ = svcctx.getPrincipal();
	
		answer.setAuthorUid(princ.getUserId().getId());
		
		Set<String> keys = params.keySet();
		keys.remove("answer_id");
		answer.setPropertyFilter(keys);
		
		topicAnswerService.updateAnswer(svcctx, answer);
		
		// set object id to context
		svcctx.setOperationObject(answer.getInfoId());
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="answer-remove")
	public void handleTopicRemoveAnswer(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ANS_RMV);
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.requireId("answer_id")
			.validate(true);
		
		long answerid = Filters.filterLong(params, "answer_id");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.remove.answer"));
		
		InfoId answerKey = IdKeys.getInfoId(NodeIdKey.TOPIC_ANSWER, answerid);
		
		// set object id to context
		Long topicid = commonService.queryColumn(answerKey, "topic_id", Long.class);
		Long wgrpid = commonService.queryColumn(answerKey, "workgroup_id", Long.class);
		svcctx.addOperationPredicate("topic_id", topicid);
		svcctx.addOperationPredicate("workgroup_id", wgrpid);
		svcctx.setOperationObject(answerKey);
		
		topicAnswerService.removeAnswer(svcctx, answerKey);		
		
		this.sendResult(exchange, result);
	}
	

	@WebApi(path="answer-like")
	public void handleTopicThumbup(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ANS_LIK);
		svcctx.addOperationPredicates(params);
		
		InfoId voterId = svcctx.getPrincipal().getUserId();
		// check the parameter map
		ArgsValidator.newValidator(params)
			.requireId("answer_id")
			.validate(true);
		
		InfoId answerKey = Filters.filterInfoId(params, "answer_id", NodeIdKey.TOPIC_ANSWER);
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.topic.vote"));
		topicVoteService.addTopicOrAnswerLike(svcctx, answerKey, voterId);
		
		Map<String, Object> data = Maps.newHashMap();
		params = Maps.newHashMap();
		params.put("target_id", answerKey.getId());
		
		commonService.queryRows(NodeIdKey.TOPIC_STAT, params, (rs) -> {
			data.put("upvote_cnt", rs.getInt("upvote_cnt"));
			data.put("downvote_cnt", rs.getInt("downvote_cnt"));
		});
				
		result.setData(data);
		
		// set object id to context
		svcctx.setOperationObject(answerKey);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="answer-dislike")
	public void handleTopicThumbdown(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ANS_DLK);
		svcctx.addOperationPredicates(params);
		
		InfoId voterId = svcctx.getPrincipal().getUserId();
		// check the parameter map
		ArgsValidator.newValidator(params)
			.requireId("answer_id")
			.validate(true);
		
		InfoId answerKey = Filters.filterInfoId(params, "answer_id", NodeIdKey.TOPIC);
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.topic.vote"));
		topicVoteService.addTopicOrAnswerDislike(svcctx, answerKey, voterId);
		Map<String, Object> data = Maps.newHashMap();		
		params = Maps.newHashMap();
		params.put("target_id", answerKey.getId());
		
		commonService.queryRows(NodeIdKey.TOPIC_STAT, params, (rs) -> {
			data.put("upvote_cnt", rs.getInt("upvote_cnt"));
			data.put("downvote_cnt", rs.getInt("downvote_cnt"));
		});
		result.setData(data);
		
		// set object id to context
		svcctx.setOperationObject(answerKey);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="answer-state-switch")
	public void handleTopicStateSwitch(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ANS_STW);
		svcctx.addOperationPredicates(params);
		
		long topicid = Filters.filterLong(params, "answer_id");
		String stateStr = Filters.filterString(params, "state");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.topic.info"));
		
		Optional<State> opt = Enums.getIfPresent(State.class, stateStr);
		
		InfoId topicId = IdKeys.getInfoId(NodeIdKey.TOPIC_ANSWER, topicid);
		topicAnswerService.changeAnswerState(topicId, opt.get());
		
		this.sendResult(exchange, result);
	}
}
