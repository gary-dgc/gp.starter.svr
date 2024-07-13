package com.gp.dao.info;

import com.gp.info.TraceableInfo;

import java.util.Date;

/**-------------------RESERVE SEPARATOR LINE---------------------*/

public class AuditInfo extends TraceableInfo {

	private static final long serialVersionUID = 1L;

	private String client;
	private String host;
	private String app;
	private String path;
	private String version;
	private String device;
	private String subject;
	private String operation;
	private String objectId;
	private String predicates;
	private String state;
	private String message;
	private Date auditTime;
	private Integer elapsedTime;
	private Integer instanceId;
	
	public String getClient() {
		return this.client;
	}
	public void setClient(String client) {
		this.client = client;
	}
	
	public String getHost() {
		return this.host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	
	public String getApp() {
		return this.app;
	}
	public void setApp(String app) {
		this.app = app;
	}
	
	public String getPath() {
		return this.path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	
	public String getVersion() {
		return this.version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	
	public String getDevice() {
		return this.device;
	}
	public void setDevice(String device) {
		this.device = device;
	}
	
	public String getSubject() {
		return this.subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	
	public String getOperation() {
		return this.operation;
	}
	public void setOperation(String operation) {
		this.operation = operation;
	}
	
	public String getObjectId() {
		return this.objectId;
	}
	public void setObjectId(String objectId) {
		this.objectId = objectId;
	}
	
	public String getPredicates() {
		return this.predicates;
	}
	public void setPredicates(String predicates) {
		this.predicates = predicates;
	}
	
	public String getState() {
		return this.state;
	}
	public void setState(String state) {
		this.state = state;
	}
	
	public String getMessage() {
		return this.message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	
	public Date getAuditTime() {
		return this.auditTime;
	}
	public void setAuditTime(Date auditTime) {
		this.auditTime = auditTime;
	}
	
	public Integer getElapsedTime() {
		return this.elapsedTime;
	}
	public void setElapsedTime(Integer elapsedTime) {
		this.elapsedTime = elapsedTime;
	}
	
	public Integer getInstanceId() {
		return this.instanceId;
	}
	public void setInstanceId(Integer instanceId) {
		this.instanceId = instanceId;
	}
	
	
	@Override
	public String toString(){
		return "AuditInfo ["
		+ "client=" + client + ", "
		+ "host=" + host + ", "
		+ "app=" + app + ", "
		+ "path=" + path + ", "
		+ "version=" + version + ", "
		+ "device=" + device + ", "
		+ "subject=" + subject + ", "
		+ "operation=" + operation + ", "
		+ "objectId=" + objectId + ", "
		+ "predicates=" + predicates + ", "
		+ "state=" + state + ", "
		+ "message=" + message + ", "
		+ "auditTime=" + auditTime + ", "
		+ "elapsedTime=" + elapsedTime + ", "
		+ "instanceId=" + instanceId + ", "
		+ "]";
	}
/**-------------------RESERVE SEPARATOR LINE---------------------*/

}