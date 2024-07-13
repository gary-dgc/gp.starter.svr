/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.common;

import com.gp.info.BaseIdKey;

public class Sources {

	public static final InfoId LOCAL_INST_ID = IdKeys.getInfoId(BaseIdKey.SOURCE, GeneralConsts.LOCAL_SOURCE);
	
	/**
	 * The source states 
	 **/
	public static enum State{
		ISOLATED,
		DEACTIVE,
		ACTIVE,
		FROZEN,
	}

	/**
	 * The source type 
	 **/
	public static enum Type{
		LOCAL,
		EXTERNAL
	}
	
	/**
	 * The destination of out message
	 **/
	public static enum Scope{
		GLOBAL,      // global-wise synchronize
		CENTER,      // center-wise synchronize
		NODE,        // node-wise synchronize
	}
}
