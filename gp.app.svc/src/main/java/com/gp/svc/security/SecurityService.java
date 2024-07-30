/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.svc.security;

import com.google.common.base.Objects;
import com.google.common.base.*;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.gp.bind.BindAutowired;
import com.gp.bind.BindComponent;
import com.gp.cache.CacheManager;
import com.gp.cache.ICache;
import com.gp.common.*;
import com.gp.common.GroupUsers.AuthenType;
import com.gp.common.GroupUsers.UserState;
import com.gp.dao.TokenDAO;
import com.gp.dao.UserDAO;
import com.gp.dao.UserLoginDAO;
import com.gp.dao.info.*;
import com.gp.db.JdbiTran;
import com.gp.exception.ServiceException;
import com.gp.info.BaseIdKey;
import com.gp.info.FilterMode;
import com.gp.info.Principal;
import com.gp.sql.SqlBuilder;
import com.gp.sql.delete.DeleteBuilder;
import com.gp.sql.select.SelectBuilder;
import com.gp.sql.update.UpdateBuilder;
import com.gp.svc.BaseService;
import com.gp.svc.ServiceSupport;
import com.gp.svc.SystemService;
import com.gp.util.CryptoUtils;
import com.gp.util.BaseUtils;
import com.gp.util.JwtTokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.util.*;

@BindComponent(priority = BaseService.BASE_PRIORITY)
public class SecurityService extends ServiceSupport implements BaseService {

	public static Logger LOGGER = LoggerFactory.getLogger(SecurityService.class);

	@BindAutowired
	private UserDAO userdao;

	@BindAutowired
	TokenDAO tokendao;

	@BindAutowired
	UserLoginDAO userLoginDao;

	@BindAutowired
	SystemService systemservice;

	private ICache securityCache;
	
	public SecurityService() {
		
		this.securityCache = CacheManager.instance().getCache(Caches.EXTRA_CACHE);
	}
	
	/**
	 * Get user information by multiple condition
	 * - user id
	 * - username
	 * - bizcard
	 * - user global id
	 * 
	 **/
	@JdbiTran(readOnly = true)
	public UserInfo getUserInfo(InfoId userId, String username, String bizcard, String userGid) {
		UserInfo uinfo = null;

		if (userId != null && IdKeys.isValidId(userId)) {

			uinfo = userdao.row(userId);

		} else if (!Strings.isNullOrEmpty(username)) {

			SelectBuilder builder = SqlBuilder.select(BaseIdKey.USER.schema());
			builder.where("username = ?");
			
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("SQL : {} / PARAMS : {}", builder.toString(), username);
			}
			List<UserInfo> list = rows(builder.build(), UserDAO.INFO_MAPPER, username);
			uinfo = Iterables.getFirst(list, null);

		} else if (!Strings.isNullOrEmpty(bizcard)) {

			SelectBuilder builder = SqlBuilder.select(BaseIdKey.USER.schema());
			builder.where("trace_code = ?");
			
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("SQL : {} / PARAMS : {}", builder.toString(), bizcard);
			}
			List<UserInfo> list = rows(builder.build(), UserDAO.INFO_MAPPER, bizcard);
			uinfo = Iterables.getFirst(list, null);

		} else if (!Strings.isNullOrEmpty(userGid)) {

			SelectBuilder builder = SqlBuilder.select(BaseIdKey.USER.schema());
			builder.where("user_gid = ?");
			
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("SQL : {} / PARAMS : {}", builder.toString(), userGid);
			}
			List<UserInfo> list = rows(builder.build(), UserDAO.INFO_MAPPER, userGid);
			uinfo = Iterables.getFirst(list, null);

		} 

		return uinfo;
	}

	@JdbiTran(readOnly = true)
	public Principal getPrincipal(String login, String userGid, String nodeGid, AuthenType... authTypes) {

		String cacheKey = Joiner.on(GeneralConsts.KEYS_SEPARATOR).join(Strings.nullToEmpty(login), 
				Strings.nullToEmpty(userGid), 
				Strings.nullToEmpty(nodeGid));
		
		Object cachedOne = this.securityCache.fetch(cacheKey);
		if(cachedOne != null) {
			LOGGER.debug("find cached principal - {}", login);
			return (Principal)cachedOne;
		}
		
		SelectBuilder builder = SqlBuilder.select();
		builder.column("usr.user_id", "usr.username", "usr.source_id", "usr.user_gid", "usr.classification");
		builder.column("usr.language", "usr.timezone", "usr.category");

		builder.from(BaseIdKey.USER.schema("usr"));
		builder.from("gp_user_login l");

		builder.where("l.user_id = usr.user_id");
		List<Object> params = Lists.newArrayList();
		
		builder.and("l.login = ?");
		params.add(login);
		if (authTypes != null && authTypes.length > 0) {
			builder.and("l.authen_type in ('" + Joiner.on("','").join(authTypes) + "')");
		}
		
		if(!Strings.isNullOrEmpty(userGid)) {
			builder.and("usr.user_gid = ?");
			params.add(userGid);
		}

		KVPair<String, Principal> pholder = KVPair.newPair("principal", null);
		
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : {} / PARAMS : {}", builder.toString(), params);
		}
		
		query(builder.build(), (rs) -> {

			Principal principal = new Principal(IdKeys.getInfoId(BaseIdKey.USER, rs.getLong("user_id")));

			principal.setUserGid(rs.getString("user_gid"));
			principal.setUsername(rs.getString("username"));
			principal.setCategory(rs.getString("category"));
			principal.setClassification(rs.getString("classification"));
			String lang = rs.getString("language");
			if (!Strings.isNullOrEmpty(lang)) {
				// zh_CN / en_US / fr_FR
				String[] localeStr = Iterables.toArray(Splitter.on("_").split(lang), String.class);
				Locale locale = Locale.ENGLISH;
				if (localeStr.length == 2) {
					locale = new Locale(localeStr[0], localeStr[1]);
				}
				principal.setLocale(locale);
			}

			String tzone = rs.getString("timezone");
			if (!Strings.isNullOrEmpty(tzone)) {
				principal.setTimeZone(ZoneId.of(tzone));
			}

			pholder.setValue(principal);
		}, params);

		if (null != pholder.getValue()) {

			SelectBuilder rlist = SqlBuilder.select();
			rlist.column("r.role_abbr");
			rlist.from(from -> {
				from.table(AppIdKey.ROLE.schema() + " r");
				from.join(AppIdKey.USER_ROLE.schema() + " ur", "ur.role_id = r.role_id");
			});
			rlist.where("ur.user_id = ?");

			Set<String> roles = Sets.newHashSet();
			query(rlist.build(), (rs) -> {
				roles.add(rs.getString("role_abbr"));
			}, pholder.getValue().getUserId().getId());

			pholder.getValue().setRoles(roles);
			
			// cache found principal
			securityCache.put(cacheKey, pholder.getValue());
		}
		
		return pholder.getValue();

	}

	@JdbiTran
	public boolean changePassword(ServiceContext svcctx, String username, String password, String authType) {

		int cnt = -1;

		SelectBuilder builder = SqlBuilder.select("gp_user_login l");
		builder.from("gp_user u");
		builder.column("u.*");
		builder.where("u.user_id = l.user_id");
		builder.and("u.username = ?");
		builder.and("l.authen_type =?");

		List<UserInfo> list = rows(builder.build(), UserDAO.INFO_MAPPER, username, authType);
		UserInfo uinfo = Iterables.getFirst(list, null);

		if (null == uinfo)
			return false;

		SysOptionInfo opt = systemservice.getOption("symmetric.crypto.iv");
		String randomIV = opt.getOptValue();
		SymmetricToken itk = new SymmetricToken();

		itk.initial(uinfo.getCryptoKey(), randomIV);
		String hashpwd = itk.encrypt(password);

		UpdateBuilder updbuilder = SqlBuilder.update(AppIdKey.USER_LOGIN.schema());
		updbuilder.column("credential");
		updbuilder.where("user_id = ?");
		updbuilder.and("authen_type = ?");

		List<Object> params = Arrays.asList( hashpwd, uinfo.getId(), authType );
		if (LOGGER.isDebugEnabled()) {

			LOGGER.debug("SQL: {} / PARMS {} : ", updbuilder.toString(),  params);
		}

		cnt = update(updbuilder.toString(), params);

		return cnt > 0;

	}

	@JdbiTran
	public boolean updateLogonTrace(ServiceContext svcctx, InfoId userId, boolean resetRetry) {

		UpdateBuilder builder = SqlBuilder.update(BaseIdKey.USER.schema());
		if (resetRetry) {

			builder.set("retry_times", "0");
			builder.set("last_logon", "?");
			builder.where("user_id = ?");

		} else {

			builder.set("retry_times", "retry_times + 1");
			builder.set("last_logon", "?");
			builder.where("user_id = ?");

		}
		List<Object> params = Arrays.asList(BaseUtils.now(), userId.getId());

		if (LOGGER.isDebugEnabled()) {

			LOGGER.debug("SQL : {} / PARAMS : {}", builder.toString(),  params);
		}
		int cnt = -1;

		cnt = update(builder.toString(), params);

		return cnt > 0;
	}

	@JdbiTran
	public boolean changeUserState(ServiceContext svcctx, InfoId userId, UserState state) {

		List <Object> params = null;
		UpdateBuilder builder = SqlBuilder.update(BaseIdKey.USER.schema());
		if (UserState.ACTIVE == state) {

			builder.column("retry_times", "0");
			builder.column("state", "?");
			builder.where("user_id = ?");

			params = Arrays.asList(state.name(), userId.getId());
		} else if (UserState.DEACTIVE == state) {

			builder.column("retry_times", "0");
			builder.column("state", "?");
			builder.where("user_id = ?");

			params = Arrays.asList( state.name(), userId.getId());
		} else {
			builder.column("last_logon", "?");
			builder.column("state", "?");
			builder.where("user_id = ?");

			params = Arrays.asList( BaseUtils.now(), state.name(), userId.getId());
		}

		if (LOGGER.isDebugEnabled()) {

			LOGGER.debug("SQL : {} / PARAMS : {}", builder.toString() , params);
		}
		int cnt = -1;

		cnt = update(builder.toString(), params);

		return cnt > 0;
	}

	public TokenInfo getToken(InfoId tokenKey, String refreshToken) {

		SelectBuilder builder = tokendao.selectSql();
		List<Object> params = Lists.newArrayList();
		if(IdKeys.isValidId(tokenKey)) {
			builder.and("token_id = ?");
			params.add(tokenKey.getId());
		}
		if(!Strings.isNullOrEmpty(refreshToken)) {
			builder.and("refresh_token = ?");
			params.add(refreshToken);
		}
		
		return row(builder.build(), TokenDAO.INFO_MAPPER, params);

	}

	/**
	 * !!! IMPORTANT: subject, audience, device are 3 keys to locate a unique token 
	 * 
	 **/
	@JdbiTran
	public boolean newToken(ServiceContext svcctx, TokenInfo token) {

		svcctx.setTraceInfo(token);
		DeleteBuilder builder = SqlBuilder.delete(BaseIdKey.TOKEN.schema());
		builder.where("subject = ?");
		builder.and("audience = ?");
		builder.and("device = ?");
		builder.and("issuer = ?");
		
		List<Object> params = Arrays.asList(token.getSubject(), token.getAudience(), token.getDevice(), token.getIssuer());
		if (LOGGER.isDebugEnabled()) {

			LOGGER.debug("SQL : {} / PARAMS : {}", builder.toString(), params);
		}
		update(builder.toString(), params);

		return tokendao.create(token) > 0;

	}

	@JdbiTran
	public boolean refreshToken(ServiceContext svcctx, TokenInfo token) {

		svcctx.setTraceInfo(token);
		token.setFilter(FilterMode.include(FlatColumns.EXP_TIME.getColumn(), FlatColumns.ISSUE_AT.getColumn(),
				FlatColumns.NOT_BEFORE.getColumn(), FlatColumns.JWT_TOKEN.getColumn(), "refresh_token"));

		int cnt = tokendao.update(token);

		return cnt > 0;

	}

	@JdbiTran
	public boolean removeToken(ServiceContext svcctx, InfoId tokenId) {

		int cnt = tokendao.delete(tokenId);

		return cnt > 0;

	}

	@JdbiTran(readOnly = true)
	public Boolean authorize(String username, String accessPath) {

		SelectBuilder builder = SqlBuilder.select();
		builder.column("ur.role_id");
		builder.from("gp_user u");
		builder.from("gp_user_role ur");
		builder.where("u.username = ?");
		builder.and("ur.user_id = u.user_id");
		List<Object> params = Arrays.asList( username );
		if (LOGGER.isDebugEnabled()) {

			LOGGER.debug("SQL : {} / PARAMS : {}",builder.toString(), params);
		}
		List<Long> roleids = rows(builder.toString(), Long.class, params);

		if (Iterables.isEmpty(roleids)) {
			return false;
		}

		builder = SqlBuilder.select(AppIdKey.ROLE_PERM.schema());
		builder.column("count(perm_id)");
		builder.where("role_id in ( <role_ids> )");
		builder.and("authorized = :authorized");

		Map<String, Object> paramap = Maps.newHashMap();
		paramap.put("role_ids", roleids);
		paramap.put("authorized", true);
		if (LOGGER.isDebugEnabled()) {

			LOGGER.debug("SQL : " + builder.toString() + " / params : " + paramap);
		}

		Integer cnt = row(builder.toString(), Integer.class, paramap);

		return cnt > 0;

	}

	@JdbiTran(readOnly = true)
	public InfoId checkLogin(String... logins) throws ServiceException{

		if (null != logins && logins.length > 0) {
			SelectBuilder builder = SqlBuilder.select(AppIdKey.USER_LOGIN.schema());
			builder.distinct();
			builder.column("user_id");
			builder.where("login in ('" + Joiner.on("','").join(logins) + "')");

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("SQL : {}", builder.toString());
			}
			List<InfoId> ids = rows(builder.toString(), (rs, i) -> {
				return IdKeys.getInfoId(BaseIdKey.USER, rs.getLong("user_id"));
			});
			
			if(ids.size() > 1) {
				throw new ServiceException("excp.more.users");
			}
			
			return Iterables.getFirst(ids, null);
		}
		return null;

	}

	/***
	 * Authenticate the login and password, login may be: mobile, email, user_gid. 
	 * user_gid is used for interim token scenario, and it requires the device to create login key
	 * 
	 * @param login the login key
	 * @param password the credential
	 * @param authenType the authen types
	 * 
	 * @return Boolean authen pass or not
	 * 
	 **/
	@JdbiTran
	public Boolean authenticate(ServiceContext svcctx, String login, String password, AuthenType... authenType) throws ServiceException {

		boolean matched = false;

		List<String> types = Lists.newArrayList();
		for (AuthenType type : authenType) {
			types.add(type.name());
		}
	
		SysOptionInfo opt = systemservice.getOption("symmetric.crypto.iv");
		String randomIV = opt.getOptValue();
		
		Map<String, Object> params = new HashMap<String, Object>();
		SelectBuilder builder = SqlBuilder.select(AppIdKey.USER_LOGIN.schema());
		builder.where("authen_type IN ( <authen_types> )");
		params.put("authen_types", types);
		
		String device = svcctx.getContextData(JwtTokenUtils.DEVICE_ID, String.class);
		
		if(Strings.isNullOrEmpty(device)) {
			
			builder.and("login = :login");
			params.put("login", login);
		
		}else {
			// Add interim login case match 
			String intrimKey = login + GeneralConsts.KEYS_SEPARATOR + device;
			builder.and("(login = :login OR login = :interim)");
			params.put("login", login);
			params.put("interim", intrimKey);
		}
		
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL: " + builder.toString() + " / PARAMS: " + params.toString());
		}
	
		List<String> parts = Splitter.on(GeneralConsts.NAMES_SEPARATOR).splitToList(password);
		password = parts.get(0); // hmac password encrypted in request client.
		String secret = parts.get(1); // hmac encrypt secret
		
		List<UserLoginInfo> logins = rows(builder.toString(), UserLoginDAO.INFO_MAPPER, params);
		
		if(logins.size() == 0) {
			// no login bind to user
			throw new ServiceException("excp.login.none");
		} else {
			
			UserLoginInfo linfo = logins.get(0);
			InfoId uKey = IdKeys.getInfoId(BaseIdKey.USER, linfo.getUserId());
			UserInfo uinfo = userdao.row(uKey);
			UserState state = Enums.getIfPresent(UserState.class, uinfo.getState()).get();
			
			if(UserState.DEACTIVE == state || UserState.FROZEN == state) {
				// user login is locked
				throw new ServiceException("excp.login.lock");
			}
		}
		String cryptoKey = null;
		SymmetricToken itk = new SymmetricToken();
		for (UserLoginInfo loginInfo : logins) {

			if (Strings.isNullOrEmpty(cryptoKey)) {
				cryptoKey = column(IdKeys.getInfoId(BaseIdKey.USER, loginInfo.getUserId()),
						FlatColumns.CRYPTO_KEY, String.class);
				itk.initial(cryptoKey, randomIV);
			}
			// if login no credential then use the account credential.
			String passcode = itk.decrypt(loginInfo.getCredential());

			String hmacStr = CryptoUtils.hmacCrypt(secret, passcode);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("token {} - hmac 64 {}", secret, hmacStr);
			}
			if (Objects.equal(hmacStr, password)) {
				InfoId uid = IdKeys.getInfoId(BaseIdKey.USER, loginInfo.getUserId());
				// password match means logon success reset the retry_times
				updateLogonTrace(svcctx, uid, matched);
				
				String userGid = column(uid, FlatColumns.USER_GID, String.class);
				svcctx.putContextData(JwtTokenUtils.USER_GLOBAL_ID, userGid);

				matched = true;
				break;
			}
		}

		return matched;
	}

	/**
	 * Create a new interim login information, as for interim login the login key
	 * is concatenate with user global id and device, eg. [ugi]:[dvi] - 929383:83373773-939378-03389
	 * 
	 * @param userId the user id
	 * @param device the login device
	 * 
	 * @return the credential of interim login
	 **/
	@JdbiTran
	public String newInterimLogin(ServiceContext svcctx, InfoId userId, String device) {

		DeleteBuilder delSql = SqlBuilder.delete(AppIdKey.USER_LOGIN.schema());
		delSql.where("user_id = ?");
		delSql.and("login = ?");
		delSql.and("authen_type = 'INTERIM'");

		SysOptionInfo opt = systemservice.getOption("symmetric.crypto.iv");
		String randomIV = opt.getOptValue();
		SymmetricToken symToken = new SymmetricToken();

		Map<String, Object> row = columns(userId, "crypto_key", "user_gid");
		String userGid = (String) row.get("user_gid");
		String cryptoKey = (String) row.get("crypto_key");
		
		String loginKey = userGid + GeneralConsts.KEYS_SEPARATOR + device;
		
		// clear old interim token firstly
		update(delSql.build(), userId.getId(), loginKey);

		UserLoginInfo login = new UserLoginInfo();
		svcctx.setTraceInfo(login);
		login.setUserId(userId.getId());
		login.setAuthenType(AuthenType.INTERIM.name());
		
		login.setLogin(loginKey);
		symToken.initial(cryptoKey, randomIV);

		String password = String.valueOf(GroupUsers.PWD_GENERATOR.generate());
		String hashpwd = symToken.encrypt(password);
		login.setCredential(hashpwd);

		userLoginDao.create(login);

		return password;

	}

}
