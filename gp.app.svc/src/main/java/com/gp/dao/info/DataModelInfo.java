package com.gp.dao.info;

import com.gp.info.TraceableInfo;

/**-------------------RESERVE SEPARATOR LINE---------------------*/

public class DataModelInfo extends TraceableInfo {

	private static final long serialVersionUID = 1L;

	private Long sysId;
	private String dataName;
	private String description;
	
	public Long getSysId() {
		return this.sysId;
	}
	public void setSysId(Long sysId) {
		this.sysId = sysId;
	}
	
	public String getDataName() {
		return this.dataName;
	}
	public void setDataName(String dataName) {
		this.dataName = dataName;
	}
	
	public String getDescription() {
		return this.description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	
	@Override
	public String toString(){
		return "DataModelInfo ["
		+ "sysId=" + sysId + ", "
		+ "dataName=" + dataName + ", "
		+ "description=" + description + ", "
		+ "]";
	}
/**-------------------RESERVE SEPARATOR LINE---------------------*/

}