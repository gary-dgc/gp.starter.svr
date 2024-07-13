package com.gp.dao.info;

import com.gp.info.TraceableInfo;
/**-------------------RESERVE SEPARATOR LINE---------------------*/

public class GroupUserInfo extends TraceableInfo {

	private static final long serialVersionUID = 1L;

	private Long groupId;
	private Long memberUid;
	private Long delegateUid;
	private String classification;
	private String type;
	private String role;
	private String tag;
	
	public Long getGroupId() {
		return this.groupId;
	}
	public void setGroupId(Long groupId) {
		this.groupId = groupId;
	}
	
	public Long getMemberUid() {
		return this.memberUid;
	}
	public void setMemberUid(Long memberUid) {
		this.memberUid = memberUid;
	}
	
	public Long getDelegateUid() {
		return this.delegateUid;
	}
	public void setDelegateUid(Long delegateUid) {
		this.delegateUid = delegateUid;
	}
	
	public String getClassification() {
		return this.classification;
	}
	public void setClassification(String classification) {
		this.classification = classification;
	}
	
	public String getType() {
		return this.type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	public String getRole() {
		return this.role;
	}
	public void setRole(String role) {
		this.role = role;
	}
	
	public String getTag() {
		return this.tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}
	
	
	@Override
	public String toString(){
		return "GroupUserInfo ["
		+ "groupId=" + groupId + ", "
		+ "memberUid=" + memberUid + ", "
		+ "delegateUid=" + delegateUid + ", "
		+ "classification=" + classification + ", "
		+ "type=" + type + ", "
		+ "role=" + role + ", "
		+ "tag=" + tag + ", "
		+ "]";
	}
/**-------------------RESERVE SEPARATOR LINE---------------------*/

}