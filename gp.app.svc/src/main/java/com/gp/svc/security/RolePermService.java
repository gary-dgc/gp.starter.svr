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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.gp.bind.BindAutowired;
import com.gp.bind.BindComponent;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.AppIdKey;
import com.gp.common.ServiceContext;
import com.gp.dao.*;
import com.gp.dao.info.*;
import com.gp.db.JdbiTran;
import com.gp.exception.ServiceException;
import com.gp.paging.PageQuery;
import com.gp.paging.Pagination;
import com.gp.paging.Paginator;
import com.gp.sql.BaseBuilder.SortOrder;
import com.gp.sql.SqlBuilder;
import com.gp.sql.delete.DeleteBuilder;
import com.gp.sql.select.SelectBuilder;
import com.gp.sql.update.UpdateBuilder;
import com.gp.svc.BaseService;
import com.gp.svc.ServiceSupport;
import org.jdbi.v3.core.mapper.RowMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


@BindComponent( priority = BaseService.BASE_PRIORITY)
public class RolePermService extends ServiceSupport implements BaseService {

	Logger LOGGER = LoggerFactory.getLogger(RolePermService.class);

	public static RowMapper<EndpointInfo> EPOINT_EXT_MAPPER = (rs, context) -> {
		EndpointInfo info = new EndpointInfo();
		InfoId id = IdKeys.getInfoId(AppIdKey.ENDPOINT, rs.getLong("endpoint_id"));
		info.setInfoId(id);

		info.setEndpointName(rs.getString("endpoint_name"));
		info.setModule(rs.getString("module"));
		info.setType(rs.getString("type"));
		info.setEndpointAbbr(rs.getString("endpoint_abbr"));
		info.setAccessPath(rs.getString("access_path"));
		info.setDescription(rs.getString("description"));
		info.setModifierUid(rs.getLong("modifier_uid"));
		info.setModifyTime(rs.getTimestamp("modify_time"));
		
		if(DAOSupport.isExistColumn(rs, "authorized")) {
			info.setProperty("authorized", rs.getBoolean("authorized"));
		}
		
		return info;
	}; 

	@BindAutowired
	RoleDAO roleDao;

	@BindAutowired
	RolePermDAO rolePermDao;

	@BindAutowired
	UserRoleDAO userRoleDao;

	@BindAutowired
	EndpointDAO endpointDao;

	@JdbiTran(readOnly = true)
	public List<RoleInfo> getRoles(String roleName, String defaultCase, Long sysId, PageQuery pquery) {

		Map<String, Object> paramMap = Maps.newHashMap();

		SelectBuilder builder = SqlBuilder.select();
		builder.column("r.*");
		builder.column("s.sys_name");

		builder.from(from -> {
			from.table(AppIdKey.ROLE.schema("r"));
			from.leftJoin("gp_sys_sub s", "s.sys_id = r.sys_id");
		});
		builder.where(cond -> {
			cond.or("r.role_name like :name");
			cond.or("r.role_abbr like :name");
		});
		paramMap.put("name", "%" + roleName + "%");
		if (!Strings.isNullOrEmpty(defaultCase)) {
			builder.and("r.default_case = :category");
			paramMap.put("category", defaultCase);
		}

		if(IdKeys.isValidId(sysId)){
			builder.and("r.sys_id = :sys");
			paramMap.put("sys", sysId);
		}

		if (Objects.nonNull(pquery)) {
			SelectBuilder countBuilder = builder.clone();
			countBuilder.column().column("count(" + AppIdKey.ROLE.idColumn() + ")");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("SQL : {} / PARAMS : {}", countBuilder.build(), paramMap);
			}
			Integer total = row(countBuilder.toString(), Integer.class, paramMap);
			Paginator paginator = new Paginator(total, pquery);
			Pagination pagination = paginator.getPagination();
			pquery.setPagination(pagination);

			SortOrder orderType = SortOrder.valueOf(pquery.getOrder().toUpperCase());
			builder.orderBy(pquery.getOrderBy(), orderType);
			builder.limit(pagination.getPageStartRow(), pquery.getPageSize());
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : {} / params : {}", builder.build(), paramMap);
		}
		
		return rows(builder.build(), roleDao.getRowMapper((info , rs) -> {
			info.setProperty("sys_name", rs.getString("sys_name"));
		}), paramMap);

	}

	@JdbiTran(readOnly = true)
	public List<EndpointInfo> getEndpoints(String module) {

		SelectBuilder builder = SqlBuilder.select(AppIdKey.ENDPOINT.schema());
		builder.all();

		List<Object> params = Lists.newArrayList();
		if (!Strings.isNullOrEmpty(module)) {
			params.add( module );
			builder.where("module = ?");
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : {} / params : {}", builder.build(), params);
		}
		return rows(builder.build(), EndpointDAO.INFO_MAPPER, params);

	}

	@JdbiTran
	public boolean newRole(ServiceContext svcctx, RoleInfo role) throws ServiceException {
		
		InfoId check = getInfoId(AppIdKey.ROLE,
						"role_abbr = '" + role.getRoleAbbr() + "'",
						"(del_flag < 1 OR del_flag IS NULL)"
					);
		
		if(IdKeys.isValidId(check)) {
			throw new ServiceException(svcctx.getPrincipal().getLocale(), "excp.exist.role");
		}
		
		svcctx.setTraceInfo(role);

		return roleDao.create(role) > 0;

	}

	@JdbiTran
	public boolean updateRole(ServiceContext svcctx, RoleInfo role) {
		svcctx.setTraceInfo(role);

		return roleDao.update(role) > 0;

	}

	@JdbiTran
	public boolean removeRole(InfoId roleId) {

		List<Object> params = Arrays.asList( roleId.getId() );

		DeleteBuilder builder = SqlBuilder.delete(AppIdKey.ROLE_PERM.schema());
		builder.where("role_id = ?");

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : {} / params : {}", builder.build(), params);
		}
		update(builder.build(), params);

		builder = SqlBuilder.delete(AppIdKey.USER_ROLE.schema());
		builder.where("role_id = ?");
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : {} / params : {}", builder.build(), params);
		}
		update(builder.build(), params);


		int cnt = roleDao.delete(roleId, true);

		return cnt > 0;

	}

	@JdbiTran(readOnly = true)
	public List<RolePermInfo> getUserPerms(InfoId userId) {
		SelectBuilder builder = SqlBuilder.select();
		builder.column("rp.*");
		builder.from("gp_role_perm rp");
		builder.from("gp_user_role ur");

		builder.where("rp.authorized > 0");
		builder.and("rp.role_id = ur.role_id");
		builder.and("ur.user_id = " + userId.getId());

		return rows(builder.build(), RolePermDAO.INFO_MAPPER);

	}

	@JdbiTran
	public boolean grantPerm(ServiceContext svcctx, InfoId roleId, InfoId endpointId) {

		SelectBuilder builder = SqlBuilder.select(AppIdKey.ROLE_PERM.schema());
		builder.column("count(" + AppIdKey.ROLE_PERM.idColumn() + ")");
		builder.where("role_id = ?");
		builder.and("endpoint_id = ?");

		List<Object> params = Arrays.asList(roleId.getId(), endpointId.getId());
		
		Integer cnt = row(builder.build(), Integer.class, params);

		if (cnt == 0) {
			EndpointInfo endpoint = endpointDao.row(endpointId);

			RolePermInfo rolePerm = new RolePermInfo();
			rolePerm.setRoleId(roleId.getId());
			rolePerm.setEndpointId(endpointId.getId());
			rolePerm.setAccessPath(endpoint.getAccessPath());
			rolePerm.setAuthorized(true);

			svcctx.setTraceInfo(rolePerm);

			return rolePermDao.create(rolePerm) > 0;
		} else {
			UpdateBuilder upd = SqlBuilder.update(AppIdKey.ROLE_PERM.schema());
			upd.column("authorized", "1");
			upd.where("role_id = ?");
			upd.and("endpoint_id = ?");

			return update(upd.build(), params) > 0;
		}

	}

	@JdbiTran
	public boolean revokePerm(ServiceContext svcctx, InfoId roleId, InfoId endpointId) {

		DeleteBuilder builder = SqlBuilder.delete(AppIdKey.ROLE_PERM.schema());
		builder.where("role_id = ?");
		builder.and("endpoint_id = ?");

		List<Object> params = Arrays.asList(roleId.getId(), endpointId.getId());

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : {} / params : {}", builder.build(), params);
		}
		return update(builder.build(), params) > 0;

	}

	@JdbiTran
	public boolean[] addRoleMember(ServiceContext svcctx, InfoId roleId, Long... members) {
		boolean[] cnts = new boolean[members.length];

		SelectBuilder builder = SqlBuilder.select(AppIdKey.USER_ROLE.schema());
		builder.column("count(" + AppIdKey.USER_ROLE.idColumn() + ")");
		builder.where("role_id = ?");
		builder.and("user_id = ?");

		for (int i = 0; i < members.length; i++) {

			Long member = members[i];
			List<Object> params = Arrays.asList(roleId.getId(), member);
			Integer exist = row(builder.build(), Integer.class, params);
			if (exist > 0) {
				cnts[i] = false;
				continue;
			}
			UserRoleInfo userRole = null;

			userRole = new UserRoleInfo();
			userRole.setRoleId(roleId.getId());
			userRole.setUserId(member);
			svcctx.setTraceInfo(userRole);

			int cnt = userRoleDao.create(userRole);
			cnts[i] = cnt > 0;

		}

		return cnts;
	}

	@JdbiTran
	public boolean[] removeRoleMember(ServiceContext svcctx, InfoId roleId, Long... members) {
		DeleteBuilder builder = SqlBuilder.delete(AppIdKey.USER_ROLE.schema());
		builder.where("role_id = :rid");
		builder.and("user_id in ( <accts> )");

		Map<String, Object> paraMap = Maps.newHashMap();
		paraMap.put("rid", roleId.getId());
		paraMap.put("accts", Lists.newArrayList(members));

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : {} / PARAMS : {} ", builder.toString(), paraMap);
		}

		update(builder.build(), paraMap);
		SelectBuilder select = SqlBuilder.select(AppIdKey.USER_ROLE.schema());
		select.column(AppIdKey.USER_ROLE.idColumn());
		select.where("role_id = :rid");
		select.and("user_id in ( <accts> )");
		Set<Long> accts = Sets.newHashSet();
		query(select.build(), (rs) -> {
			accts.add(rs.getLong("user_id"));
		}, paraMap);
		boolean[] cnts = new boolean[members.length];
		for (int i = 0; i < members.length; i++) {
			cnts[i] = !accts.contains(members[i]);
		}
		return cnts;

	}

	@JdbiTran(readOnly = true)
	public List<EndpointInfo> getRoleEndpoints(InfoId roleId) {

		SelectBuilder builder = SqlBuilder.select();
		builder.column("ep.*", "rp.authorized");

		builder.from(from -> {
			from.table("gp_endpoint ep");
			from.leftJoin("gp_role_perm rp", "rp.endpoint_id = ep.endpoint_id and rp.role_id = ?");
		});

		builder.orderBy("ep.endpoint_id");

		List<Object> params = Arrays.asList( roleId.getId());

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : {} / params : {}", builder.build(), params);
		}
		return rows(builder.build(), EPOINT_EXT_MAPPER, params);

	}

	@JdbiTran(readOnly = true)
	public RoleInfo getRole(InfoId roleId) {

		return roleDao.row(roleId);

	}

	@JdbiTran(readOnly = true)
	public RoleInfo getRole(String abbr) {
		SelectBuilder builder = SqlBuilder.select(AppIdKey.ROLE.schema());
		builder.all();
		builder.where("role_abbr = ?");

		List<Object> params = Arrays.asList( abbr );

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : {} / params : {}", builder.build(), params);
		}
		List<RoleInfo> roles = rows(builder.build(), RoleDAO.INFO_MAPPER, params);
		return Iterables.isEmpty(roles) ? null : roles.get(0);

	}

	@JdbiTran(readOnly = true)
	public List<UserInfo> getRoleMembers(InfoId roleId) {
		
		SelectBuilder builder = SqlBuilder.select();
		builder.column("u.*");
		builder.column("s.source_name", "s.abbr", "s.node_gid");

		builder.from("gp_user_role ur");
		builder.from("gp_user u");
		builder.from("gp_source s");

		builder.where("ur.user_id = u.user_id");
		builder.and("u.source_id = s.source_id");
		builder.and("ur.role_id = ?");

		List<Object> params = Arrays.asList( roleId.getId() );

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : {} / params : {}", builder.build(), params);
		}
		
		Set<String> fields = Sets.newHashSet("source_name", "abbr", "node_gid");
		
		List<UserInfo> rtv = rows(builder.toString(), (rs, idx) -> {
			
			UserInfo info = UserDAO.INFO_MAPPER.map(rs, idx);
			
			DAOSupport.setInfoProperty(rs, info, String.class, fields.toArray(new String[0]));
			info.setProperty("source_id", rs.getLong("source_id"));
			return info;
		}, params);
		
		return rtv;
	}

}


