/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.master;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.gp.bind.BindScanner;
import com.gp.common.*;
import com.gp.dao.info.GroupUserInfo;
import com.gp.dao.info.OrgHierInfo;
import com.gp.exception.BaseException;
import com.gp.exception.CoreException;
import com.gp.exception.ServiceException;
import com.gp.info.DataBuilder;
import com.gp.info.Principal;
import com.gp.paging.PageQuery;
import com.gp.svc.CommonService;
import com.gp.svc.master.OrgHierService;
import com.gp.util.NumberUtils;
import com.gp.validate.ArgsValidator;
import com.gp.validate.ValidateMessage;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import com.gp.web.api.ServiceApiHelper;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class OrgHierHandler extends BaseApiSupport {

	static Logger LOGGER = LoggerFactory.getLogger(OrgHierHandler.class);
	
	private OrgHierService orghiderSvc;
	private CommonService commonSvc;
	
	public OrgHierHandler() {
		orghiderSvc = BindScanner.instance().getBean(OrgHierService.class);
		commonSvc = BindScanner.instance().getBean(CommonService.class);
		
	}

	@WebApi(path="org-node-add")
	public void handleAddOrgHier(HttpServerExchange exchange)throws BaseException{
		
		ActionResult result = new ActionResult();

		Map<String, Object> params = this.getRequestBody(exchange);
		
		ArgsValidator.newValidator(params)
		.require("org_name")
		.validate(true);
	
		long parentId = Filters.filterLong(params, "org_pid");
		if(parentId <= 0)
			parentId = GeneralConsts.HIER_ROOT;
			
		Principal principal = this.getPrincipal(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ORG_NEW);
		
		OrgHierInfo orghier = new OrgHierInfo();
		
		orghier.setOrgPid(parentId);
		orghier.setInfoId(IdKeys.newInfoId(MasterIdKey.ORG_HIER));
		
		orghier.setDescription(Filters.filterString(params, "description"));
		orghier.setEmail(Filters.filterString(params, "email"));
		orghier.setOrgName(Filters.filterString(params, "org_name"));
		
		String avatarUrl = Filters.filterString(params, "avatar_url");
		if(!Strings.isNullOrEmpty(avatarUrl) && avatarUrl.startsWith("data:image/")){
			// process the avatar base64 image
			avatarUrl = ServiceApiHelper.instance().cacheAvatar(avatarUrl);
			orghier.setAvatarUrl(avatarUrl);
		}else {
			String relativeUrl = ServiceApiHelper.instance().relativeBinaryUrl(avatarUrl);
			
			orghier.setAvatarUrl(relativeUrl);
		}

		svcctx.setOperationObject(orghier.getInfoId());
		svcctx.addOperationPredicates(orghier);

		orghiderSvc.newOrgHierNode(svcctx, orghier);
	
		result = ActionResult.success(getMessage(exchange, "mesg.new.orghier"));
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="org-node-save")
	public void handleSaveOrgHier(HttpServerExchange exchange)throws BaseException{
		
		ActionResult result = new ActionResult();
	
		Principal principal = this.getPrincipal(exchange);
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ORG_UPD);
		
		ArgsValidator.newValidator(params)
			.require("org_id", "org_name")
			.validate(true);
		InfoId nodeId = Filters.filterInfoId(params, "org_id", MasterIdKey.ORG_HIER);
			
		svcctx.setOperationObject(nodeId);
		OrgHierInfo orghier =  orghiderSvc.getOrgHierNode( nodeId );
		if(Objects.isNull(orghier)){
			
			throw new CoreException(principal.getLocale(), "mesg.save.none");
		}
		orghier.setDescription(Filters.filterString(params, "description"));
		orghier.setEmail(Filters.filterString(params, "email"));

		orghier.setOrgName(Filters.filterString(params, "org_name"));
		
		String avatarUrl = Filters.filterString(params, "avatar_url");
		if(!Strings.isNullOrEmpty(avatarUrl) && avatarUrl.startsWith("data:image/")){
			// process the avatar base64 image
			avatarUrl = ServiceApiHelper.instance().cacheAvatar(avatarUrl);
			orghier.setAvatarUrl(avatarUrl);
		}else {
			String relativeUrl = ServiceApiHelper.instance().relativeBinaryUrl(avatarUrl);
			
			orghier.setAvatarUrl(relativeUrl);
		}
		orghiderSvc.saveOrgHierNode(svcctx, orghier);
		
		result = ActionResult.success(getMessage(exchange, "mesg.target.orghier"));
			
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="org-node-remove")
	public void handleRemoveOrgHier(HttpServerExchange exchange)throws BaseException{
		
		ActionResult result = new ActionResult();
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ORG_RMV);
		
		Map<String, Object> params = this.getRequestBody(exchange);
		
		InfoId id = Filters.filterInfoId(params, "org_id", MasterIdKey.ORG_HIER);
		
		svcctx.setOperationObject(id);
		orghiderSvc.removeOrgHierNode( id );
			
		result = ActionResult.success(getMessage(exchange, "mesg.remove.orghier"));
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="org-member-add")
	public void handleAddOrgHierMember(HttpServerExchange exchange) throws BaseException{
		
		Principal principal = this.getPrincipal(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ORG_ADD_MBR);
		
		ActionResult result = new ActionResult();
		Map<String, Object> params = this.getRequestBody(exchange);
		ArgsValidator.newValidator(params)
			.require("org_id")
			.validate(true);
		
		try{
		
			InfoId nodeId = Filters.filterInfoId(params, "org_id", MasterIdKey.ORG_HIER);
			
			List<Long> mbrs = Lists.newArrayList();
			
			List<String> mbrNodes = (List<String>)params.get("members");
			mbrNodes.forEach((node) -> { 
				mbrs.add(NumberUtils.toLong(node));
			});
				
			if(null == mbrs || mbrs.size() == 0){
				ServiceException cexcp = new ServiceException(principal.getLocale(), "excp.add.org.mbr");
				cexcp.addValidateMessage(ValidateMessage.newMessage("account", "mesg.prop.miss"));
				throw cexcp;
			}
			
			svcctx.setOperationObject(nodeId);
			orghiderSvc.addOrgHierMember(svcctx, nodeId, mbrs.toArray(new Long[0]));
				
			result = ActionResult.success(getMessage(exchange, "mesg.save.org.mbr"));
			
		}catch (Exception e) {
			LOGGER.error("error", e);
			result = ActionResult.failure(getMessage(exchange, "excp.save.org.mbr"));
		} 
		
		this.sendResult(exchange, result);
	}

	@WebApi(path="org-member-remove")
	public void handleRemoveOrgHierMember(HttpServerExchange exchange)throws BaseException {
		
		ActionResult result = new ActionResult();
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ORG_RMV_MBR);
		
		Map<String, Object > paramap = this.getRequestBody(exchange);
		ArgsValidator.newValidator(paramap)
			.require("org_id", "member")
			.validate(true);
		
		InfoId nodeId = Filters.filterInfoId(paramap, "org_id", MasterIdKey.ORG_HIER);
		String mbrUid = Filters.filterString(paramap, "member");
		
		svcctx.setOperationObject(nodeId);

		Long member = NumberUtils.toLong(mbrUid);
		
		orghiderSvc.removeOrgHierMember(svcctx, nodeId, member);
		
		result = ActionResult.success(getMessage(exchange, "mesg.remove.org.mbr"));

		this.sendResult(exchange, result);
	}
	
	@WebApi(path="org-members-query")
	public void handleOrgHierMembersQuery(HttpServerExchange exchange)throws BaseException {
		
		ActionResult result = new ActionResult();
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ORG_FND_MBR);
		
		Map<String, Object > paramap = this.getRequestBody(exchange);
		ArgsValidator.newValidator(paramap)
			.require("org_id")
			.validate(true);
		
		PageQuery pquery = Filters.filterPageQuery(paramap);
		InfoId nodeId = Filters.filterInfoId(paramap, "org_id", MasterIdKey.ORG_HIER);
		String keyword = Filters.filterString(paramap, "keyword");
		List<String> features = Filters.filterList(paramap, "features", String.class);
		
		svcctx.setOperationObject(nodeId);
		// query accounts information
		List<GroupUserInfo> ulist = orghiderSvc.getOrgHierMembers( svcctx, keyword, nodeId, features, pquery);
		List<Map<String, Object>> list = ulist.stream().map((info)->{
		
			DataBuilder builder = new DataBuilder();
			
			builder.set("user_id", info.getMemberUid().toString());
			
			builder.set(info, "username", "email", "mobile", "category", "full_name", "state");
						
			String avatarUrl = info.getProperty("avatar_url", String.class);
			avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
			
			builder.set("avatar_url", avatarUrl);
			
			builder.set("source", sbuilder -> {
				sbuilder.set("source_id", info.getProperty("source_id", Long.class).toString());
				sbuilder.set(info, "source_name", "abbr", "node_gid");
			});
			
			Long favId = info.getProperty("favorite_id", Long.class);
			builder.set("fav", (favId != null && favId > 0L));
			
			return builder.build();
			
		}).collect(Collectors.toList());
		
		result = ActionResult.success(getMessage(exchange, "mesg.find.org.mbrs"));
		if(null == pquery) {
			result.setData(list);
		} else {
			result.setData(pquery.getPagination(), list);
		}
		this.sendResult(exchange, result);
	}

	@WebApi(path="org-hier-query", operation="org:fnd")
	public void handleGetOrgHierNodes(HttpServerExchange exchange)throws BaseException {
		
		ActionResult result = null;

		Map<String, Object > paramap = this.getRequestBody(exchange);
		ArgsValidator.newValidator(paramap)
			.requireOne("org_pid")
			.validate(true);
		
		InfoId oid = Filters.filterInfoId(paramap, "org_pid", MasterIdKey.ORG_HIER);

		List<OrgHierInfo> gresult =  orghiderSvc.getOrgHierChildNodes(true, oid);
		List<Map<String, Object>> olist =  gresult.stream().map((orghier)->{
			DataBuilder builder = new DataBuilder();
			builder.set("org_id", orghier.getId().toString());
			if(GeneralConsts.HIER_ROOT != orghier.getOrgPid()){
				builder.set("org_pid", orghier.getOrgPid().toString());
			} else {
				builder.set("org_pid", GeneralConsts.HIER_ROOT.toString());
			}
			builder.set(orghier, "org_name", "description", "email");
			String avatarUrl = ServiceApiHelper.absoluteBinaryUrl( orghier.getAvatarUrl());
			builder.set("avatar_url", avatarUrl);
						
			int childCnt = orghier.getProperty("child_count", Integer.class);
			builder.set("has_child", childCnt > 0);
			
			return builder.build();
		}).collect(Collectors.toList());
		
		result = ActionResult.success(getMessage(exchange, "mesg.find.orgs"));
		result.setData(olist);
			
		this.sendResult(exchange, result);	
	}
	
	@WebApi(path="org-node-info", operation="org:inf")
	public void handleGetOrgHierNode(HttpServerExchange exchange)throws BaseException {
		
		ActionResult result = null;
		ServiceContext svcctx = this.getServiceContext(exchange);
		
		Map<String, Object > paramap = this.getRequestBody(exchange);
		ArgsValidator.newValidator(paramap)
			.require("org_id")
			.validate(true);

		InfoId oid = Filters.filterInfoId(paramap, "org_id", MasterIdKey.ORG_HIER);;
		svcctx.setOperationObject(oid);
		OrgHierInfo orghier = orghiderSvc.getOrgHierNode( oid );
		if(orghier == null) {
			svcctx.abort("excp.find.orgs");
		}
		
		DataBuilder builder = new DataBuilder();
		builder.set("org_id", orghier.getId().toString());
		if(GeneralConsts.HIER_ROOT != orghier.getOrgPid()){
			builder.set("org_pid", orghier.getOrgPid().toString());
		} else {
			builder.set("org_pid", GeneralConsts.HIER_ROOT.toString());
		}
		builder.set(orghier, "org_name", "description", "email", "node_gid");
		String avatarUrl = ServiceApiHelper.absoluteBinaryUrl( orghier.getAvatarUrl());
		builder.set("avatar_url", avatarUrl);
			
		result = ActionResult.success(getMessage(exchange, "mesg.find.orgs"));
		result.setData(builder.build());
	
		this.sendResult(exchange, result);		
	}
	
	@WebApi(path="org-node-root", operation="org:inf")
	public void handleGetOrgHierRoots(HttpServerExchange exchange)throws BaseException {
		
		ActionResult result = null;
		ServiceContext svcctx = this.getServiceContext(exchange);
		OrgHierInfo orghier =  orghiderSvc.getOrgHierRoot();
		
		if(orghier == null) {
			svcctx.abort("excp.find.orgs");
		}
		
		DataBuilder builder = new DataBuilder();
		builder.set("org_id", orghier.getId().toString());
		if(GeneralConsts.HIER_ROOT != orghier.getOrgPid()){
			builder.set("org_pid", orghier.getOrgPid().toString());
		} else {
			builder.set("org_pid", GeneralConsts.HIER_ROOT.toString());
		}
		builder.set(orghier, "org_name", "description", "email");
		String avatarUrl = ServiceApiHelper.absoluteBinaryUrl( orghier.getAvatarUrl());
		builder.set("avatar_url", avatarUrl);
					
		result = ActionResult.success(getMessage(exchange, "mesg.find.orgs"));
		result.setData(builder);
	
		this.sendResult(exchange, result);		
	}
}
