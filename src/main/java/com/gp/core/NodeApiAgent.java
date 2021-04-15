/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.core;

import java.util.Map;

import com.gp.exception.BaseException;
import com.gp.web.ActionResult;
import com.gp.web.client.NodeAccess;
import com.gp.web.client.NodeClient;

/**
 * NodeApiAgent delegate all the requests to other servers
 *  
 **/
public class NodeApiAgent {

	public static final String GlobalName = "global.site";
	public static final String SyncName = "sync.site";
	public static final String ConvertName = "convert.site";
	
	private NodeClient agentClient ;
	
	static private NodeApiAgent Instance;
	
	static {
		Instance = new NodeApiAgent();
	}
	
	/**
	 * Create API agent instance 
	 **/
	static public NodeApiAgent instance() {
		return Instance;
	}
	
	/**
	 * Get Node client instance 
	 **/
	public NodeClient getNodeClient() {
		
		return agentClient;
	}
	
	/**
	 * Default constructor 
	 **/
	private NodeApiAgent() {
		
		// Prepare the global client
		agentClient = new NodeClient();

	}

	/**
	 * Set node access settings
	 * 
	 * @param nodes the node access array
	 * 
	 **/
	public void setNodeAccess(NodeAccess ...nodes) {
		
		if(null == nodes) {
			return;
		}
		
		for(NodeAccess node: nodes) {
			agentClient.setNodeAccess(node);
		}
	}
	
	/**
	 * Send post to global server
	 * 
	 * @param url the target request url
	 * @param data the map data
	 * 
	 **/
	public ActionResult sendGlobalPost(String url, Map<String, Object> data) throws BaseException{
		
		return agentClient.sendPost(GlobalName, url, data);
	}
	
	/**
	 * Send post to global server
	 * 
	 * @param url the target request url
	 * @param data the post data
	 * 
	 **/
	public ActionResult sendGlobalPost(String url, String data) throws BaseException{
		
		return agentClient.sendPost(GlobalName, url, data);
	}
	
	/**
	 * Send get to global server
	 * 
	 * @param url the target request url
	 * @param data the post data
	 * 
	 **/
	public ActionResult sendGlobalGet(String url, Map<String, Object> data) throws BaseException{
		
		return agentClient.sendGet(GlobalName, url, data);
	}
	
	/**
	 * Send post to sync node server
	 * 
	 * @param url the target request url
	 * @param data the map data
	 * 
	 **/
	public ActionResult sendSyncPost(String url, Map<String, Object> data) throws BaseException{
		
		return agentClient.sendPost(SyncName, url, data);
	}
	
	/**
	 * Send post to sync node server
	 * 
	 * @param url the target request url
	 * @param data the post data
	 * 
	 **/
	public ActionResult sendSyncPost(String url, String data) throws BaseException{
		
		return agentClient.sendPost(SyncName, url, data);
	}
	
	/**
	 * Send get to sync node server
	 * 
	 * @param url the target request url
	 * @param data the post data
	 * 
	 **/
	public ActionResult sendSyncGet(String url, Map<String, Object> data) throws BaseException{
		
		return agentClient.sendGet(SyncName, url, data);
	}

	/**
	 * Send post to convert node server
	 * 
	 * @param url the target request url
	 * @param data the post data
	 * 
	 **/
	public ActionResult sendConvertPost(String url, String data) throws BaseException{
		
		return agentClient.sendPost(ConvertName, url, data);
	}
	
	/**
	 * Send get to convert node server
	 * 
	 * @param url the target request url
	 * @param data the post data
	 * 
	 **/
	public ActionResult sendConvertGet(String url, Map<String, Object> data) throws BaseException{
		
		return agentClient.sendGet(ConvertName, url, data);
	}
	
	/**
	 * Send get to anonymous node server
	 * 
	 * @param url the target request url
	 * @param data the post data
	 * 
	 **/
	public String sendGet(String url, Map<String, Object> data) throws BaseException{
		
		return agentClient.sendGet(url, data);
	}
}
