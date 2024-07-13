package com.gp.dao.info;

import com.gp.info.TraceableInfo;
/**-------------------RESERVE SEPARATOR LINE---------------------*/

public class SysOptionInfo extends TraceableInfo {

	private static final long serialVersionUID = 1L;

	private String optGroup;
	private String optKey;
	private String optValue;
	private String description;
	
	public String getOptGroup() {
		return this.optGroup;
	}
	public void setOptGroup(String optGroup) {
		this.optGroup = optGroup;
	}
	
	public String getOptKey() {
		return this.optKey;
	}
	public void setOptKey(String optKey) {
		this.optKey = optKey;
	}
	
	public String getOptValue() {
		return this.optValue;
	}
	public void setOptValue(String optValue) {
		this.optValue = optValue;
	}
	
	public String getDescription() {
		return this.description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	
	@Override
	public String toString(){
		return "SysOptionInfo ["
		+ "optGroup=" + optGroup + ", "
		+ "optKey=" + optKey + ", "
		+ "optValue=" + optValue + ", "
		+ "description=" + description + ", "
		+ "]";
	}
/**-------------------RESERVE SEPARATOR LINE---------------------*/

}