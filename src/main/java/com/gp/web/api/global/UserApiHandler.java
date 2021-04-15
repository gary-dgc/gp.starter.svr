/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.global;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.GeneralConstants;
import com.gp.common.GroupUsers.AuthenType;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.SymmetricToken;
import com.gp.core.CoreConstants;
import com.gp.core.NodeApiAgent;
import com.gp.dao.info.SysOptionInfo;
import com.gp.dao.info.UserInfo;
import com.gp.dao.info.UserLoginInfo;
import com.gp.dao.info.UserTitleInfo;
import com.gp.exception.BaseException;
import com.gp.info.BaseIdKey;
import com.gp.info.DataBuilder;
import com.gp.info.Principal;
import com.gp.svc.CommonService;
import com.gp.svc.SystemService;
import com.gp.svc.user.UserService;
import com.gp.util.JsonUtils;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import com.gp.web.api.ServiceApiHelper;

import io.undertow.server.HttpServerExchange;

public class UserApiHandler extends BaseApiSupport {

	static Logger LOGGER = LoggerFactory.getLogger(UserApiHandler.class);

	UserService userService;
	
	SystemService systemService;
	
	CommonService commonService;
	
	public UserApiHandler() {
		
		userService = BindScanner.instance().getBean(UserService.class);
		systemService = BindScanner.instance().getBean(SystemService.class);
		commonService = BindScanner.instance().getBean(CommonService.class);
		
	}
	
	@WebApi(path="user-global-filter", operation="gbl:ivk")
	public void handleAccountsQuery(HttpServerExchange exchange)throws BaseException {
		
		Map<String, Object> data = this.getRequestBody(exchange);
		ActionResult result = NodeApiAgent.instance().sendGlobalPost( CoreConstants.AUTH_API_PATH + "/users-query", data);
		
		JsonNode dataNode = null;
		try {
			dataNode = JsonUtils.JSON_MAPPER.readTree(result.getRawData());
		} catch (IOException e) {
			throw new BaseException("excp.parse.json");
		}
		
		if(result.isSuccess()) {
			result.getMeta().setMessage(getMessage(exchange, "mesg.gbl.invoke"));
			
			List<Map<String, Object>> users = Lists.newArrayList();
			Set<String> fields = Sets.newHashSet("user_gid", "username", "full_name",
					"state", "email", "phone", "mobile");
			dataNode.forEach((jNode) -> {
				DataBuilder builder = new DataBuilder();
				fields.forEach(f -> {
					builder.set(f, jNode.get(f).textValue());
				});
				
				users.add(builder.build());
			});
			// prepare the external result
			Map<String, Object> extraData = Maps.newHashMap();
			extraData.put("candidates", users);
			
			if(Iterables.isEmpty(users)) {
				
				result.getMeta().setCode("NO_GLOBAL_USER");
			}else {
				
				result.getMeta().setCode("FOUND_GLOBAL_USER");
			}
			result.setData(extraData);
		}
		
		this.sendResult(exchange, result);
	}
	
	/**
	 * Register the local user in global site(/user-submit). the default password is same as the local server.
	 * the user information includes: "node_gid", "nickname","trace_code",
	 *				"username", "email", "full_name","title", "department",
	 *				"mobile", "nickname", "id_card", "biography"
	 **/
	@WebApi(path="user-global-register", operation="gbl:ivk")
	public void handleAccountRegister(HttpServerExchange exchange) throws BaseException{
		
		Map<String, Object> data = this.getRequestBody(exchange);
		
		String username = Filters.filterString(data, "username");
		Long userId = Filters.filterLong(data, "user_id");
		
		InfoId uid =IdKeys.getInfoId(BaseIdKey.USER, userId);
		UserInfo full = userService.getUserFull(uid, username);
	
		Map<String, Object> post = full.toMap("node_gid", "nickname", "trace_code",
				"username", "email", "full_name", "classification", 
				"title", "department", "phone", "avatar_url",
				"mobile", "nickname", "id_card", "biography");
		
		String avatar = (String)post.get("avatar_url");
		String avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatar);
		post.put("avatar_url", avatarUrl);
		
		SysOptionInfo opt = systemService.getOption("symmetric.crypto.iv");
		String randomIV = opt.getOptValue();
		SymmetricToken symToken = new SymmetricToken(full.getCryptoKey(), randomIV);
		UserLoginInfo login = userService.getUserLogin(full.getInfoId(), AuthenType.INLINE.name());
		String hashpwd = symToken.decrypt(login.getCredential());
		
		post.put("password", hashpwd);
		
		ActionResult result = NodeApiAgent.instance().sendGlobalPost( CoreConstants.AUTH_API_PATH + "/user-submit", post);
		if(result.isSuccess()) {
			Map<String, String> feedback = JsonUtils.toMap(result.getRawData(), String.class);
			String userGid = feedback.get("user_gid");
			commonService.updateColumn(uid, "user_gid", userGid);
			result.setData(feedback);
		}
		
		this.sendResult(exchange, result);
	}
	
	/**
	 * Bind local user with the global user(/user-bind)
	 * 
	 **/
	@WebApi(path="user-global-bind", operation="gbl:ivk")
	public void handleAccountBind(HttpServerExchange exchange) throws BaseException{
		
		Map<String, Object> data = this.getRequestBody(exchange);
		
		String userGid = Filters.filterString(data, "user_gid");
		Long userId = Filters.filterLong(data, "user_id");
		String reason = Filters.filterString(data, "reason");
		
		Principal principal = this.getPrincipal(exchange);
		
		InfoId uid =IdKeys.getInfoId(BaseIdKey.USER, userId);
		UserInfo full = userService.getUserFull(uid, null);
	
		Map<String, Object> post = full.toMap("trace_code",
				"full_name", "username", "nickname",
				"phone", "email", "mobile", "classification", "title", "department");
		
		String avatarUrl = ServiceApiHelper.absoluteBinaryUrl(full.getAvatarUrl());
		post.put("avatar_url", avatarUrl);
		
		post.put("node_gid", full.getProperty("node_gid", String.class));
		post.put("invitee_gid", userGid); 
		
		post.put("inviter_gid", principal.getUserGid());
		post.put("reason", reason);
		
		Map<String, Object> conds = Maps.newHashMap();
		conds.put("user_id", userId);
		List<UserTitleInfo> titles = userService.getUserTitles(full.getInfoId());
		
		List<String> tlist = titles.stream().map(t -> t.getTitle()).collect(Collectors.toList());
		String title = Joiner.on(',').join(tlist);
		post.put("title", title);
		post.put("department", "");
		
		ActionResult result = NodeApiAgent.instance().sendGlobalPost( CoreConstants.AUTH_API_PATH + "/user-bind", post);
		if(result.isSuccess()) {
			commonService.updateColumn(uid, "user_gid", userGid);
			result.setData((Object)null);
		}
		
		this.sendResult(exchange, result);
	}
	
	/**
	 * Bind local user with the global user(/user-bind)
	 * 
	 **/
	@WebApi(path="user-global-unbind", operation="gbl:ivk")
	public void handleAccountUnbind(HttpServerExchange exchange) throws BaseException{
		
		Map<String, Object> data = this.getRequestBody(exchange);
		
		String userGid = Filters.filterString(data, "user_gid");
		Long userId = Filters.filterLong(data, "user_id");
					
		InfoId uid =IdKeys.getInfoId(BaseIdKey.USER, userId);
		
		InfoId sid =IdKeys.getInfoId(BaseIdKey.SOURCE, GeneralConstants.LOCAL_SOURCE);
		String nodeGid = commonService.queryColumn(sid, "node_gid", String.class);
		Map<String, Object> post = Maps.newHashMap();
		
		post.put("node_gid", nodeGid);
		post.put("user_gid", userGid);
		
		ActionResult result = NodeApiAgent.instance().sendGlobalPost( CoreConstants.AUTH_API_PATH + "/user-unbind", post);
		
		if(result.isSuccess()) {
			commonService.updateColumn(uid, "user_gid", "");
			result.setData((Object)null);
		}
		
		this.sendResult(exchange, result);
	}
	
}
