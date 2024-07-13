package com.gp.dao.info;

import com.gp.info.TraceableInfo;

/**-------------------RESERVE SEPARATOR LINE---------------------*/

public class ClientInfo extends TraceableInfo {

	private static final long serialVersionUID = 1L;

	private String clientName;
	private String scope;
	private String clientKey;
	private String clientSecret;
	private String claimsJson;
	
	public String getClientName() {
		return this.clientName;
	}
	public void setClientName(String clientName) {
		this.clientName = clientName;
	}
	
	public String getScope() {
		return this.scope;
	}
	public void setScope(String scope) {
		this.scope = scope;
	}
	
	public String getClientKey() {
		return this.clientKey;
	}
	public void setClientKey(String clientKey) {
		this.clientKey = clientKey;
	}
	
	public String getClientSecret() {
		return this.clientSecret;
	}
	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}
	
	public String getClaimsJson() {
		return this.claimsJson;
	}
	public void setClaimsJson(String claimsJson) {
		this.claimsJson = claimsJson;
	}
	
	
	@Override
	public String toString(){
		return "ClientInfo ["
		+ "clientName=" + clientName + ", "
		+ "scope=" + scope + ", "
		+ "clientKey=" + clientKey + ", "
		+ "clientSecret=" + clientSecret + ", "
		+ "claimsJson=" + claimsJson + ", "
		+ "]";
	}
/**-------------------RESERVE SEPARATOR LINE---------------------*/

}