package com.gp.dao.info;

import com.gp.info.TraceableInfo;
/**-------------------RESERVE SEPARATOR LINE---------------------*/

public class DeptFlatInfo extends TraceableInfo {

	private static final long serialVersionUID = 1L;

	private Long deptPid;
	private Long deptLeafId;
	private Boolean hierLvl;
	
	public Long getDeptPid() {
		return this.deptPid;
	}
	public void setDeptPid(Long deptPid) {
		this.deptPid = deptPid;
	}
	
	public Long getDeptLeafId() {
		return this.deptLeafId;
	}
	public void setDeptLeafId(Long deptLeafId) {
		this.deptLeafId = deptLeafId;
	}
	
	public Boolean getHierLvl() {
		return this.hierLvl;
	}
	public void setHierLvl(Boolean hierLvl) {
		this.hierLvl = hierLvl;
	}
	
	
	@Override
	public String toString(){
		return "DeptFlatInfo ["
		+ "deptPid=" + deptPid + ", "
		+ "deptLeafId=" + deptLeafId + ", "
		+ "hierLvl=" + hierLvl + ", "
		+ "]";
	}
/**-------------------RESERVE SEPARATOR LINE---------------------*/

}