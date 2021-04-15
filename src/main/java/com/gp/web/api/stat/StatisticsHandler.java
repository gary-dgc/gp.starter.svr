/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.stat;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.NodeIdKey;
import com.gp.dao.info.UserInfo;
import com.gp.exception.BaseException;
import com.gp.info.DataBuilder;
import com.gp.info.TraceableInfo;
import com.gp.pagination.PageQuery;
import com.gp.svc.CommonService;
import com.gp.svc.topic.TopicStatService;
import com.gp.svc.user.UserStatService;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import com.gp.web.api.ServiceApiHelper;

import io.undertow.server.HttpServerExchange;

public class StatisticsHandler extends BaseApiSupport{

	private UserStatService userStatService;
	private TopicStatService topicStatService;
	private CommonService commonService;
	
	public StatisticsHandler() {
		userStatService = BindScanner.instance().getBean(UserStatService.class);
		topicStatService = BindScanner.instance().getBean(TopicStatService.class);
		commonService = BindScanner.instance().getBean(CommonService.class);	
	}

	@WebApi(path="top-users-query", operation="wgrp:opr")
	public void handleTopicUsersQuery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
	
		PageQuery pquery = Filters.filterPageQuery(params);
		
		List<Map<String, Object>> data = Lists.newArrayList();
		
		if(Objects.nonNull(pquery)) {
			pquery.setOrderBy("usr.score");
			pquery.setOrder("DESC");
		}
		
		List<UserInfo> extList = userStatService.getTopUsers(pquery);
		
		data = extList.stream().map((info) -> {
			
			DataBuilder builder = new DataBuilder();
			
			builder.set("user_id", info.getId().toString());
			builder.set(info, "user_gid", "username", "email", "mobile", "state",
					"category", "full_name");
		
			builder.set("source", sbuilder -> {
				sbuilder.set("source_id", info.getProperty("source_id", Long.class).toString());
				sbuilder.set(info, "source_name");
			});
			
			if(info.getCreateTime() != null) {
				builder.set("create_date", String.valueOf(info.getCreateTime().getTime()));
			}
			
			String avatarUrl = ServiceApiHelper.absoluteBinaryUrl(info.getAvatarUrl());
			builder.set("avatar_url", avatarUrl);
			
			return builder.build();
		}).collect(Collectors.toList());
		
		ActionResult result = ActionResult.success("success get operation");
		if(null != pquery) {
			result.setData(pquery.getPagination(), data);
		}else {
			result.setData(data);
		}
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="topic-sum-query", operation="wgrp:opr")
	public void handleTopicSumQuery(HttpServerExchange exchange) throws BaseException {
		Map<String, Object> params = this.getRequestBody(exchange);
		
		long wgid = Filters.filterLong(params, "wgroup_id");
		String wcode = Filters.filterString(params, "wgroup_code");
		InfoId wid = IdKeys.getInfoId(NodeIdKey.WORKGROUP, wgid);
		if(!IdKeys.isValidId(wid)) {
			wid = commonService.queryInfoId(wcode);
		}
		
		TraceableInfo info = topicStatService.getTopicSummaryByWGroup(wid);
		
		DataBuilder builder = new DataBuilder();			
		builder.set(info, "view_sum", "answer_sum", "comment_sum", "publish_sum");
	
		ActionResult result = ActionResult.success("success get summary");		
		result.setData(builder.build());
				
		this.sendResult(exchange, result);
	}
}
