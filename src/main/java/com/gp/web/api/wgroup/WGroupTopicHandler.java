/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.wgroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.NodeIdKey;
import com.gp.common.Operations;
import com.gp.common.ServiceContext;
import com.gp.dao.info.TopicChoiceInfo;
import com.gp.dao.info.TopicInfo;
import com.gp.dao.info.WorkgroupInfo;
import com.gp.exception.BaseException;
import com.gp.info.DataBuilder;
import com.gp.info.Principal;
import com.gp.pagination.PageQuery;
import com.gp.svc.CommonService;
import com.gp.svc.topic.TopicExtraService;
import com.gp.svc.topic.TopicQueryService;
import com.gp.validate.ArgsValidator;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import com.gp.web.api.ServiceApiHelper;
import com.gp.web.api.topic.TopicHandler;

import io.undertow.server.HttpServerExchange;

public class WGroupTopicHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(TopicHandler.class);
	
	static String DT_FORMAT = "yyyy/MM/dd";
	
	private TopicQueryService topicQueryService;	
	private TopicExtraService topicExtraService;	
	private CommonService commonService;
			
	public WGroupTopicHandler() {
		
		topicQueryService = BindScanner.instance().getBean(TopicQueryService.class);
		topicExtraService = BindScanner.instance().getBean(TopicExtraService.class);
		commonService = BindScanner.instance().getBean(CommonService.class);		
	}

	@WebApi(path="wgroup-topics-query")
	public void handleTopicsQuery(HttpServerExchange exchange) throws BaseException {
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.fnd.task"));
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_FND);
		
		ArgsValidator.newValidator(params)
			.requireOne("wgroup_id", "wgroup_code")
			.validate(true);
		
		long wgid = Filters.filterLong(params, "wgroup_id");
		String wcode = Filters.filterString(params, "wgroup_code");
		String keyword = Filters.filterString(params, "keyword");
		
		List<String> types = Filters.filterList(params, "types", String.class);
		List<String> states = Filters.filterList(params, "states", String.class);
		List<String> features = Filters.filterList(params, "features", String.class);
		List<String> marks = Filters.filterList(params, "marks", String.class);
		List<String> scopes = Filters.filterList(params, "scopes", String.class);
		
		String ownerGid = Filters.filterString(params, "owner_gid");
		String authorGid = Filters.filterString(params, "joiner_gid");
				
		PageQuery pquery = Filters.filterPageQuery(params);
		
		InfoId wid = IdKeys.getInfoId(NodeIdKey.WORKGROUP, wgid);
		if(!IdKeys.isValidId(wid)) {
			wid = commonService.queryInfoId(wcode);
		}
		
		InfoId ownerId = commonService.queryInfoId(ownerGid);
		InfoId authorId = commonService.queryInfoId(authorGid);
	
		List<TopicInfo> infos = topicQueryService.getWorkgroupTopics(svcctx, wid, 
				ownerId, authorId, keyword, marks, types, states, features, scopes,
				null, pquery);
				
		List<Object> list = new ArrayList<Object>();
		// prepare to find all topic choices
		List<InfoId> ids = infos.stream()
				.filter(t -> Objects.equal(t.getTopicType(), "VOTING"))
				.map(t -> t.getInfoId())
				.collect(Collectors.toList());
		
		Map<Long, List<TopicChoiceInfo>> cmap = Maps.newHashMap();
		
		if(!ids.isEmpty()) {
			cmap = topicQueryService.getTopicChoices(ids);
		}
		
		for(TopicInfo info: infos) {
			
			ids.add(info.getInfoId());
			
			DataBuilder builder = new DataBuilder();
			builder.set("topic_id", info.getId().toString());
			builder.set("topic_code", info.getTraceCode());
			
			builder.set("workgroup_id", info.getWorkgroupId().toString());
			
			builder.set("create_time", info.getCreateTime().getTime());
			builder.set("close_time", info.getCloseTime() == null ? "" : info.getCloseTime().getTime());
			
			builder.set(info, "topic_type", "title", "content", "excerpt",
					"state", "precedence", "mark");
			
			Long favId = info.getProperty("favorite_id", Long.class);
			builder.set("fav", (favId != null && favId > 0L));
			
			String thumbUrl = ServiceApiHelper.absoluteBinaryUrl(info.getThumbUrl());
			builder.set("thumb_url", thumbUrl);
			
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
			
			if(cmap.containsKey(info.getId())) {
				List<TopicChoiceInfo> cinfos = cmap.get(info.getId());
				
				List<Object> opts = cinfos.stream().map(cinfo -> {
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
			
			list.add( builder.build());
		}
		
		result.setData(pquery == null ? null : pquery.getPagination(), list);
	
		this.sendResult(exchange, result);
		
	}

	
	@WebApi(path="wgroup-topic-assigns-query")
	public void handleTopicAvailAssigns(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_FND_ASG);
		
		ArgsValidator.newValidator(params)
			.require("topic_id")
			.validate(true);
		
		long topicid = Filters.filterLong(params, "topic_id");
		String keyword = Filters.filterString(params, "keyword");
		String category = Filters.filterString(params, "category");
		@SuppressWarnings("unchecked")
		Collection<String> state = (Collection<String>)params.get("state");
		PageQuery pquery = Filters.filterPageQuery(params);
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.task.info"));
		
		if(topicid <= 0 ){
			result = ActionResult.failure("miss parameters");
			this.sendResult(exchange, result);
			return ;
		}
		InfoId topicId = IdKeys.getInfoId(NodeIdKey.TOPIC, topicid);
		Principal princ = svcctx.getPrincipal();
		
		List<WorkgroupInfo> infos = topicExtraService.getAvailableAssigns(princ.getUserId(), topicId, keyword, category, state, pquery);
		List<Object> data = Lists.newArrayList();
		
		data = infos.stream().map((info)->{
			DataBuilder builder = new DataBuilder();
			
			builder.set("workgroup_id", info.getId().toString());
			builder.set("workgroup_code", info.getTraceCode());
			
			builder.set("admin_uid", info.getAdminUid().toString());
			builder.set("manger_uid", info.getManagerUid().toString());
			builder.set("workgroup_name", info.getWorkgroupName());
			
			builder.set(info, "description","state", "publish_scope", "visible_scope");
			builder.set("category", info.getCategory());
			
			
			builder.set("admin", abuilder -> {
				abuilder.set("user_id", info.getAdminUid().toString());
				abuilder.set("user_gid", info.getProperty("admin_gid", String.class));
				abuilder.set("username", info.getProperty("admin_username", String.class));
				abuilder.set("full_name", info.getProperty("admin_full_name", String.class));
			});
			
			builder.set("manager", abuilder -> {
				abuilder.set("user_id", info.getManagerUid().toString());
				abuilder.set("user_gid", info.getProperty("manager_gid", String.class));
				abuilder.set("username", info.getProperty("manager_username", String.class));
				abuilder.set("full_name", info.getProperty("manager_full_name", String.class));
			});
			
			String avatarUrl = ServiceApiHelper.absoluteBinaryUrl(info.getAvatarUrl());
			builder.set("avatar_url", avatarUrl);
			
			builder.set("create_time", info.getCreateTime().getTime());
				
			return builder.build();
			
		}).collect(Collectors.toList());
		
		result = ActionResult.success(getMessage(exchange, "mesg.find.wgroup"));
		result.setData(pquery == null ? null : pquery.getPagination(), data);
 		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="wgroup-topic-assign")
	public void handleTopicAssignWorkgroup(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_ASG);
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.topic.info"));
		ArgsValidator.newValidator(params)
			.require("topic_id", "wgroup_id")
			.validate(true);
		
		long topicid = Filters.filterLong(params, "topic_id");
		long wgrpid = Filters.filterLong(params, "wgroup_id");
		
		InfoId topicId = IdKeys.getInfoId(NodeIdKey.TOPIC, topicid);
		InfoId wgroupId = IdKeys.getInfoId(NodeIdKey.WORKGROUP, wgrpid);
		
		topicExtraService.assignWorkgroup(svcctx, topicId, wgroupId);
		result = ActionResult.success(getMessage(exchange, "mesg.assign.wgroup"));
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="wgroup-topic-unassign")
	public void handleTopicUnassignWorkgroup(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_UASG);
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.topic.info"));
		ArgsValidator.newValidator(params)
			.require("topic_id")
			.validate(true);
		
		long topicid = Filters.filterLong(params, "topic_id");
		
		InfoId topicId = IdKeys.getInfoId(NodeIdKey.TOPIC, topicid);
		
		topicExtraService.unassignWorkgroup(svcctx, topicId);
		result = ActionResult.success(getMessage(exchange, "mesg.assign.wgroup"));
		
		this.sendResult(exchange, result);
	}

}
