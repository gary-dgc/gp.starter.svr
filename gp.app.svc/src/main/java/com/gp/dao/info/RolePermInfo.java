package com.gp.dao.info;

import com.gp.info.TraceableInfo;

/**-------------------RESERVE SEPARATOR LINE---------------------*/

public class RolePermInfo extends TraceableInfo {

	private static final long serialVersionUID = 1L;

	private Long roleId;
	private Long endpointId;
	private String accessPath;
	private Boolean authorized;
	
	public Long getRoleId() {
		return this.roleId;
	}
	public void setRoleId(Long roleId) {
		this.roleId = roleId;
	}
	
	public Long getEndpointId() {
		return this.endpointId;
	}
	public void setEndpointId(Long endpointId) {
		this.endpointId = endpointId;
	}
	
	public String getAccessPath() {
		return this.accessPath;
	}
	public void setAccessPath(String accessPath) {
		this.accessPath = accessPath;
	}
	
	public Boolean getAuthorized() {
		return this.authorized;
	}
	public void setAuthorized(Boolean authorized) {
		this.authorized = authorized;
	}
	
	
	@Override
	public String toString(){
		return "RolePermInfo ["
		+ "roleId=" + roleId + ", "
		+ "endpointId=" + endpointId + ", "
		+ "accessPath=" + accessPath + ", "
		+ "authorized=" + authorized + ", "
		+ "]";
	}
/**-------------------RESERVE SEPARATOR LINE---------------------*/

}