/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.dao.info;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.Strings;
import com.gp.info.TraceableInfo;

import java.util.HashMap;
import java.util.Map;

public class DictionaryInfo extends TraceableInfo{
	
	private static final long serialVersionUID = 1L;

	private String dictGroup;

	private String dictKey;

	private String dictValue;

	private Map<String, String> labelMap = new HashMap<String, String>();
	
	public String getDictGroup() {
		return dictGroup;
	}

	public void setDictGroup(String dictGroup) {
		this.dictGroup = dictGroup;
	}

	public String getDictKey() {
		return dictKey;
	}

	public void setDictKey(String dictKey) {
		this.dictKey = dictKey;
	}

	public String getDictValue() {
		return dictValue;
	}

	public void setDictValue(String dictValue) {
		this.dictValue = dictValue;
	}

	public Map<String, String> getLabelMap() {
		return labelMap;
	}

	public void setLabelMap(Map<String, String> labelMap) {
		this.labelMap = labelMap;
	}
	
	@JsonIgnore
	public String getLabel(String col){
		return labelMap.get(Strings.nullToEmpty(col).toLowerCase());
	}
	
	@JsonIgnore
	public void putLabel(String col, String label){
		labelMap.put(Strings.nullToEmpty(col).toLowerCase(), label);
	}
}
