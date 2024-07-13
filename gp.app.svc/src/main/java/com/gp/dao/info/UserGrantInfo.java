package com.gp.dao.info;

import com.gp.info.TraceableInfo;
/**-------------------RESERVE SEPARATOR LINE---------------------*/

public class UserGrantInfo extends TraceableInfo {

	private static final long serialVersionUID = 1L;

	private Long userId;
	private Long orgId;
	private Long deptId;
	private Long dutyId;
	
	public Long getUserId() {
		return this.userId;
	}
	public void setUserId(Long userId) {
		this.userId = userId;
	}
	
	public Long getOrgId() {
		return this.orgId;
	}
	public void setOrgId(Long orgId) {
		this.orgId = orgId;
	}
	
	public Long getDeptId() {
		return this.deptId;
	}
	public void setDeptId(Long deptId) {
		this.deptId = deptId;
	}
	
	public Long getDutyId() {
		return this.dutyId;
	}
	public void setDutyId(Long dutyId) {
		this.dutyId = dutyId;
	}
	
	
	@Override
	public String toString(){
		return "UserGrantInfo ["
		+ "userId=" + userId + ", "
		+ "orgId=" + orgId + ", "
		+ "deptId=" + deptId + ", "
		+ "dutyId=" + dutyId + ", "
		+ "]";
	}
/**-------------------RESERVE SEPARATOR LINE---------------------*/

}