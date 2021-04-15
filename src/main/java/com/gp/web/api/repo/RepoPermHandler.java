/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.repo;

import com.gp.bind.BindScanner;
import com.gp.exception.BaseException;
import com.gp.svc.cab.AclService;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;

import io.undertow.server.HttpServerExchange;

public class RepoPermHandler extends BaseApiSupport{

	private AclService aclService;
	
	public RepoPermHandler() {
		aclService = BindScanner.instance().getBean(AclService.class);
		
	}

	@WebApi(path="cabinet-acl-query")
	public void handleAclQuery(HttpServerExchange exchange) throws BaseException{
		
		
	}
}
