package com.gp.dao.info;

import com.gp.info.TraceableInfo;
/**-------------------RESERVE SEPARATOR LINE---------------------*/

public class RoleAcsInfo extends TraceableInfo {

	private static final long serialVersionUID = 1L;

	private Long roleId;
	private Long dataId;
	private String dataRule;
	private String applyMode;
	
	public Long getRoleId() {
		return this.roleId;
	}
	public void setRoleId(Long roleId) {
		this.roleId = roleId;
	}
	
	public Long getDataId() {
		return this.dataId;
	}
	public void setDataId(Long dataId) {
		this.dataId = dataId;
	}
	
	public String getDataRule() {
		return this.dataRule;
	}
	public void setDataRule(String dataRule) {
		this.dataRule = dataRule;
	}
	
	public String getApplyMode() {
		return this.applyMode;
	}
	public void setApplyMode(String applyMode) {
		this.applyMode = applyMode;
	}
	
	
	@Override
	public String toString(){
		return "RoleAcsInfo ["
		+ "roleId=" + roleId + ", "
		+ "dataId=" + dataId + ", "
		+ "dataRule=" + dataRule + ", "
		+ "applyMode=" + applyMode + ", "
		+ "]";
	}
/**-------------------RESERVE SEPARATOR LINE---------------------*/

}