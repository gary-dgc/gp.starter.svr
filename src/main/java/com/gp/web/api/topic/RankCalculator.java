/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.topic;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import com.google.common.collect.Maps;
import com.gp.common.IdGenerator;

public class RankCalculator {

	public RankCalculator() {
		// TODO Auto-generated constructor stub
	}

	public void calc(Map<String, Object> params) {
		
		long postTime = (long)params.get("create_time");
		long agingTime = IdGenerator.TWEPOCH;
		
		long diffSeconds = (postTime - agingTime) / 1000;
	
		int voteUp = (int)params.get("voteup");
		int voteDown = (int)params.get("votedown");
		
		int xVote = voteUp - voteDown;
		
		int y = xVote > 0 ? 1 : (xVote == 0 ? 0 : -1);
		
		int z = Math.max(xVote, 1);
		
		// 45000 is the amount of seconds in 12.5 hours.
		double m = Math.log10(z) + (y * diffSeconds)/86400;
		System.out.println("---- logz: " + Math.log10(z));
		System.out.println("---- rank: " + m);
	}
	
	public static void main(String[] args) throws ParseException {
		
		final DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date dt = df.parse("2017-01-01 00:00:00");
		System.out.println("---- time: " + dt.getTime());
		
		System.out.println("---- time: " + 24*60*60);
		
		RankCalculator runner = new RankCalculator();
		Map<String, Object> params = Maps.newHashMap();
		params.put("create_time", Calendar.getInstance().getTime().getTime());
		params.put("voteup", 129);
		params.put("votedown", 12);
		
		runner.calc(params);
		
		params = Maps.newHashMap();
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MONTH, 12);
		params.put("create_time", cal.getTime().getTime());
		params.put("voteup", 129);
		params.put("votedown", 127);
		
		runner.calc(params);
	}
}
