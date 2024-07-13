/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.svc.dev;

import com.google.common.collect.Iterables;
import com.gp.bind.BindAutowired;
import com.gp.bind.BindComponent;
import com.gp.common.SymmetricToken;
import com.gp.dao.UserDAO;
import com.gp.dao.ext.Sync1Ext;
import com.gp.dao.info.SysOptionInfo;
import com.gp.dao.info.UserInfo;
import com.gp.db.JdbiTran;
import com.gp.svc.BaseService;
import com.gp.svc.ServiceSupport;
import com.gp.svc.SystemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@BindComponent(priority = BaseService.BASE_PRIORITY)
public class DebugService extends ServiceSupport implements BaseService {

	public static Logger LOGGER = LoggerFactory.getLogger(DebugService.class);

	@BindAutowired
	UserDAO userdao;
	
	@BindAutowired
	SystemService systemservice;

	@BindAutowired
	Sync1Ext sync1Ext;

	@JdbiTran
	public String encryptPassword(String username, String password) {

		List<UserInfo> list =  userdao.query(cond -> {
			cond.and("username = '" + username +"'");
		});
		UserInfo uinfo = Iterables.getFirst(list, null);
		
		if (null == uinfo)
			return null;

		SysOptionInfo opt = systemservice.getOption("symmetric.crypto.iv");
		String randomIV = opt.getOptValue();
		SymmetricToken itk = new SymmetricToken();

		itk.initial(uinfo.getCryptoKey(), randomIV);
		String hashpwd = itk.encrypt(password);

		sync1Ext.testCount();

		return hashpwd;

	}

}
