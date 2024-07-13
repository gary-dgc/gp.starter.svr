package com.gp.dao.info;

import com.gp.info.TraceableInfo;
/**-------------------RESERVE SEPARATOR LINE---------------------*/

public class DeptHierInfo extends TraceableInfo {

	private static final long serialVersionUID = 1L;

	private Long orgId;
	private Long deptPid;
	private String deptName;
	private Long mbrGroupId;
	private String description;
	
	public Long getOrgId() {
		return this.orgId;
	}
	public void setOrgId(Long orgId) {
		this.orgId = orgId;
	}
	
	public Long getDeptPid() {
		return this.deptPid;
	}
	public void setDeptPid(Long deptPid) {
		this.deptPid = deptPid;
	}
	
	public String getDeptName() {
		return this.deptName;
	}
	public void setDeptName(String deptName) {
		this.deptName = deptName;
	}
	
	public Long getMbrGroupId() {
		return this.mbrGroupId;
	}
	public void setMbrGroupId(Long mbrGroupId) {
		this.mbrGroupId = mbrGroupId;
	}
	
	public String getDescription() {
		return this.description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	
	@Override
	public String toString(){
		return "DeptHierInfo ["
		+ "orgId=" + orgId + ", "
		+ "deptPid=" + deptPid + ", "
		+ "deptName=" + deptName + ", "
		+ "mbrGroupId=" + mbrGroupId + ", "
		+ "description=" + description + ", "
		+ "]";
	}
/**-------------------RESERVE SEPARATOR LINE---------------------*/

}