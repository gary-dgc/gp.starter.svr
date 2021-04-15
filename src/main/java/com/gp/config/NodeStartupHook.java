/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gp.core.CoreEngine;
import com.networknt.server.StartupHookProvider;

public class NodeStartupHook implements StartupHookProvider{

	static Logger LOGGER = LoggerFactory.getLogger(NodeStartupHook.class);
	
	public static final String DATA_SOURCE = "datasource";
	
	@Override
	public void onStartup() {

		CoreEngine.startup();
	}
}
