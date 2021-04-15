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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Enums;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.gp.bind.BindScanner;
import com.gp.common.Channels.BindScope;
import com.gp.common.Filters;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.LocalDates;
import com.gp.common.NodeIdKey;
import com.gp.common.Operations;
import com.gp.common.ServiceContext;
import com.gp.common.Topics;
import com.gp.common.Topics.State;
import com.gp.dao.info.TopicChoiceInfo;
import com.gp.dao.info.TopicInfo;
import com.gp.dao.info.TopicPublishInfo;
import com.gp.dao.info.TopicUserInfo;
import com.gp.exception.BaseException;
import com.gp.info.BaseIdKey;
import com.gp.info.DataBuilder;
import com.gp.info.Principal;
import com.gp.info.TraceableInfo;
import com.gp.svc.CommonService;
import com.gp.svc.master.StorageService;
import com.gp.svc.topic.TopicPublishService;
import com.gp.svc.topic.TopicQueryService;
import com.gp.svc.topic.TopicService;
import com.gp.svc.topic.TopicStatService;
import com.gp.util.NumberUtils;
import com.gp.validate.ArgsValidator;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import com.gp.web.api.ServiceApiHelper;

import io.undertow.server.HttpServerExchange;

public class TopicHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(TopicHandler.class);
	
	static String DT_FORMAT = "yyyy/MM/dd";
	
	private TopicService topicService;
	private TopicQueryService topicQueryService;	
	private TopicStatService topicStatService;	
	private CommonService commonService;		
	private TopicPublishService publishService;	
	private StorageService storageService;
	
	public TopicHandler() {
		
		topicService = BindScanner.instance().getBean(TopicService.class);
		topicQueryService = BindScanner.instance().getBean(TopicQueryService.class);
		topicStatService = BindScanner.instance().getBean(TopicStatService.class);
		commonService = BindScanner.instance().getBean(CommonService.class);
		publishService = BindScanner.instance().getBean(TopicPublishService.class);
		storageService = BindScanner.instance().getBean(StorageService.class);
	}
	
	/**
	 * Create new topic record, operation statistics to work on workgroup, topic and user
	 * 
	 **/
	@SuppressWarnings("unchecked")
	@WebApi(path="topic-add")
	public void handleTopicAdd(HttpServerExchange exchange) throws BaseException {
	
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.topic.add"));
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_NEW);
		svcctx.addOperationPredicates(params);
		
		ArgsValidator argsChecker = ArgsValidator.newValidator(params)
			.requireOne("wgroup_code", "wgroup_id")
			.require("title", "topic_type");
					
		TopicInfo topic = new TopicInfo();
		
		String type = Filters.filterString(params, "topic_type");
		switch(type) {
			case "VOTING": 
				argsChecker.require("choices");
				argsChecker.list("choices", agent -> {
					agent.require("opt_val", "opt_label");
				});
				topic.setCommentOn(Filters.filterBoolean(params, "comment_on"));
				topic.setProperty("choices", params.get("choices"));
				
			case "ARTICLE":
				argsChecker.require("content");
				topic.setCommentOn(Filters.filterBoolean(params, "comment_on"));
			
			case "DISCUSSION":
				topic.setCommentOn(Filters.filterBoolean(params, "comment_on"));
				
			case "QUESTION":
				topic.setAnswerOn(Filters.filterBoolean(params, "answer_on"));
				
			default:
				argsChecker.match("\\d{4}/\\d{2}/\\d{2}", "open_time", "close_time");
				argsChecker.validate(true);
		}
		
		long wgid = Filters.filterLong(params, "wgroup_id");
		String wgroupCode = Filters.filterString(params, "wgroup_code");
		
		if(wgid <= 0 && !Strings.isNullOrEmpty(wgroupCode)) {
			InfoId wgrpId = commonService.queryInfoId(wgroupCode);
			wgid = wgrpId.getId();
		}
		
		topic.setWorkgroupId(wgid);
		topic.setTitle(Filters.filterString(params, "title"));
		topic.setTopicType(type);
		topic.setContent(Filters.filterString(params, "content"));
		topic.setExcerpt(Filters.filterString(params, "excerpt"));
		topic.setState(Topics.State.DRAFT.name());
		
		topic.setClassification(Filters.filterString(params, "classification"));
		topic.setVoteOn(Filters.filterBoolean(params, "vote_on"));
		
		Date openTime = Filters.filterDate(params, "open_time", DT_FORMAT);
		topic.setOpenTime(openTime);
		
		String avatarUrl = Filters.filterString(params, "thumb_url");		
		if(!Strings.isNullOrEmpty(avatarUrl) && avatarUrl.startsWith("data:image/")){
			// process the avatar base64 image
			InfoId stgId = storageService.getDefaultStorage().getInfoId();
			avatarUrl = ServiceApiHelper.instance().cacheAvatar(stgId, avatarUrl);
			topic.setThumbUrl(avatarUrl);
		}else {
			String relativeUrl = ServiceApiHelper.instance().relativeBinaryUrl(avatarUrl);
			topic.setThumbUrl(relativeUrl);
		}
		
		if(params.containsKey("attachs") && params.get("attachs") != null) {
			
			List<Map<String, Object>> list = Lists.newArrayList();
			for(Map<String, String> attach: (Collection<Map<String, String>>)params.get("attachs")) {
				Map<String, Object> item = Maps.newHashMap();
				item.put("file_name", attach.get("file_name"));
				item.put("binary_id", NumberUtils.toLong(attach.get("binary_id")));
				
				list.add(item);
			}
			topic.setProperty("attachs", list);
		}
		
		Principal princ = svcctx.getPrincipal();
		topic.setOwnerUid(princ.getUserId().getId());
		topic.setCreateTime(LocalDates.now());
		topic.setState(Topics.State.DRAFT.name());
		
		long authorUid = Filters.filterLong(params, "author_uid");
		topic.setAuthorUid(( authorUid <= 0 ) ? princ.getUserId().getId() : authorUid);
		
		if(params.containsKey("attendee_uids")) {
			Collection<String> collIds = (Collection<String>)params.get("attendee_uids");
			if(collIds != null) {
				List<InfoId> infoIds = collIds.stream().map(cid -> {
					return IdKeys.getInfoId(BaseIdKey.USER, NumberUtils.toLong(cid));
				}).collect(Collectors.toList());
				
				topic.setProperty("attendee_uids", infoIds);
			}
		}
	
		topicService.newTopic(svcctx, topic);
		
		// set object id into service context
		svcctx.setOperationObject(topic.getInfoId());
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="topic-info")
	public void handleTopicInfo(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		ArgsValidator.newValidator(params)
				.requireOne("topic_code", "topic_id")
				.validate(true);
		
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_FND);
		svcctx.addOperationPredicates(params);
		
		long topicid = Filters.filterLong(params, "topic_id");
		String topiccode = Filters.filterString(params, "topic_code");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.fnd.task"));
		
		InfoId topicKey = null;
		if(topicid > 0) {
			topicKey = IdKeys.getInfoId(NodeIdKey.TOPIC, topicid);
		}else {
			topicKey = commonService.queryInfoId(topiccode);
		}
		
		TopicInfo info = topicService.getTopic(svcctx, topicKey);
		if(null != info) {
			
			DataBuilder builder = new DataBuilder();
			builder.set("topic_id", info.getId().toString());
			builder.set("topic_code", info.getTraceCode());
			builder.set("workgroup_id", info.getWorkgroupId().toString());
		
			builder.set("create_time", info.getCreateTime().getTime());
			builder.set("close_time", info.getCloseTime() == null ? "" : info.getCloseTime().getTime());
			
			builder.set(info, "topic_type", "title", "content", "excerpt",
					"state", "precedence", "mark");
			builder.set(info, "answer_on", "comment_on", "vote_on");
			
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
			
			builder.set("workgroup", sbuilder -> {
				sbuilder.set("workgroup_code", info.getProperty("workgroup_code"));
				sbuilder.set("workgroup_name", info.getProperty("workgroup_name"));
				sbuilder.set("category", info.getProperty("category"));
				Long cabId = info.getProperty("cabinet_id", Long.class);
				sbuilder.set("cabinet_id", cabId == null ? "": cabId.toString());
			});
			
			if(Objects.equal(info.getTopicType(), "VOTING")) {
				
				List<TopicChoiceInfo> clist = topicQueryService.getTopicChoices(info.getInfoId());
				List<Object> opts = clist.stream().map(cinfo -> {
					Map<String, Object> c = Maps.newHashMap();
					c.put("choice_id", cinfo.getId().toString());
					c.put("opt_val", cinfo.getOptVal());
					c.put("opt_label", cinfo.getOptLabel());
					c.put("vote_count", cinfo.getVoteCount());
					
					return c;
				}).collect(Collectors.toList());
			
				builder.set("choices", opts);
			}
			
			builder.set("stat", sbuilder -> {
				sbuilder.set(info, "upvote_cnt", "downvote_cnt", "answer_cnt");				
			});
			
			result.setData(builder.build());
		}
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="topic-save")
	public void handleTopicSave(HttpServerExchange exchange) throws BaseException {

		Map<String, Object> params = this.getRequestBody(exchange);
		ArgsValidator.newValidator(params)
			.requireOne("topic_code", "topic_id")
			.validate(true);
		
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_UPD);
		svcctx.addOperationPredicates(params);
		
		long topicid = Filters.filterLong(params, "topic_id");
		String topiccode = Filters.filterString(params, "topic_code");
		InfoId topicKey = null;
		if(topicid > 0) {
			topicKey = IdKeys.getInfoId(NodeIdKey.TOPIC, topicid);
		}else {
			topicKey = commonService.queryInfoId(topiccode);
		}
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.task.save"));
				
		TopicInfo topic = new TopicInfo();
		topic.setInfoId(topicKey);
		topic.setTitle(Filters.filterString(params, "title"));
		topic.setTopicType(Filters.filterString(params, "topic_type"));
		topic.setContent(Filters.filterString(params, "content"));
		topic.setExcerpt(Filters.filterString(params, "excerpt"));
		
		topic.setClassification(Filters.filterString(params, "classification"));
		
		topic.setAnswerOn(Filters.filterBoolean(params, "answer_on"));
		topic.setProperty("mark", Filters.filterString(params, "mark"));
				
		Set<String> keys = Sets.newHashSet("title", "topic_type", "content", "classification",
				"comment_on", "excerpt");

		topic.setPropertyFilter(keys);
		topicService.updateTopic(svcctx, topic);
		
		svcctx.setOperationObject(topic.getInfoId());
		
		this.sendResult(exchange, result);
		
	}
	
	@WebApi(path="topic-remove")
	public void handleTopicRemove(HttpServerExchange exchange) throws BaseException {

		Map<String, Object> params = this.getRequestBody(exchange);
		ArgsValidator.newValidator(params)
			.requireOne("topic_code", "topic_id")
			.validate(true);
		
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_RMV);
		svcctx.addOperationPredicates(params);
		
		long topicid = Filters.filterLong(params, "topic_id");
		String topiccode = Filters.filterString(params, "topic_code");
		InfoId topicKey = null;
		if(topicid > 0) {
			topicKey = IdKeys.getInfoId(NodeIdKey.TOPIC, topicid);
		}else {
			topicKey = commonService.queryInfoId(topiccode);
		}
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.task.save"));
		
		Long wgrpid = commonService.queryColumn(topicKey, "workgroup_id", Long.class);
		svcctx.addOperationPredicate("workgroup_id", wgrpid);
		Long ownerid = commonService.queryColumn(topicKey, "owner_uid", Long.class);
		svcctx.addOperationPredicate("owner_uid", ownerid);
		svcctx.setOperationObject(topicKey);
		
		topicService.removeTopic(svcctx, topicKey);
				
		this.sendResult(exchange, result);
		
	}
	
	@WebApi(path="topic-update")
	public void handleTopicUpdate(HttpServerExchange exchange) throws BaseException {

		Map<String, Object> params = this.getRequestBody(exchange);
		ArgsValidator.newValidator(params)
			.requireOne("topic_code", "topic_id")
			.validate(true);
		
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_UPD);
		
		long topicid = Filters.filterLong(params, "topic_id");
		String topiccode = Filters.filterString(params, "topic_code");
		InfoId topicKey = null;
		if(topicid > 0) {
			topicKey = IdKeys.getInfoId(NodeIdKey.TOPIC, topicid);
		}else {
			topicKey = commonService.queryInfoId(topiccode);
		}
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.task.save"));
		
		TopicInfo topic = new TopicInfo();
		topic.setInfoId(topicKey);
		if(params.containsKey("title")) {
			topic.setTitle(Filters.filterString(params, "title"));
		}
		if(params.containsKey("topic_type")) {
			String type = Filters.filterString(params, "topic_type");
			topic.setTopicType(type);
			
			if(Objects.equal("VOTING", type)) {
				topic.setProperty("choices", params.get("choices"));
			}
		}
		if(params.containsKey("content")) {
			topic.setContent(Filters.filterString(params, "content"));
			topic.setExcerpt(Filters.filterString(params, "excerpt"));
		}
		if(params.containsKey("classification")) {
			topic.setClassification(Filters.filterString(params, "classification"));
		}
		
		if(params.containsKey("state")) {
			topic.setState(Filters.filterString(params, "state"));
		}
		if(params.containsKey("owner_uid")) {
			topic.setOwnerUid(Filters.filterLong(params, "owner_uid"));
		}
		if(params.containsKey("author_uid")) {
			topic.setAuthorUid(Filters.filterLong(params, "author_uid"));
		}
		
		if(params.containsKey("comment_on")) {
			topic.setCommentOn(Filters.filterBoolean(params, "comment_on"));
		}
		if(params.containsKey("answer_on")) {
			topic.setAnswerOn(Filters.filterBoolean(params, "answer_on"));
		}
		Set<String> keys = params.keySet();
		keys.remove("topic_id"); // reserve property keys only
		
		topic.setPropertyFilter(keys);
		
		topicService.updateTopic(svcctx, topic);
		
		svcctx.setOperationObject(topic.getInfoId());
		
		this.sendResult(exchange, result);
		
	}
	
	@WebApi(path="topic-state-switch")
	public void handleTopicStateSwitch(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ArgsValidator.newValidator(params)
			.requireOne("topic_code", "topic_id")
			.validate(true);
		
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_UPD);
		svcctx.addOperationPredicates(params);
		
		long topicid = Filters.filterLong(params, "topic_id");
		String topiccode = Filters.filterString(params, "topic_code");
		InfoId topicKey = null;
		if(topicid > 0) {
			topicKey = IdKeys.getInfoId(NodeIdKey.TOPIC, topicid);
		}else {
			topicKey = commonService.queryInfoId(topiccode);
		}
		String stateStr = Filters.filterString(params, "state");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.topic.info"));
		
		Optional<State> opt = Enums.getIfPresent(State.class, stateStr);
		
		topicService.changeTopicState(topicKey, opt.get());
		
		svcctx.setOperationObject(topicKey);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="topic-summary")
	public void handleTopicSummaryInfo(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		ArgsValidator.newValidator(params)
			.requireOne("topic_code", "topic_id")
			.validate(true);
		
		this.getServiceContext(exchange, Operations.TPC_FND);

		long topicid = Filters.filterLong(params, "topic_id");
		String topiccode = Filters.filterString(params, "topic_code");
		InfoId topicKey = null;
		if(topicid > 0) {
			topicKey = IdKeys.getInfoId(NodeIdKey.TOPIC, topicid);
		}else {
			topicKey = commonService.queryInfoId(topiccode);
		}
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.task.info"));
	
		TraceableInfo info = topicStatService.getTopicSummaryByKey(topicKey);
		
		Map<String, Object> data = null == info ? Maps.newHashMap() : info.toMap("attach_count", "bind_count", "publish_count", "topic_id");
		result.setData(data);
		
		this.sendResult(exchange, result);
	}

	@WebApi(path="topic-members-query")
	public void handleTopicUsersQuery(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		ArgsValidator.newValidator(params)
			.requireOne("topic_code", "topic_id")
			.validate(true);
		
		this.getServiceContext(exchange, Operations.TPC_FND_USR);
		
		long topicid = Filters.filterLong(params, "topic_id");
		String topiccode = Filters.filterString(params, "topic_code");
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.users"));
	
		InfoId topicKey = null;
		if(topicid > 0) {			
			topicKey = IdKeys.getInfoId(NodeIdKey.TASK, topicid);
		} else {			
			topicKey = commonService.queryInfoId(topiccode);
		}
		
		List<TopicUserInfo> infos = topicService.getTopicMembers(topicKey);
		
		List<Object> data = infos.stream().map(info -> {
			DataBuilder builder = new DataBuilder();
			
			builder.set("member_uid", info.getAttendeeUid().toString());
			builder.set("user_gid", info.getProperty("attendee_gid"));
			builder.set("username", info.getProperty("attendee_username"));
			
			builder.set("full_name", info.getProperty("attendee_full_name"));
			builder.set("nickname", info.getProperty("attendee_nickname"));
			String avatarUrl = info.getProperty("attendee_avatar_url", String.class);
			avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
			builder.set("avatar_url", avatarUrl);
			
			return builder.build();
			
		}).collect(Collectors.toList());
		
		result.setData(data);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="topic-publish")
	public void handleTopicPublish(HttpServerExchange exchange) throws BaseException {
	
		Map<String, Object> params = this.getRequestBody(exchange);
		ArgsValidator.newValidator(params)
			.requireOne("topic_code", "topic_id")
			.validate(true);
		
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_PUB);	
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.publish.topic"));
		
		long topicid = Filters.filterLong(params, "topic_id");
		String topiccode = Filters.filterString(params, "topic_code");
		InfoId topicKey = null;
		if(topicid > 0) {			
			topicKey = IdKeys.getInfoId(NodeIdKey.TASK, topicid);
		} else {			
			topicKey = commonService.queryInfoId(topiccode);
		}
		List<Object> channels = Filters.filterList(params, "channels");
		
		List<Map<String, Object>> chnls = Lists.newArrayList();
		List<Object> data = Lists.newArrayList();
		
		for( Object chnl : channels) {
			
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) chnl;
			
			Map<String, Object> pred = Maps.newHashMap(map);
			pred.remove("channel_name");
			chnls.add(pred);
			
			String channelName  = Filters.filterString(map, "channel_name");
			String channelCode  = Filters.filterString(map, "channel_code");
			BindScope scope = Filters.filterEnum(map, "scope", BindScope.class);
			
			InfoId pubId = publishService.publishTopic(svcctx, topicKey, 
					channelName, channelCode, scope);
			
			map.remove("scope");
			map.put("publish_id", pubId.getId().toString());
			
			data.add(map);
		}
	
		// set object id into service context
		svcctx.setOperationObject(topicKey);
		params.put("channels", chnls);
		params.put("publisher_gid", svcctx.getPrincipal().getUserGid());
		
		svcctx.addOperationPredicates(params);
		
		svcctx.setOperationObject(topicKey);
		
		result.setData(data);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="topic-unpublish")
	public void handleTopicUnpublish(HttpServerExchange exchange) throws BaseException {
	
		Map<String, Object> params = this.getRequestBody(exchange);
		ArgsValidator.newValidator(params)
			.requireOne("topic_code", "topic_id")
			.validate(true);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_UPUB);
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.unpublish.topic"));
		
		long topicid = Filters.filterLong(params, "topic_id");
		String topiccode = Filters.filterString(params, "topic_code");
		InfoId topicKey = null;
		if(topicid > 0) {			
			topicKey = IdKeys.getInfoId(NodeIdKey.TASK, topicid);
		} else {			
			topicKey = commonService.queryInfoId(topiccode);
		}
		
		List<Object> channels = Filters.filterList(params, "channels");
		
		List<Map<String, Object>> chnls = Lists.newArrayList();
		List<Object> data = Lists.newArrayList();
		
		for( Object chnl : channels) {
			
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) chnl;
			
			Map<String, Object> pred = Maps.newHashMap(map);
			pred.remove("channel_name");
			chnls.add(pred);
			
			String channelCode  = Filters.filterString(map, "channel_code");
			BindScope scope = Filters.filterEnum(map, "scope", BindScope.class);
			
			InfoId pubId = publishService.unpublishTopic(svcctx, topicKey, 
					channelCode, scope);
			
			map.remove("scope");
			map.put("publish_id", pubId.getId().toString());
			
			data.add(map);
		}
		
		// set object id into service context
		svcctx.setOperationObject(topicKey);
		params.put("channels", chnls);
		params.put("publisher_gid", svcctx.getPrincipal().getUserGid());
		svcctx.addOperationPredicates(params);
		
		svcctx.setOperationObject(topicKey);
		
		result.setData(data);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="topic-channels-query")
	public void handleTopicChannelsQuery(HttpServerExchange exchange) throws BaseException {
	
		Map<String, Object> params = this.getRequestBody(exchange);
		ArgsValidator.newValidator(params)
			.requireOne("topic_code", "topic_id")
			.validate(true);
		
		this.getServiceContext(exchange, Operations.TPC_FND_CHNL);
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.channel"));
		
		long topicid = Filters.filterLong(params, "topic_id");
		String topiccode = Filters.filterString(params, "topic_code");
		InfoId topicKey = null;
		if(topicid > 0) {			
			topicKey = IdKeys.getInfoId(NodeIdKey.TASK, topicid);
		} else {			
			topicKey = commonService.queryInfoId(topiccode);
		}
		
		List<TopicPublishInfo> infos = publishService.getPublishChannels(topicKey);
		
		List<Object> data = Lists.newArrayList();
		for(TopicPublishInfo info : infos) {
			
			DataBuilder builder = new DataBuilder();
			
			builder.set("topic_id", info.getTopicId().toString());
			
			builder.set(info, "trace_code", "publish_scope", "channel_code", "channel_name");
			builder.set("publish_time", info.getPublishTime().getTime());
			
			builder.set(info, "contribute_on", "channel_type");
			
			String avatarUrl = ServiceApiHelper.absoluteBinaryUrl(info.getProperty("avatar_url", String.class));
			builder.set("avatar_url", avatarUrl);
			
			data.add(builder.build());
		}
		
		result.setData(data);
		this.sendResult(exchange, result);
	}
	
}
