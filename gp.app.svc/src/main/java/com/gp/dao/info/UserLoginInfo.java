package com.gp.dao.info;

import com.gp.info.TraceableInfo;
/**-------------------RESERVE SEPARATOR LINE---------------------*/

public class UserLoginInfo extends TraceableInfo {

	private static final long serialVersionUID = 1L;

	private Long userId;
	private String authenType;
	private String login;
	private String credential;
	
	public Long getUserId() {
		return this.userId;
	}
	public void setUserId(Long userId) {
		this.userId = userId;
	}
	
	public String getAuthenType() {
		return this.authenType;
	}
	public void setAuthenType(String authenType) {
		this.authenType = authenType;
	}
	
	public String getLogin() {
		return this.login;
	}
	public void setLogin(String login) {
		this.login = login;
	}
	
	public String getCredential() {
		return this.credential;
	}
	public void setCredential(String credential) {
		this.credential = credential;
	}
	
	
	@Override
	public String toString(){
		return "UserLoginInfo ["
		+ "userId=" + userId + ", "
		+ "authenType=" + authenType + ", "
		+ "login=" + login + ", "
		+ "credential=" + credential + ", "
		+ "]";
	}
/**-------------------RESERVE SEPARATOR LINE---------------------*/

}