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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.GeneralConstants;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.NodeIdKey;
import com.gp.common.Operations;
import com.gp.common.ServiceContext;
import com.gp.dao.info.GroupUserInfo;
import com.gp.dao.info.OrgHierInfo;
import com.gp.exception.BaseException;
import com.gp.exception.CoreException;
import com.gp.exception.ServiceException;
import com.gp.info.DataBuilder;
import com.gp.info.Principal;
import com.gp.svc.CommonService;
import com.gp.svc.master.OrgHierService;
import com.gp.util.NumberUtils;
import com.gp.validate.ValidateMessage;
import com.gp.validate.ValidateUtils;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import com.gp.web.api.ServiceApiHelper;

import io.undertow.server.HttpServerExchange;

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
		
		long parentId = Filters.filterLong(params, "parent_id");
		if(parentId <= 0)
			parentId = GeneralConstants.ORGHIER_ROOT;
			
		Principal principal = this.getPrincipal(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ORG_NEW);
		
		OrgHierInfo orghier = new OrgHierInfo();
		Long orgId = Filters.filterLong(params, "id") ;
		orghier.setOrgPid(parentId);
		orghier.setInfoId(IdKeys.getInfoId(NodeIdKey.ORG_HIER, orgId));
		
		orghier.setAdminUid(Filters.filterLong(params, "admin_uid"));
		orghier.setDescription(Filters.filterString(params, "description"));
		orghier.setEmail(Filters.filterString(params, "email"));
		orghier.setManagerUid(Filters.filterLong(params, "manager_uid"));
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
		// check the validation of user information
		Set<ValidateMessage> vmsg = ValidateUtils.validate(principal.getLocale(), orghier);
		
		if(!Iterables.isEmpty(vmsg)){ // fail pass validation
			ServiceException svcexcp = new ServiceException(principal.getLocale(), "excp.validate");
			svcexcp.addValidateMessages(vmsg);
			throw svcexcp;
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
		
		Long orgId = Filters.filterLong(params, "id") ;
		if(orgId <= 0){
		
			result = ActionResult.failure(getMessage(exchange, "mesg.post.unqualified"));
			this.sendResult(exchange, result);
			return;
		}
		InfoId nodeId = IdKeys.getInfoId(NodeIdKey.ORG_HIER, orgId);
			
		svcctx.setOperationObject(nodeId);
		OrgHierInfo orghier =  orghiderSvc.getOrgHierNode( nodeId );
		if(Objects.isNull(orghier)){
			
			throw new CoreException(principal.getLocale(), "mesg.save.none");
		}
		orghier.setAdminUid(Filters.filterLong(params, "admin_uid"));
		orghier.setDescription(Filters.filterString(params, "description"));
		orghier.setEmail(Filters.filterString(params, "email"));
		orghier.setManagerUid(Filters.filterLong(params, "manager_uid"));
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
		
		Long nodeId = Filters.filterLong(params, "org_id");
		InfoId id = IdKeys.getInfoId(NodeIdKey.ORG_HIER, nodeId);
		
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
		try{
		
			String orgIdStr = (String)params.get("org_id");
			
			if(Strings.isNullOrEmpty(orgIdStr) || ! NumberUtils.isDigits(orgIdStr)){
				
				result = ActionResult.failure(getMessage(exchange, "mesg.post.unqualified"));
				this.sendResult(exchange, result);
				return ;
			}
			Long nid = Long.valueOf(orgIdStr);
			InfoId nodeId = IdKeys.getInfoId(NodeIdKey.ORG_HIER, nid);
			
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
			
			result = ActionResult.failure(getMessage(exchange, "excp.save.org.mbr"));
		} 
		
		this.sendResult(exchange, result);
	}

	@WebApi(path="org-member-remove")
	public void handleRemoveOrgHierMember(HttpServerExchange exchange)throws BaseException {
		
		Principal principal = this.getPrincipal(exchange);
		ActionResult result = new ActionResult();
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ORG_RMV_MBR);
		
		Map<String, Object > paramap = this.getRequestBody(exchange);
		long orgId = Filters.filterLong(paramap, "org_id");
		if(orgId <= 0){
			
			result = ActionResult.failure(getMessage(exchange, "mesg.post.unqualified"));
			this.sendResult(exchange, result);
			return ;
		}
		
		InfoId nodeId = IdKeys.getInfoId(NodeIdKey.ORG_HIER, orgId);
		String mbrUid = Filters.filterString(paramap, "member");
		
		svcctx.setOperationObject(nodeId);
		Map<String, Object> vals = commonSvc.queryColumns(nodeId, new String[]{
			"admin_uid", "manager_uid"
		});
		Long admin = (Long)vals.get("admin_uid");
		Long manager = (Long)vals.get("manager_uid");
		
		Long member = NumberUtils.toLong(mbrUid);
		if(admin.equals(member) || manager.equals(member)){
			throw new CoreException(principal.getLocale(), "excp.reserv.org.mbr");
		}
		
		orghiderSvc.removeOrgHierMember(svcctx, nodeId, member);
		
		result = ActionResult.success(getMessage(exchange, "mesg.remove.org.mbr"));

		this.sendResult(exchange, result);
	}
	
	@WebApi(path="org-members-query")
	public void handleOrgHierMembersQuery(HttpServerExchange exchange)throws BaseException {
		
		ActionResult result = new ActionResult();
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ORG_FND_MBR);
		
		Map<String, Object > paramap = this.getRequestBody(exchange);
		String orgIdStr = Filters.filterString(paramap, "org_id");
		
		if(Strings.isNullOrEmpty(orgIdStr) || ! NumberUtils.isDigits(orgIdStr)){
			
			result = ActionResult.failure(getMessage(exchange, "mesg.post.unqualified"));
			this.sendResult(exchange, result);
			
			return;
		}
		InfoId nodeId = IdKeys.getInfoId(NodeIdKey.ORG_HIER, NumberUtils.toLong(orgIdStr));
	
		svcctx.setOperationObject(nodeId);
		// query accounts information
		List<GroupUserInfo> ulist = orghiderSvc.getOrgHierMembers( nodeId);
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
			
			return builder.build();
			
		}).collect(Collectors.toList());
		
		result = ActionResult.success(getMessage(exchange, "mesg.find.org.mbrs"));
		result.setData(list);
		
		this.sendResult(exchange, result);
	}

	@WebApi(path="org-hier-query", operation="org:fnd")
	public void handleGetOrgHierNodes(HttpServerExchange exchange)throws BaseException {
		
		ActionResult result = null;

		Map<String, Object > paramap = this.getRequestBody(exchange);
		long orgId = Filters.filterLong(paramap, "org_id");
		
		if( orgId <= 0 ){
			orgId = GeneralConstants.ORGHIER_ROOT;
		}		

		InfoId oid = IdKeys.getInfoId(NodeIdKey.ORG_HIER, orgId);

		List<OrgHierInfo> gresult =  orghiderSvc.getOrgHierChildNodes(true, oid);
		List<Map<String, Object>> olist =  gresult.stream().map((orghier)->{
			DataBuilder builder = new DataBuilder();
			builder.set("id", orghier.getId().toString());
			if(GeneralConstants.ORGHIER_ROOT != orghier.getOrgPid()){
				builder.set("parent", orghier.getOrgPid().toString());
			}
			builder.set(orghier, "org_name", "description", "email");
			builder.set("admin_uid", orghier.getAdminUid().toString());
			builder.set("manager_uid", orghier.getManagerUid().toString());
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
	
	@WebApi(path="org-node-query", operation="org:inf")
	public void handleGetOrgHierNode(HttpServerExchange exchange)throws BaseException {
		
		ActionResult result = null;
		ServiceContext svcctx = this.getServiceContext(exchange);
		
		Map<String, Object > paramap = this.getRequestBody(exchange);
		long orgId = Filters.filterLong(paramap, "org_id");

		if(orgId <= 0){
			
			result = ActionResult.success(getMessage(exchange, "excp.find.orgs"));
			this.sendResult(exchange, result);
			
			return;
		}	

		InfoId oid = IdKeys.getInfoId(NodeIdKey.ORG_HIER,orgId);
		svcctx.setOperationObject(oid);
		OrgHierInfo orghier = orghiderSvc.getOrgHierNode( oid );
		
		DataBuilder builder = new DataBuilder();
		builder.set("id", orghier.getId().toString());
		if(GeneralConstants.ORGHIER_ROOT != orghier.getOrgPid()){
			builder.set("parent", orghier.getOrgPid().toString());
		}
		builder.set(orghier, "org_name", "description", "email");
		builder.set("admin_uid", orghier.getAdminUid().toString());
		builder.set("manager_uid", orghier.getManagerUid().toString());
		String avatarUrl = ServiceApiHelper.absoluteBinaryUrl( orghier.getAvatarUrl());
		builder.set("avatar_url", avatarUrl);
			
		result = ActionResult.success(getMessage(exchange, "mesg.find.orgs"));
		result.setData(builder.build());
	
		this.sendResult(exchange, result);		
	}
}
