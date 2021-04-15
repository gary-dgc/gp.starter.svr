/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.user;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.GroupUsers;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.LocalDates;
import com.gp.common.NodeIdKey;
import com.gp.common.ServiceContext;
import com.gp.dao.info.TaskInfo;
import com.gp.dao.info.TopicInfo;
import com.gp.exception.BaseException;
import com.gp.info.DataBuilder;
import com.gp.info.Principal;
import com.gp.pagination.PageQuery;
import com.gp.svc.CommonService;
import com.gp.svc.master.SourceService;
import com.gp.svc.user.UserExtService;
import com.gp.svc.user.UserService;
import com.gp.svc.wgroup.JoinInviteService;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import com.gp.web.api.ServiceApiHelper;

import io.undertow.server.HttpServerExchange;

public class UserQueryHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(UserQueryHandler.class);
	
	private JoinInviteService joinInviteService;
	
	private UserService userService;
	
	private UserExtService userExtService;
	
	private SourceService sourceService;
	
	private CommonService commonService;
	
	public UserQueryHandler() {
		
		userService = BindScanner.instance().getBean(UserService.class);
		userExtService = BindScanner.instance().getBean(UserExtService.class);
		joinInviteService = BindScanner.instance().getBean(JoinInviteService.class);
		commonService = BindScanner.instance().getBean(CommonService.class);
		sourceService = BindScanner.instance().getBean(SourceService.class);
		
	}
	
	/**
	 * Query priority summary in specified period
	 * @param wgroup_id the workgroup id
	 **/
	@WebApi(path="user-summaries-query")
	public void handlePrioritySummaryQuery(HttpServerExchange exchange) throws BaseException {
	
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);
		String wgrpid = Filters.filterString(paramap, "wgroup_id");
				
		Principal princ = this.getPrincipal(exchange);
		InfoId wgrpKey = null;
		if(!Strings.isNullOrEmpty(wgrpid)) {
			
			wgrpKey = IdKeys.getInfoId(NodeIdKey.WORKGROUP, NumberUtils.toLong(wgrpid));
		}

		Date to = LocalDates.now();
		Date from = Date.from(LocalDate.now().minusDays(-7L).atStartOfDay(princ.getTimeZone()).toInstant());
		
		List<Map<String, Integer>> data = userExtService.getTaskPrioritySummary(princ.getUserId(), wgrpKey, from, to);

		svcctx.addOperationPredicates(paramap);
		
		ActionResult result = ActionResult.success("mesg.user.priority.sum");
		result.setData(data);
		
		this.sendResult(exchange, result);
	}
	
	/**
	 * Query latest tasks query
	 * 
	 * 
	 **/
	@WebApi(path="user-tasks-query")
	public void handleLatestTasksQuery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);
		
		svcctx.addOperationPredicates(params);
		long wgid = Filters.filterLong(params, "wgroup_id");
		String wcode = Filters.filterString(params, "wgroup_code");
		PageQuery pquery = Filters.filterPageQuery(params);
		
		List<String> features = Filters.filterList(params, "features", String.class);
		
		Principal princ = this.getPrincipal(exchange);
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.fnd.task"));
		
		InfoId gid = IdKeys.getInfoId(NodeIdKey.WORKGROUP, wgid);
		if(!IdKeys.isValidId(gid)) {
			gid = commonService.queryInfoId(wcode);
		}
		
		@SuppressWarnings("unchecked")
		Collection<String> state = (Collection<String>)params.get("states");
		
		List<TaskInfo> infos = userExtService.getLatestUserTasks(svcctx, princ.getUserId(), gid, features, state, pquery);
	
		List<Object> list = new ArrayList<Object>();
		for(TaskInfo info: infos) {
			
			DataBuilder builder = new DataBuilder();
			builder.set("task_id", info.getId().toString());
			builder.set("task_code", info.getTraceCode());
			
			builder.set("workgroup_id", info.getWorkgroupId().toString());
			builder.set("flow_id", info.getFlowId().toString());
			builder.set("task_chronical_id", info.getTaskChronicalId().toString());
			if(null != info.getTaskPid()) {
				builder.set("task_pid", info.getTaskPid().toString());
			}
			builder.set("due_time", info.getDueTime().getTime());
			builder.set("create_time", info.getCreateTime().getTime());
			
			builder.set("start_time", info.getStartTime() != null ? info.getStartTime().getTime(): "");
			builder.set("end_time", info.getEndTime() != null ? info.getEndTime().getTime() : "");
			
			builder.set(info, "title", "content", "excerpt",
					"state", "task_opinion", "precedence", "mark", "progress", "priority");
			
			builder.set("reminder_type", info.getReminderType());
			
			builder.set("owner_uid", info.getOwnerUid().toString());
			builder.set("owner", sbuilder -> {
				sbuilder.set("user_gid", info.getProperty("owner_gid"));
				sbuilder.set("username", info.getProperty("owner_username"));
				
				sbuilder.set("full_name", info.getProperty("owner_full_name"));
				sbuilder.set("nickname", info.getProperty("owner_nickname"));
				String avatarUrl = info.getProperty("owner_avatar_url", String.class);
				avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
				sbuilder.set("avatar_url", avatarUrl);
			});
			
			builder.set("assignee_uid", info.getAssigneeUid().toString());
			builder.set("assignee", sbuilder -> {
				sbuilder.set("user_gid", info.getProperty("assign_gid"));
				sbuilder.set("username", info.getProperty("assign_username"));
				
				sbuilder.set("full_name", info.getProperty("assign_full_name"));
				sbuilder.set("nickname", info.getProperty("assign_nickname"));
				String avatarUrl = info.getProperty("assign_avatar_url", String.class);
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
			
			list.add( builder.build());
		}
		
		if(null != pquery) {
			result.setData(pquery.getPagination(), list);
		}else {
			result.setData(list);
		}
	
		this.sendResult(exchange, result);
	}
	
	/**
	 * Query latest topics query
	 * 
	 * 
	 **/
	@WebApi(path="user-topics-query")
	public void handleLatestTopicsQuery(HttpServerExchange exchange) throws BaseException {
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.fnd.task"));
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);
		long wgid = Filters.filterLong(params, "wgroup_id");
		String wcode = Filters.filterString(params, "wgroup_code");
		PageQuery pquery = Filters.filterPageQuery(params);
		String type = Filters.filterAll(params, "topic_type");
		
		Principal princ = this.getPrincipal(exchange);
		
		if(Objects.equal(type, "AD_HOC") && princ.getUserId().equals(GroupUsers.ANONY_UID)){
			result = ActionResult.failure("ad hoc type requires owner global id");
			this.sendResult(exchange, result);
			return ;
		}
			
		InfoId gid = IdKeys.getInfoId(NodeIdKey.WORKGROUP, wgid);
		if(!IdKeys.isValidId(gid)) {
			gid = commonService.queryInfoId(wcode);
		}
		
		@SuppressWarnings("unchecked")
		Collection<String> state = (Collection<String>)params.get("state");
		
		List<TopicInfo> infos = userExtService.getLatestUserTopics(svcctx, princ.getUserId(), gid, type, state, pquery);
				
		List<Object> list = new ArrayList<Object>();
		for(TopicInfo info: infos) {
			
			DataBuilder builder = new DataBuilder();
			builder.set("topic_id", info.getId().toString());
			builder.set("topic_code", info.getTraceCode());
			
			builder.set("workgroup_id", info.getWorkgroupId().toString());
		
			builder.set("create_time", info.getCreateTime().getTime());
			builder.set("close_time", info.getCloseTime() == null ? "" : info.getCloseTime().getTime());
			
			builder.set(info, "topic_type", "title", "content", "excerpt",
					"state", "precedence", "mark");
			
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
			
			list.add( builder.build());
		}
		
		if(null != pquery) {
			result.setData(pquery.getPagination(), list);
		}else {
			result.setData(list);
		}
	
		this.sendResult(exchange, result);
	}
}
