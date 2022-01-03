/*******************************************************************************
.................................................................................................................................................................................................................................................................................................................................................................................................................................................. * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.Instructs;
import com.gp.svc.dev.DebugService;
import com.gp.util.AesCryptor;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.util.WebUtils;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Methods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * SupportHandler provides the apis for supporting.
 * 
 * @author gdiao
 * @since 0.3.4
 **/
public class SupportHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(SupportHandler.class);
	
	DebugService debugService = null;
	
	public SupportHandler() {
		
		debugService = BindScanner.instance().getBean(DebugService.class);
		
		this.setPathMapping(WebUtils.getOpenApiUri("encrypt"), Methods.GET, 
				ApiMeta.newMeta(this::handleEncryptGet, Instructs.UN_KWN));
		
		this.setPathMapping(WebUtils.getOpenApiUri("encrypt"), Methods.POST, 
				ApiMeta.newMeta(this::handleEncryptPost, Instructs.UN_KWN));
		
		this.setPathMapping(WebUtils.getOpenApiUri("decrypt"), Methods.POST, 
				ApiMeta.newMeta(this::handleDecryptPost, Instructs.UN_KWN));
		
		this.setPathMapping(WebUtils.getAuthApiUri("hash-pwd"), Methods.POST, 
				ApiMeta.newMeta(this::handleHashPassword, Instructs.UN_KWN));
	}

	/**
	 * Encrypt the data send via GET method 
	 **/
	public void handleEncryptGet(HttpServerExchange exchange) throws Exception {
		
		LOGGER.debug("Test sending post sync origns");

		Map<String, String> params = this.getRequestQuery(exchange);
		
		String value = params.get("value");
		
		String encValue = (new AesCryptor()).encrypt(value);
		
		Map<String, String> data = Maps.newHashMap();
		data.put("origin_value", value);
		data.put("enc_value", encValue);
		
		ActionResult result = ActionResult.success("success encrypt origin value");
		result.setData(data);
		
		this.sendResult(exchange, result);
	}

	/**
	 * Encrypt the data send via POST method 
	 **/
	public void handleEncryptPost(HttpServerExchange exchange) throws Exception {
		
		LOGGER.debug("Test sending post sync origns");
		Map<String, Object> params = this.getRequestBody(exchange);
		
		String value = Filters.filterString(params, "value");
		
		String encValue = (new AesCryptor()).encrypt(value);
		
		Map<String, String> data = Maps.newHashMap();
		data.put("origin_value", value);
		data.put("enc_value", encValue);
		
		ActionResult result = ActionResult.success("success encrypt origin value");
		result.setData(data);
		
		this.sendResult(exchange, result);
	}
	
	/**
	 * Decode the encoded data send via POST method 
	 **/
	public void handleDecryptPost(HttpServerExchange exchange) throws Exception {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		
		String value = Filters.filterString(params, "value");
		
		String decValue = (new AesCryptor()).decrypt(value);
		
		Map<String, String> data = Maps.newHashMap();
		data.put("origin_value", value);
		data.put("dec_value", decValue);
		
		ActionResult result = ActionResult.success("success decrypt origin value");
		result.setData(data);
		
		this.sendResult(exchange, result);
	}
	
	/**
	 * Hash the password of user 
	 **/
	public void handleHashPassword(HttpServerExchange exchange) throws Exception {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		
		String username = Filters.filterString(params, "username");
		String value = Filters.filterString(params, "value");
		
		String decValue = debugService.encryptPassword(username, value);
		
		Map<String, String> data = Maps.newHashMap();
		data.put("origin_value", value);
		data.put("hash_value", decValue);
		
		ActionResult result = Strings.isNullOrEmpty(decValue) ? ActionResult.failure("fail to hash password") 
				: ActionResult.success("success hash origin value");
		result.setData(data);
		
		this.sendResult(exchange, result);
	}
}
