/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.common;

public enum Measures implements FlatColLocator {
	
	WG_MEAS_FILE(1),
	WG_MEAS_POST(2),
	WG_MEAS_MEMBER(3),
	WG_MEAS_EXT_MBR(4),
	WG_MEAS_SUB_GRP(5),
	NODE_MEAS_MEMBER(1),
	NODE_MEAS_GROUP(2),
	NODE_MEAS_TOPIC(3),
	NODE_MEAS_FILE(4),
	NODE_MEAS_POINT(5),
	NODE_MEAS_EXPERT(6),
	;
	// measure type of work group summary
	public static String MEAS_TYPE_WG_SUM = "wg_summary";
	public static String MEAS_TYPE_NODE_SUM = "node_summary";
	
	private static String colPrefix = "measure_data_";
	
	private int colIndex;
	
	private Measures(int colIndex){
		this.colIndex = colIndex;
	}
	
	public Integer getColIndex() {
		
		return colIndex;
	}

	public String getColPrefix() {
		
		return colPrefix;
	}

	@Override
	public String getColumn() {
		
		return colPrefix + colIndex;
	}
	
}
