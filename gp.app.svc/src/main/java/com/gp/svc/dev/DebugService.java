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
import com.google.common.collect.Lists;
import com.gp.action.DemoAction;
import com.gp.action.DemoLinker;
import com.gp.action.param.DemoLink;
import com.gp.action.param.DemoParam;
import com.gp.bind.BindAutowired;
import com.gp.bind.BindComponent;
import com.gp.common.InfoId;
import com.gp.common.SymmetricToken;
import com.gp.dao.UserDAO;
import com.gp.dao.ext.Sync1Ext;
import com.gp.dao.info.SysOptionInfo;
import com.gp.dao.info.UserInfo;
import com.gp.db.JdbiTran;
import com.gp.exception.ServiceException;
import com.gp.exec.OptionArg;
import com.gp.exec.OptionResult;
import com.gp.sql.SqlBuilder;
import com.gp.sql.update.UpdateBuilder;
import com.gp.svc.BaseService;
import com.gp.svc.ServiceSupport;
import com.gp.svc.SystemService;
import com.gp.util.BaseUtils;
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

	@JdbiTran
	public int advanceDemoMethod(InfoId key) throws ServiceException {

		UpdateBuilder update = SqlBuilder.update();
		update.set("a", "?");
		update.where("id = ?");

		List<Object> params = Lists.newArrayList();

		BaseUtils.reset(params, "a", 1234L);

		// 在服务中嵌入Linker处理
		DemoLink link = new DemoLink();
		link.setVar1("a");

		DemoLinker linker = getLinkerService(DemoLinker.class);

		OptionResult result = linker.perform(link, linkCtx -> {
			update(update.build(), params);
		});

		// 在服务中嵌入Action处理
		DemoParam param = new DemoParam();
		// 设定可变参数
		param.setVar1("1");
		param.addArg(OptionArg.newArg("varg1", 111));

		DemoAction action = getActionService(DemoAction.class);

		result = action.perform(param);

		// 获取返回结果
		OptionArg<Integer> rtv = result.getArg("cnt");

		return rtv.getValue();
	}
}
