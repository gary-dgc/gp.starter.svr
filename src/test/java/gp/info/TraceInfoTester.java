/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package gp.info;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gp.dao.info.UserInfo;

public class TraceInfoTester {

	static Logger LOGGER = LoggerFactory.getLogger(TraceInfoTester.class);
	
	public TraceInfoTester() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		
		TraceInfoTester tester = new TraceInfoTester();
		
		tester.teste();
	}

	public void teste() {
		
		UserInfo uinfo = new UserInfo();
		uinfo.setTraceCode("sssss");
		
		Map<String, Object> map = uinfo.toMap("trace_code");
		
		System.out.println("data: "+ map.toString());
	}
}
