/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.base;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.NodeIdKey;
import com.gp.common.Operations;
import com.gp.common.ServiceContext;
import com.gp.dao.info.GroupInfo;
import com.gp.dao.info.GroupUserInfo;
import com.gp.exception.BaseException;
import com.gp.info.DataBuilder;
import com.gp.pagination.PageQuery;
import com.gp.svc.master.GroupService;
import com.gp.util.NumberUtils;
import com.gp.validate.ArgsValidator;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import com.gp.web.api.ServiceApiHelper;

import io.undertow.server.HttpServerExchange;

public class GroupHandler extends BaseApiSupport {

	static Logger LOGGER = LoggerFactory.getLogger(GroupHandler.class);
	
	private GroupService groupsvc;
	
	public GroupHandler() {
		groupsvc = BindScanner.instance().getBean(GroupService.class);
		
	}

	@WebApi(path="groups-query", operation="grp:fnd")
	public void handleGroupSearch(HttpServerExchange exchange)throws BaseException {
	
		Map<String, Object> paramap = this.getRequestBody(exchange);
	
		String name = Filters.filterString(paramap, "keyword");
		String type = Filters.filterAll(paramap, "group_type");
		PageQuery pquery = Filters.filterPageQuery(paramap);
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.groups"));
	
		List<GroupInfo> groups = groupsvc.getGroups( name, type, pquery);
		
		List<Map<String, Object>> grps = groups.stream().map((info)->{
			DataBuilder builder = new DataBuilder();
			builder.set("group_id", info.getId().toString());
			builder.set(info, "group_type", "group_name", "description");
			
			return builder.build();
		}).collect(Collectors.toList());
		
		result.setData(pquery == null ? null : pquery.getPagination(), grps);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="group-members-query", operation="grp:fnd-mbr")
	public void handleGroupMemberSearch(HttpServerExchange exchange)throws BaseException {

		Map<String, Object> paramap = this.getRequestBody(exchange);
		
		String name = Filters.filterString(paramap, "keyword");
		String groupIdStr = Filters.filterAll(paramap, "group_id");
		PageQuery pquery = Filters.filterPageQuery(paramap);
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.groups"));
	
		InfoId grpId = IdKeys.getInfoId(NodeIdKey.GROUP, NumberUtils.toLong(groupIdStr));
		
		List<GroupUserInfo> users =  groupsvc.getGroupMembers(grpId, name, pquery);
		
		List<Map<String, Object>> accounts =  users.stream().map((info)->{
		
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
		
		result.setData(pquery == null ? null : pquery.getPagination(), accounts);
		
		this.sendResult(exchange, result);
		
	}
	
	@SuppressWarnings("unchecked")
	@WebApi(path="group-member-add")
	public void handleGroupMemberAdd(HttpServerExchange exchange) throws BaseException{
				
		ActionResult result = ActionResult.success("success add members to group");
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ArgsValidator.newValidator(paramap)
			.require("group_id", "members")
			.validate(true);
		
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.GRP_ADD_MBR);
	
		String idStr = (String)paramap.get("group_id");
		
		List<Long> acnts = Lists.newArrayList();
		
		List<String> mbrNodes = (List<String>)paramap.get("members");
		mbrNodes.forEach((node) -> { 
			acnts.add(NumberUtils.toLong(node));
		});
		
		InfoId grpId = IdKeys.getInfoId(NodeIdKey.GROUP, NumberUtils.toLong(idStr));
		
		svcctx.setOperationObject(grpId);
		svcctx.addOperationPredicate("members", acnts);
		boolean[] rst =  groupsvc.addGroupMember(svcctx, grpId, acnts.toArray(new Long[0]));

		result.setData(rst);
		this.sendResult(exchange, result);
	}
	
	@SuppressWarnings("unchecked")
	@WebApi(path="group-member-remove")
	public void handleGroupMemberRemove(HttpServerExchange exchange) throws BaseException{
		
		ActionResult result = new ActionResult();
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.GRP_RMV);
		
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ArgsValidator.newValidator(paramap)
			.require("group_id", "members")
			.validate(true);
			
		String idStr = (String)paramap.get("group_id");
		List<Long> acnts = Lists.newArrayList();
		List<String> mbrNodes = (List<String>)paramap.get("members");
		if(mbrNodes != null) {
			mbrNodes.forEach((node) -> { 
				acnts.add(NumberUtils.toLong(node));
			});
		}
		String mbrNode = (String)paramap.get("member");
		if(mbrNode != null) {
			acnts.add(NumberUtils.toLong(mbrNode));
		}
		
		InfoId grpId = IdKeys.getInfoId(NodeIdKey.GROUP, NumberUtils.toLong(idStr));
	
		svcctx.setOperationObject(grpId);
		svcctx.addOperationPredicate("members", acnts);
		boolean[] rst =  groupsvc.removeGroupMember(svcctx, grpId, acnts.toArray(new Long[0]));
		result = ActionResult.success("success add members to group");
		result.setData(rst);
		
		this.sendResult(exchange, result);
	}

	@WebApi(path="group-add")
	public void handleGroupAdd(HttpServerExchange exchange)throws BaseException{
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ArgsValidator.newValidator(params)
			.require("group_type", "group_name")
			.validate(true);
		
		ActionResult result = new ActionResult();
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.GRP_NEW);
	
		GroupInfo ginfo = new GroupInfo();
		ginfo.setGroupType(Filters.filterString(params, "group_type"));
		ginfo.setGroupName(Filters.filterString(params, "group_name"));
		ginfo.setDescription(Filters.filterString(params, "description"));
			
		InfoId grpId = IdKeys.newInfoId(NodeIdKey.GROUP);
		ginfo.setInfoId(grpId);
		svcctx.setOperationObject(grpId);
		svcctx.addOperationPredicate("group", ginfo);
		groupsvc.addGroup(svcctx, ginfo);
		
		result = ActionResult.success("success create the group");
		result.setData(grpId.getId().toString());
		
		this.sendResult(exchange, result);
	}

	@WebApi(path="group-save")
	public void handleGroupSave(HttpServerExchange exchange)throws BaseException{
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ArgsValidator.newValidator(params)
			.require("group_type", "group_name")
			.validate(true);
		
		ActionResult result = new ActionResult();
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.GRP_UPD);
		
		GroupInfo ginfo = new GroupInfo();
		InfoId grpId = IdKeys.getInfoId(NodeIdKey.GROUP, Filters.filterLong(params, "group_id"));
		ginfo.setInfoId(grpId);
		
		ginfo.setGroupType(Filters.filterString(params, "group_type"));
		ginfo.setGroupName(Filters.filterString(params, "group_name"));
		ginfo.setDescription(Filters.filterString(params, "description"));
	
		svcctx.setOperationObject(ginfo.getInfoId());
		svcctx.addOperationPredicate("group", ginfo);
		
		Set<String> filter = params.containsKey("group_type") ? Sets.newHashSet("group_type", "group_name", "description"):
			 Sets.newHashSet("group_name", "description");
		
		ginfo.setPropertyFilter(filter);
		groupsvc.updateGroup(svcctx, ginfo);

		result = ActionResult.success("success update the group");
		result.setData(ginfo.getId().toString());
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="group-remove")
	public void handleGroupRemove(HttpServerExchange exchange)throws BaseException{
		
		ActionResult result = new ActionResult();
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.GRP_RMV);
		
		Map<String, Object> map = this.getRequestBody(exchange);
		ArgsValidator.newValidator(map)
			.require("group_id")
			.validate(true);
		
		long gId = Filters.filterLong(map, "group_id");
		InfoId grpId = IdKeys.getInfoId(NodeIdKey.GROUP, gId);
		
		svcctx.setOperationObject(grpId);
		groupsvc.removeGroup(svcctx, grpId);
	
		result = ActionResult.success("success update the group");
		
		this.sendResult(exchange, result);
	}
}
