/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.common;

public enum MasterIdKey implements Identifier{
	
	ADDRESS("addr_id"),

	ORG_HIER("org_id"),
	DEPT_HIER("dept_id"),
	DEPT_FLAT("flat_id"),
	DATA_MODEL("data_id"),

	USER_GRANT("rel_id"),
	USER_LOGIN("rel_id"),
	USER_ROLE("rel_id"),
	USER_TITLE("rel_id"),

	GROUP("group_id"),
	GROUP_USER("rel_id"),

	ENDPOINT("endpoint_id"),

	DUTY_HIER("duty_id"),
	DUTY_FLAT("flat_id"),
	DUTY_ACS("access_id"),

	STORAGE("storage_id"),

	SYS_SUB("sys_id"),

	SYNC_MQ_IN("msg_id"),
	SYNC_MQ_OUT("msg_id"),
	SYNC_TRACE("trace_id"),

	ROLE("role_id"),
	DATA_PROP("data_id"),
	ROLE_ACS("role_id"),
	ROLE_PERM("perm_id");

	private final String idColumn;
	private final boolean trace;

	private MasterIdKey(String idColumn){
		this.idColumn = idColumn;
		this.trace = false;
	}

	private MasterIdKey(String idColumn, boolean trace){
		this.idColumn = idColumn;
		this.trace = trace;
	}

	@Override
	public String schema() {

		return TBL_PREFIX + name().toLowerCase();
	}

	@Override
	public String idColumn() {

		return this.idColumn;
	}

	@Override
	public int order() {

		return Integer.MIN_VALUE + 99999;
	}

	@Override
	public String traceColumn() {

		switch(this) {
			// XXX:
			//   ignore other cases

			default :
				if(trace) {
					return Identifier.super.traceColumn();
				}

				return "";
		}

	}
	
	/**
	 * Here Register self Enums into the IdKeys 
	 **/
	static {
		IdKeys.addIdentifier(MasterIdKey.values());
	}
}
