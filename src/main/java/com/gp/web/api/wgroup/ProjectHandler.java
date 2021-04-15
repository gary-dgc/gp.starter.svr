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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.GeneralConstants;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.NodeIdKey;
import com.gp.common.Operations;
import com.gp.common.ServiceContext;
import com.gp.common.Workgroups.Category;
import com.gp.dao.info.TaskInfo;
import com.gp.dao.info.TopicInfo;
import com.gp.dao.info.WorkgroupInfo;
import com.gp.exception.BaseException;
import com.gp.exception.ServiceException;
import com.gp.info.DataBuilder;
import com.gp.info.Principal;
import com.gp.pagination.PageQuery;
import com.gp.svc.CommonService;
import com.gp.svc.master.StorageService;
import com.gp.svc.wgroup.ProjectService;
import com.gp.svc.wgroup.WGroupService;
import com.gp.validate.ArgsValidator;
import com.gp.validate.ValidateMessage;
import com.gp.validate.ValidateUtils;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import com.gp.web.api.ServiceApiHelper;

import io.undertow.server.HttpServerExchange;

public class ProjectHandler extends BaseApiSupport {

	static Logger LOGGER = LoggerFactory.getLogger(ProjectHandler.class);
	
	private WGroupService workgroupService;
	private ProjectService projectService;
	private CommonService commonService;
	private StorageService storageService;
	
	public ProjectHandler() {
		workgroupService = BindScanner.instance().getBean(WGroupService.class);
		projectService = BindScanner.instance().getBean(ProjectService.class);
		commonService = BindScanner.instance().getBean(CommonService.class);
		storageService = BindScanner.instance().getBean(StorageService.class);
		
	}
	
	/**
	 * Find the user related workgroups
	 * 
	 * @param keyword the word to filter the group name
	 * @param scope the scope of workgroup
	 * @param state the state of workgroup
	 * @param mode the filter mode: ALL, MANAGE, SUPERVISE, JOINED, FAVORITE
	 * 
	 **/
	@WebApi(path="project-add")
	public void handleProjectAdd(HttpServerExchange exchange) throws BaseException {
	
		Map<String, Object> params = this.getRequestBody(exchange);
		
		Principal principal = this.getPrincipal(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.PROJ_NEW);
		
		ActionResult result = null;
		
		WorkgroupInfo info = new WorkgroupInfo();
		
		info.setSourceId(GeneralConstants.LOCAL_SOURCE);// set local workgroup id
		info.setWorkgroupName(Filters.filterString(params, "workgroup_name"));
		info.setDescription(Filters.filterString(params, "description"));
		info.setState(Filters.filterString(params, "state"));
		
		if(params.containsKey("admin_uid")) {
			info.setAdminUid(Filters.filterLong(params, "admin_uid"));
		}else {
			info.setAdminUid(principal.getUserId().getId());
		}
		if(params.containsKey("manager_uid")) {
			info.setManagerUid(Filters.filterLong(params, "manager_uid"));
		}else {
			info.setManagerUid(principal.getUserId().getId());
		}
		info.setCreatorUid(principal.getUserId().getId());
		info.setCreateTime(new Date(System.currentTimeMillis()));
		info.setOrgId(Filters.filterLong(params, "org_id"));
		info.setCategory(Category.PROJECT.name());
		info.setVisibleScope(Filters.filterString(params, "visible_scope"));
		
		info.setRepoEnable(Filters.filterBoolean(params, "repo_enable"));
		info.setTopicEnable(Filters.filterBoolean(params, "topic_enable"));
		info.setTaskEnable(Filters.filterBoolean(params, "task_enable"));
		info.setShareEnable(Filters.filterBoolean(params, "share_enable"));
		
		info.setJoinConfirm(Filters.filterBoolean(params, "join_confirm"));
		
		info.setClassification(Filters.filterString(params, "classification"));
		
		String avatarUrl = Filters.filterString(params, "avatar_url");
		
		if(!Strings.isNullOrEmpty(avatarUrl) && avatarUrl.startsWith("data:image/")){
			// process the avatar base64 image
			InfoId stgId = storageService.getDefaultStorage().getInfoId();
			avatarUrl = ServiceApiHelper.instance().cacheAvatar(stgId, avatarUrl);
			info.setAvatarUrl(avatarUrl);
		}else {
			String relativeUrl = ServiceApiHelper.instance().relativeBinaryUrl(avatarUrl);
			info.setAvatarUrl(relativeUrl);
		}

		// check the validation of user information
		Set<ValidateMessage> vmsg = ValidateUtils.validate(principal.getLocale(), info);
		vmsg.forEach( v -> {			
			if(Objects.equals(v.getProperty(), "publishScope")) {
				vmsg.remove(v);
			}
		});
		if(!Iterables.isEmpty(vmsg)){ // fail pass validation
			ServiceException svcexcp = new ServiceException(principal.getLocale(), "excp.validate");
			svcexcp.addValidateMessages(vmsg);
			throw svcexcp;
		}
	
		InfoId infoId = IdKeys.newInfoId(NodeIdKey.WORKGROUP);
		info.setInfoId(infoId);
		
		svcctx.setOperationObject(infoId);
		info.setProperty("capacity", Filters.filterLong(params, "capacity") * 1024l * 1024l);
		
		// append the capacity setting to context and send to service
		workgroupService.newWorkgroup(svcctx, info);
		
		svcctx.addOperationPredicates(info);
		
		result= ActionResult.success(getMessage(exchange, "mesg.new.project"));
 
		this.sendResult(exchange, result);
	}

	@WebApi(path="project-save")
	public void handleProjectSave(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		
		Principal principal = this.getPrincipal(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.PROJ_UPD);
		
		ActionResult result = null;
		
		WorkgroupInfo info = new WorkgroupInfo();
		
		Collection<String> keys = Sets.newHashSet("workgroup_name", "workgroup_pid", "description", "state", 
				"classification", "admin_uid", "manager_uid", "visible_scope",
				"org_id", "repo_enable", "topic_enable", "share_enable", "avatar_url");
		
		InfoId wgroupId = IdKeys.getInfoId(NodeIdKey.WORKGROUP, Filters.filterLong(params, "wgroup_id"));
		info.setInfoId(wgroupId);
		svcctx.setOperationObject(wgroupId);
		
		info.setSourceId(GeneralConstants.LOCAL_SOURCE);// set local workgroup id
		info.setWorkgroupName(Filters.filterString(params, "workgroup_name"));
		info.setDescription(Filters.filterString(params, "description"));
		info.setState(Filters.filterString(params, "state"));
		
		if(params.containsKey("admin_uid")) {
			info.setAdminUid(Filters.filterLong(params, "admin_uid"));
		}else {
			info.setAdminUid(principal.getUserId().getId());
		}
		if(params.containsKey("manager_uid")) {
			info.setManagerUid(Filters.filterLong(params, "manager_uid"));
		}else {
			info.setManagerUid(principal.getUserId().getId());
		}
		info.setCreatorUid(principal.getUserId().getId());
		info.setCreateTime(new Date(System.currentTimeMillis()));
		info.setOrgId(Filters.filterLong(params, "org_id"));
		info.setCategory(Category.PROJECT.name());
		info.setVisibleScope(Filters.filterString(params, "visible_scope"));
		
		info.setRepoEnable(Filters.filterBoolean(params, "repo_enable"));
		info.setTopicEnable(Filters.filterBoolean(params, "topic_enable"));
		info.setTaskEnable(Filters.filterBoolean(params, "task_enable"));
		info.setShareEnable(Filters.filterBoolean(params, "share_enable"));
		
		info.setJoinConfirm(Filters.filterBoolean(params, "join_confirm"));
		
		info.setClassification(Filters.filterString(params, "classification"));
		
		String avatarUrl = Filters.filterString(params, "avatar_url");
		
		if(!Strings.isNullOrEmpty(avatarUrl) && avatarUrl.startsWith("data:image/")){
			// process the avatar base64 image
			InfoId stgId = storageService.getDefaultStorage().getInfoId();
			avatarUrl = ServiceApiHelper.instance().cacheAvatar(stgId, avatarUrl);
			info.setAvatarUrl(avatarUrl);
		}else {
			String relativeUrl = ServiceApiHelper.instance().relativeBinaryUrl(avatarUrl);
			info.setAvatarUrl(relativeUrl);
		}

		// check the validation of user information
		Set<ValidateMessage> vmsg = ValidateUtils.validateProperty(principal.getLocale(), info, keys.toArray(new String[0]));
		vmsg.forEach( v -> {
			
			if(Objects.equals(v.getProperty(), "publishScope")) {
				vmsg.remove(v);
			}
		});
		if(!Iterables.isEmpty(vmsg)){ // fail pass validation
			ServiceException svcexcp = new ServiceException(principal.getLocale(), "excp.validate");
			svcexcp.addValidateMessages(vmsg);
			throw svcexcp;
		}
					
		info.setProperty("capacity", Filters.filterLong(params, "capacity") * 1024l * 1024l);
		keys.add("capacity");
		
		// append the capacity setting to context and send to service
		info.setPropertyFilter(keys);
		workgroupService.updateWorkgroup(svcctx, info );
		
		svcctx.addOperationPredicates(info);
		
		result= ActionResult.success(getMessage(exchange, "mesg.save.project"));
 
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="projects-query")
	public void handleProjectsQuery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.PROJ_FND);
		svcctx.addOperationPredicates(paramap);
		
		String keyword = Filters.filterString(paramap, "keyword");
		String state = Filters.filterString(paramap, "state");
		PageQuery pquery = Filters.filterPageQuery(paramap);
		
		ActionResult result = null;
		List<Object> data = Lists.newArrayList();
		
		List<WorkgroupInfo> infos = projectService.getProjects(svcctx, keyword, 
				Strings.isNullOrEmpty(state) ? null: Sets.newHashSet(state),
				pquery);

		data = infos.stream().map((info)->{
			DataBuilder builder = new DataBuilder();
			
			builder.set("workgroup_id", info.getId().toString());
			builder.set("workgroup_code", info.getTraceCode());
			
			builder.set("workgroup_name", info.getWorkgroupName());
			
			builder.set(info, "description","state", "visible_scope");
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

	@WebApi(path="bind-tasks-query", operation="proj:fnd")
	public void handleBindTasksQuery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);

		svcctx.addOperationPredicates(paramap);
		
		String projectCode = Filters.filterString(paramap, "project_code");
		String wgroupCode = Filters.filterAll(paramap, "wgroup_code");
		String keyword = Filters.filterAll(paramap, "keyword");
		
		InfoId pkey = commonService.queryInfoId(projectCode);
		InfoId wkey = commonService.queryInfoId(wgroupCode);
		
		PageQuery pquery = Filters.filterPageQuery(paramap);
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.fnd.task"));
		
		List<TaskInfo> infos = projectService.getBindTasks(svcctx, pkey, wkey, keyword, null, pquery);
		
		List<Object> data = new ArrayList<Object>();
		for(TaskInfo info: infos) {
			
			DataBuilder builder = new DataBuilder();
			builder.set("task_id", info.getId().toString());
			builder.set("task_code", info.getTraceCode());
			
			builder.set("workgroup_id", info.getWorkgroupId().toString());
			builder.set("task_chronical_id", info.getTaskChronicalId().toString());
			if(null != info.getTaskPid()) {
				builder.set("task_pid", info.getTaskPid().toString());
			}
			builder.set("due_time", info.getDueTime().getTime());
			builder.set("create_time", info.getCreateTime().getTime());
			
			builder.set("start_time", info.getStartTime() != null ? info.getStartTime().getTime(): "");
			builder.set("end_time", info.getEndTime() != null ? info.getEndTime().getTime() : "");
			
			builder.set(info, "title", "content", "excerpt",
					"state", "task_opinion");
			
			builder.set("reminder_type", info.getReminderType());
			
			builder.set("owner", sbuilder -> {
				sbuilder.set("user_gid", info.getProperty("owner_gid"));
				sbuilder.set("username", info.getProperty("owner_username"));
				
				sbuilder.set("full_name", info.getProperty("owner_full_name"));
				sbuilder.set("nickname", info.getProperty("owner_nickname"));
				String avatarUrl = info.getProperty("owner_avatar_url", String.class);
				avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
				sbuilder.set("avatar_url", avatarUrl);
			});
			builder.set("assignee", sbuilder -> {
				sbuilder.set("user_gid", info.getProperty("assign_gid"));
				sbuilder.set("username", info.getProperty("assign_username"));
				
				sbuilder.set("full_name", info.getProperty("assign_full_name"));
				sbuilder.set("nickname", info.getProperty("assign_nickname"));
				String avatarUrl = info.getProperty("assign_avatar_url", String.class);
				avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
				sbuilder.set("avatar_url", avatarUrl);
			});
			
			data.add( builder.build());
		}
		
		result.setData(data);
	
		this.sendResult(exchange, result);
		
	}
	
	@WebApi(path="bind-topics-query", operation="proj:fnd")
	public void handleBindTopicsQuery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);

		svcctx.addOperationPredicates(paramap);
		
		String projectCode = Filters.filterString(paramap, "project_code");
		String wgroupCode = Filters.filterAll(paramap, "wgroup_code");
		String keyword = Filters.filterAll(paramap, "keyword");
		
		InfoId pkey = commonService.queryInfoId(projectCode);
		InfoId wkey = commonService.queryInfoId(wgroupCode);
		
		PageQuery pquery = Filters.filterPageQuery(paramap);
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.fnd.topic"));
		
		List<Object> data = Lists.newArrayList();
		
		List<TopicInfo> infos = projectService.getBindTopics(svcctx, pkey, wkey, keyword, null, pquery);
				
		for(TopicInfo info: infos) {
			
			DataBuilder builder = new DataBuilder();
			builder.set("topic_id", info.getId().toString());
			builder.set("topic_code", info.getTraceCode());
			
			builder.set("workgroup_id", info.getWorkgroupId().toString());
		
			builder.set("create_time", info.getCreateTime().getTime());
			
			builder.set(info, "topic_type", "title", "content", "excerpt",
					"state");
			
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
			
			data.add( builder.build());
		}
		
		result.setData(data);
	
		this.sendResult(exchange, result);
	}

	@WebApi(path="task-binds-query", operation="proj:fnd")
	public void handleTaskBindsQuery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);

		svcctx.addOperationPredicates(paramap);
		
		String taskCode = Filters.filterString(paramap, "task_code");
		Long taskId = Filters.filterLong(paramap, "task_id");
		
		InfoId tkey = IdKeys.getInfoId(NodeIdKey.TASK, taskId);
		if(!IdKeys.isValidId(tkey)) {
			tkey = commonService.queryInfoId(taskCode);
		}
		ActionResult result = null;
		List<Object> data = Lists.newArrayList();
		
		List<WorkgroupInfo> infos = projectService.getResourceBindProjects(svcctx, tkey, null, null);

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
		result.setData(data);
 
		this.sendResult(exchange, result);

	}
	
	@WebApi(path="topic-binds-query", operation="proj:fnd")
	public void handleTopicBindsQuery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);

		svcctx.addOperationPredicates(paramap);
		
		String topicCode = Filters.filterString(paramap, "topic_code");
		Long topicId = Filters.filterLong(paramap, "topic_id");
		
		InfoId tkey = IdKeys.getInfoId(NodeIdKey.TOPIC, topicId);
		if(!IdKeys.isValidId(tkey)) {
			tkey = commonService.queryInfoId(topicCode);
		}
		ActionResult result = null;
		List<Object> data = Lists.newArrayList();
		
		List<WorkgroupInfo> infos = projectService.getResourceBindProjects(svcctx, tkey, null, null);

		data = infos.stream().map((info)->{
			DataBuilder builder = new DataBuilder();
			
			builder.set("workgroup_id", info.getId().toString());
			builder.set("admin_uid", info.getAdminUid().toString());
			builder.set("manger_uid", info.getManagerUid().toString());
			builder.set("workgroup_name", info.getWorkgroupName());
			
			builder.set(info, "description","state", "publish_scope", "visible_scope");
			builder.set("category", info.getCategory());
			builder.set("workgroup_code", info.getTraceCode());
			
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
		result.setData(data);
 
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="project-binds-query", operation="proj:fnd")
	public void handleProjectBindsQuery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);

		svcctx.addOperationPredicates(paramap);
		
		Long projectid = Filters.filterLong(paramap, "wgroup_id");
		String projectCode = Filters.filterString(paramap, "wgroup_code");
		
		@SuppressWarnings("unchecked")
		Collection<String> state = (Collection<String>)paramap.get("state");
		PageQuery pquery = Filters.filterPageQuery(paramap);
		
		ActionResult result = null;
		List<Object> data = Lists.newArrayList();
		
		InfoId pkey = IdKeys.getInfoId(NodeIdKey.WORKGROUP, projectid);
		if(!IdKeys.isValidId(pkey)) {
			pkey = commonService.queryInfoId(projectCode);
		}
		List<WorkgroupInfo> infos = projectService.getBindWorkgroups(svcctx, pkey, state, pquery);

		data = infos.stream().map((info)->{
			DataBuilder builder = new DataBuilder();
			
			builder.set("workgroup_id", info.getId().toString());
			builder.set("admin_uid", info.getAdminUid().toString());
			builder.set("manger_uid", info.getManagerUid().toString());
			builder.set("workgroup_name", info.getWorkgroupName());
			
			builder.set(info, "description","state", "publish_scope", "visible_scope");
			builder.set("category", info.getCategory());
			builder.set("workgroup_code", info.getTraceCode());
			
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

	@WebApi(path="wgroup-binds-query", operation="proj:fnd")
	public void handleWorkgroupBindsQuery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);

		svcctx.addOperationPredicates(paramap);
		
		Long wgrpid = Filters.filterLong(paramap, "wgroup_id");
		String wgrpCode = Filters.filterString(paramap, "wgroup_code");
		@SuppressWarnings("unchecked")
		Collection<String> state = (Collection<String>)paramap.get("state");
		PageQuery pquery = Filters.filterPageQuery(paramap);
		
		ActionResult result = null;
		List<Object> data = Lists.newArrayList();
		
		InfoId wkey = IdKeys.getInfoId(NodeIdKey.WORKGROUP, wgrpid);
		if(!IdKeys.isValidId(wkey)) {
			wkey = commonService.queryInfoId(wgrpCode);
		}
		List<WorkgroupInfo> infos = projectService.getBindProjects(svcctx, wkey, state, pquery);

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
	
	@WebApi(path="project-bind")
	public void handleProjectBind(HttpServerExchange exchange) throws BaseException {
	
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.PROJ_UPD);
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.requireOne("project_id", "project_code")
			.require("resource_code")
			.validate(true);
		
		Long projectid = Filters.filterLong(params, "project_id");
		String projectCode = Filters.filterString(params, "project_code");
		String resourceCode = Filters.filterAll(params, "resource_code");
		
		ActionResult result = null;
		InfoId projectId = IdKeys.getInfoId(NodeIdKey.WORKGROUP, projectid);
		if(!IdKeys.isValidId(projectId)) {
			projectId = commonService.queryInfoId(projectCode);
		}
		
		InfoId resourceId = commonService.queryInfoId(resourceCode);
		String category = commonService.queryColumn(projectId, "category", String.class);
		
		if(!Category.PROJECT.name().equals(category)) {
			
			result = ActionResult.failure("excp.illegal.proj");
			this.sendResult(exchange, result);
			return;
		}
		
		if(resourceId.getIdKey().isSameSchema(NodeIdKey.WORKGROUP)) {
			
			projectService.bindWorkgroup(svcctx, resourceId, projectId);
			
		} else if(resourceId.getIdKey().isSameSchema(NodeIdKey.TASK)) {
			
			projectService.bindTask(svcctx, resourceId, projectId);
		} else if(resourceId.getIdKey().isSameSchema(NodeIdKey.TOPIC)) {
			
			projectService.bindTopic(svcctx, resourceId, projectId);
		}
		result = ActionResult.success("mesg.bind.project");
		
		this.sendResult(exchange, result);
	}

	@WebApi(path="project-unbind")
	public void handleProjectUnBind(HttpServerExchange exchange) throws BaseException {
	
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.PROJ_UPD);
		svcctx.addOperationPredicates(params);
		
		Long projectid = Filters.filterLong(params, "project_id");
		String projectCode = Filters.filterString(params, "project_code");
		String resourceCode = Filters.filterAll(params, "resource_code");
		
		ActionResult result = null;
		
		InfoId projectId = IdKeys.getInfoId(NodeIdKey.WORKGROUP, projectid);
		if(!IdKeys.isValidId(projectId)) {
			projectId = commonService.queryInfoId(projectCode);
		}
		InfoId resourceId = commonService.queryInfoId(resourceCode);
		String category = commonService.queryColumn(projectId, "category", String.class);
		
		if(!Category.PROJECT.name().equals(category)) {
			
			result = ActionResult.failure("excp.illegal.proj");
			this.sendResult(exchange, result);
			return ;
		}
		
		if(resourceId.getIdKey().isSameSchema(NodeIdKey.WORKGROUP)) {
			
			projectService.unbindWorkgroup(svcctx, resourceId, projectId);
			
		} else if(resourceId.getIdKey().isSameSchema(NodeIdKey.TASK)) {
			
			projectService.unbindTask(svcctx, resourceId, projectId);
		} else if(resourceId.getIdKey().isSameSchema(NodeIdKey.TOPIC)) {
			
			projectService.unbindTopic(svcctx, resourceId, projectId);
		}
		result = ActionResult.success("mesg.unbind.project");
		
		this.sendResult(exchange, result);
	}
}
