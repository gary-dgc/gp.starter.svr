package com.gp.dao.info;

import com.gp.info.TraceableInfo;
/**-------------------RESERVE SEPARATOR LINE---------------------*/

public class DutyFlatInfo extends TraceableInfo {

	private static final long serialVersionUID = 1L;

	private Long dutyPid;
	private Long dutyLeafId;
	private Boolean hierLvl;
	
	public Long getDutyPid() {
		return this.dutyPid;
	}
	public void setDutyPid(Long dutyPid) {
		this.dutyPid = dutyPid;
	}
	
	public Long getDutyLeafId() {
		return this.dutyLeafId;
	}
	public void setDutyLeafId(Long dutyLeafId) {
		this.dutyLeafId = dutyLeafId;
	}
	
	public Boolean getHierLvl() {
		return this.hierLvl;
	}
	public void setHierLvl(Boolean hierLvl) {
		this.hierLvl = hierLvl;
	}
	
	
	@Override
	public String toString(){
		return "DutyFlatInfo ["
		+ "dutyPid=" + dutyPid + ", "
		+ "dutyLeafId=" + dutyLeafId + ", "
		+ "hierLvl=" + hierLvl + ", "
		+ "]";
	}
/**-------------------RESERVE SEPARATOR LINE---------------------*/

}