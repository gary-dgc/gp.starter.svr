package com.gp.dao.info;

import com.gp.info.TraceableInfo;

import java.util.Date;
/**-------------------RESERVE SEPARATOR LINE---------------------*/

public class SyncMqOutInfo extends TraceableInfo {

	private static final long serialVersionUID = 1L;

	private Long traceId;
	private String destTopic;
	private String destSys;
	private Date operTime;
	private Long operatorId;
	private String operCmd;
	private String payload;
	private String state;
	private String result;
	
	public Long getTraceId() {
		return this.traceId;
	}
	public void setTraceId(Long traceId) {
		this.traceId = traceId;
	}
	
	public String getDestTopic() {
		return this.destTopic;
	}
	public void setDestTopic(String destTopic) {
		this.destTopic = destTopic;
	}
	
	public String getDestSys() {
		return this.destSys;
	}
	public void setDestSys(String destSys) {
		this.destSys = destSys;
	}
	
	public Date getOperTime() {
		return this.operTime;
	}
	public void setOperTime(Date operTime) {
		this.operTime = operTime;
	}
	
	public Long getOperatorId() {
		return this.operatorId;
	}
	public void setOperatorId(Long operatorId) {
		this.operatorId = operatorId;
	}
	
	public String getOperCmd() {
		return this.operCmd;
	}
	public void setOperCmd(String operCmd) {
		this.operCmd = operCmd;
	}
	
	public String getPayload() {
		return this.payload;
	}
	public void setPayload(String payload) {
		this.payload = payload;
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
	
	
	@Override
	public String toString(){
		return "SyncMqOutInfo ["
		+ "traceId=" + traceId + ", "
		+ "destTopic=" + destTopic + ", "
		+ "destSys=" + destSys + ", "
		+ "operTime=" + operTime + ", "
		+ "operatorId=" + operatorId + ", "
		+ "operCmd=" + operCmd + ", "
		+ "payload=" + payload + ", "
		+ "state=" + state + ", "
		+ "result=" + result + ", "
		+ "]";
	}
/**-------------------RESERVE SEPARATOR LINE---------------------*/

}