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
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.GeneralConstants;
import com.gp.common.GroupUsers;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.NodeIdKey;
import com.gp.common.Operations;
import com.gp.common.ServiceContext;
import com.gp.common.Workgroups;
import com.gp.common.Workgroups.Category;
import com.gp.dao.info.GroupUserInfo;
import com.gp.dao.info.OrgHierInfo;
import com.gp.dao.info.WorkgroupInfo;
import com.gp.dao.info.WorkgroupStatInfo;
import com.gp.exception.BaseException;
import com.gp.exception.ServiceException;
import com.gp.exception.WebException;
import com.gp.info.BaseIdKey;
import com.gp.info.DataBuilder;
import com.gp.info.InfoCopier;
import com.gp.info.Principal;
import com.gp.pagination.PageQuery;
import com.gp.svc.CommonService;
import com.gp.svc.master.OrgHierService;
import com.gp.svc.master.StorageService;
import com.gp.svc.wgroup.WGroupExtraService;
import com.gp.svc.wgroup.WGroupService;
import com.gp.util.NumberUtils;
import com.gp.validate.ValidateMessage;
import com.gp.validate.ValidateUtils;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import com.gp.web.api.ServiceApiHelper;

import io.undertow.server.HttpServerExchange;

public class WGroupHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(WGroupHandler.class);
	
	private WGroupService workgroupService;
	private WGroupExtraService workgroupExtraService;
	
	private StorageService storageService;
	
	private OrgHierService orghierService;

	private CommonService commonService;
	
	public WGroupHandler() {
		
		workgroupService = BindScanner.instance().getBean(WGroupService.class);
		workgroupExtraService = BindScanner.instance().getBean(WGroupExtraService.class);
		storageService = BindScanner.instance().getBean(StorageService.class);
		orghierService = BindScanner.instance().getBean(OrgHierService.class);
		commonService = BindScanner.instance().getBean(CommonService.class);
		
	}

	@WebApi(path="wgroups-query")
	public void handleWorkgroupsQuery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.WGRP_FND);
		svcctx.addOperationPredicates(paramap);
		
		String keywords = Filters.filterAll(paramap, "keyward");
		String scope = Filters.filterAll(paramap, "scope");
		String state = Filters.filterAll(paramap, "state");
		String category = Filters.filterAll(paramap, "category");
		PageQuery pquery = Filters.filterPageQuery(paramap);
		
		ActionResult result = null;

		List<Map<String, Object>> list = Lists.newArrayList();
		
		// amend the operation information
		svcctx.addOperationPredicates(paramap);
		List<WorkgroupInfo> wlist = workgroupService.getWorkgroups(svcctx, keywords, 
				Strings.isNullOrEmpty(scope) ? null: new String[] {scope}, 
				Strings.isNullOrEmpty(state) ? null: new String[] {state}, 
				Strings.isNullOrEmpty(category) ? null: new String[] {category}, 
				pquery);
		
		list = wlist.stream().map((info)->{
			DataBuilder builder = new DataBuilder();
			
			builder.set("workgroup_id", info.getId().toString());
			builder.set("workgroup_code", info.getTraceCode());

			builder.set("workgroup_name", info.getWorkgroupName());
			
			builder.set(info, "description", "state", "publish_scope", "visible_scope");
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
		result.setData(pquery == null ? null : pquery.getPagination(), list);
 
		this.sendResult(exchange, result);
	}

	@WebApi(path="wgroup-add")
	public void handleWorkgroupAdd(HttpServerExchange exchange)throws BaseException{
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.WGRP_NEW);
		
		Principal principal = this.getPrincipal(exchange);
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
		info.setCategory(Category.NORMAL.name());
		info.setPublishScope(Filters.filterString(params, "publish_scope"));
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
		
		if(!Iterables.isEmpty(vmsg)){ // fail pass validation
			ServiceException svcexcp = new ServiceException(svcctx.getPrincipal().getLocale(), "excp.validate");
			svcexcp.addValidateMessages(vmsg);
			throw svcexcp;
		}
	
		InfoId infoId = IdKeys.newInfoId(NodeIdKey.WORKGROUP);
		info.setInfoId(infoId);
		
		svcctx.setOperationObject(infoId);
		
		info.setProperty("capacity", Filters.filterLong(params, "capacity") * 1024l * 1024l);
		// append the capacity setting to context and send to service
		workgroupService.newWorkgroup(svcctx, info);
		// add to core event as predicates
		svcctx.addOperationPredicates(info);
		
		result= ActionResult.success(getMessage(exchange, "mesg.new.wgroup"));
		
		this.sendResult(exchange, result);

	}
	
	@WebApi(path="wgroup-lite-add")
	public void handleWorkgroupAddLite(HttpServerExchange exchange)throws BaseException{
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.WGRP_NEW);
		Principal principal = this.getPrincipal(exchange);
		ActionResult result = null;
		
		WorkgroupInfo info = new WorkgroupInfo();
		
		info.setSourceId(GeneralConstants.LOCAL_SOURCE);// set local workgroup id
		info.setWorkgroupName(Filters.filterString(params, "workgroup_name"));
		info.setDescription(Filters.filterString(params, "description"));
		info.setState(Filters.filterString(params, "state"));
		
		if(Strings.isNullOrEmpty(info.getState())) {
			info.setState(Workgroups.State.READ_WRITE.name());
		}
		
		info.setCategory(Category.NORMAL.name());
		
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
		
		info.setPublishScope(Filters.filterString(params, "publish_scope"));
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
			
		if(!Iterables.isEmpty(vmsg)){ // fail pass validation
			ServiceException svcexcp = new ServiceException(svcctx.getPrincipal().getLocale(), "excp.validate");
			svcexcp.addValidateMessages(vmsg);
			throw svcexcp;
		}
	
		InfoId infoId = IdKeys.newInfoId(NodeIdKey.WORKGROUP);
		info.setInfoId(infoId);
		
		svcctx.setOperationObject(infoId);
		info.setProperty("capacity", Filters.filterInt(params, "capacity") * 1024 * 1024);
		// append the capacity setting to context and send to service
		workgroupService.newWorkgroup(svcctx, info);
		
		svcctx.addOperationPredicates(info);
	
		result= ActionResult.success(getMessage(exchange, "mesg.new.wgroup"));
 
		this.sendResult(exchange, result);

	}
	
	@WebApi(path="wgroup-save-prop")
	public void handleWorkgroupUpdateProp(HttpServerExchange exchange)throws BaseException{
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.WGRP_UPD);
		
		Principal principal = this.getPrincipal(exchange);

		ActionResult result = new ActionResult();
		WorkgroupInfo info = new WorkgroupInfo();
		
		String category = Filters.filterString(params, "category");
		InfoId wgroupId = IdKeys.getInfoId(NodeIdKey.WORKGROUP, Filters.filterLong(params, "wgroup_id"));
		
		if(!IdKeys.isValidId(wgroupId) || Strings.isNullOrEmpty(category)) {
			result= ActionResult.success(getMessage(exchange, "excp.miss.param"));
			this.sendResult(exchange, result);
			
			return;
		}
		
		Collection<String> valids = Sets.newHashSet("workgroup_name", "workgroup_pid", "description", "state", 
				"classification", "admin_uid", "manager_uid", "publish_scope", "visible_scope",
				"org_id", "repo_enable", "topic_enable", "share_enable", "avatar_url");
		
		// reserve only the valid keys
		Set<String> keys = valids.stream().filter(k -> {
			return params.containsKey(k);
		}).collect(Collectors.toSet());
		
		// Project not support publish scope setting
		if(Category.PROJECT.name().equals(category)) {
			keys.remove("publish_scope");
			svcctx.setOperation(Operations.PROJ_UPD);
		}
		
		info.setInfoId(wgroupId);
		info.setSourceId(GeneralConstants.LOCAL_SOURCE);// set local workgroup id
		info.setWorkgroupName(Filters.filterString(params, "workgroup_name"));
		info.setWorkgroupPid(Filters.filterLong(params, "workgroup_pid"));
		info.setDescription(Filters.filterString(params, "description"));
		info.setState(Filters.filterString(params, "state"));
		info.setClassification(Filters.filterString(params, "classification"));
		info.setAdminUid(Filters.filterLong(params, "admin_uid"));
		info.setManagerUid(Filters.filterLong(params, "manager_uid"));
		info.setPublishScope(Filters.filterString(params, "publish_scope"));
		info.setVisibleScope(Filters.filterString(params, "visible_scope"));
		info.setOrgId(Filters.filterLong(params, "org_id"));

		info.setRepoEnable(Filters.filterBoolean(params, "repo_enable"));
		info.setTopicEnable(Filters.filterBoolean(params, "topic_enable"));
		info.setTaskEnable(Filters.filterBoolean(params, "task_enable"));
		info.setShareEnable(Filters.filterBoolean(params, "share_enable"));

		String avatarUrl = Filters.filterString(params, "avatar_url");
		
		if(!Strings.isNullOrEmpty(avatarUrl) && avatarUrl.startsWith("data:image/")){
			// process the avatar base64 image
			InfoId stgId = storageService.getDefaultStorage().getInfoId();
			avatarUrl = ServiceApiHelper.instance().cacheAvatar(stgId, avatarUrl);
			info.setAvatarUrl(avatarUrl);
		
		} else if (!Strings.isNullOrEmpty(avatarUrl)) {
			
			String relativeUrl = ServiceApiHelper.instance().relativeBinaryUrl(avatarUrl);
			info.setAvatarUrl(relativeUrl);
		}
		
		Set<ValidateMessage> vmsg = ValidateUtils.validateProperty(principal.getLocale(), info, keys.toArray(new String[0]));
		
		if(null != vmsg && vmsg.size() > 0){ // fail pass validation
			ServiceException svcexcp = new ServiceException(svcctx.getPrincipal().getLocale(), "excp.validate");
			svcexcp.addValidateMessages(vmsg);
			throw svcexcp;
		}
	
		// amend the operation information
		svcctx.setOperationObject(wgroupId);
		Map<String, Object> prop = Maps.newHashMap();
		InfoCopier.copyToMap(info, prop, (String key) -> {
			return keys.contains(key);
		});
		svcctx.addOperationPredicates(prop);
		// set target affected properties
		info.setPropertyFilter(keys);
		workgroupService.updateWorkgroup(svcctx, info);
		
		result= ActionResult.success(getMessage(exchange, "mesg.save.wgroup"));
		
		this.sendResult(exchange, result);

	}
	
	@WebApi(path="wgroup-save")
	public void handleWorkgroupSave(HttpServerExchange exchange)throws BaseException{
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.WGRP_UPD);
		
		Principal principal = this.getPrincipal(exchange);

		ActionResult result = new ActionResult();
		WorkgroupInfo info = new WorkgroupInfo();
		
		Collection<String> keys = Sets.newHashSet("workgroup_name", "workgroup_pid", "description", "state", 
				"classification", "admin_uid", "manager_uid", "publish_scope", "visible_scope",
				"org_id", "repo_enable", "topic_enable", "share_enable", "avatar_url");
		
		InfoId wgroupId = IdKeys.getInfoId(NodeIdKey.WORKGROUP, Filters.filterLong(params, "wgroup_id"));
		info.setInfoId(wgroupId);
		
		info.setSourceId(GeneralConstants.LOCAL_SOURCE);// set local workgroup id
		info.setWorkgroupName(Filters.filterString(params, "workgroup_name"));
		info.setWorkgroupPid(Filters.filterLong(params, "workgroup_pid"));
		info.setDescription(Filters.filterString(params, "description"));
		info.setState(Filters.filterString(params, "state"));
		info.setClassification(Filters.filterString(params, "classification"));
		info.setAdminUid(Filters.filterLong(params, "admin_uid"));
		info.setManagerUid(Filters.filterLong(params, "manager_uid"));
		info.setPublishScope(Filters.filterString(params, "publish_scope"));
		info.setVisibleScope(Filters.filterString(params, "visible_scope"));
		info.setOrgId(Filters.filterLong(params, "org_id"));
		
		info.setRepoEnable(Filters.filterBoolean(params, "repo_enable"));
		info.setTopicEnable(Filters.filterBoolean(params, "topic_enable"));
		info.setTaskEnable(Filters.filterBoolean(params, "task_enable"));
		info.setShareEnable(Filters.filterBoolean(params, "share_enable"));
		
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
		if(null != vmsg && vmsg.size() > 0){ // fail pass validation
			ServiceException svcexcp = new ServiceException(svcctx.getPrincipal().getLocale(), "excp.validate");
			svcexcp.addValidateMessages(vmsg);
			throw svcexcp;
		}
		
		// amend the operation information
		svcctx.setOperationObject(info.getInfoId());
		svcctx.addOperationPredicates(info);

		info.setProperty("capacity", Filters.filterLong(params, "capacity") * 1024l * 1024l);
		keys.add("capacity");
		
		info.setPropertyFilter(keys);
		workgroupService.updateWorkgroup(svcctx, info );
	
		result= ActionResult.success(getMessage(exchange, "mesg.save.wgroup"));

		this.sendResult(exchange, result);

	}
	
	@WebApi(path="wgroup-info")
	public void handleWorkgroupInfo(HttpServerExchange exchange)throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.WGRP_INF);
		
		long wgid = Filters.filterLong(params, "wgroup_id");
		String wcode = Filters.filterString(params, "wgroup_code");
		ActionResult result = new ActionResult();
		
		if(wgid <= 0 && Strings.isNullOrEmpty(wcode)){
			result = ActionResult.failure("miss parameters");
			this.sendResult(exchange, result);
			return ;
		}
		
		DataBuilder builder = new DataBuilder();
		
		// amend the operation information
		svcctx.addOperationPredicates(params);
		
		InfoId wgroupId = IdKeys.getInfoId(NodeIdKey.WORKGROUP, wgid);
		WorkgroupInfo info = workgroupService.getWorkgroupExt(wgroupId, wcode);
		svcctx.setOperationObject(info.getInfoId());
		
		builder.set(info, "workgroup_id", "workgroup_name", "workgroup_pid",
					"description", "state", 
					"publish_scope", "repo_enable", "topic_enable", "share_enable",
					"task_enable", "join_confirm", "visible_scope", "classification");
		
		builder.set("workgroup_code", info.getTraceCode());
		builder.set("category", info.getCategory());
		
		builder.set("create_time", info.getCreateTime().getTime());
		
		String avatarUrl = ServiceApiHelper.absoluteBinaryUrl(info.getAvatarUrl());
		builder.set("avatar_url", avatarUrl);
		
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
		
		builder.set("source", sbuilder -> {
			sbuilder.set(info, "source_name", "entity_gid", "node_gid", "entity_name");
		});
		
		OrgHierInfo orghier = orghierService.getOrgHierNode(IdKeys.getInfoId(NodeIdKey.ORG_HIER, info.getOrgId()));
		builder.set("org", obuilder -> {
			obuilder.set("org_id", info.getOrgId().toString());
			if(null != orghier) {
				obuilder.set("org_name", orghier.getOrgName());
			}
		});
		
		builder.set("storage", sbuilder-> {
			
			sbuilder.set("storage_id", String.valueOf(info.getProperty("storage_id")));
			sbuilder.set("storage_name", info.getProperty("storage_name"));
			sbuilder.set("cabinet_id", String.valueOf(info.getProperty("cabinet_id")));
			
			long capacity = info.getProperty("capacity", Long.class)/ (1024l * 1024l);
			sbuilder.set("capacity", capacity);
		});
			
		result = ActionResult.success(getMessage(exchange, "mesg.find.wgroup"));
		result.setData(builder.build());
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="wgroup-members-query")
	public void handleWorkgroupMembersQuery(HttpServerExchange exchange)throws BaseException {
		
		Map<String, Object> params =  this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.WGRP_FND_MBR);
		
		String keyword = Filters.filterString(params, "keyword");
		String role = Filters.filterString(params, "role");
		String wgroupCode = Filters.filterString(params, "wgroup_code");
		Long wgroupid = Filters.filterLong(params, "wgroup_id");
		
		ActionResult result = null;
		if(Strings.isNullOrEmpty(wgroupCode) && wgroupid == 0){
			result = ActionResult.failure("miss parameters");
			this.sendResult(exchange, result);
			return ;
		}
		
		
		InfoId wkey = IdKeys.getInfoId(NodeIdKey.WORKGROUP, wgroupid);
		
		if(!IdKeys.isValidId(wkey)) {
			wkey = commonService.queryInfoId(wgroupCode);
		}
		List<Object> list = new ArrayList<Object>();
		
		// amend the operation information
		svcctx.setOperationObject(wkey);
		
		List<GroupUserInfo> ulist =  workgroupService.getWorkgroupMembers( wkey, wgroupCode, keyword, role, null);
		list = ulist.stream().map((info)->{
			
			DataBuilder builder = new DataBuilder();
			builder.set(info, "user_gid", "username", "email", "mobile", 
					"category", "full_name");
			
			builder.set("member_uid", info.getMemberUid().toString());
			
			builder.set("source", sbuilder -> {
				sbuilder.set("source_id", info.getProperty("source_id", Long.class).toString());
				sbuilder.set(info, "source_name");
			});
			String avatarUrl = info.getProperty("avatar_url", String.class);
			avatarUrl = ServiceApiHelper.absoluteBinaryUrl( avatarUrl);
			builder.set("avatar_url", avatarUrl);
			
			return builder.build();
			
		}).collect(Collectors.toList());
		
		result = ActionResult.success(getMessage(exchange, "mesg.find.wgroup.mbr"));
		result.setData(list);

		this.sendResult(exchange, result);
	}

	@SuppressWarnings("unchecked")
	@WebApi(path="wgroup-member-remove")
	public void handleRemoveWorkgroupMember(HttpServerExchange exchange)throws BaseException {
		
		ActionResult aresult = null;
		Map<String, Object> params =  this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.WGRP_RMV_MBR);
		
		List<String> mbrs = (List<String>) params.get("member");
		List<Long> memberIds = Lists.newArrayList();
		if(null == mbrs || mbrs.isEmpty()) {
			throw new WebException("excp.miss.param");
		}
		
		mbrs.forEach(m -> {
			memberIds.add(NumberUtils.toLong(m));
		});
		
		String wgroupcode = (String)params.get("wgroup_code");
		Long wgroupid = Filters.filterLong(params, "wgroup_id");
		
		InfoId wkey = IdKeys.getInfoId(NodeIdKey.WORKGROUP, wgroupid);
		if(!IdKeys.isValidId(wkey)) {
			wkey = commonService.queryInfoId(wgroupcode);
		}
		// amend the operation information
		svcctx.setOperationObject(wkey);
		svcctx.addOperationPredicate("members",memberIds);
		
		List<InfoId> ids = memberIds.stream().map( id -> {
			return IdKeys.getInfoId(BaseIdKey.USER, id);
		}).collect(Collectors.toList());
		
		boolean[] rtv = workgroupService.removeWorkgroupMember(svcctx, wkey, ids.toArray(new InfoId[0]));
		
		Map<String, Boolean> data = Maps.newHashMap();
		boolean failFlag = false;
		for(int i = 0 ; i < memberIds.size(); i++) {
			data.put(memberIds.get(i).toString(),  rtv[i]);
			if(!rtv[i] && !failFlag) {
				failFlag = true;
			}
		}
		aresult = failFlag ?  ActionResult.failure( getMessage(exchange, "mesg.remove.partial.mbr")) 
				: ActionResult.success( getMessage(exchange, "mesg.remove.wgroup.mbr"));
		
		aresult.setData(data);
		
		this.sendResult(exchange, aresult);
	}

	@SuppressWarnings("unchecked")
	@WebApi(path="wgroup-member-add")
	public void handleAddWorkgroupMember(HttpServerExchange exchange)throws BaseException {
		
		Map<String, Object> params =  this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.WGRP_ADD_MBR);
		
		List<String> mbrs = (List<String>) params.get("member");
		List<Long> memberIds = Lists.newArrayList();
		if(null == mbrs || mbrs.isEmpty()) {
			throw new WebException("excp.miss.param");
		}
		
		mbrs.forEach(m -> {
			memberIds.add(NumberUtils.toLong(m));
		});
		
		String wgroupcode = (String)params.get("wgroup_code");
		Long wgroupid = Filters.filterLong(params, "wgroup_id");
		String classification = (String)params.get("classification"); // (GroupUsers.Classification.INTERN_ONLY.name());
		String role = (String)params.get("role"); // (Workgroups.Role.MANAGE.name());
				
		InfoId wkey = IdKeys.getInfoId(NodeIdKey.WORKGROUP, wgroupid);
		if(!IdKeys.isValidId(wkey)) {
			wkey = commonService.queryInfoId(wgroupcode);
		}

		ActionResult result = null;

		List<GroupUserInfo> infos = memberIds.stream().map( memberid -> {
			
			GroupUserInfo wuinfo = new GroupUserInfo();
			
			wuinfo.setMemberUid(memberid);
			wuinfo.setType(GroupUsers.MemberType.USER.name());
			wuinfo.setRole(role);
			wuinfo.setClassification(classification);
			
			return wuinfo;
			
		}).collect(Collectors.toList());
		
		// amend the operation information
		svcctx.setOperationObject(wkey);
		svcctx.addOperationPredicate("members", memberIds);
		svcctx.addOperationPredicate("classification", classification);
		svcctx.addOperationPredicate("role", role);
		
		workgroupService.addWorkgroupMember(svcctx, wkey, infos.toArray(new GroupUserInfo[0]));

		result = ActionResult.success(getMessage(exchange, "mesg.add.wgroup.mbr"));
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="wgroup-summary")
	public void handleWorkgroupSummaryInfo(HttpServerExchange exchange)throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.WGRP_SUM);
		
		long groupId = Filters.filterLong(params, "wgroup_id");
		String groupCode = Filters.filterString(params, "wgroup_code");
		
		InfoId gid = IdKeys.getInfoId(NodeIdKey.WORKGROUP, groupId);
		if(!IdKeys.isValidId(gid)) {
			gid = commonService.queryInfoId(NodeIdKey.WORKGROUP, "trace_code = '" + groupCode + "'");
		}
		// amend the operation information
		svcctx.setOperationObject(gid);
		WorkgroupStatInfo sumInfo = workgroupExtraService.getWorkgroupSummary(gid);
		
		DataBuilder builder = new DataBuilder();
		builder.set("workgroup_id", null == sumInfo ? "" : sumInfo.getWorkgroupId().toString());
		builder.set("file_cnt", null == sumInfo ? 0 : sumInfo.getFileCnt());
		builder.set("task_cnt", null == sumInfo ? 0 : sumInfo.getTaskCnt());
		builder.set("member_cnt", null == sumInfo ? 0 : sumInfo.getMemberCnt());
		builder.set("publish_cnt", null == sumInfo ? 0 : sumInfo.getPublishCnt());
		builder.set("topic_cnt", null == sumInfo ? 0 : sumInfo.getTopicCnt());
		builder.set("folder_cnt", null == sumInfo ? 0 : sumInfo.getFolderCnt());
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.wgroup.sum"));
		result.setData(builder.build());
		
		this.sendResult(exchange, result);
	}

}
