/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package gp.core;

import java.util.Map;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.io.BaseEncoding;
import com.gp.common.GeneralConfig;
import com.gp.common.GeneralConsts;
import com.gp.core.CoreConsts;
import com.gp.exception.BaseException;
import com.gp.util.CryptoUtils;
import com.gp.util.JsonUtils;
import com.gp.web.ActionResult;
import com.gp.web.client.NodeClient;
import com.gp.web.model.AuthenData;
import com.gp.web.client.NodeAccess;

public class ClientTester {

	static Logger LOGGER = LoggerFactory.getLogger(ClientTester.class);
	
	public ClientTester() { }

	public static void main(String[] args) throws BaseException {
		
		ClientTester tester = new ClientTester();
		//tester.accessRemoteAuth();
		//tester.accessRemoteClinet();
		tester.accessRemoteAuthApi();
		//tester.accessFileApi();
		
	}
	public void accessFileApi() throws BaseException {
		NodeClient nclient = new NodeClient();
		nclient.setCachePath(GeneralConfig.getStringByKeys("file", "cache.path"));
		
		String f = nclient.getBinary(null, "http://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1590907176204&di=d0a97269ce7c0d5214c6e0451dc28361&imgtype=0&src=http%3A%2F%2Fwww.zhka.com%2Fuploads%2Fallimg%2F180107%2F598-1P10G41002638.jpg");
		LOGGER.debug("file: {}", f);
	}
	public void accessRemoteAuthApi() {
		
    	NodeClient nclient = new NodeClient();
    	NodeAccess nauth = NodeAccess.newUserAccess("localhost");
//        *  - client_id
//        *  - client_secret
//        *  - username
//        *  - password
//        *  - scope
//        *  - device
		AuthenData authen = new AuthenData();
		authen.setAudience("1101"); // client id
		authen.setGrantType(CoreConsts.GRANT_PASSWD);
		authen.setPrincipal("dev1");
		authen.setCredential("1");
		
    	Map<String, Object> extra = Maps.newHashMap();
    	extra.put("client_secret", "sslssl");
    	extra.put("scope", "read");
    	extra.put("device", "101010111");
    	authen.setExtra(extra);
    	
    	nauth.setAuthenData(authen);
    	
    	nauth.setHost("127.0.0.1");
    	nauth.setPort(8082);
    	
    	nclient.setNodeAccess(nauth);
    	//nclient.issueUserToken("localhost");
    	
    	try {
			nclient.sendPost("localhost", "/gpapi/pets", "");
		} catch (BaseException e) {
		
			e.printStackTrace();
		}
    	
    	nclient.destroy();
	}
	
	public void accessRemoteAuth() {
		
    	NodeClient nclient = new NodeClient();
    	NodeAccess nauth = NodeAccess.newUserAccess("localhost");
   
//        *  - client_id
//        *  - client_secret
//        *  - username
//        *  - password
//        *  - scope
//        *  - device
    	AuthenData authen = new AuthenData();
		authen.setAudience("1101"); // client id
		authen.setGrantType(CoreConsts.GRANT_PASSWD);
		authen.setPrincipal("dev1");
		authen.setCredential("1");
		
    	Map<String, Object> extra = Maps.newHashMap();
    	extra.put("client_secret", "sslssl");
    	extra.put("scope", "read");
    	extra.put("device", "101010111");
    	authen.setExtra(extra);
    	
    	nauth.setAuthenData(authen);
    	
    	nauth.setHost("127.0.0.1");
    	nauth.setPort(8080);
    	
    	nclient.setNodeAccess(nauth);
    	nclient.issueUserToken("localhost");
    	
    	nclient.refreshUserToken("localhost");
    	
    	nclient.destroy();
	}
	
	public void accessRemoteClinet() {
		
    	NodeClient nclient = new NodeClient();
    	NodeAccess nauth = NodeAccess.newUserAccess("localhost");

//        *  - client_id, client_secret, scope
    	AuthenData authen = new AuthenData();
		authen.setAudience("1101"); // client id
		authen.setGrantType(CoreConsts.GRANT_CLIENT_CRED);
		authen.setPrincipal("1101");
		authen.setCredential("sslssl");
		
    	Map<String, Object> extra = Maps.newHashMap();
    	extra.put("client_secret", "sslssl");
    	extra.put("scope", "read");
    	authen.setExtra(extra);
    	
    	nauth.setAuthenData(authen);
    	
    	nauth.setHost("127.0.0.1");
    	nauth.setPort(8080);
    	
    	nclient.setNodeAccess(nauth);
    	nclient.issueClientToken("localhost");
    	
    	nclient.destroy();
	}
	
	public void accessRemote() {
		
    	NodeClient nclient = new NodeClient();
    	NodeAccess nauth = NodeAccess.newUserAccess("localhost");
 
//        *  - client_id
//        *  - client_secret
//        *  - username
//        *  - password
//        *  - scope
//        *  - device
    	AuthenData authen = new AuthenData();
		authen.setAudience("1101"); // client id
		authen.setGrantType(CoreConsts.GRANT_PASSWD);
		authen.setPrincipal("dev1");
		authen.setCredential("1");
		
    	Map<String, Object> extra = Maps.newHashMap();
    	extra.put("client_secret", "sslssl");
    	extra.put("scope", "read");
    	extra.put("device", "101010111");
    	authen.setExtra(extra);
    	
    	nauth.setAuthenData(authen);
    	
    	nauth.setHost("127.0.0.1");
    	nauth.setPort(8080);
    	
    	nclient.setNodeAccess(nauth);
    	
    	nclient.destroy();
	}

}
