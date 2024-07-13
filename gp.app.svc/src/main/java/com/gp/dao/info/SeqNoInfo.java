package com.gp.dao.info;

import com.gp.info.TraceableInfo;
/**-------------------RESERVE SEPARATOR LINE---------------------*/

public class SeqNoInfo extends TraceableInfo {

	private static final long serialVersionUID = 1L;

	private String seqKey;
	private String prefix;
	private Integer length;
	private Integer currVal;
	private Integer stepIntvl;
	private String description;
	private Integer initVal;
	
	public String getSeqKey() {
		return this.seqKey;
	}
	public void setSeqKey(String seqKey) {
		this.seqKey = seqKey;
	}
	
	public String getPrefix() {
		return this.prefix;
	}
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
	
	public Integer getLength() {
		return this.length;
	}
	public void setLength(Integer length) {
		this.length = length;
	}
	
	public Integer getCurrVal() {
		return this.currVal;
	}
	public void setCurrVal(Integer currVal) {
		this.currVal = currVal;
	}
	
	public Integer getStepIntvl() {
		return this.stepIntvl;
	}
	public void setStepIntvl(Integer stepIntvl) {
		this.stepIntvl = stepIntvl;
	}
	
	public String getDescription() {
		return this.description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	public Integer getInitVal() {
		return this.initVal;
	}
	public void setInitVal(Integer initVal) {
		this.initVal = initVal;
	}
	
	
	@Override
	public String toString(){
		return "SeqNoInfo ["
		+ "seqKey=" + seqKey + ", "
		+ "prefix=" + prefix + ", "
		+ "length=" + length + ", "
		+ "currVal=" + currVal + ", "
		+ "stepIntvl=" + stepIntvl + ", "
		+ "description=" + description + ", "
		+ "initVal=" + initVal + ", "
		+ "]";
	}
/**-------------------RESERVE SEPARATOR LINE---------------------*/

}