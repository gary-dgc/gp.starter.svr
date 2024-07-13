package com.gp.dao.info;

import com.gp.info.TraceableInfo;
import java.util.Date;
/**-------------------RESERVE SEPARATOR LINE---------------------*/

public class UserInfo extends TraceableInfo {

	private static final long serialVersionUID = 1L;

	private Long sourceId;
	private String username;
	private String userGid;
	private String traceCode;
	private String category;
	private String mobile;
	private String phone;
	private String fullName;
	private String nickname;
	private String email;
	private String idCard;
	private String state;
	private String cryptoKey;
	private String extraInfo;
	private String timezone;
	private String language;
	private Long cabinetId;
	private String classification;
	private Integer score;
	private String biography;
	private String supInfo;
	private String avatarUrl;
	private Integer retryTimes;
	private Date lastLogon;
	private String remark;
	private Date createTime;
	
	public Long getSourceId() {
		return this.sourceId;
	}
	public void setSourceId(Long sourceId) {
		this.sourceId = sourceId;
	}
	
	public String getUsername() {
		return this.username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getUserGid() {
		return this.userGid;
	}
	public void setUserGid(String userGid) {
		this.userGid = userGid;
	}
	
	public String getTraceCode() {
		return this.traceCode;
	}
	public void setTraceCode(String traceCode) {
		this.traceCode = traceCode;
	}
	
	public String getCategory() {
		return this.category;
	}
	public void setCategory(String category) {
		this.category = category;
	}
	
	public String getMobile() {
		return this.mobile;
	}
	public void setMobile(String mobile) {
		this.mobile = mobile;
	}
	
	public String getPhone() {
		return this.phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}
	
	public String getFullName() {
		return this.fullName;
	}
	public void setFullName(String fullName) {
		this.fullName = fullName;
	}
	
	public String getNickname() {
		return this.nickname;
	}
	public void setNickname(String nickname) {
		this.nickname = nickname;
	}
	
	public String getEmail() {
		return this.email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	
	public String getIdCard() {
		return this.idCard;
	}
	public void setIdCard(String idCard) {
		this.idCard = idCard;
	}
	
	public String getState() {
		return this.state;
	}
	public void setState(String state) {
		this.state = state;
	}
	
	public String getCryptoKey() {
		return this.cryptoKey;
	}
	public void setCryptoKey(String cryptoKey) {
		this.cryptoKey = cryptoKey;
	}
	
	public String getExtraInfo() {
		return this.extraInfo;
	}
	public void setExtraInfo(String extraInfo) {
		this.extraInfo = extraInfo;
	}
	
	public String getTimezone() {
		return this.timezone;
	}
	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}
	
	public String getLanguage() {
		return this.language;
	}
	public void setLanguage(String language) {
		this.language = language;
	}
	
	public Long getCabinetId() {
		return this.cabinetId;
	}
	public void setCabinetId(Long cabinetId) {
		this.cabinetId = cabinetId;
	}
	
	public String getClassification() {
		return this.classification;
	}
	public void setClassification(String classification) {
		this.classification = classification;
	}
	
	public Integer getScore() {
		return this.score;
	}
	public void setScore(Integer score) {
		this.score = score;
	}
	
	public String getBiography() {
		return this.biography;
	}
	public void setBiography(String biography) {
		this.biography = biography;
	}
	
	public String getSupInfo() {
		return this.supInfo;
	}
	public void setSupInfo(String supInfo) {
		this.supInfo = supInfo;
	}
	
	public String getAvatarUrl() {
		return this.avatarUrl;
	}
	public void setAvatarUrl(String avatarUrl) {
		this.avatarUrl = avatarUrl;
	}
	
	public Integer getRetryTimes() {
		return this.retryTimes;
	}
	public void setRetryTimes(Integer retryTimes) {
		this.retryTimes = retryTimes;
	}
	
	public Date getLastLogon() {
		return this.lastLogon;
	}
	public void setLastLogon(Date lastLogon) {
		this.lastLogon = lastLogon;
	}
	
	public String getRemark() {
		return this.remark;
	}
	public void setRemark(String remark) {
		this.remark = remark;
	}
	
	public Date getCreateTime() {
		return this.createTime;
	}
	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}
	
	
	@Override
	public String toString(){
		return "UserInfo ["
		+ "sourceId=" + sourceId + ", "
		+ "username=" + username + ", "
		+ "userGid=" + userGid + ", "
		+ "traceCode=" + traceCode + ", "
		+ "category=" + category + ", "
		+ "mobile=" + mobile + ", "
		+ "phone=" + phone + ", "
		+ "fullName=" + fullName + ", "
		+ "nickname=" + nickname + ", "
		+ "email=" + email + ", "
		+ "idCard=" + idCard + ", "
		+ "state=" + state + ", "
		+ "cryptoKey=" + cryptoKey + ", "
		+ "extraInfo=" + extraInfo + ", "
		+ "timezone=" + timezone + ", "
		+ "language=" + language + ", "
		+ "cabinetId=" + cabinetId + ", "
		+ "classification=" + classification + ", "
		+ "score=" + score + ", "
		+ "biography=" + biography + ", "
		+ "supInfo=" + supInfo + ", "
		+ "avatarUrl=" + avatarUrl + ", "
		+ "retryTimes=" + retryTimes + ", "
		+ "lastLogon=" + lastLogon + ", "
		+ "remark=" + remark + ", "
		+ "createTime=" + createTime + ", "
		+ "]";
	}
/**-------------------RESERVE SEPARATOR LINE---------------------*/

}