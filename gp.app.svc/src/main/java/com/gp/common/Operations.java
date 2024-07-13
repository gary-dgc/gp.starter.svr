/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.common;

/**
 * Operation definition which indicate the command
 * 
 * @author gary diao
 * @date 2017-10-11
 * @version 0.1
 * 
 **/
public enum Operations implements Instruct{
	
	/** security operation */
	ACNT_NEW("acnt","new"),
	ACNT_AUTHOR("acnt","author"),
	ACNT_UPD("acnt","upd"),
	ACNT_INF("acnt","inf"),
	ACNT_FND("acnt","fnd"),
	ACNT_RMV("acnt","rmv"),
	ACNT_CHG_PWD("acnt","chg-pwd"),

	/** system options */
	OPT_FND("opt","fnd"),
	OPT_UPD("opt","upd"),
	OPT_FND_GRP("opt","fnd-grp"),

	/** organization */
	ORG_FND("org","fnd"),
	ORG_INF("org","inf"),
	ORG_NEW("org","new"),
	ORG_UPD("org","upd"),
	ORG_RMV("org","rmv"),
	ORG_ADD_MBR("org","add-mbr"),
	ORG_RMV_MBR("org","rmv-mbr"),
	ORG_FND_MBR("org","fnd-mbr"),

	/** source */
	SRC_FND("src","fnd"),
	SRC_INF("src","inf"),
	SRC_UPD("org","upd"),

	/** group */
	GRP_NEW("grp","new"),
	GRP_UPD("grp","upd"),
	GRP_RMV("grp","rmv"),
	GRP_INF("grp","inf"),
	GRP_FND("grp","fnd"),
	GRP_FND_MBR("grp","fnd-mbr"),
	GRP_ADD_MBR("grp","add-mbr"),
	GRP_RMV_MBR("grp","rmv-mbr"),
	
	/** dictionary */
	DICT_UPD("dict","upd"),
	DICT_FND("dict","fnd"),
	DICT_GRP_FND("dict","grp-fnd"),

	/** activity log */
	OPR_FND("opr","fnd"), // find operation log

	/** storage */
	STG_FND("stg","fnd"),
	STG_NEW("stg","new"),
	STG_UPD("stg","upd"),
	STG_RMV("stg","rmv"),
	STG_INF("stg","inf"),

	/** jwt token */
	TKN_FND("tkn","fnd"),
	TKN_NEW("tkn","new"),
	TKN_ISS("tkn","iss"),
	TKN_SWP("tkn","swp"),
	
	/** role permission */
	ROL_FND("rol","fnd"),
	ROL_NEW("rol","new"),
	ROL_UPD("rol","upd"),
	ROL_GRT_PEM("rol","grt-pem"),
	ROL_FND_MBR("rol","fnd-mbr"),
	ROL_ADD_MBR("rol","add-mbr"),
	ROL_RMV_MBR("rol","rmv-mbr"),

	/** Endpoint */
	EDP_FND("edp","fnd"),
	GBL_IVK("gbl","ivk"),
	
	/** Address */
	ADR_FND("adr","fnd"),
	ADR_NEW("adr","new"),
	ADR_RMV("adr","rmv"),
	ADR_PRM("adr","upd");
	
	private String schema;
	private String directive;
	
	private Operations(String schema, String directive) {
		this.schema = schema;
		this.directive = directive;
	}
	
	@Override
	public String schema() {
		
		return schema;
	}

	@Override
	public String directive(){
		
		return directive;
	}
	
	@Override
	public String toString() {
		return this.schema + GeneralConsts.KEYS_SEPARATOR + this.directive;
	}

	// cache all the operations
	static {
		Instructs.putInstruct(Operations.values());
	}
}
