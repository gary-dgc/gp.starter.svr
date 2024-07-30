/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.master;

import com.google.common.collect.Lists;
import com.gp.bind.BindScanner;
import com.gp.common.*;
import com.gp.dao.info.DeptHierInfo;
import com.gp.dao.info.GroupUserInfo;
import com.gp.exception.BaseException;
import com.gp.exception.CoreException;
import com.gp.info.DataBuilder;
import com.gp.info.Principal;
import com.gp.paging.PageQuery;
import com.gp.svc.master.DeptService;
import com.gp.util.NumberUtils;
import com.gp.validate.ArgsValidator;
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

public class DeptHierHandler extends BaseApiSupport {

	static Logger LOGGER = LoggerFactory.getLogger(DeptHierHandler.class);
	
	private DeptService depthiderSvc;

	public DeptHierHandler() {
		depthiderSvc = BindScanner.instance().getBean(DeptService.class);
	}

	@WebApi(path="dept-node-add")
	public void handleAddDeptHier(HttpServerExchange exchange)throws BaseException{
		
		ActionResult result = new ActionResult();

		Map<String, Object> params = this.getRequestBody(exchange);
		
		ArgsValidator.newValidator(params)
			.require("org_id", "dept_name")
			.validate(true);
		
		long parentId = Filters.filterLong(params, "dept_pid");
		if(parentId <= 0)
			parentId = GeneralConsts.HIER_ROOT;
			
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ORG_NEW);
		
		DeptHierInfo dept = new DeptHierInfo();
		Long orgId = Filters.filterLong(params, "org_id") ;
		dept.setOrgId(orgId);
		dept.setDeptPid(parentId);
		
		dept.setDescription(Filters.filterString(params, "description"));
		dept.setDeptName(Filters.filterString(params, "dept_name"));
		
		depthiderSvc.newDeptHierNode(svcctx, dept);
		
		// collect the operation event data
		svcctx.setOperationObject(dept.getInfoId());
		svcctx.addOperationPredicates(dept);
		
		result = ActionResult.success(getMessage(exchange, "mesg.new.depthier"));
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="dept-node-save")
	public void handleSaveDeptHier(HttpServerExchange exchange)throws BaseException{
		
		ActionResult result = new ActionResult();
	
		Principal principal = this.getPrincipal(exchange);
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ORG_UPD);
		
		ArgsValidator.newValidator(params)
			.require("dept_id", "org_id", "dept_name")
			.validate(true);
		
		long nodeid = Filters.filterLong(params, "dept_id");
		InfoId nodeId = IdKeys.getInfoId(AppIdKey.DEPT_HIER, nodeid);
			
		svcctx.setOperationObject(nodeId);
		
		DeptHierInfo orghier =  depthiderSvc.getDeptHierNode( nodeId );
		if(Objects.isNull(orghier)){
			
			throw new CoreException(principal.getLocale(), "mesg.save.none");
		}
		orghier.setDescription(Filters.filterString(params, "description"));		
		orghier.setDeptName(Filters.filterString(params, "dept_name"));
		
		depthiderSvc.saveDeptHierNode(svcctx, orghier);
		
		result = ActionResult.success(getMessage(exchange, "mesg.target.dept"));
			
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="dept-node-remove")
	public void handleRemoveDeptHier(HttpServerExchange exchange)throws BaseException{
		
		ActionResult result = new ActionResult();
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ORG_RMV);
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ArgsValidator.newValidator(params)
			.require("dept_id")
			.validate(true);
		
		InfoId id = Filters.filterInfoId(params, "dept_id", AppIdKey.DEPT_HIER);
		
		svcctx.setOperationObject(id);
		depthiderSvc.removeDeptHierNode( id );
			
		result = ActionResult.success(getMessage(exchange, "mesg.remove.dept"));
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="dept-member-add")
	public void handleAddDeptHierMember(HttpServerExchange exchange) throws BaseException{
		
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ORG_ADD_MBR);
		
		ActionResult result = new ActionResult();
		Map<String, Object> params = this.getRequestBody(exchange);
		ArgsValidator.newValidator(params)
			.require("dept_id")
			.list("members")
			.validate(true);
		
		try{
		
			String orgIdStr = (String)params.get("dept_id");
			
			Long nid = Long.valueOf(orgIdStr);
			InfoId nodeId = IdKeys.getInfoId(AppIdKey.ORG_HIER, nid);
			
			List<Long> mbrs = Lists.newArrayList();
			
			List<String> mbrNodes = (List<String>)params.get("members");
			mbrNodes.forEach((node) -> { 
				mbrs.add(NumberUtils.toLong(node));
			});
		
			svcctx.setOperationObject(nodeId);
			depthiderSvc.addDeptHierMember(svcctx, nodeId, mbrs.toArray(new Long[0]));
				
			result = ActionResult.success(getMessage(exchange, "mesg.save.org.mbr"));
			
		}catch (Exception e) {
			LOGGER.error("error", e);
			result = ActionResult.failure(getMessage(exchange, "excp.save.org.mbr"));
		} 
		
		this.sendResult(exchange, result);
	}

	@WebApi(path="dept-member-remove")
	public void handleRemoveDeptHierMember(HttpServerExchange exchange)throws BaseException {
		
		ActionResult result = new ActionResult();
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ORG_RMV_MBR);
		
		Map<String, Object > paramap = this.getRequestBody(exchange);
		ArgsValidator.newValidator(paramap)
			.require("dept_id", "member")
			.validate(true);
		
		InfoId nodeId = IdKeys.getInfoId(AppIdKey.DEPT_HIER, Filters.filterLong(paramap, "dept_id"));
		Long mbrUid = Filters.filterLong(paramap, "member");
		
		svcctx.setOperationObject(nodeId);
		svcctx.addOperationPredicates(paramap);
		
		depthiderSvc.removeDeptHierMember(svcctx, nodeId, mbrUid);
		
		result = ActionResult.success(getMessage(exchange, "mesg.remove.org.mbr"));

		this.sendResult(exchange, result);
	}
	
	/**
	 * dept_id 
	 **/
	@WebApi(path="dept-members-query")
	public void handleDeptHierMembersQuery(HttpServerExchange exchange)throws BaseException {
		
		ActionResult result = new ActionResult();
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ORG_FND_MBR);
		
		Map<String, Object > paramap = this.getRequestBody(exchange);
		ArgsValidator.newValidator(paramap)
			.require("dept_id")
			.validate(true);
		
		InfoId deptId = Filters.filterInfoId(paramap, "dept_id", AppIdKey.DEPT_HIER);
		String keyword = Filters.filterString(paramap, "keyword");
		List<String> features = Filters.filterList(paramap, "features", String.class);
		PageQuery pquery = Filters.filterPageQuery(paramap);
	
		svcctx.setOperationObject(deptId);
		// query accounts information
		List<GroupUserInfo> ulist = depthiderSvc.getDeptHierMembers(svcctx, keyword, deptId, features, pquery);
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

	@WebApi(path="dept-hier-query", operation="dept:fnd")
	public void handleGetDeptHierNodes(HttpServerExchange exchange)throws BaseException {
		
		ActionResult result = null;

		Map<String, Object > paramap = this.getRequestBody(exchange);
		ArgsValidator.newValidator(paramap)
		.requireOne("dept_pid", "org_id")
		.validate(true);	

		InfoId deptid = Filters.filterInfoId(paramap, "dept_pid", AppIdKey.DEPT_HIER);
		InfoId orgid = Filters.filterInfoId(paramap, "org_id", AppIdKey.ORG_HIER);
		
		if(!IdKeys.isValidId(deptid)) {
			DeptHierInfo dept = depthiderSvc.getDeptHierRoot(orgid);
			deptid = dept.getInfoId();
		}
		List<DeptHierInfo> gresult =  depthiderSvc.getDeptHierChildNodes(true, orgid, deptid);
		List<Map<String, Object>> olist =  gresult.stream().map((orghier)->{
			DataBuilder builder = new DataBuilder();
			builder.set("dept_id", orghier.getId().toString());
			builder.set("org_id", orghier.getOrgId().toString());
			
			builder.set("dept_pid", orghier.getDeptPid().toString());
			
			builder.set(orghier, "dept_name", "description");
								
			int childCnt = orghier.getProperty("child_count", Integer.class);
			builder.set("has_child", childCnt > 0);
			
			return builder.build();
		}).collect(Collectors.toList());
		
		result = ActionResult.success(getMessage(exchange, "mesg.find.orgs"));
		result.setData(olist);
			
		this.sendResult(exchange, result);	
	}
	
	@WebApi(path="dept-node-info", operation="org:inf")
	public void handleGetDeptHierNode(HttpServerExchange exchange)throws BaseException {
		
		ActionResult result = null;
		ServiceContext svcctx = this.getServiceContext(exchange);
		
		Map<String, Object > paramap = this.getRequestBody(exchange);
		ArgsValidator.newValidator(paramap)
			.require("dept_id")
			.validate(true);

		InfoId oid = Filters.filterInfoId(paramap, "dept_id", AppIdKey.DEPT_HIER);
		svcctx.setOperationObject(oid);
		DeptHierInfo orghier = depthiderSvc.getDeptHierNode( oid );
		
		DataBuilder builder = new DataBuilder();
		builder.set("dept_id", orghier.getId().toString());
		builder.set("org_id", orghier.getOrgId().toString());

		builder.set("dept_pid", orghier.getDeptPid().toString());
		
		builder.set(orghier, "dept_name", "description");
		
		result = ActionResult.success(getMessage(exchange, "mesg.find.depts"));
		result.setData(builder.build());
	
		this.sendResult(exchange, result);		
	}
	
	@WebApi(path="dept-node-root", operation="org:inf")
	public void handleGetDeptHierRoot(HttpServerExchange exchange)throws BaseException {
		
		ActionResult result = null;
		ServiceContext svcctx = this.getServiceContext(exchange);
		
		Map<String, Object > paramap = this.getRequestBody(exchange);
		ArgsValidator.newValidator(paramap)
			.require("org_id")
			.validate(true);

		InfoId oid = Filters.filterInfoId(paramap, "org_id", AppIdKey.ORG_HIER);
		svcctx.setOperationObject(oid);
		DeptHierInfo orghier = depthiderSvc.getDeptHierRoot( oid );
		
		DataBuilder builder = new DataBuilder();
		builder.set("dept_id", orghier.getId().toString());
		builder.set("org_id", orghier.getOrgId().toString());

		builder.set("dept_pid", orghier.getDeptPid().toString());
		
		builder.set(orghier, "dept_name", "description");
		
		result = ActionResult.success(getMessage(exchange, "mesg.find.depts"));
		result.setData(builder.build());
	
		this.sendResult(exchange, result);		
	}
}
