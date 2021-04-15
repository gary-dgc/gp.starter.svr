/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.jp.japi;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import com.gp.web.BaseApiProvider.ApiKey;
import com.gp.web.BaseApiSupport.ApiHandler;
import com.gp.web.anno.ApiCompiler;
import com.gp.web.anno.ApiHandlerProxy;
import com.gp.web.anno.WebApi;
import com.gp.web.api.base.AddressHandler;

public class JCompAllTester {

	public JCompAllTester() {
		// TODO Auto-generated constructor stub
	}

	void test(String pack){
		ClassPath clzpath;
		ApiCompiler comp = new ApiCompiler(this.getClass().getClassLoader());
		
		try {
			String format = comp.readTemplate();
			clzpath = ClassPath.from(ClassLoader.getSystemClassLoader());
			Set<ClassInfo> allClasses = clzpath.getTopLevelClassesRecursive(pack);
			
			Set<ApiKey> apiKeys ;
			int packHash = Math.abs(pack.hashCode());
			Map<String, Object> params = Maps.newHashMap();	
			String proxyName = "ApiAllProxy" + packHash;
			
			params.put("proxy_class", proxyName);
			StringBuilder hdlrCases = new StringBuilder();
			StringBuilder hdlrMethods = new StringBuilder();
			Map<String, WebApi> methodMap = Maps.newHashMap();
			
			for(ClassInfo cinfo: allClasses) {
				
				Class<?> clazz = cinfo.load();
				String fullname = clazz.getName();
				String purename = clazz.getSimpleName();
				String packname = clazz.getPackage().getName();
		
				purename = purename + Math.abs(packname.hashCode());
				
				hdlrCases.append("			case \"" + purename + "\":\n");
				hdlrCases.append("				return getApi" + purename + "((" + fullname + ")stub, methodName);\n");
				
				hdlrMethods.append("	public ApiHandler getApi" + purename + "(" + fullname + " stub, String methodName) {\n");
				hdlrMethods.append("		switch (methodName) {\n");
				for (Method method : clazz.getMethods()){
					WebApi apiAnno = method.getAnnotation(WebApi.class);
					if(apiAnno != null) {
						
						methodMap.put(method.getName(), apiAnno);
						hdlrMethods.append("			case \""+method.getName()+"\":\n");
						hdlrMethods.append("				return stub::"+method.getName()+";\n");
						
					}
				}
				hdlrMethods.append("			default:\n");
				hdlrMethods.append("				return null;\n");
				hdlrMethods.append("		}\n");
				hdlrMethods.append("	}\n");
				hdlrMethods.append("\n");
				
			}
			
			params.put("handler_cases", hdlrCases.toString());
			params.put("handler_methods", hdlrMethods.toString());
			
			String srcString = comp.format(format, params);
			System.out.println(srcString);
			String fullProxyName = "com.gp.web.proxy." + proxyName;
			Class<? extends ApiHandlerProxy> compiledClass = 
					comp.compile(fullProxyName, srcString);
			
			ApiHandlerProxy proxy = compiledClass.getDeclaredConstructor().newInstance();
			AddressHandler hdlr = new AddressHandler();
			ApiHandler apiref = proxy.getApiReference(hdlr, "handleQueryAddresses");
			System.out.println("--- proxy"+ proxy);
			
			
		}catch(Exception ioe) {
			ioe.printStackTrace();
		} 
	}
	
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		JCompAllTester testr = new JCompAllTester();
		testr.test("com.gp.web.api");
		
		//ApiHandlerProxy proxy = new ApiHandlerProxy();
		
		//AddressHandler hdlr = new AddressHandler();
		//ApiHandler apiref = proxy.getApiReference(hdlr, "handleQueryAddresses");
		
		System.out.println("-----");
	}
}
