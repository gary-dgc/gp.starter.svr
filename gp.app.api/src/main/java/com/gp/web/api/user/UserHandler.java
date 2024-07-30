/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.user;

import com.google.common.base.Enums;
import com.google.common.base.Strings;
import com.gp.bind.BindScanner;
import com.gp.common.*;
import com.gp.common.GroupUsers.UserCategory;
import com.gp.common.GroupUsers.UserState;
import com.gp.dao.info.RoleInfo;
import com.gp.dao.info.UserInfo;
import com.gp.exception.BaseException;
import com.gp.exception.CoreException;
import com.gp.exception.WebException;
import com.gp.info.BaseIdKey;
import com.gp.info.DataBuilder;
import com.gp.info.Principal;
import com.gp.paging.PageQuery;
import com.gp.svc.CommonHelper;
import com.gp.svc.security.AuthorizeService;
import com.gp.svc.security.SecurityService;
import com.gp.svc.user.UserService;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import com.gp.web.api.ServiceApiHelper;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap.SimpleEntry;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class UserHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(UserHandler.class);
	
	private UserService userService;
	private SecurityService securityService;
	private CommonHelper commonService;

	private AuthorizeService authService;
	
	public UserHandler() {
		
		userService = BindScanner.instance().getBean(UserService.class);
		securityService = BindScanner.instance().getBean(SecurityService.class);
		commonService = BindScanner.instance().getBean(CommonHelper.class);
		authService = BindScanner.instance().getBean(AuthorizeService.class);
		
	}

	@WebApi(path="users-query")
	public void handleAccountsQuery(HttpServerExchange exchange) throws BaseException{
		
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);
		svcctx.setOperation(Operations.ACNT_FND);
		String keywords = Filters.filterString(paramap, "keyword");
		
		String state = Filters.filterAll(paramap, "state");

		String category = Filters.filterAll(paramap, "category");
		
		PageQuery pquery = Filters.filterPageQuery(paramap);
		if(!Objects.isNull(pquery) && pquery.getOrderBy().indexOf("name") > 0){
			pquery.setOrderBy("full_name");
		}
		ActionResult result = null;
		svcctx.addOperationPredicates(paramap);
		// query accounts information
		List<UserInfo> extList = userService.getUsers( keywords,
				Strings.isNullOrEmpty(category) ? null : new UserCategory[] { Enums.getIfPresent(UserCategory.class, category).get() }, 
				Strings.isNullOrEmpty(state) ? null : new UserState[] { Enums.getIfPresent(UserState.class, category).get() }, false,
				pquery);
		List<Map<String, Object>> list = extList.stream().map((info) -> {
			
			DataBuilder builder = new DataBuilder();
			
			builder.set("user_id", info.getId().toString());
			builder.set(info, "user_gid", "username", "email", "mobile", "state",
					"category", "full_name");

			if(info.getCreateTime() != null) {
				builder.set("create_date", String.valueOf(info.getCreateTime().getTime()));
			}
			
			String avatarUrl = ServiceApiHelper.absoluteBinaryUrl(info.getAvatarUrl());
			builder.set("avatar_url", avatarUrl);
			
			return builder.build();
		}).collect(Collectors.toList());
		
		result = ActionResult.success(this.getMessage(exchange, "mesg.find.account"));
		result.setData(pquery == null ? null : pquery.getPagination(), list);
	
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="user-save")
	public void handleAccountSave(HttpServerExchange exchange)throws BaseException  {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ACNT_UPD);
		ActionResult result = null;
		
		UserInfo uinfo = new UserInfo();
		InfoId uid = IdKeys.getInfoId(BaseIdKey.USER, Filters.filterLong(params, "user_id"));
		uinfo.setInfoId(uid);
		uinfo.setUsername(Filters.filterString(params, "username"));
		uinfo.setFullName(Filters.filterString(params, "full_name"));
		uinfo.setNickname(Filters.filterString(params, "nickname"));
		uinfo.setLanguage(Filters.filterString(params, "language"));
		uinfo.setEmail(Filters.filterString(params, "email"));
		
		uinfo.setIdCard(Filters.filterString(params, "id_card"));
		uinfo.setPhone(Filters.filterString(params, "phone"));
		uinfo.setMobile(Filters.filterString(params, "mobile"));
		uinfo.setTimezone(Filters.filterString(params, "timezone"));
		uinfo.setCategory(Filters.filterString(params, "category"));
	
		uinfo.setState(Filters.filterString(params, "state"));
		uinfo.setClassification(Filters.filterString(params, "classification"));
		uinfo.setBiography(Filters.filterString(params, "biography"));
		
		Long capacity = Filters.filterLong(params, "capacity") * 1024L * 1024L;
		uinfo.setProperty("capacity", capacity);
		
		uinfo.setProperty("roles", params.get("roles"));
		
		// amend the operation information
		svcctx.setOperationObject(uinfo.getInfoId());
		svcctx.addOperationPredicates(uinfo);
		
		String avatarUrl = Filters.filterString(params, "avatar_url");
		if(!Strings.isNullOrEmpty(avatarUrl) && avatarUrl.startsWith("data:image/")){
			// process the avatar base64 image
			InfoId storageId = IdKeys.getInfoId(AppIdKey.STORAGE, Filters.filterLong(params, "storage_id"));
			//avatarUrl = ServiceApiHelper.instance().cacheAvatar(storageId, avatarUrl);
			
			uinfo.setAvatarUrl(avatarUrl);
			
		}else {
			String relativeUrl = ServiceApiHelper.instance().relativeBinaryUrl(avatarUrl);
			
			uinfo.setAvatarUrl(relativeUrl);
		}
		
		// check account existence
		InfoId checkUid = securityService.checkLogin(uinfo.getUsername(), uinfo.getEmail(), uinfo.getMobile());
		if(null != checkUid && !checkUid.equals(uid)) {
			throw new CoreException(svcctx.getPrincipal().getLocale(), "excp.valid.logins");
		}
		
		userService.updateUser(svcctx, uinfo);
		
		result = ActionResult.success(getMessage(exchange, "mesg.save.account"));

		this.sendResult(exchange, result);
	}
	
	/**
	 * {roles:[]}, roles item can be id or abbreviation 
	 **/
	@WebApi(path="user-add")
	public void handleAccountAdd(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ACNT_NEW);
		Principal principal = this.getPrincipal(exchange);
			
		String confirmPwd = Filters.filterString(params, "confirm");
		String password = Filters.filterString(params, "password");
		UserInfo uinfo = new UserInfo();
		uinfo.setUsername(Filters.filterString(params, "username"));
		uinfo.setFullName(Filters.filterString(params, "full_name"));
		uinfo.setNickname(Filters.filterString(params, "nickname"));
		uinfo.setLanguage(Filters.filterString(params, "language"));
		uinfo.setEmail(Filters.filterString(params, "email"));
		
		uinfo.setIdCard(Filters.filterString(params, "id_card"));
		uinfo.setPhone(Filters.filterString(params, "phone"));
		uinfo.setMobile(Filters.filterString(params, "mobile"));
		uinfo.setTimezone(Filters.filterString(params, "timezone"));
		uinfo.setCategory(UserCategory.NODE.name()); // here create local user only
		
		uinfo.setState(Filters.filterString(params, "state"));
		uinfo.setClassification(Filters.filterString(params, "classification"));
		uinfo.setBiography(Filters.filterString(params, "biography"));
		
		Long capacity = Filters.filterLong(params, "capacity") * 1024l * 1024l;
		uinfo.setProperty("capacity", capacity);
		
		uinfo.setProperty("roles", params.get("roles"));
		uinfo.setProperty("storage_id", Filters.filterLong(params, "storage_id"));
		
		String avatarUrl = Filters.filterString(params, "avatar_url");
		if(!Strings.isNullOrEmpty(avatarUrl) && avatarUrl.startsWith("data:image/")){
			// process the avatar base64 image
			InfoId storageId = IdKeys.getInfoId(AppIdKey.STORAGE, Filters.filterLong(params, "storage_id"));
			//avatarUrl = ServiceApiHelper.instance().cacheAvatar(storageId, avatarUrl);
			
			uinfo.setAvatarUrl(avatarUrl);
		}else {
			String relativeUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
			
			uinfo.setAvatarUrl(relativeUrl);
		}
		
		// set local entity id
		uinfo.setSourceId(GeneralConsts.LOCAL_SOURCE);

		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.save.account"));
		
		// check account existence
		InfoId uid = securityService.checkLogin(uinfo.getUsername(), uinfo.getEmail(), uinfo.getMobile());
		if(null != uid) {
			throw new CoreException(svcctx.getPrincipal().getLocale(), "excp.exist.account");
		}
		
		// amend the information key data
		if(uinfo.getInfoId() == null){
			
			InfoId ukey = IdKeys.newInfoId( BaseIdKey.USER);
			uinfo.setInfoId(ukey);
		}
		
		
		userService.newUser(svcctx, uinfo, confirmPwd);
		
		// amend the operation information
		svcctx.setOperationObject(uinfo.getInfoId());
		svcctx.addOperationPredicates(uinfo);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="user-remove")
	public void handleAccountDelete(HttpServerExchange exchange)throws BaseException {
		
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ACNT_RMV);
		String username = Filters.filterString(paramap, "username");
		long uid = Filters.filterLong(paramap, "user_id");
		
		ActionResult result = new ActionResult();
	
		Long userId = null;
		
		if(uid > 0){
			userId = Long.valueOf(uid);
		}
		
		if(userId == GeneralConsts.USER_ADMIN){
			
			result = ActionResult.failure(getMessage(exchange, "mesg.rsrv.admin"));
			this.sendResult(exchange, result);
			return ;
		}
		
		InfoId userkey = IdKeys.getInfoId(BaseIdKey.USER,userId);

		svcctx.addOperationPredicates(new SimpleEntry<String,String>("username", username));
			
		// password match means logon success reset the retry_times
		userService.removeUser(svcctx, userkey, username);
		
		result = ActionResult.success(getMessage(exchange, "mesg.remove.account"));
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="user-info")
	public void handleAccountInfo(HttpServerExchange exchange)throws BaseException {

		ActionResult result = null;
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ACNT_INF);
		String account = Filters.filterString(paramap, "username");
		
		InfoId userkey = null;
		Principal principal = this.getPrincipal(exchange);
		Long userId = Filters.filterLong(paramap, "user_id");
		if(userId == null || userId == 0) {
			userkey = principal.getUserId();
		}else {
			userkey = IdKeys.getInfoId(BaseIdKey.USER,userId);
		}
		if(!IdKeys.isValidId(userkey) && Strings.isNullOrEmpty(account)) {
			result = ActionResult.failure(super.getMessage(exchange, "excp.miss.param"));
			this.sendResult(exchange, result);
			return;
		}
				
		svcctx.addOperationPredicates(paramap);
		
		UserInfo info = userService.getUserFull( userkey, account);
		
		DataBuilder builder = new DataBuilder();
		builder.set("user_id", info.getId().toString());
		builder.set("source_id", info.getSourceId().toString());
		builder.set("user_gid", Strings.nullToEmpty(info.getUserGid()));
		builder.set("email", info.getEmail());
		builder.set("id_card", info.getIdCard());
		builder.set("mobile", info.getMobile());
		builder.set("phone", info.getPhone());
		builder.set("full_name", info.getFullName());
		builder.set("username", info.getUsername());
		builder.set("nickname", info.getNickname());
		builder.set("state", info.getState());
	
		builder.set("category", info.getCategory());
		builder.set("language", info.getLanguage());
		builder.set("timezone", info.getTimezone());
		builder.set("classification", info.getClassification());
		builder.set("biography", info.getBiography());
		
		if(info.getCreateTime() != null) {
			builder.set("create_date", String.valueOf(info.getCreateTime().getTime()));
		}
		String avatarUrl = ServiceApiHelper.absoluteBinaryUrl(info.getAvatarUrl());
		builder.set("avatar_url", avatarUrl);
				
		List<RoleInfo> roles = authService.getUserRoles(userkey, account);
		List<String> rlist = roles.stream().map(r -> r.getRoleAbbr()).collect(Collectors.toList());
		
		builder.set("roles", rlist);

		builder.set("modifier_uid", info.getModifierUid().toString());
		builder.set("modify_time", String.valueOf(info.getModifyTime().getTime()));
				
		result = ActionResult.success(getMessage(exchange, "mesg.find.account"));
		result.setData(builder.build());
			
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="user-profile")
	public void handleProfileInfo(HttpServerExchange exchange)throws BaseException {

		ActionResult result = null;
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ACNT_INF);
		
		String username = Filters.filterString(paramap, "username");
		String userGid = Filters.filterString(paramap, "user_gid");
		
		if(Strings.isNullOrEmpty(userGid) && Strings.isNullOrEmpty(username)) {
			userGid = svcctx.getPrincipal().getUserGid();
		}
	
		InfoId userkey = commonService.getInfoId(userGid);
				
		UserInfo info = userService.getUserFull( userkey, username);
		
		DataBuilder builder = new DataBuilder();

		builder.set("user_id", info.getId().toString());
		builder.set("cabinet_id", info.getCabinetId().toString());
		
		builder.set(info, "username", "email", "mobile", "phone", 
				"full_name", "nick_name", "state", "language",
				"time_zone", "biography", "classification");
		
		String avatarUrl = ServiceApiHelper.absoluteBinaryUrl(info.getAvatarUrl());
		builder.set("avatar_url", avatarUrl);
		
		// collect user roles
		List<RoleInfo> roles = authService.getUserRoles(userkey, username);
		List<String> rlist = roles.stream().map(r -> r.getRoleAbbr()).collect(Collectors.toList());
		
		builder.set("roles", rlist);

		result = ActionResult.success(getMessage(exchange, "mesg.find.account"));
		result.setData(builder.build());
		
		this.sendResult(exchange, result);
	}
	
	/**
	 * Reset User password
	 * 
	 * @param user_id the user id
	 * @param password the password
	 * @param confirm the pass confirm
	 * @param authen_type the authen type
	 * 
	 **/
	@WebApi(path="reset-password")
	public void handleResetPassword(HttpServerExchange exchange) throws BaseException {
		ActionResult result = null;
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ACNT_UPD);
		
		InfoId userkey = null;
		Principal principal = this.getPrincipal(exchange);
		Long userId = Filters.filterLong(paramap, "user_id");
		if(userId == null || userId == 0) {
			userkey = principal.getUserId();
		}else {
			userkey = IdKeys.getInfoId(BaseIdKey.USER,userId);
		}
		
		String password = Filters.filterString(paramap, "password");
		String confirm = Filters.filterString(paramap, "confirm");
		String typeStr = Filters.filterString(paramap, "authen_type");
		if(Strings.isNullOrEmpty(typeStr)){
			typeStr = GroupUsers.AuthenType.INLINE.name();
		}
		if(Strings.isNullOrEmpty(password) || Strings.isNullOrEmpty(confirm)) {
			throw new WebException(principal.getLocale(), "excp.miss.param");
		}
		
		if(!Objects.equals(password, confirm)) {
			throw new WebException(principal.getLocale(), "excp.confirm.unmatch");
		}
		
		String username = commonService.column(userkey, "username", String.class);
		if(Strings.isNullOrEmpty(username)) {
			throw new WebException(principal.getLocale(), "excp.not.exist");
		}
		
		boolean done = securityService.changePassword(svcctx, username, password, typeStr);
		
		result = done ? ActionResult.success(getMessage(exchange, "mesg.reset.password")) 
				: ActionResult.failure(getMessage(exchange, "excp.reset.password")) ;
		
		this.sendResult(exchange, result);
	}

}
