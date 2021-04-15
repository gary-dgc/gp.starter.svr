/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.global;

import static com.gp.core.NodeApiAgent.GlobalName;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.GeneralConstants;
import com.gp.common.GroupUsers;
import com.gp.common.KeyValuePair;
import com.gp.common.ServiceContext;
import com.gp.common.Sources;
import com.gp.core.CoreConstants;
import com.gp.core.NodeApiAgent;
import com.gp.dao.info.SourceInfo;
import com.gp.dao.info.SysOptionInfo;
import com.gp.dao.info.UserInfo;
import com.gp.exception.ServiceException;
import com.gp.svc.SystemService;
import com.gp.svc.master.SourceService;
import com.gp.svc.user.UserService;
import com.gp.util.CryptoUtils;
import com.gp.util.JsonUtils;
import com.gp.util.NumberUtils;
import com.gp.validate.ArgsValidator;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import com.gp.web.client.NodeClient;
import com.gp.web.model.AuthenData;
import com.gp.web.util.WebUtils;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Methods;

public class SystemHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(SystemHandler.class);
	
	private SourceService sourceService;
	private SystemService systemService;
	private UserService userService;
		
	public SystemHandler() {
		
		sourceService = BindScanner.instance().getBean(SourceService.class);
		systemService = BindScanner.instance().getBean(SystemService.class);
		userService = BindScanner.instance().getBean(UserService.class);
		
		this.setPathMapping(WebUtils.getOpenApiUri("system-bind"), Methods.POST, this::handleSystemBind);
	}

	/**
	 * Submit current node system information to global server
	 * 
	 * @param app_key the application key
	 * @param app_secret the application secret
	 * 
	 **/
	@WebApi(path="system-bind", operation="gbl:ivk", open=true)
	public void handleSystemBind (HttpServerExchange exchange) throws Exception {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);
		svcctx.addOperationPredicate("url", "source-node-bind");
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.require("app_key", "app_secret")
			.validate(true);
		
		String appKey = Filters.filterString(params, "app_key");
		String appSecret = Filters.filterString(params, "app_secret");
					
		ActionResult presult = NodeApiAgent.instance().sendGlobalGet(WebUtils.getOpenApiUri("ping"), null);
		Map<String, String> map = JsonUtils.toMap((String)presult.getData(), String.class);
		String trace = map.get("trace");
		String token = map.get("token");
		
		AuthenData authenData = new AuthenData ();
		authenData.setGrantType(CoreConstants.GRANT_NODE_CRED);
		authenData.setPrincipal(appKey);
		authenData.setCredential(appSecret);
		String hmac = CryptoUtils.hmacCrypt(token, authenData.getCredential());
		// encrypt the password
		authenData.setCredential(hmac + GeneralConstants.NAMES_SEPARATOR + trace);
		String json = JsonUtils.toJson(authenData.getDataMap());
		
		ActionResult gresult = NodeApiAgent.instance().sendGlobalPost( WebUtils.getOpenApiUri("source-node-bind"), json);
		
		if(gresult.isSuccess()) {
			if(LOGGER.isDebugEnabled()) {
				LOGGER.debug("result : {}", gresult.getData());
			}
			Map<String, Object> data = JsonUtils.toMap((String)gresult.getData(), Object.class);
						
			SourceInfo source = new SourceInfo();
			source.setAbbr(Filters.filterString(data, "abbr"));
			
			source.setDescription(Filters.filterString(data, "description"));
			source.setEmail(Filters.filterString(data, "email"));
			source.setEntityGid(Filters.filterString(data, "entity_gid"));
			source.setEntityName(Filters.filterString(data, "entity_name"));
			source.setNodeGid(Filters.filterString(data, "node_gid"));
			source.setShortName(Filters.filterString(data, "short_name"));
			source.setSourceName(Filters.filterString(data, "source_name"));
			
			source.setState(Sources.State.ACTIVE.name());
			// update source info 
			sourceService.bindSource(svcctx, source, appKey, appSecret);
			
			SysOptionInfo sysOpt = systemService.getOption("personal.cabinet.quota");
			Integer capacity = NumberUtils.toInt(sysOpt.getOptValue());
			
			KeyValuePair<UserInfo, String> user = buildAdminUser();
			user.getKey().setProperty("capatity", capacity);
			// re-create an admin user
			userService.removeUser(svcctx, null, user.getKey().getUsername());
			userService.newUser(svcctx, user.getKey(), user.getValue());
			
			KeyValuePair<UserInfo, String> sync = buildSyncUser();
			user.getKey().setProperty("capatity", capacity);
			// re-create an admin user
			userService.removeUser(svcctx, null, sync.getKey().getUsername());
			userService.newUser(svcctx, sync.getKey(), sync.getValue());
			
			// reset globalApi with new key&secret
			NodeClient client = NodeApiAgent.instance().getNodeClient();
			AuthenData authen = client.getNodeAccess(GlobalName).getAuthenData();
			authen.setPrincipal(appKey);
			authen.setCredential(appSecret);
			
		}
		
		this.sendResult(exchange, gresult);
	}
	
	/**
	 * Build the administrator information 
	 **/
	private KeyValuePair<UserInfo, String> buildAdminUser() throws ServiceException{
		
		UserInfo uinfo = new UserInfo();
		uinfo.setInfoId(GroupUsers.ADMIN_UID);
		uinfo.setUsername("admin");
		uinfo.setFullName("Administrator");
		uinfo.setNickname("admin");
		uinfo.setLanguage("zh_CN");
		uinfo.setEmail("admin@gpress.com");
		uinfo.setSourceId(GeneralConstants.LOCAL_SOURCE);
		uinfo.setIdCard("");
		uinfo.setPhone("");
		uinfo.setMobile("");
		uinfo.setCategory(GroupUsers.UserCategory.SYSTEM.name());
		
		uinfo.setState(GroupUsers.UserState.ACTIVE.name());
		uinfo.setClassification(GroupUsers.Classification.TOP_SECRET.name());
		uinfo.setBiography("the system administrator");
		uinfo.setTimezone("GMT+08:00");
		
		return KeyValuePair.newPair(uinfo, "gpress");
	}
	
	private KeyValuePair<UserInfo, String> buildSyncUser() throws ServiceException{
		
		UserInfo uinfo = new UserInfo();
		uinfo.setUsername("sync");
		uinfo.setFullName("Sync User");
		uinfo.setNickname("sync");
		uinfo.setLanguage("zh_CN");
		uinfo.setEmail("sync@gpress.com");
		uinfo.setSourceId(GeneralConstants.LOCAL_SOURCE);
		uinfo.setIdCard("");
		uinfo.setPhone("");
		uinfo.setMobile("");
		uinfo.setCategory(GroupUsers.UserCategory.SYSTEM.name());
	
		uinfo.setState(GroupUsers.UserState.ACTIVE.name());
		uinfo.setClassification(GroupUsers.Classification.TOP_SECRET.name());
		uinfo.setBiography("the system sync app internal user");
		uinfo.setTimezone("GMT+08:00");
		
		return KeyValuePair.newPair(uinfo, "1");
	}
	
}
