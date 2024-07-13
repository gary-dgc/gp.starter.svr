package com.gp.dao.info;

import com.gp.info.TraceableInfo;

import java.util.Date;

/**-------------------RESERVE SEPARATOR LINE---------------------*/

public class TokenInfo extends TraceableInfo {

	private static final long serialVersionUID = 1L;

	private String issuer;
	private String audience;
	private Date expireTime;
	private Date notBefore;
	private String subject;
	private String device;
	private Date issueAt;
	private String claims;
	private String jwtToken;
	private String scope;
	private String refreshToken;

	public String getIssuer() {
		return this.issuer;
	}
	public void setIssuer(String issuer) {
		this.issuer = issuer;
	}
	
	public String getAudience() {
		return this.audience;
	}
	public void setAudience(String audience) {
		this.audience = audience;
	}
	
	public Date getExpireTime() {
		return this.expireTime;
	}
	public void setExpireTime(Date expireTime) {
		this.expireTime = expireTime;
	}
	
	public Date getNotBefore() {
		return this.notBefore;
	}
	public void setNotBefore(Date notBefore) {
		this.notBefore = notBefore;
	}
	
	public String getSubject() {
		return this.subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	
	public String getDevice() {
		return this.device;
	}
	public void setDevice(String device) {
		this.device = device;
	}
	
	public Date getIssueAt() {
		return this.issueAt;
	}
	public void setIssueAt(Date issueAt) {
		this.issueAt = issueAt;
	}
	
	public String getClaims() {
		return this.claims;
	}
	public void setClaims(String claims) {
		this.claims = claims;
	}
	
	public String getJwtToken() {
		return this.jwtToken;
	}
	public void setJwtToken(String jwtToken) {
		this.jwtToken = jwtToken;
	}
	
	public String getScope() {
		return this.scope;
	}
	public void setScope(String scope) {
		this.scope = scope;
	}
	
	public String getRefreshToken() {
		return this.refreshToken;
	}
	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	@Override
	public String toString(){
		return "TokenInfo ["
		+ "issuer=" + issuer + ", "
		+ "audience=" + audience + ", "
		+ "expireTime=" + expireTime + ", "
		+ "notBefore=" + notBefore + ", "
		+ "subject=" + subject + ", "
		+ "device=" + device + ", "
		+ "issueAt=" + issueAt + ", "
		+ "claims=" + claims + ", "
		+ "jwtToken=" + jwtToken + ", "
		+ "scope=" + scope + ", "
		+ "refreshToken=" + refreshToken + ", "
		+ "modifier=" + getModifierUid()
		+ "]";
	}
/**-------------------RESERVE SEPARATOR LINE---------------------*/

}