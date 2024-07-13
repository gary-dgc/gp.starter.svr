package com.gp.dao.info;

import com.gp.info.TraceableInfo;
/**-------------------RESERVE SEPARATOR LINE---------------------*/

public class DataPropInfo extends TraceableInfo {

	private static final long serialVersionUID = 1L;

	private Long dataId;
	private String propCode;
	private String propLabel;
	private String propType;
	private String defaultValue;
	private String enums;
	private String format;
	private Boolean required;
	
	public Long getDataId() {
		return this.dataId;
	}
	public void setDataId(Long dataId) {
		this.dataId = dataId;
	}
	
	public String getPropCode() {
		return this.propCode;
	}
	public void setPropCode(String propCode) {
		this.propCode = propCode;
	}
	
	public String getPropLabel() {
		return this.propLabel;
	}
	public void setPropLabel(String propLabel) {
		this.propLabel = propLabel;
	}
	
	public String getPropType() {
		return this.propType;
	}
	public void setPropType(String propType) {
		this.propType = propType;
	}
	
	public String getDefaultValue() {
		return this.defaultValue;
	}
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}
	
	public String getEnums() {
		return this.enums;
	}
	public void setEnums(String enums) {
		this.enums = enums;
	}
	
	public String getFormat() {
		return this.format;
	}
	public void setFormat(String format) {
		this.format = format;
	}
	
	public Boolean getRequired() {
		return this.required;
	}
	public void setRequired(Boolean required) {
		this.required = required;
	}
	
	
	@Override
	public String toString(){
		return "DataPropInfo ["
		+ "dataId=" + dataId + ", "
		+ "propCode=" + propCode + ", "
		+ "propLabel=" + propLabel + ", "
		+ "propType=" + propType + ", "
		+ "defaultValue=" + defaultValue + ", "
		+ "enums=" + enums + ", "
		+ "format=" + format + ", "
		+ "required=" + required + ", "
		+ "]";
	}
/**-------------------RESERVE SEPARATOR LINE---------------------*/

}