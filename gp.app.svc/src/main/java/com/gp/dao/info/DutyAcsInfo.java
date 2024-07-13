package com.gp.dao.info;

import com.gp.info.TraceableInfo;

/**-------------------RESERVE SEPARATOR LINE---------------------*/

public class DutyAcsInfo extends TraceableInfo {

	private static final long serialVersionUID = 1L;

	private Long dutyId;
	private Long sysId;
	private Long dataId;
	private String roleIds;
	private Boolean resideDept;
	private Boolean directDept;
	private Boolean anySubDept;
	private Boolean directSubord;
	private Boolean anySubord;
	
	public Long getDutyId() {
		return this.dutyId;
	}
	public void setDutyId(Long dutyId) {
		this.dutyId = dutyId;
	}
	
	public Long getSysId() {
		return this.sysId;
	}
	public void setSysId(Long sysId) {
		this.sysId = sysId;
	}
	
	public Long getDataId() {
		return this.dataId;
	}
	public void setDataId(Long dataId) {
		this.dataId = dataId;
	}
	
	public String getRoleIds() {
		return this.roleIds;
	}
	public void setRoleIds(String roleIds) {
		this.roleIds = roleIds;
	}
	
	public Boolean getResideDept() {
		return this.resideDept;
	}
	public void setResideDept(Boolean resideDept) {
		this.resideDept = resideDept;
	}
	
	public Boolean getDirectDept() {
		return this.directDept;
	}
	public void setDirectDept(Boolean directDept) {
		this.directDept = directDept;
	}
	
	public Boolean getAnySubDept() {
		return this.anySubDept;
	}
	public void setAnySubDept(Boolean anySubDept) {
		this.anySubDept = anySubDept;
	}
	
	public Boolean getDirectSubord() {
		return this.directSubord;
	}
	public void setDirectSubord(Boolean directSubord) {
		this.directSubord = directSubord;
	}
	
	public Boolean getAnySubord() {
		return this.anySubord;
	}
	public void setAnySubord(Boolean anySubord) {
		this.anySubord = anySubord;
	}
	
	
	@Override
	public String toString(){
		return "DutyAcsInfo ["
		+ "dutyId=" + dutyId + ", "
		+ "sysId=" + sysId + ", "
		+ "dataId=" + dataId + ", "
		+ "roleIds=" + roleIds + ", "
		+ "resideDept=" + resideDept + ", "
		+ "directDept=" + directDept + ", "
		+ "anySubDept=" + anySubDept + ", "
		+ "directSubord=" + directSubord + ", "
		+ "anySubord=" + anySubord + ", "
		+ "]";
	}
/**-------------------RESERVE SEPARATOR LINE---------------------*/

}