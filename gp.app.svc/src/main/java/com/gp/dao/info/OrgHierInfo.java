package com.gp.dao.info;

import com.gp.info.TraceableInfo;
/**-------------------RESERVE SEPARATOR LINE---------------------*/

public class OrgHierInfo extends TraceableInfo {

	private static final long serialVersionUID = 1L;

	private Long orgPid;
	private String orgEcd;
	private String orgName;
	private String orgType;
	private String avatarUrl;
	private String email;
	private String description;
	private String fax;
	private String phone;
	private String liaison;
	private String taxNo;
	private String state;
	private String mobile;
	
	public Long getOrgPid() {
		return this.orgPid;
	}
	public void setOrgPid(Long orgPid) {
		this.orgPid = orgPid;
	}
	
	public String getOrgEcd() {
		return this.orgEcd;
	}
	public void setOrgEcd(String orgEcd) {
		this.orgEcd = orgEcd;
	}
	
	public String getOrgName() {
		return this.orgName;
	}
	public void setOrgName(String orgName) {
		this.orgName = orgName;
	}
	
	public String getOrgType() {
		return this.orgType;
	}
	public void setOrgType(String orgType) {
		this.orgType = orgType;
	}
	
	public String getAvatarUrl() {
		return this.avatarUrl;
	}
	public void setAvatarUrl(String avatarUrl) {
		this.avatarUrl = avatarUrl;
	}
	
	public String getEmail() {
		return this.email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	
	public String getDescription() {
		return this.description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getFax() {
		return this.fax;
	}
	public void setFax(String fax) {
		this.fax = fax;
	}
	
	public String getPhone() {
		return this.phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}
	
	public String getLiaison() {
		return this.liaison;
	}
	public void setLiaison(String liaison) {
		this.liaison = liaison;
	}
	
	public String getTaxNo() {
		return this.taxNo;
	}
	public void setTaxNo(String taxNo) {
		this.taxNo = taxNo;
	}
	
	public String getState() {
		return this.state;
	}
	public void setState(String state) {
		this.state = state;
	}
	
	public String getMobile() {
		return this.mobile;
	}
	public void setMobile(String mobile) {
		this.mobile = mobile;
	}
	
	
	@Override
	public String toString(){
		return "OrgHierInfo ["
		+ "orgPid=" + orgPid + ", "
		+ "orgEcd=" + orgEcd + ", "
		+ "orgName=" + orgName + ", "
		+ "orgType=" + orgType + ", "
		+ "avatarUrl=" + avatarUrl + ", "
		+ "email=" + email + ", "
		+ "description=" + description + ", "
		+ "fax=" + fax + ", "
		+ "phone=" + phone + ", "
		+ "liaison=" + liaison + ", "
		+ "taxNo=" + taxNo + ", "
		+ "state=" + state + ", "
		+ "mobile=" + mobile + ", "
		+ "]";
	}
/**-------------------RESERVE SEPARATOR LINE---------------------*/

}