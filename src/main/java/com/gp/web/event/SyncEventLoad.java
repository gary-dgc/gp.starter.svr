package com.gp.web.event;

import java.util.Map;

import com.gp.common.InfoId;
import com.gp.common.Instruct;
import com.gp.eventbus.EventPayload;
import com.gp.eventbus.EventType;

public class SyncEventLoad extends EventPayload {

	/** the operation id */
	private InfoId operId;
	
	/** the object id */
	private InfoId objectId;
	
	/** the operation predicates */
	private Map<String, Object> predicates = null;

	/** the operation */
	private Instruct operation;
	
	/** the operator of action */
	private String operator;

	public SyncEventLoad(){
		this.setEventType(EventType.SYNC);
	}
	
	public InfoId getObjectId() {
		return objectId;
	}

	public void setObjectId(InfoId objectId) {
		this.objectId = objectId;
	}

	public Map<String, Object> getPredicates() {
		return predicates;
	}

	public void setPredicates(Map<String, Object> predicates) {
		this.predicates = predicates;
	}

	public Instruct getOperation() {
		return operation;
	}

	public void setOperation(Instruct operation) {
		this.operation = operation;
	}
	
	public String getOperator() {
		return operator;
	}

	public void setOperator(String operator) {
		this.operator = operator;
	}

	public InfoId getOperId() {
		return operId;
	}

	public void setOperId(InfoId operId) {
		this.operId = operId;
	}

	@Override
	public SyncEventLoad data() {
		return this;
	}
	
}
