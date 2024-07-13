package com.gp.dao.info;

import com.gp.info.TraceableInfo;
/**-------------------RESERVE SEPARATOR LINE---------------------*/

public class UserRoleInfo extends TraceableInfo {

	private static final long serialVersionUID = 1L;

	private Long userId;
	private Long roleId;
	
	public Long getUserId() {
		return this.userId;
	}
	public void setUserId(Long userId) {
		this.userId = userId;
	}
	
	public Long getRoleId() {
		return this.roleId;
	}
	public void setRoleId(Long roleId) {
		this.roleId = roleId;
	}
	
	
	@Override
	public String toString(){
		return "UserRoleInfo ["
		+ "userId=" + userId + ", "
		+ "roleId=" + roleId + ", "
		+ "]";
	}
/**-------------------RESERVE SEPARATOR LINE---------------------*/

}