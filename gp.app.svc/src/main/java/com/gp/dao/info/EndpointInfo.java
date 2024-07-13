package com.gp.dao.info;

import com.gp.info.TraceableInfo;
/**-------------------RESERVE SEPARATOR LINE---------------------*/

public class EndpointInfo extends TraceableInfo {

	private static final long serialVersionUID = 1L;

	private String endpointName;
	private String module;
	private String type;
	private String endpointAbbr;
	private String accessPath;
	private String description;
	
	public String getEndpointName() {
		return this.endpointName;
	}
	public void setEndpointName(String endpointName) {
		this.endpointName = endpointName;
	}
	
	public String getModule() {
		return this.module;
	}
	public void setModule(String module) {
		this.module = module;
	}
	
	public String getType() {
		return this.type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	public String getEndpointAbbr() {
		return this.endpointAbbr;
	}
	public void setEndpointAbbr(String endpointAbbr) {
		this.endpointAbbr = endpointAbbr;
	}
	
	public String getAccessPath() {
		return this.accessPath;
	}
	public void setAccessPath(String accessPath) {
		this.accessPath = accessPath;
	}
	
	public String getDescription() {
		return this.description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	
	@Override
	public String toString(){
		return "EndpointInfo ["
		+ "endpointName=" + endpointName + ", "
		+ "module=" + module + ", "
		+ "type=" + type + ", "
		+ "endpointAbbr=" + endpointAbbr + ", "
		+ "accessPath=" + accessPath + ", "
		+ "description=" + description + ", "
		+ "]";
	}
/**-------------------RESERVE SEPARATOR LINE---------------------*/

}