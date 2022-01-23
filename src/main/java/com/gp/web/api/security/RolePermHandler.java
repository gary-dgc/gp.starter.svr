/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.security;

import com.google.common.collect.Lists;
import com.gp.bind.BindScanner;
import com.gp.common.*;
import com.gp.dao.info.EndpointInfo;
import com.gp.dao.info.RoleInfo;
import com.gp.dao.info.UserInfo;
import com.gp.exception.BaseException;
import com.gp.exception.CoreException;
import com.gp.info.DataBuilder;
import com.gp.info.Principal;
import com.gp.paging.PageQuery;
import com.gp.svc.security.RolePermService;
import com.gp.util.NumberUtils;
import com.gp.validate.ArgsValidator;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import com.gp.web.api.ServiceApiHelper;
import io.undertow.server.HttpServerExchange;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RolePermHandler extends BaseApiSupport{

	private RolePermService rolePermSvc;
	
	public RolePermHandler() {
		rolePermSvc = BindScanner.instance().getBean(RolePermService.class);
		
	}

	@WebApi(path="roles-query", operation="rol:fnd")
	public void handleRoleQuery(HttpServerExchange exchange)throws BaseException{
		
		ActionResult result = new ActionResult();
		Map<String, Object> params = this.getRequestBody(exchange);
		
		PageQuery pquery = Filters.filterPageQuery(params);
		String name = Filters.filterString(params, "keyword");
		String category = Filters.filterString(params, "category");
		Long sysId = Filters.filterLong( params, "sys_id");
		List<RoleInfo> roles = rolePermSvc.getRoles(name, category, sysId, pquery);
		
		List<Map<String, Object>> rps = roles.stream().map((RoleInfo r) -> {
			DataBuilder builder = new DataBuilder();
			builder.set("role_id", r.getId().toString());
			builder.set(r, "role_name", "role_abbr", "reserved", "description",
					"default_case");
			
			return builder.build();
		}).collect(Collectors.toList());
	
		result = ActionResult.success(getMessage(exchange, "mesg.find.roles"));
		result.setData(pquery == null ? null : pquery.getPagination(), rps);
		
		this.sendResult(exchange, result);
	}

	@WebApi(path="role-add")
	public void handleRoleAdd(HttpServerExchange exchange)throws BaseException{
		Map<String, Object> params = this.getRequestBody(exchange);
		
		ActionResult result = new ActionResult();
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ROL_NEW);
		svcctx.addOperationPredicates(params);		
		
		Principal principal = super.getPrincipal(exchange);
		
		RoleInfo role = new RoleInfo();
		role.setInfoId(IdKeys.getInfoId(MasterIdKey.ROLE, Filters.filterLong(params, "role_id")));
		role.setSysId(Filters.filterLong(params, "sys_id"));
		role.setRoleName(Filters.filterString(params, "role_name"));
		role.setRoleAbbr(Filters.filterString(params, "role_abbr"));
		role.setDefaultCase(Filters.filterString(params, "default_case"));
		role.setReserved(Filters.filterBoolean(params, "reserved"));
		role.setDescription(Filters.filterString(params, "description"));

		boolean success = rolePermSvc.newRole(svcctx, role);

		result = success ? ActionResult.success(getMessage(exchange, "mesg.save.role")) : 
			ActionResult.failure(getMessage(exchange, "excp.save.role")) ;

		this.sendResult(exchange, result);
	}
	
	@WebApi(path="role-save")
	public void handleRoleUpdate(HttpServerExchange exchange)throws BaseException{
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ActionResult result = new ActionResult();
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ROL_UPD);
		svcctx.addOperationPredicates(params);		
				
		RoleInfo role = new RoleInfo();
		role.setInfoId(IdKeys.getInfoId(MasterIdKey.ROLE, Filters.filterLong(params, "role_id")));

		role.setRoleName(Filters.filterString(params, "role_name"));
		role.setRoleAbbr(Filters.filterString(params, "role_abbr"));
		role.setDefaultCase(Filters.filterString(params, "default_case"));
		role.setReserved(Filters.filterBoolean(params, "reserved"));
		role.setDescription(Filters.filterString(params, "description"));

		boolean success = rolePermSvc.updateRole(svcctx, role);
	
		result = success ? ActionResult.success(getMessage(exchange, "mesg.save.role")) : 
			ActionResult.failure(getMessage(exchange, "excp.save.role")) ;

		this.sendResult(exchange, result);
	}

	@WebApi(path="role-remove")
	public void handleRoleRemove(HttpServerExchange exchange)throws BaseException{

		Map<String, Object> params = this.getRequestBody(exchange);
		ArgsValidator.newValidator(params)
				.require("role_id")
				.validate(true);

		ActionResult result = new ActionResult();
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ROL_UPD);
		svcctx.addOperationPredicates(params);

		InfoId roleKey = Filters.filterInfoId(params, "role_id", MasterIdKey.ROLE);
		boolean success = rolePermSvc.removeRole(roleKey);

		result = success ? ActionResult.success(getMessage(exchange, "mesg.remove.role")) :
				ActionResult.failure(getMessage(exchange, "excp.remove.role")) ;

		this.sendResult(exchange, result);
	}

	@WebApi(path="role-endpoints-query")
	public void handleRoleEndpointsQuery(HttpServerExchange exchange)throws BaseException{
		
		ActionResult result = new ActionResult();

		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);
		svcctx.setOperation(Operations.ROL_FND);
		svcctx.addOperationPredicates(params);		
		
		long roleId = Filters.filterLong(params, "role_id");
		if( roleId <=0 ) {
			throw new CoreException("excp.param.missed");
		}
		
		InfoId rId = IdKeys.getInfoId(MasterIdKey.ROLE, roleId);
		
		List<EndpointInfo> endpoints = rolePermSvc.getRoleEndpoints(rId);
		List<Map<String, Object>> rps = endpoints.stream().map((EndpointInfo epi) -> {
			DataBuilder builder = new DataBuilder();
			builder.set("endpoint_id", epi.getId().toString());
			builder.set("role_id", rId.getId().toString());
			builder.set("authorized", epi.getProperty("authorized", Boolean.class));
			
			builder.set(epi, "access_path", "module", "endpoint_abbr", "endpoint_name",
					"description");
			
			return builder.build();
			
		}).collect(Collectors.toList());
	
		result = ActionResult.success(getMessage(exchange, "mesg.find.roles"));
		result.setData(rps);

		this.sendResult(exchange, result);
	}
	
	@WebApi(path="perm-grant")
	public void handlePermissionGrant(HttpServerExchange exchange)throws BaseException{
		ActionResult result = new ActionResult();
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ROL_GRT_PEM);
		
		Map<String, Object> params = this.getRequestBody(exchange);
		
		svcctx.addOperationPredicates(params);
		
		long roleId = Filters.filterLong(params, "role_id");
		long endpointId = Filters.filterLong(params, "endpoint_id");
		Boolean authorized = Filters.filterBoolean(params,"authorized");
		if( roleId <= 0 || endpointId <= 0) {
			throw new CoreException("excp.param.missed");
		}
		
		boolean success ;
		InfoId rId = IdKeys.getInfoId(MasterIdKey.ROLE, roleId);
		InfoId endId = IdKeys.getInfoId(MasterIdKey.ENDPOINT, endpointId);
		if(authorized) {
			success = rolePermSvc.grantPerm(svcctx, rId, endId);
		} else {
			success = rolePermSvc.revokePerm(svcctx, rId, endId);
		}
	
		result = success ? ActionResult.success(getMessage(exchange, "mesg.grant.perm")) : 
			ActionResult.failure(getMessage(exchange, "excp.grant.perm")) ;
		
		this.sendResult(exchange, result);
	}

	@WebApi(path="role-members-query", operation="rol:fnd-mbr")
	public void handleRoleMembersQuery(HttpServerExchange exchange)throws BaseException{
		
		ActionResult result = new ActionResult();

		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);

		svcctx.addOperationPredicates(params);
		
		long roleId = Filters.filterLong(params, "role_id");
		if( roleId <= 0) {
			throw new CoreException("excp.param.missed");
		}
		
		InfoId rId = IdKeys.getInfoId(MasterIdKey.ROLE, roleId);
		List<UserInfo> infos = rolePermSvc.getRoleMembers(rId);
		
		List<Map<String, Object>> usrs = infos.stream().map((UserInfo item) -> {
			
			DataBuilder builder = new DataBuilder();
			builder.set("user_id", item.getId().toString());
			builder.set("full_name", item.getFullName());
			builder.set("username", item.getUsername());
			
			builder.set("state", item.getState());
			builder.set("email", item.getEmail());
			builder.set("mobile", item.getMobile());
			builder.set("category", item.getCategory());
			
			String avatarUrl = ServiceApiHelper.absoluteBinaryUrl(item.getAvatarUrl());
			builder.set("avatar_url", avatarUrl);
			
			builder.set("source", sbuilder -> {
				sbuilder.set("source_id", item.getProperty("source_id", Long.class).toString());
				sbuilder.set(item, "source_name", "abbr", "node_gid");
				
				sbuilder.set("source_id", item.getProperty("source_id", Long.class).toString());
			});
			
			return builder.build();
		}).collect(Collectors.toList());
	
		result = ActionResult.success(getMessage(exchange, "mesg.find.members"));
		result.setData(usrs);
	
		this.sendResult(exchange, result);
	}

	@WebApi(path="role-member-add")
	public void handleRoleMemberAdd(HttpServerExchange exchange) throws BaseException{
		
		Map<String, Object> params = this.getRequestBody(exchange);	
		ActionResult result = new ActionResult();
		
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ROL_ADD_MBR);
		svcctx.addOperationPredicates(params);
		
		String idStr = (String)params.get("role_id");
		
		List<Long> acnts = Lists.newArrayList();
		List<String>  mbrNodes = (List<String>)params.get("members");
		if(mbrNodes != null) {
				mbrNodes.forEach((node) -> { 
					acnts.add(NumberUtils.toLong(node));
				});
			}
		
		InfoId roleId = IdKeys.getInfoId(MasterIdKey.ROLE, NumberUtils.toLong(idStr));
		
		svcctx.setOperationObject(roleId);
		svcctx.addOperationPredicate("members", acnts);
		boolean[] rst = rolePermSvc.addRoleMember(svcctx, roleId, acnts.toArray(new Long[0]));

		result = ActionResult.success("success add members to group");
		result.setData(rst);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="role-member-remove")
	public void handleRoleMemberRemove(HttpServerExchange exchange) throws BaseException{
		
		Map<String, Object> params = this.getRequestBody(exchange);	
		ActionResult result = new ActionResult();
		ServiceContext svcctx = this.getServiceContext(exchange);
		svcctx.setOperation(Operations.ROL_RMV_MBR);
		svcctx.addOperationPredicates(params);
		
		String idStr = (String)params.get("role_id");
		
		List<Long> acnts = Lists.newArrayList();
		List<String>  mbrNodes = (List<String>)params.get("members");
		if(mbrNodes != null) {
				mbrNodes.forEach((node) -> { 
					acnts.add(NumberUtils.toLong(node));
				});
			}
		String mbrNode = (String)params.get("member");
		if(mbrNode != null) {
			acnts.add(NumberUtils.toLong(mbrNode));
		}
	
		InfoId roleId = IdKeys.getInfoId(MasterIdKey.ROLE, NumberUtils.toLong(idStr));
		
		svcctx.setOperationObject(roleId);
		svcctx.addOperationPredicate("members", acnts);
		boolean[] rst = rolePermSvc.removeRoleMember(svcctx, roleId, acnts.toArray(new Long[0]));
	
		result = ActionResult.success("success add members to group");
		result.setData(rst);
		
		this.sendResult(exchange, result);
	}
}
