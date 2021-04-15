/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.topic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.NodeIdKey;
import com.gp.common.Operations;
import com.gp.common.ServiceContext;
import com.gp.common.Sources.Scope;
import com.gp.dao.info.TopicChoiceInfo;
import com.gp.dao.info.TopicInfo;
import com.gp.exception.BaseException;
import com.gp.info.DataBuilder;
import com.gp.pagination.PageQuery;
import com.gp.svc.CommonService;
import com.gp.svc.topic.TopicQueryService;
import com.gp.svc.topic.TopicService;
import com.gp.validate.ArgsValidator;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import com.gp.web.api.ServiceApiHelper;

import io.undertow.server.HttpServerExchange;

public class TopicQueryHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(TopicQueryHandler.class);
	
	private TopicService topicService;
	private TopicQueryService topicQueryService;	
	private CommonService commonService;
	
	public TopicQueryHandler() {
		topicService = BindScanner.instance().getBean(TopicService.class);
		topicQueryService = BindScanner.instance().getBean(TopicQueryService.class);
		commonService = BindScanner.instance().getBean(CommonService.class);
	}


	@WebApi(path="topics-query")
	public void handleTopicsQuery(HttpServerExchange exchange) throws BaseException {
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.fnd.task"));
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_FND);
		svcctx.addOperationPredicates(params);
		
		String keyword = Filters.filterString(params, "keyword");
		
		List<String> types = Filters.filterList(params, "types", String.class);
		List<String> states = Filters.filterList(params, "states", String.class);
		List<String> features = Filters.filterList(params, "features", String.class);
		List<String> marks = Filters.filterList(params, "marks", String.class);
		List<String> scopes = Filters.filterList(params, "scopes", String.class);
		
		String ownerGid = Filters.filterString(params, "owner_gid");
		String authorGid = Filters.filterString(params, "author_gid");
			
		PageQuery pquery = Filters.filterPageQuery(params);
			
		InfoId ownerId = commonService.queryInfoId(ownerGid);
		InfoId authorId = commonService.queryInfoId(authorGid);
		
		List<TopicInfo> infos = topicService.getTopics(svcctx, ownerId, authorId, keyword, 
				marks, types, states, features, scopes,
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
	

	@WebApi(path="hottest-topics-query")
	public void handleHottestTopicsQuery(HttpServerExchange exchange) throws BaseException {
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.fnd.task"));
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_FND);
		svcctx.addOperationPredicates(params);
			
		long wgid = Filters.filterLong(params, "wgroup_id");
		String wcode = Filters.filterString(params, "wgroup_code");
		String keyword = Filters.filterString(params, "keyword");
		List<String> types = Filters.filterList(params, "types", String.class);
		List<String> states = Filters.filterList(params, "states", String.class);
		
		String mark = Filters.filterString(params, "mark");
		String ownerGid = Filters.filterString(params, "owner_gid");
		String authorGid = Filters.filterString(params, "author_gid");
				
		PageQuery pquery = Filters.filterPageQuery(params);
		
		InfoId wid = IdKeys.getInfoId(NodeIdKey.WORKGROUP, wgid);
		if(!IdKeys.isValidId(wid)) {
			wid = commonService.queryInfoId(wcode);
		}
		
		InfoId ownerId = commonService.queryInfoId(ownerGid);
		InfoId authorId = commonService.queryInfoId(authorGid);
	
		List<TopicInfo> infos = topicQueryService.getHotestTopics(svcctx, wid, ownerId, authorId, keyword, mark, types, states, null, pquery);
				
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
	

	@WebApi(path="publish-topics-query")
	public void handlePublishTopicsQuery(HttpServerExchange exchange) throws BaseException {
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.fnd.task"));
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_FND);
		svcctx.addOperationPredicates(params);
			
		long wgid = Filters.filterLong(params, "wgroup_id");
		String wcode = Filters.filterString(params, "wgroup_code");
		String keyword = Filters.filterString(params, "keyword");
		List<String> types = Filters.filterList(params, "types", String.class);
		List<String> states = Filters.filterList(params, "states", String.class);
		
		String mark = Filters.filterString(params, "mark");
		String ownerGid = Filters.filterString(params, "owner_gid");
		String authorGid = Filters.filterString(params, "author_gid");
				
		PageQuery pquery = Filters.filterPageQuery(params);
		
		InfoId wid = IdKeys.getInfoId(NodeIdKey.WORKGROUP, wgid);
		if(!IdKeys.isValidId(wid)) {
			wid = commonService.queryInfoId(wcode);
		}
		
		InfoId ownerId = commonService.queryInfoId(ownerGid);
		InfoId authorId = commonService.queryInfoId(authorGid);
	
		Scope scope = Filters.filterEnum(params, "scope", Scope.class);
		List<TopicInfo> infos = topicQueryService.getPublishTopics(svcctx, wid, scope, ownerId, authorId, keyword, mark, types, states, null, pquery);
				
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
}
