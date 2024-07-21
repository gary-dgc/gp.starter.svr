/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.svc.security;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.gp.bind.BindAutowired;
import com.gp.bind.BindComponent;
import com.gp.common.GroupUsers;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.AppIdKey;
import com.gp.dao.ClientDAO;
import com.gp.dao.EndpointDAO;
import com.gp.dao.RoleDAO;
import com.gp.dao.RolePermDAO;
import com.gp.dao.info.ClientInfo;
import com.gp.dao.info.EndpointInfo;
import com.gp.dao.info.RoleInfo;
import com.gp.dao.info.RolePermInfo;
import com.gp.db.JdbiTran;
import com.gp.info.BaseIdKey;
import com.gp.sql.SqlBuilder;
import com.gp.sql.select.SelectBuilder;
import com.gp.svc.BaseService;
import com.gp.svc.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@BindComponent( priority = BaseService.BASE_PRIORITY)
public class AuthorizeService extends ServiceSupport implements BaseService {

	Logger LOGGER = LoggerFactory.getLogger(AuthorizeService.class);

	@BindAutowired
	RoleDAO roledao;

	@BindAutowired
	EndpointDAO endpointdao;

	@BindAutowired
	RolePermDAO rolepermdao;

	@BindAutowired
	ClientDAO clientdao;
	
	@JdbiTran
	public List<RoleInfo> getAllRoles() {
		SelectBuilder builder = SqlBuilder.select(AppIdKey.ROLE.schema());

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : {}" + builder.toString());
		}
		
		
		return rows(builder.toString(), RoleDAO.INFO_MAPPER);
		
	}

	@JdbiTran
	public List<RoleInfo> getUserRoles(InfoId uid, String username) {
		
		SelectBuilder builder = SqlBuilder.select();
		
		builder.column("r.*");
		
		builder.from(from ->{
			from.table(AppIdKey.USER_ROLE.schema() + " ur");
			from.leftJoin(AppIdKey.ROLE.schema() + " r", "ur.role_id = r.role_id");
			from.leftJoin(BaseIdKey.USER.schema() + " u", "ur.user_id = u.user_id");
		});
		
		List<Object> param = Lists.newArrayList();
		if(IdKeys.isValidId(uid)) {
			builder.or("u.user_id = ? ");
			param.add(uid.getId());
		}
		
		if(!Strings.isNullOrEmpty(username)) {
			builder.or("u.username = ? ");
			param.add(username);
		}
		
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : {} / PARAM: {}", builder.toString(), param);
		}
				
		return rows(builder.toString(), RoleDAO.INFO_MAPPER, param);
		
	}
	
	@JdbiTran
	public List<RolePermInfo> getRolePerms(InfoId roleId) {
		SelectBuilder builder = SqlBuilder.select();
		builder.from(AppIdKey.ROLE_PERM.schema());
		builder.where("role_id = ?");

		List<Object> params = Arrays.asList( roleId.getId() );

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : {} / PARAMS : {}", builder.toString(), params);
		}
		
		return rows(builder.toString(), RolePermDAO.INFO_MAPPER, params);
		
	}

	@JdbiTran
	public int setRolePerms(InfoId roleId, boolean authorized, InfoId... endpointId) {
	
		int cnt = 0;
		for (InfoId epid : endpointId) {
			
			SelectBuilder builder = SqlBuilder.select(AppIdKey.ROLE_PERM.schema());
			builder.where("role_id = ?");
			builder.and("endpoint_id = ?");

			List<Object> params = Arrays.asList( roleId.getId(), epid );

			if (LOGGER.isDebugEnabled())
				LOGGER.debug("SQL : {} / PARAMS : {}", builder, params);
			
			List<RolePermInfo> pinfos = rows(builder.toString(), RolePermDAO.INFO_MAPPER, params);
			
			RolePermInfo rpinfo = Iterables.getFirst(pinfos, null);
			if (null == rpinfo) {
				EndpointInfo epinfo = endpointdao.row(epid);
				InfoId rpid = IdKeys.newInfoId(AppIdKey.ROLE_PERM);
				RolePermInfo newrp = new RolePermInfo();
				newrp.setInfoId(rpid);
				newrp.setAccessPath(epinfo.getAccessPath());
				newrp.setAuthorized(authorized);
				newrp.setRoleId(roleId.getId());
				newrp.setEndpointId(epid.getId());
				newrp.setModifierUid(GroupUsers.pseudo().getUserId().getId());
				newrp.setModifyTime(new Date());

				rolepermdao.create(rpinfo);
				cnt++;
			} else {

				rpinfo.setAuthorized(authorized);
				rpinfo.setModifyTime(new Date());

				rolepermdao.update(rpinfo);
				cnt++;
			}
		}

		return cnt;
		
	}

	@JdbiTran
	public ClientInfo getClient(String clientKey) {
		
		SelectBuilder builder = clientdao.selectSql();
		builder.where("client_key = ?");
		
		List<Object> params = Arrays.asList( clientKey );
		
		return row(builder.toString(), ClientDAO.INFO_MAPPER, params);
	}

}
