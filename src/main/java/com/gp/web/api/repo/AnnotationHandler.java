/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.repo;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gp.exception.BaseException;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;

import io.undertow.server.HttpServerExchange;

public class AnnotationHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(AnnotationHandler.class);
	
	public AnnotationHandler() {
		
	}

	@WebApi(path="anno-query", operation="ann:fnd")
	public void handleAnnosQuery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> paramap = this.getRequestBody(exchange);
		LOGGER.debug("params {}" , paramap);
	
	}
	
	@WebApi(path="anno-add", operation="ann:new")
	public void handleAnnoAdd(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> paramap = this.getRequestBody(exchange);
		LOGGER.debug("params {}" , paramap);
		
	}
}
