package com.gp.dao.info;

import com.gp.info.TraceableInfo;
/**-------------------RESERVE SEPARATOR LINE---------------------*/

public class GroupInfo extends TraceableInfo {

	private static final long serialVersionUID = 1L;

	private Long manageId;
	private String groupType;
	private String groupName;
	private String description;
	
	public Long getManageId() {
		return this.manageId;
	}
	public void setManageId(Long manageId) {
		this.manageId = manageId;
	}
	
	public String getGroupType() {
		return this.groupType;
	}
	public void setGroupType(String groupType) {
		this.groupType = groupType;
	}
	
	public String getGroupName() {
		return this.groupName;
	}
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
	
	public String getDescription() {
		return this.description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	
	@Override
	public String toString(){
		return "GroupInfo ["
		+ "manageId=" + manageId + ", "
		+ "groupType=" + groupType + ", "
		+ "groupName=" + groupName + ", "
		+ "description=" + description + ", "
		+ "]";
	}
/**-------------------RESERVE SEPARATOR LINE---------------------*/

}