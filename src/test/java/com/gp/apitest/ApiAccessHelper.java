package com.gp.apitest;

import java.util.Map;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.gp.core.CoreConstants;
import com.gp.core.CoreConstants.TokenState;
import com.gp.exception.BaseException;
import com.gp.util.JsonUtils;
import com.gp.web.ActionResult;
import com.gp.web.client.NodeAccess;
import com.gp.web.client.NodeClient;
import com.gp.web.model.AuthenData;

/**
 * Api Test Access Helper 
 * 
 * @author gary diao
 * @since 0.1
 **/
public class ApiAccessHelper {

	public static ObjectMapper JSON_MAPPER = JsonUtils.JSON_MAPPER;
	static Logger LOGGER = LoggerFactory.getLogger(ApiAccessHelper.class);
	static NodeClient nodeClient ;
	static {
		nodeClient = new NodeClient();
	}
	// Invoke Remote 
	public static void callRemoteRpc(ViewTracer tracer) {
		
		String url = tracer.rootPath + tracer.api;
		
		ActionResult response;
		try {
			response = nodeClient.sendPost("localhost", url, tracer.request);
			tracer.data = JsonUtils.toJson(response.getData());
			tracer.meta = JsonUtils.toJson(response.getMeta());
			tracer.state = response.getMeta().getMessage();
			
		} catch (BaseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			tracer.state = e.getMessage();
		} finally {
		
			// call the 
			tracer.doCallback();
		}
	}
	
	// authenticate
	public static void callAuthenRpc(ViewTracer tracer) {
		
    	NodeAccess nauth = NodeAccess.newUserAccess("localhost");
    	Map<String, Object> authData = Maps.newHashMap();
//        *  - client_id
//        *  - client_secret
//        *  - username
//        *  - password
//        *  - scope
//        *  - device
    	authData.put("client_id", tracer.client);
    	authData.put("client_secret", tracer.secret);
    	authData.put("scope", tracer.scope);
    	
    	authData.put("device", "101010111");
    	
    	AuthenData data = new AuthenData();
    	
    	data.setGrantType(CoreConstants.GRANT_PASSWD);
    	data.setAudience(tracer.client);
    	data.setCredential(tracer.pass);
    	data.setPrincipal(tracer.user);
    	data.setExtra(authData);
    	
    	nauth.setAuthenData(data);
    	
    	nauth.setHost(tracer.host);
    	nauth.setPort(NumberUtils.toInt(tracer.port));
    	
    	nodeClient.setNodeAccess(nauth);
    	TokenState state = nodeClient.issueUserToken("localhost");
    	
    	if(state == TokenState.VALID_TOKEN) {
    		
    		tracer.state = "success get access token";
    		tracer.token = nodeClient.getNodeAccess("localhost").getAccessToken();
    		
    	}else {
    		
    		tracer.state = "fail get access token";
    	}
	    // call the 
		tracer.doCallback();
	}

}
