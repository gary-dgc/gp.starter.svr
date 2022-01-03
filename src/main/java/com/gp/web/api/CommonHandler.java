/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.gp.bind.BindScanner;
import com.gp.common.*;
import com.gp.dao.info.OrgHierInfo;
import com.gp.dao.info.RoleInfo;
import com.gp.dao.info.SourceInfo;
import com.gp.dao.info.UserInfo;
import com.gp.exception.BaseException;
import com.gp.exception.WebException;
import com.gp.info.DataBuilder;
import com.gp.svc.CommonService;
import com.gp.svc.master.OrgHierService;
import com.gp.svc.master.SourceService;
import com.gp.svc.security.RolePermService;
import com.gp.svc.user.UserService;
import com.gp.util.NumberUtils;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CommonHandler extends BaseApiSupport {

	static Logger LOGGER = LoggerFactory.getLogger(CommonHandler.class);
	
	private SourceService sourceService;

	private UserService userService;
	
	private RolePermService rolePermService;
	
	private OrgHierService orghierService;

	private CommonService commonService;

	public CommonHandler() {
		
		sourceService = BindScanner.instance().getBean(SourceService.class);
		userService = BindScanner.instance().getBean(UserService.class);
		rolePermService = BindScanner.instance().getBean(RolePermService.class);
		orghierService = BindScanner.instance().getBean(OrgHierService.class);
		commonService = BindScanner.instance().getBean(CommonService.class);

	}

	@WebApi(path="common-source-query")
	public void handleNodeList(HttpServerExchange exchange) throws BaseException {

		Map<String, Object> paramMap = this.getRequestBody(exchange) ;
		
		String namecond = Filters.filterString( paramMap, "instance_name");
				
		List<KVPair<String,String>> enlist = Lists.newArrayList();
		ActionResult result = null;
	
		// query accounts information
		List<SourceInfo> gresult =  sourceService.getSources(null, namecond, null);
		
		for(SourceInfo einfo : gresult){
			Long id = einfo.getInfoId().getId();
			KVPair<String, String> kv = KVPair.newPair(String.valueOf(id), einfo.getSourceName());
			enlist.add(kv);
		}
		
		result = ActionResult.failure(getMessage(exchange, "mesg.find.sources"));
		result.setData(enlist);

		this.sendResult(exchange, result);
	}

	/**
	 * Get the storage list,  
	 **/
	@WebApi(path="common-role-list")
	public void handleRoleList(HttpServerExchange exchange)throws BaseException {
		
		Map<String, Object> paramMap = this.getRequestBody(exchange);
		
		String namecond = Filters.filterString( paramMap, "keyword");
		String defaultCase = Filters.filterString( paramMap, "default_case");
				
		List<KVPair<String,String>> rolelist = Lists.newArrayList();
		ActionResult result = null;
		
		List<RoleInfo> gresult = rolePermService.getRoles(namecond, defaultCase, null);	
		for(RoleInfo rinfo : gresult){
			Long id = rinfo.getInfoId().getId();
			KVPair<String, String> kv = KVPair.newPair(String.valueOf(id), rinfo.getRoleName());
			rolelist.add(kv);
		}
 		
		result = ActionResult.success(getMessage(exchange, "mesg.find.roles"));
		result.setData(rolelist);

		this.sendResult(exchange, result);
	} 
	
	/**
	 * Support Select User Dialog to list all the users in system 
	 **/
	@WebApi(path="common-user-list")
	public void handleUserList(HttpServerExchange exchange)throws BaseException{

		Map<String, Object> paramMap = this.getRequestBody(exchange);
		
		String uname = Filters.filterString( paramMap, "keyword");
		Long sourceId = Filters.filterLong(paramMap, "source_id");
		Boolean boundOnly = Filters.filterBoolean(paramMap, "bound_only");
		
		ActionResult result = new ActionResult();
		List<Object> list = null;

		// query accounts information
		List<UserInfo> cresult = userService.getUsers( uname, sourceId, null, null, boundOnly, null);
		list = cresult.stream().map((info) -> {
			
			DataBuilder builder = new DataBuilder();
			builder.set("user_id", info.getId().toString());
			
			builder.set(info, "user_gid", "username", "email", "mobile", 
					"category", "full_name");
			
			builder.set("source", sbuilder -> {
				sbuilder.set("source_id", info.getProperty("source_id", Long.class).toString());
				sbuilder.set(info, "source_name");
			});
			String avatarUrl = info.getProperty("avatar_url", String.class);
			avatarUrl = ServiceApiHelper.absoluteBinaryUrl( avatarUrl);
			builder.set("avatar_url", avatarUrl);

			return builder.build();
			
		}).collect(Collectors.toList());
						
		result = ActionResult.success(getMessage(exchange, "mesg.find.users"));
		result.setData(list);
			
		this.sendResult(exchange, result);
		
	}

	@WebApi(path="common-org-nodes")
	public void handleOrghierNodes(HttpServerExchange exchange)throws BaseException {
	
		Map<String, Object> paramMap = this.getRequestBody(exchange);
		String orgIdStr = Filters.filterString( paramMap, "org_id");

		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.orgnodes"));
		List<Map<String, Object>> olist = Lists.newArrayList();		
		Long orgId = null;
		if(!Strings.isNullOrEmpty(orgIdStr)){
		
			orgId = NumberUtils.toLong(orgIdStr, GeneralConsts.HIER_ROOT);
		}else{
			
			result.setData(olist);
			this.sendResult(exchange, result);
			return;
		}
		
		InfoId oid = IdKeys.getInfoId(MasterIdKey.ORG_HIER,orgId);
		
		List<OrgHierInfo> gresult = orghierService.getOrgHierAllNodes( oid);
		  
		for(OrgHierInfo orghier : gresult){
			DataBuilder builder = new DataBuilder();
			
			builder.set("id", orghier.getId().toString());
			if(GeneralConsts.HIER_ROOT != orghier.getOrgPid()){
				builder.set("parent", orghier.getOrgPid().toString());
			}
			
			builder.set(orghier, "org_name", "description", "email");
		
			String avatarUrl = ServiceApiHelper.absoluteBinaryUrl(orghier.getAvatarUrl());
			builder.set("avatar_url", avatarUrl);
			
			olist.add(builder.build());
		}
		
		result.setData(olist);

		this.sendResult(exchange, result);	
	}

	@WebApi(path="common-org-list")
	public void handleOrghierNodeList(HttpServerExchange exchange)throws BaseException {
		Map<String, Object> paramMap = this.getRequestBody(exchange);
		
		String orgName = Filters.filterString( paramMap, "org_name");
		String orgPid = Filters.filterString( paramMap, "org_pid");
		
		List<Map<String, Object>> list = Lists.newArrayList();

		ActionResult result = new ActionResult();
		
		InfoId pid = orgPid == null ? null: IdKeys.getInfoId(MasterIdKey.ORG_HIER, NumberUtils.toLong(orgPid));
		
		List<OrgHierInfo> olist = orghierService.getOrgHierNodes( orgName, pid);
		
		for(OrgHierInfo orghier: olist){
			DataBuilder builder = new DataBuilder();
			
			builder.set("id", orghier.getId().toString());
			if(GeneralConsts.HIER_ROOT != orghier.getOrgPid()){
				builder.set("parent", orghier.getOrgPid().toString());
			}
			
			builder.set(orghier, "org_name", "description", "email");
		
			String avatarUrl = ServiceApiHelper.absoluteBinaryUrl(orghier.getAvatarUrl());
			builder.set("avatar_url", avatarUrl);
			
			list.add(builder.build());
			
		}	
		
		result = ActionResult.success(getMessage(exchange, "mesg.find.orgnodes"));
		result.setData(list);

		this.sendResult(exchange, result);
	}

	/**
	 * This is used in dropdown widget to list available users could be assigned to a given workgroup
	 **/
	@WebApi(path="common-generate-id")
	public void handleGenerateId(HttpServerExchange exchange) throws WebException {
		
		InfoId newId = IdKeys.newInfoId("gp_blind");
		ActionResult result = new ActionResult();

		result = ActionResult.success(getMessage(exchange, "mesg.generate.id"));
		result.setData(newId.getId().toString());
		
		this.sendResult(exchange, result);
	}

}
