package com.gp.dao.info;

import com.gp.info.TraceableInfo;

/**-------------------RESERVE SEPARATOR LINE---------------------*/

public class RoleInfo extends TraceableInfo {

	private static final long serialVersionUID = 1L;

	private Long sysId;
	private String roleName;
	private String roleAbbr;
	private Boolean reserved;
	private String description;
	private String defaultCase;
	
	public Long getSysId() {
		return this.sysId;
	}
	public void setSysId(Long sysId) {
		this.sysId = sysId;
	}
	
	public String getRoleName() {
		return this.roleName;
	}
	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}
	
	public String getRoleAbbr() {
		return this.roleAbbr;
	}
	public void setRoleAbbr(String roleAbbr) {
		this.roleAbbr = roleAbbr;
	}
	
	public Boolean getReserved() {
		return this.reserved;
	}
	public void setReserved(Boolean reserved) {
		this.reserved = reserved;
	}
	
	public String getDescription() {
		return this.description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getDefaultCase() {
		return this.defaultCase;
	}
	public void setDefaultCase(String defaultCase) {
		this.defaultCase = defaultCase;
	}
	
	
	@Override
	public String toString(){
		return "RoleInfo ["
		+ "sysId=" + sysId + ", "
		+ "roleName=" + roleName + ", "
		+ "roleAbbr=" + roleAbbr + ", "
		+ "reserved=" + reserved + ", "
		+ "description=" + description + ", "
		+ "defaultCase=" + defaultCase + ", "
		+ "]";
	}
/**-------------------RESERVE SEPARATOR LINE---------------------*/

}