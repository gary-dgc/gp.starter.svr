/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api;

import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.gp.bind.BindScanner;
import com.gp.cache.CacheManager;
import com.gp.cache.ICache;
import com.gp.common.Binaries.BinaryMode;
import com.gp.common.Caches;
import com.gp.common.Filters;
import com.gp.common.InfoId;
import com.gp.exception.BaseException;
import com.gp.info.Principal;

import com.gp.svc.CommonHelper;
import com.gp.validate.ArgsValidator;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ServiceApiHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(ServiceApiHandler.class);
		
	private ICache extraCache;
	
	private CommonHelper commonService;
	
	public ServiceApiHandler() {
		
		commonService = BindScanner.instance().getBean(CommonHelper.class);
		extraCache = CacheManager.instance().getCache(Caches.EXTRA_CACHE);

	}

	@WebApi(path="interim-token")
	public void handleGetSymToken(HttpServerExchange exchange) throws BaseException {
			
		ActionResult result = null;
		
		Map<String, Object> paraMap = this.getRequestBody(exchange);
		ArgsValidator argsValid = ArgsValidator.newValidator(paraMap);
		String pattern = Filters.filterString(paraMap, "mode");
		Principal principal = this.getPrincipal(exchange);
		
		InfoId uid = principal.getUserId();
		String token = null;
		Optional<BinaryMode> optional = Enums.getIfPresent(BinaryMode.class, Strings.nullToEmpty(pattern).toUpperCase());
		switch (optional.get()) {
		
			case FILE:
				argsValid.require("cabinet_id", "folder_pid")
						.validate(true);
				// {cabinet id}.{folder id}.{user id}.{user name}
				String cab = Filters.filterString(paraMap, "cabinet_id");
				String fid = Filters.filterString(paraMap, "folder_pid");
				token = ServiceApiHelper.instance().getInterimToken(optional.get(), cab, fid, uid.getId().toString(), principal.getUsername());
				break;
				
			case BINARY:
			case ATTACH:
				// {cabinet id}.{user id}.{user name}
				String cab1 = Filters.filterString(paraMap, "cabinet_id");
				token = ServiceApiHelper.instance().getInterimToken(optional.get(), cab1, uid.getId().toString(), principal.getUsername());
				break;

			case AVATAR:
			case IMAGE:
				// {cabinet id}.{user id}.{user name}
				Long cabId = commonService.column(principal.getUserId(), "cabinet_id", Long.class);
				token = ServiceApiHelper.instance().getInterimToken(optional.get(), cabId.toString(), uid.getId().toString(), principal.getUsername());
				break;
			
			default:
				break;
		}
		
		if(!Strings.isNullOrEmpty(token)) {
			if(LOGGER.isDebugEnabled()) {
				LOGGER.debug("Generate [{}] new toke: {}", pattern, token);
			}

			result = ActionResult.success(getMessage(exchange, "mesg.gen.symtoken"));
			Map<String, String> data = Maps.newHashMap();
			data.put("token", token);
			result.setData(data);
		}else {
			result = ActionResult.failure(getMessage(exchange, "excp.gen.symtoken"));
		}
		
		this.sendResult(exchange, result);
	}

}
