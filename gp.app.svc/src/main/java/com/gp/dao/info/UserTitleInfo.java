package com.gp.dao.info;

import com.gp.info.TraceableInfo;
/**-------------------RESERVE SEPARATOR LINE---------------------*/

public class UserTitleInfo extends TraceableInfo {

	private static final long serialVersionUID = 1L;

	private Long userId;
	private Boolean isPrimary;
	private String title;
	private String department;
	
	public Long getUserId() {
		return this.userId;
	}
	public void setUserId(Long userId) {
		this.userId = userId;
	}
	
	public Boolean getIsPrimary() {
		return this.isPrimary;
	}
	public void setIsPrimary(Boolean isPrimary) {
		this.isPrimary = isPrimary;
	}
	
	public String getTitle() {
		return this.title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getDepartment() {
		return this.department;
	}
	public void setDepartment(String department) {
		this.department = department;
	}
	
	
	@Override
	public String toString(){
		return "UserTitleInfo ["
		+ "userId=" + userId + ", "
		+ "isPrimary=" + isPrimary + ", "
		+ "title=" + title + ", "
		+ "department=" + department + ", "
		+ "]";
	}
/**-------------------RESERVE SEPARATOR LINE---------------------*/

}