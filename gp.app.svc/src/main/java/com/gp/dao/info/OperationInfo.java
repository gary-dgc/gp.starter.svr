package com.gp.dao.info;

import com.gp.info.TraceableInfo;
import java.util.Date;
/**-------------------RESERVE SEPARATOR LINE---------------------*/

public class OperationInfo extends TraceableInfo {

	private static final long serialVersionUID = 1L;

	private Long auditId;
	private Long workgroupId;
	private Long subjectUid;
	private String subject;
	private String subjectLabel;
	private Date operationTime;
	private String operation;
	private String operationLabel;
	private String object;
	private String objectLabel;
	private String second;
	private String secondLabel;
	private String predicates;
	
	public Long getAuditId() {
		return this.auditId;
	}
	public void setAuditId(Long auditId) {
		this.auditId = auditId;
	}
	
	public Long getWorkgroupId() {
		return this.workgroupId;
	}
	public void setWorkgroupId(Long workgroupId) {
		this.workgroupId = workgroupId;
	}
	
	public Long getSubjectUid() {
		return this.subjectUid;
	}
	public void setSubjectUid(Long subjectUid) {
		this.subjectUid = subjectUid;
	}
	
	public String getSubject() {
		return this.subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	
	public String getSubjectLabel() {
		return this.subjectLabel;
	}
	public void setSubjectLabel(String subjectLabel) {
		this.subjectLabel = subjectLabel;
	}
	
	public Date getOperationTime() {
		return this.operationTime;
	}
	public void setOperationTime(Date operationTime) {
		this.operationTime = operationTime;
	}
	
	public String getOperation() {
		return this.operation;
	}
	public void setOperation(String operation) {
		this.operation = operation;
	}
	
	public String getOperationLabel() {
		return this.operationLabel;
	}
	public void setOperationLabel(String operationLabel) {
		this.operationLabel = operationLabel;
	}
	
	public String getObject() {
		return this.object;
	}
	public void setObject(String object) {
		this.object = object;
	}
	
	public String getObjectLabel() {
		return this.objectLabel;
	}
	public void setObjectLabel(String objectLabel) {
		this.objectLabel = objectLabel;
	}
	
	public String getSecond() {
		return this.second;
	}
	public void setSecond(String second) {
		this.second = second;
	}
	
	public String getSecondLabel() {
		return this.secondLabel;
	}
	public void setSecondLabel(String secondLabel) {
		this.secondLabel = secondLabel;
	}
	
	public String getPredicates() {
		return this.predicates;
	}
	public void setPredicates(String predicates) {
		this.predicates = predicates;
	}
	
	
	@Override
	public String toString(){
		return "OperationInfo ["
		+ "auditId=" + auditId + ", "
		+ "workgroupId=" + workgroupId + ", "
		+ "subjectUid=" + subjectUid + ", "
		+ "subject=" + subject + ", "
		+ "subjectLabel=" + subjectLabel + ", "
		+ "operationTime=" + operationTime + ", "
		+ "operation=" + operation + ", "
		+ "operationLabel=" + operationLabel + ", "
		+ "object=" + object + ", "
		+ "objectLabel=" + objectLabel + ", "
		+ "second=" + second + ", "
		+ "secondLabel=" + secondLabel + ", "
		+ "predicates=" + predicates + ", "
		+ "]";
	}
/**-------------------RESERVE SEPARATOR LINE---------------------*/

}