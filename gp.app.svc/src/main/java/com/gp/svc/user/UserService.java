/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.svc.user;

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Longs;
import com.gp.bind.BindAutowired;
import com.gp.bind.BindComponent;
import com.gp.common.*;
import com.gp.common.GroupUsers.AuthenType;
import com.gp.common.GroupUsers.UserCategory;
import com.gp.common.GroupUsers.UserState;
import com.gp.dao.*;
import com.gp.dao.info.*;
import com.gp.db.JdbiTran;
import com.gp.exception.ServiceException;
import com.gp.info.BaseIdKey;
import com.gp.info.FilterMode;
import com.gp.paging.PageQuery;
import com.gp.paging.Pagination;
import com.gp.paging.Paginator;
import com.gp.sql.BaseBuilder.SortOrder;
import com.gp.sql.SqlBuilder;
import com.gp.sql.delete.DeleteBuilder;
import com.gp.sql.select.SelectBuilder;
import com.gp.sql.update.UpdateBuilder;
import com.gp.svc.BaseService;
import com.gp.svc.CommonService;
import com.gp.svc.ServiceSupport;
import com.gp.svc.SystemService;
import com.gp.util.BaseUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap.SimpleEntry;
import java.util.*;
import java.util.Map.Entry;

/**
 * User Operation  
 **/
@BindComponent(priority = BaseService.BASE_PRIORITY)
public class UserService extends ServiceSupport implements BaseService {

	public static Logger LOGGER = LoggerFactory.getLogger(UserService.class);

	@BindAutowired
	private UserDAO userdao;

	@BindAutowired
	private UserLoginDAO userlogindao;

	@BindAutowired
	private UserRoleDAO userRoleDao;

	@BindAutowired
	private RoleDAO roleDao;

	@BindAutowired
	TokenDAO tokendao;

	@BindAutowired
	CommonService idService;

	@BindAutowired
	SystemService systemservice;

	@SuppressWarnings("unchecked")
	@JdbiTran
	public boolean newUser(ServiceContext svcctx, UserInfo uinfo, String password) {

		svcctx.setTraceInfo(uinfo);
		uinfo.setCreateTime(BaseUtils.now());
		uinfo.setCryptoKey(String.valueOf(GroupUsers.PWD_GENERATOR.generate(16)));

		SysOptionInfo opt = systemservice.getOption("symmetric.crypto.iv");
		String randomIV = opt.getOptValue();

		int cnt = 0;

		if (!IdKeys.isValidId(uinfo.getInfoId())) {
			uinfo.setInfoId(IdKeys.newInfoId(BaseIdKey.USER));
		}
		if (Strings.isNullOrEmpty(uinfo.getTraceCode())) {
			String nodeGid = column(IdKeys.getInfoId(BaseIdKey.SOURCE, GeneralConsts.LOCAL_SOURCE),
					"node_gid", String.class);
			uinfo.setTraceCode(IdKeys.getTraceCode(nodeGid, uinfo.getInfoId()));
		}
		cnt = userdao.create(uinfo);
		
		// create logins
		SymmetricToken symToken = new SymmetricToken(uinfo.getCryptoKey(), randomIV);
		Set<UserLoginInfo> logins = getLogins(uinfo, password);
		logins.forEach(login -> {
			svcctx.setTraceInfo(login);
			login.setUserId(uinfo.getId());

			String hashpwd = symToken.encrypt(login.getCredential());
			login.setCredential(hashpwd);

			userlogindao.create(login);
		});

		// update user role settings
		List<String> roles = (List<String>)uinfo.getProperty("roles");
		if(null != roles && !roles.isEmpty()) {
			
			userRoleDao.delete(cond -> {
				cond.and("user_id = " + uinfo.getId());
			});
			
			for(String role: roles) {
				
				SelectBuilder select = SqlBuilder.select(MasterIdKey.ROLE.schema());
				select.column("role_id");
				select.where("role_id = ?");
				select.or("role_abbr = ?");
				
				Long roleId = getInfoId(select.build(), Lists.newArrayList(role, role));
				
				UserRoleInfo urole = new UserRoleInfo();
				urole.setUserId(uinfo.getId());
				urole.setRoleId(roleId);
				
				svcctx.setTraceInfo(urole);
				
				userRoleDao.create(urole);
			}
		}
		
		return cnt > 0;
	}

	/**
	 * Prepare the logins
	 **/
	private Set<UserLoginInfo> getLogins(UserInfo uinfo, String password) {
		Set<UserLoginInfo> logins = Sets.newHashSet();

		UserLoginInfo login = null;

		login = new UserLoginInfo();
		login.setUserId(uinfo.getId());
		login.setLogin(uinfo.getUsername());
		login.setCredential(password);
		login.setAuthenType(AuthenType.INLINE.name());
		logins.add(login);

		if (!Strings.isNullOrEmpty(uinfo.getEmail())) {
			login = new UserLoginInfo();
			login.setUserId(uinfo.getId());
			login.setLogin(uinfo.getEmail());
			login.setCredential(password);
			login.setAuthenType(AuthenType.EMAIL.name());
			logins.add(login);
		}
		if (!Strings.isNullOrEmpty(uinfo.getMobile())) {
			login = new UserLoginInfo();
			login.setUserId(uinfo.getId());
			login.setLogin(uinfo.getMobile());
			login.setCredential(password);
			login.setAuthenType(AuthenType.MOBILE.name());
			logins.add(login);
		}

		return logins;
	}

	@SuppressWarnings("unchecked")
	@JdbiTran
	public int updateUser(ServiceContext svcctx, UserInfo uinfo) {

		svcctx.setTraceInfo(uinfo);
		uinfo.setCreateTime(BaseUtils.now());

		UserInfo oldinfo = userdao.row(uinfo.getInfoId());

		// update user logins
		Set<UserLoginInfo> logins = getLogins(uinfo, null);
		
		logins.forEach(login -> {

			UpdateBuilder builder = SqlBuilder.update(MasterIdKey.USER_LOGIN.schema());

			builder.set("login", "?");
			builder.where("user_id = ?");
			builder.and("authen_type = ?");

			List<Object> params = Arrays.asList( login.getLogin(), login.getUserId(), login.getAuthenType() );

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("SQL: {} / PARAMS: {}", builder.toString(), params);
			}

			update(builder.toString(), params);
		});
		
		// update user role settings
		List<String> roles = (List<String>)uinfo.getProperty("roles");
		if(null != roles && !roles.isEmpty()) {
			
			userRoleDao.delete(cond -> {
				cond.and("user_id = " + uinfo.getId());
			});
			
			for(String role: roles) {
				
				SelectBuilder select = SqlBuilder.select(MasterIdKey.ROLE.schema());
				select.column("role_id");
				select.where("role_id = ?");
				select.or("role_abbr = ?");
				
				Long roleId = getInfoId(select.build(), role, role);
				
				UserRoleInfo urole = new UserRoleInfo();
				urole.setUserId(uinfo.getId());
				urole.setRoleId(roleId);
				
				svcctx.setTraceInfo(urole);
				
				userRoleDao.create(urole);
				
			}
		}

		uinfo.setFilter(FilterMode.exclude("username", "user_gid", "trace_code", "crypto_key",
				"retry_times", "last_logon", "source_id", "cabinet_id"));
		// define the columns to be updated.
		int cnt = userdao.update(uinfo);

		return cnt;

	}

	@JdbiTran
	public boolean newUserExt(ServiceContext svcctx, UserInfo uinfo, UserLoginInfo login) {

		svcctx.setTraceInfo(uinfo);
		uinfo.setCreateTime(BaseUtils.now());
		int cnt = 0;
		
		if(Strings.isNullOrEmpty(uinfo.getCryptoKey())) {
			uinfo.setCryptoKey(String.valueOf(GroupUsers.PWD_GENERATOR.generate(16)));
		}
		
		cnt = userdao.create(uinfo);
		
		if(null != login) {
			
			InfoId uid = uinfo.getInfoId();
	
			login.setUserId(uid.getId());
			SysOptionInfo opt = systemservice.getOption("symmetric.crypto.iv");
			String randomIV = opt.getOptValue();
			SymmetricToken itk = new SymmetricToken();
	
			itk.initial(uinfo.getCryptoKey(), randomIV);
			String hashpwd = itk.encrypt(login.getCredential());
			login.setCredential(hashpwd);
	
			userlogindao.create(login);
		}
		
		return cnt > 0;
	}

	@JdbiTran(readOnly = true)
	public List<UserInfo> getUsers(List<Long> userids, List<String> usernames) {

		StringBuffer SQL = new StringBuffer();
		Map<String, Object> params = new HashMap<>();
		SelectBuilder builder = SqlBuilder.select();
		builder.column("usr.*");
		builder.column("src.node_gid", "src.source_name", "src.abbr");

		builder.from((from) -> {
			from.table("gp_user usr");
			from.leftJoin("(select node_gid, source_name, abbr from gp_source) src", "usr.source_id = src.source_id");
		});

		userids = MoreObjects.firstNonNull(userids, Collections.emptyList());
		usernames = MoreObjects.firstNonNull(usernames, Collections.emptyList());

		if (!Iterables.isEmpty(userids) && !Iterables.isEmpty(usernames)) {
			builder.and((where) -> {
				where.or("usr.user_id in ( <user_ids> )");
				where.or("usr.account in ( <accounts> )");
			});

			params.put("user_ids", userids);
			params.put("accounts", userids);

		} else if (Iterables.isEmpty(userids) && !Iterables.isEmpty(usernames)) {

			builder.and((where) -> {
				where.or("usr.account in ( <accounts> )");
			});
			params.put("accounts", usernames);

		} else if (!Iterables.isEmpty(userids) && Iterables.isEmpty(usernames)) {
			builder.and((where) -> {
				where.or("usr.user_id in ( <user_ids> )");
			});
			params.put("user_ids", userids);

		}

		if (LOGGER.isDebugEnabled())
			LOGGER.debug("SQL : {} / PARAMS : {}", SQL.toString(), params.toString());

		Set<String> fields = Sets.newHashSet("source_name", "abbr", "node_gid");
		
		return rows(builder.toString(), (rs, idx) -> {
			
			UserInfo info = UserDAO.INFO_MAPPER.map(rs, idx);
			
			DAOSupport.setInfoProperty(rs, info, String.class, fields.toArray(new String[0]));
			
			return info;
		}, params);
		
	}

	@JdbiTran(readOnly = true)
	public UserInfo getUserFull(InfoId userId, String username) {

		SelectBuilder builder = SqlBuilder.select();
		
		builder.column("a.*", "b.*");
		builder.column("t.title", "t.department");
		
		builder.from((from) -> {
			from.table("gp_user a");
			from.leftJoin("(SELECT source_id, node_gid, source_name, short_name, abbr FROM gp_source) b", "a.source_id = b.source_id");
			from.leftJoin("(SELECT user_id, title, department, is_primary FROM gp_user_title) t", "a.user_id = t.user_id AND t.is_primary > 0");
		});

		Map<String, Object> params = new HashMap<String, Object>();
		// account or name condition
		if (!Strings.isNullOrEmpty(username)) {

			builder.and("a.username = :username");
			params.put("username", username.trim());
		}
		// entity condition
		if (null != userId && IdKeys.isValidId(userId)) {

			builder.and("a.user_id = :userid");
			params.put("userid", userId.getId());
		}

		String querysql = builder.toString();
		if (LOGGER.isDebugEnabled()) {

			LOGGER.debug("SQL : " + querysql + " / PARAMS : " + params.toString());
		}
	
		List<String> ExtFields = Lists.newArrayList("abbr", "short_name", "node_gid", "source_name",
				"title", "department");
		return row(querysql, (rs, i) -> {

			// save extend data
			UserInfo info = UserDAO.INFO_MAPPER.map(rs, i);

			for (String field : ExtFields) {
				if (DAOSupport.isExistColumn(rs, field)) {
					info.setProperty(field, rs.getString(field));
				}
			}
			return info;
		}, params);

	}

	@JdbiTran(readOnly = true)
	public List<UserInfo> getUsers(String username, Long sourceId, UserCategory[] category, UserState[] states,
			Boolean boundOnly, PageQuery pquery) {

		SelectBuilder builder = SqlBuilder.select();
		builder.column("a.*", "b.*");
		
		builder.from((from) -> {
			from.table("gp_user a");
			from.leftJoin("(SELECT source_id, node_gid, source_name,short_name, abbr FROM gp_source) b",
					"a.source_id = b.source_id");
		});

		Map<String, Object> params = new HashMap<String, Object>();
		// account or name condition
		if (!Strings.isNullOrEmpty(username)) {

			builder.and("(a.username like :uname or a.full_name like :fname)");
			params.put("uname", "%" + username.trim() + "%");
			params.put("fname", "%" + username.trim() + "%");
		}
		// entity condition

		if (null != sourceId) {
			if (0 == sourceId) {

				builder.and("a.source_id > :srcid");
				params.put("srcid", sourceId);
			} else if (-9999 == sourceId) {
				builder.and("a.source_id = :srcid");
				params.put("srcid", sourceId);
			} else {
				builder.and("a.source_id = :srcid");
				params.put("srcid", sourceId);
			}
		}
		// user type condition
		if (null != category && category.length > 0) {
			builder.and("a.category in ( <types> )");
			params.put("types", Arrays.asList(category));
		}
		// user state condition
		if (null != states && states.length > 0) {
			builder.and("a.state in ( <states> )");
			params.put("states", Arrays.asList(states));
		}

		if (null != boundOnly && boundOnly) {
			builder.and("a.user_gid <> ''").and("a.user_gid IS NOT NULL");
		}
		
		if (Objects.nonNull(pquery)) {
			SelectBuilder countBuilder = builder.clone();
			countBuilder.column().column("count(" + BaseIdKey.USER.idColumn() + ")");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("SQL : {} / PARAMS : {}", countBuilder.build(), params);
			}
			Integer total = row(countBuilder.toString(), Integer.class, params);
			Paginator paginator = new Paginator(total, pquery);
			Pagination pagination = paginator.getPagination();
			pquery.setPagination(pagination);

			SortOrder orderType = SortOrder.valueOf(pquery.getOrder().toUpperCase());
			builder.orderBy(pquery.getOrderBy(), orderType);
			builder.limit(pagination.getPageStartRow(), pquery.getPageSize());
		}

		// paginate the query
		// paginate("count(" + BaseIdKey.USER.idColumn() + ")", builder, params, pquery);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : " + builder.toString() + " / PARAMS : " + params.toString());
		}
		List<String> ExtFields = Lists.newArrayList("abbr", "short_name", "node_gid", "source_name",
				"title", "department");

		return rows(builder.toString(), userdao.getRowMapper((info, rs) -> {
			for (String field : ExtFields) {
				if (DAOSupport.isExistColumn(rs, field)) {
					info.setProperty(field, rs.getString(field));
				}
			}
		}), params);

	}

	@JdbiTran
	public boolean removeUser(ServiceContext svcctx, InfoId userId, String username) {

		int cnt = -1;

		UserInfo info = null;
		if (null != userId && IdKeys.isValidId(userId)) {
			info = userdao.row(userId);
		} else {
			SelectBuilder builder = SqlBuilder.select(BaseIdKey.USER.schema());
			builder.where("username = ?");

			List<UserInfo> list = rows(builder.build(), UserDAO.INFO_MAPPER, username);
			info = Iterables.getFirst(list, null);
		}
		if (null == info)
			return true;

		InfoId uid = info.getInfoId();
		svcctx.setOperationObject(uid);

		cnt = userdao.delete(info.getInfoId());

		DeleteBuilder builder = userlogindao.deleteSql();
		builder.where("user_id = ?");
		update(builder.build(), uid.getId());

		return cnt > 0;
	}

	@JdbiTran
	public boolean removeLogin(ServiceContext svcctx, String... login) {
		DeleteBuilder builder = SqlBuilder.delete(MasterIdKey.USER_LOGIN.schema());
		builder.where("login in ('" + Joiner.on("','").join(login) + "')");

		int cnt = update(builder.build());
		return cnt > 0;

	}

	@JdbiTran(readOnly = true)
	public UserLoginInfo getUserLogin(InfoId userId, String authenType) {

		SelectBuilder builder = SqlBuilder.select(MasterIdKey.USER_LOGIN.schema());
		builder.all();
		List<Object> params = Lists.newArrayList();

		if (IdKeys.isValidId(userId)) {
			builder.and("user_id = ?");
			params.add(userId.getId());
		}

		if (Strings.isNullOrEmpty(authenType)) {

			builder.and("authen_type = ?");
			params.add(authenType);
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : {} / PARAMS : {}", builder.toString(), params);
		}

		return row(builder.toString(), UserLoginDAO.INFO_MAPPER, params);

	}

	@JdbiTran(readOnly = true)
	public List<UserTitleInfo> getUserTitles(InfoId userId) {
		SelectBuilder builder = SqlBuilder.select(MasterIdKey.USER_TITLE.schema());
		builder.all();

		builder.and("user_id = ?");
		List<Object> params = Arrays.asList( userId.getId() );

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : {} / PARAMS : {}", builder.toString(), params);
		}

		return rows(builder.toString(), UserTitleDAO.INFO_MAPPER, params);
	
	}

}
