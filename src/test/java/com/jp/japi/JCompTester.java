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

import com.google.common.collect.Maps;
import com.gp.web.anno.ApiCompiler;
import com.gp.web.anno.ApiHandlerProxy;
import com.gp.web.anno.WebApi;
import com.gp.web.api.DevDebugHandler;
import com.gp.web.api.base.AddressHandler;

public class JCompTester {

	public JCompTester(AddressHandler handler) {
		
		new DevDebugHandler();
	}
	
	public void test(AddressHandler handler) {
		ApiCompiler comp = new ApiCompiler(this.getClass().getClassLoader());
		
		try {
			Class<?> clazz = handler.getClass();
			String format = comp.readTemplate();
			System.out.println(clazz.getPackage().getName());
			System.out.println(clazz.getSimpleName());
			
			Map<String, Object> params = Maps.newHashMap();
			params.put("ApiPackage", clazz.getPackage().getName());
			params.put("ApiStubClass", clazz.getSimpleName());
			
			Map<String, WebApi> methodMap = Maps.newHashMap();
			StringBuilder cases = new StringBuilder();
			for (Method method : clazz.getMethods()){
				WebApi apiAnno = method.getAnnotation(WebApi.class);
				if(apiAnno != null) {
					System.out.println(apiAnno);
					methodMap.put(method.getName(), apiAnno);
					
					cases.append("			case \""+method.getName()+"\":\n");
					cases.append("				return stub::"+method.getName()+";\n");
					
				}
			}
			
			params.put("ApiMethodCases", cases);
			
			String srcString = comp.format(format, params);
			System.out.println(srcString);
			String fullProxyName = "com.gp.web.proxy." + clazz.getSimpleName() +"Proxy";
			Class<? extends ApiHandlerProxy> compiledClass = 
					comp.compile(fullProxyName, srcString);
			
			ApiHandlerProxy proxy = compiledClass.getDeclaredConstructor(clazz).newInstance(handler);
			System.out.println("--- proxy"+ proxy);
			
			
		}catch(IOException ioe) {
			ioe.printStackTrace();
		} catch (NoSuchMethodException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		JCompTester testr = new JCompTester(new AddressHandler());
	}

}
