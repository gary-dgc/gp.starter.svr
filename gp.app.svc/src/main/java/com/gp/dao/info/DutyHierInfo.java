package com.gp.dao.info;

import com.gp.info.TraceableInfo;

/**-------------------RESERVE SEPARATOR LINE---------------------*/

public class DutyHierInfo extends TraceableInfo {

	private static final long serialVersionUID = 1L;

	private Long orgId;
	private String dutyEcd;
	private Integer headcount;
	private Long dutyPid;
	private String dutyName;
	private String dutyLvl;
	private String dutyCate;
	private String description;
	private String state;
	
	public Long getOrgId() {
		return this.orgId;
	}
	public void setOrgId(Long orgId) {
		this.orgId = orgId;
	}
	
	public String getDutyEcd() {
		return this.dutyEcd;
	}
	public void setDutyEcd(String dutyEcd) {
		this.dutyEcd = dutyEcd;
	}
	
	public Integer getHeadcount() {
		return this.headcount;
	}
	public void setHeadcount(Integer headcount) {
		this.headcount = headcount;
	}
	
	public Long getDutyPid() {
		return this.dutyPid;
	}
	public void setDutyPid(Long dutyPid) {
		this.dutyPid = dutyPid;
	}
	
	public String getDutyName() {
		return this.dutyName;
	}
	public void setDutyName(String dutyName) {
		this.dutyName = dutyName;
	}
	
	public String getDutyLvl() {
		return this.dutyLvl;
	}
	public void setDutyLvl(String dutyLvl) {
		this.dutyLvl = dutyLvl;
	}
	
	public String getDutyCate() {
		return this.dutyCate;
	}
	public void setDutyCate(String dutyCate) {
		this.dutyCate = dutyCate;
	}
	
	public String getDescription() {
		return this.description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getState() {
		return this.state;
	}
	public void setState(String state) {
		this.state = state;
	}
	
	
	@Override
	public String toString(){
		return "DutyHierInfo ["
		+ "orgId=" + orgId + ", "
		+ "dutyEcd=" + dutyEcd + ", "
		+ "headcount=" + headcount + ", "
		+ "dutyPid=" + dutyPid + ", "
		+ "dutyName=" + dutyName + ", "
		+ "dutyLvl=" + dutyLvl + ", "
		+ "dutyCate=" + dutyCate + ", "
		+ "description=" + description + ", "
		+ "state=" + state + ", "
		+ "]";
	}
/**-------------------RESERVE SEPARATOR LINE---------------------*/

}