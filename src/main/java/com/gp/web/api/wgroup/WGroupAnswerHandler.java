/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.wgroup;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.NodeIdKey;
import com.gp.common.Operations;
import com.gp.common.ServiceContext;
import com.gp.dao.info.TopicAnswerInfo;
import com.gp.exception.BaseException;
import com.gp.info.DataBuilder;
import com.gp.pagination.PageQuery;
import com.gp.svc.CommonService;
import com.gp.svc.topic.TopicAnswerService;
import com.gp.validate.ArgsValidator;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import com.gp.web.api.ServiceApiHelper;

import io.undertow.server.HttpServerExchange;

public class WGroupAnswerHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(WGroupAnswerHandler.class);
	
	private TopicAnswerService topicAnswerService;
	private CommonService commonService;
	
	public WGroupAnswerHandler() {
		topicAnswerService = BindScanner.instance().getBean(TopicAnswerService.class);
		commonService = BindScanner.instance().getBean(CommonService.class);
	}
	
	@WebApi(path="wgroup-answers-query")
	public void handleAnswersQuery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ANS_FND);
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.requireOne("wgroup_code", "wgroup_id")
			.validate(true);
		
		long wgrpid = Filters.filterLong(params, "wgroup_id");
		String wgrpcode = Filters.filterString(params, "wgroup_code");
		String keyword = Filters.filterString(params, "keyword");
				
		List<String> states = Filters.filterList(params, "states", String.class);
		List<String> features = Filters.filterList(params, "features", String.class);
		List<String> marks = Filters.filterList(params, "marks", String.class);
		List<String> scopes = Filters.filterList(params, "scopes", String.class);
		
		String authorGid = Filters.filterString(params, "joiner_gid");
		
		InfoId authorId = commonService.queryInfoId(authorGid);
		
		PageQuery pquery = Filters.filterPageQuery(params);
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.answers"));
		
		InfoId wgrpKey = wgrpid > 0 ? IdKeys.getInfoId(NodeIdKey.WORKGROUP, wgrpid) : commonService.queryInfoId(wgrpcode);
		
		List<TopicAnswerInfo> cmnts = topicAnswerService.getAnswers(svcctx, wgrpKey, authorId, keyword, marks, states, features, scopes, pquery);
		
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
		
		result.setData(pquery == null ? null : pquery.getPagination(), rows);
		
		this.sendResult(exchange, result);
	}

}
