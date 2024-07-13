package com.gp.dao.info;

import com.gp.info.TraceableInfo;

import java.util.Date;
/**-------------------RESERVE SEPARATOR LINE---------------------*/

public class SyncTraceInfo extends TraceableInfo {

	private static final long serialVersionUID = 1L;

	private Long chronicalId;
	private String originGid;
	private String traceCode;
	private String hierKey;
	private String attrKeys;
	private String traceOp;
	private String data;
	private String state;
	private String result;
	private Date traceTime;
	private Date syncTime;
	
	public Long getChronicalId() {
		return this.chronicalId;
	}
	public void setChronicalId(Long chronicalId) {
		this.chronicalId = chronicalId;
	}
	
	public String getOriginGid() {
		return this.originGid;
	}
	public void setOriginGid(String originGid) {
		this.originGid = originGid;
	}
	
	public String getTraceCode() {
		return this.traceCode;
	}
	public void setTraceCode(String traceCode) {
		this.traceCode = traceCode;
	}
	
	public String getHierKey() {
		return this.hierKey;
	}
	public void setHierKey(String hierKey) {
		this.hierKey = hierKey;
	}
	
	public String getAttrKeys() {
		return this.attrKeys;
	}
	public void setAttrKeys(String attrKeys) {
		this.attrKeys = attrKeys;
	}
	
	public String getTraceOp() {
		return this.traceOp;
	}
	public void setTraceOp(String traceOp) {
		this.traceOp = traceOp;
	}
	
	public String getData() {
		return this.data;
	}
	public void setData(String data) {
		this.data = data;
	}
	
	public String getState() {
		return this.state;
	}
	public void setState(String state) {
		this.state = state;
	}
	
	public String getResult() {
		return this.result;
	}
	public void setResult(String result) {
		this.result = result;
	}
	
	public Date getTraceTime() {
		return this.traceTime;
	}
	public void setTraceTime(Date traceTime) {
		this.traceTime = traceTime;
	}
	
	public Date getSyncTime() {
		return this.syncTime;
	}
	public void setSyncTime(Date syncTime) {
		this.syncTime = syncTime;
	}
	
	
	@Override
	public String toString(){
		return "SyncTraceInfo ["
		+ "chronicalId=" + chronicalId + ", "
		+ "originGid=" + originGid + ", "
		+ "traceCode=" + traceCode + ", "
		+ "hierKey=" + hierKey + ", "
		+ "attrKeys=" + attrKeys + ", "
		+ "traceOp=" + traceOp + ", "
		+ "data=" + data + ", "
		+ "state=" + state + ", "
		+ "result=" + result + ", "
		+ "traceTime=" + traceTime + ", "
		+ "syncTime=" + syncTime + ", "
		+ "]";
	}
/**-------------------RESERVE SEPARATOR LINE---------------------*/

}