/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.wgroup;

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
import com.google.common.collect.Maps;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.NodeIdKey;
import com.gp.common.Operations;
import com.gp.common.ServiceContext;
import com.gp.dao.info.GroupInfo;
import com.gp.dao.info.UserInviteInfo;
import com.gp.dao.info.UserJoinInfo;
import com.gp.dao.info.WorkgroupStatInfo;
import com.gp.exception.BaseException;
import com.gp.exception.ServiceException;
import com.gp.info.DataBuilder;
import com.gp.info.Principal;
import com.gp.pagination.PageQuery;
import com.gp.svc.CommonService;
import com.gp.svc.wgroup.WGroupExtraService;
import com.gp.svc.wgroup.WGroupService;
import com.gp.validate.ArgsValidator;
import com.gp.validate.ValidateMessage;
import com.gp.validate.ValidateUtils;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;

import io.undertow.server.HttpServerExchange;

public class WGroupExtHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(WGroupExtHandler.class);
	
	private CommonService commonService;
	private WGroupService workgroupService;
	private WGroupExtraService workgroupExtraService;
	
	public WGroupExtHandler() {
		commonService = BindScanner.instance().getBean(CommonService.class);
		workgroupService = BindScanner.instance().getBean(WGroupService.class);
		workgroupExtraService = BindScanner.instance().getBean(WGroupExtraService.class);
		
	}

	@WebApi(path="wgroup-member-count")
	public void handleMemberCount(HttpServerExchange exchange)throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.WGRP_FND_MBR);
		
		String wgroupcode = Filters.filterString(params, "wgroup_code");

		ActionResult result = ActionResult.success("mesg.mbr.count");
		
		InfoId wkey = commonService.queryInfoId(wgroupcode);
		
		if(!IdKeys.isValidId(wkey)) {
			svcctx.abort();
		}
		int count = commonService.queryColumn(wkey, "member_sum", Integer.class);
		
		Map<String, Object> data = Maps.newHashMap();
		data.put("member_count", count);
		
		result.setData(data);
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="wgroup-group-add")
	public void handleAddGroup(HttpServerExchange exchange)throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.GRP_NEW);
		
		String group = Filters.filterString(params, "group_name");
		String wgroupcode = Filters.filterString(params, "wgroup_code");
		String description = Filters.filterString(params, "description");
		
		Principal principal = this.getPrincipal(exchange);

		GroupInfo ginfo = new GroupInfo();

		InfoId wkey = commonService.queryInfoId(wgroupcode);
	
		if(!IdKeys.isValidId(wkey)) {
			svcctx.abort("excp.grp.add");
		}
		ginfo.setManageId(wkey.getId());
		
		ginfo.setDescription(description);
		ginfo.setGroupName(group);

		ActionResult aresult = new ActionResult();

		if(Strings.isNullOrEmpty(ginfo.getGroupType())){
			ginfo.setGroupType(NodeIdKey.WORKGROUP.name());
		}
		// check the validation of user information
		Set<ValidateMessage> vmsg = ValidateUtils.validate(principal.getLocale(), ginfo);
		if(!Iterables.isEmpty(vmsg)){ // fail pass validation
			ServiceException svcexcp = new ServiceException(svcctx.getPrincipal().getLocale(), "excp.validate");
			svcexcp.addValidateMessages(vmsg);
			throw svcexcp;
		}

		svcctx.setOperationObject(ginfo.getInfoId());
		svcctx.addOperationPredicates(ginfo);
		// query accounts information
		workgroupService.addWorkgroupGroup(svcctx, ginfo);

		aresult = ActionResult.success(getMessage(exchange, "mesg.new.wgroup.group"));
		
		this.sendResult(exchange, aresult);
	}
	
	@WebApi(path="wgroup-groups-query")
	public void handleWorkgroupGroupQuery(HttpServerExchange exchange)throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.GRP_FND);
		
		String group = Filters.filterString(params, "group");
		String wgroupcode = Filters.filterString(params, "wgroup_code");
		
		InfoId wkey = commonService.queryInfoId(wgroupcode);
		if(!IdKeys.isValidId(wkey)) {
			svcctx.abort("excp.grp.query");
		}
		
		ActionResult result = null;
		List<Map<String, Object>> list = Lists.newArrayList();

		// amend the operation information
		svcctx.setOperationObject( wkey);
		Long mbrGrpId = commonService.queryColumn(wkey, "mbr_group_id", Long.class);
		List<GroupInfo> ulist = workgroupService.getWorkgroupGroups( wkey, group);
		list = ulist.stream().map((info) -> {
			
			DataBuilder builder = new DataBuilder();
			builder.set("group_id", info.getId().toString());
			builder.set(info, "group_name", "description");
			builder.set("workgroup_id", info.getManageId().toString());
			builder.set("member_count", info.getProperty("member_count"));
			// primary flag: includes all the memebers
			builder.set("is_primary", Objects.equals(mbrGrpId, info.getId()));
			
			return builder.build();
		}).collect(Collectors.toList());

		result = ActionResult.success(getMessage(exchange, "mesg.find.wgroup.group"));

		result.setData(list);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="wgroup-group-remove")
	public void handleRemoveGroup(HttpServerExchange exchange)throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.GRP_RMV);
		long groupid =  Filters.filterLong(params, "group_id");

		InfoId gid = IdKeys.getInfoId(NodeIdKey.GROUP, groupid);
	
		ActionResult aresult = new ActionResult();

		// amend the operation information
		svcctx.setOperationObject(gid);
		workgroupService.removeWorkgroupGroup(svcctx,  gid);

		aresult = ActionResult.success(getMessage(exchange, "mesg.remove.wgroup.group"));
		
		this.sendResult(exchange, aresult);
	}
	
	@WebApi(path="wgroup-invites-query")
	public void handleWorkgroupInviteQuery(HttpServerExchange exchange)throws BaseException {

		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.WGRP_INF);
		svcctx.addOperationPredicates(params);
		
		String state = Filters.filterString(params, "state");
		String wgroupcode = Filters.filterString(params, "wgroup_code");
		
		PageQuery pquery = Filters.filterPageQuery(params);
		InfoId wkey = commonService.queryInfoId(wgroupcode);
		if(!IdKeys.isValidId(wkey)) {
			svcctx.abort("excp.grp.query");
		}
		
		ActionResult result = null;
		List<Object> list = Lists.newArrayList();

		// amend the operation information
		svcctx.setOperationObject( wkey);
	
		List<UserInviteInfo> infos = workgroupExtraService.getWorkgroupInvites(wkey, 
				Strings.isNullOrEmpty(state) ? null : new String[] {state}, 
				pquery);
		list = infos.stream().map((info) -> {
			
			DataBuilder builder = new DataBuilder();
			builder.set("invite_id", info.getId().toString());
			builder.set("invite_code", info.getTraceCode());
			
			builder.set("inviter", sbuilder -> {
				sbuilder.set("user_id", info.getInviterUid().toString());
				sbuilder.set("user_gid", info.getProperty("inviter_gid", String.class));
				sbuilder.set("username", info.getProperty("inviter_username", String.class));
				sbuilder.set("full_name", info.getProperty("inviter_full_name", String.class));
			});
			
			builder.set("invitee", sbuilder -> {
				sbuilder.set("user_gid", info.getProperty("invitee_gid", String.class));
				sbuilder.set("username", info.getProperty("invitee_username", String.class));
				sbuilder.set("full_name", info.getProperty("invitee_full_name", String.class));
			});
			
			builder.set("workgroup_id", info.getWorkgroupId().toString());
			builder.set("invite_time", info.getInviteTime().getTime());
			builder.set("review_time", info.getReviewTime().getTime());
			builder.set("state", info.getState());
			
			return builder.build();
		}).collect(Collectors.toList());

		result = ActionResult.success(getMessage(exchange, "mesg.find.wgroup.group"));
		if(null != pquery) {
			result.setData(pquery.getPagination(), list);
		}else {
			result.setData(list);
		}

		this.sendResult(exchange, result);		
	}
	
	@WebApi(path="wgroup-joins-query")
	public void handleWorkgroupJoinQuery(HttpServerExchange exchange)throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.WGRP_INF);
		svcctx.addOperationPredicates(params);
		
		String state = Filters.filterString(params, "state");
		String wgroupcode = Filters.filterString(params, "wgroup_code");
		PageQuery pquery = Filters.filterPageQuery(params);
		InfoId wkey = commonService.queryInfoId(wgroupcode);
		if(!IdKeys.isValidId(wkey)) {
			svcctx.abort("excp.grp.query");
		}
		
		ActionResult result = null;
		List<Object> list = Lists.newArrayList();

		// amend the operation information
		svcctx.setOperationObject( wkey);

		List<UserJoinInfo> ulist = workgroupExtraService.getWorkgroupJoins( wkey, 
				Strings.isNullOrEmpty(state) ? null : new String[] {state}, 
				pquery);
		list = ulist.stream().map((info) -> {
			
			DataBuilder builder = new DataBuilder();
			builder.set("join_id", info.getId().toString());
			builder.set("join_code", info.getTraceCode());
			
			builder.set("applier", sbuilder -> {
				sbuilder.set("user_id", info.getApplierUid().toString());
				sbuilder.set("user_gid", info.getProperty("applier_gid", String.class));
				sbuilder.set("username", info.getProperty("applier_username", String.class));
				sbuilder.set("full_name", info.getProperty("applier_full_name", String.class));
			});
			
			builder.set("reviewer", sbuilder -> {
				sbuilder.set("user_id", info.getReviewerUid().toString());
				sbuilder.set("user_gid", info.getProperty("reviewer_gid", String.class));
				sbuilder.set("username", info.getProperty("reviewer_username", String.class));
				sbuilder.set("full_name", info.getProperty("reviewer_full_name", String.class));
			});
			
			builder.set("workgroup_id", info.getWorkgroupId().toString());
			builder.set("apply_time", info.getApplyTime().getTime());
			builder.set("review_time", info.getReviewTime().getTime());
			builder.set("state", info.getState());
			
			return builder.build();
		}).collect(Collectors.toList());

		result = ActionResult.success(getMessage(exchange, "mesg.find.wgroup.group"));
		if(null != pquery) {
			result.setData(pquery.getPagination(), list);
		}else {
			result.setData(list);
		}

		this.sendResult(exchange, result);	
	}
	
	
	@WebApi(path="wgroup-summary")
	public void handleSummaryWorkgroup(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);

		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.topic.info"));
		ArgsValidator.newValidator(params)
			.requireOne("wgroup_id", "wgroup_code")
			.validate(true);
		
		InfoId wgrpKey = Filters.filterInfoId(params, "wgroup_id", NodeIdKey.WORKGROUP);
		if(!IdKeys.isValidId(wgrpKey)) {
			wgrpKey = commonService.queryInfoId(Filters.filterString(params, "wgroup_code"));
		}
		
		WorkgroupStatInfo statInfo = workgroupExtraService.getWorkgroupSummary(wgrpKey);
		
		Map<String, Object> data = Maps.newHashMap();
		if(null != statInfo) {
			data = statInfo.toMap("file_cnt", "folder_cnt", "task_cnt", "member_cnt",
					"publish_cnt", "topic_cnt");
		}
		
		result = ActionResult.success(getMessage(exchange, "mesg.sum.wgroup"));
		result.setData(data);
		
		this.sendResult(exchange, result);
	}
}
