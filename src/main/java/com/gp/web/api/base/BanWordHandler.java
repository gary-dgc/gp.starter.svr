/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.base;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.NodeIdKey;
import com.gp.common.Operations;
import com.gp.common.ServiceContext;
import com.gp.dao.info.BanWordInfo;
import com.gp.exception.BaseException;
import com.gp.exception.CoreException;
import com.gp.exception.ServiceException;
import com.gp.info.DataBuilder;
import com.gp.info.Principal;
import com.gp.pagination.PageQuery;
import com.gp.svc.master.BanWordService;
import com.gp.validate.ArgsValidator;
import com.gp.validate.ValidateMessage;
import com.gp.validate.ValidateUtils;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;

import io.undertow.server.HttpServerExchange;

public class BanWordHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(BanWordHandler.class);
	
	private BanWordService profansvc;
	
	public BanWordHandler() {
		profansvc = BindScanner.instance().getBean(BanWordService.class);
		
	}

	@WebApi(path="banwords-query", operation="wrd:fnd")
	public void handleQueryProfans(HttpServerExchange exchange)throws BaseException {
		
		Map<String,Object> paramap = this.getRequestBody(exchange);
		String scope = Filters.filterAll(paramap, "scope");
		String state = Filters.filterAll(paramap, "state");
		String word = Filters.filterString(paramap, "keywords");
		
		PageQuery pquery = Filters.filterPageQuery(paramap);
		ActionResult result = null;

		List<BanWordInfo> plist = profansvc.getBanWords(scope, state, word, pquery);
		List<Map<String, Object>> dataList =  plist.stream().map((pinfo)->{
			
			DataBuilder builder = new DataBuilder();
			
			builder.set("ban_id", pinfo.getId().toString());
			builder.set(pinfo, "ban_scope", "ban_name", "state", "words");
			builder.set(pinfo, "description");
			builder.set("modifier_uid", pinfo.getModifierUid().toString());
			builder.set("modify_time", pinfo.getModifyTime().getTime());

			return builder.build();
		}).collect(Collectors.toList());

		result = ActionResult.success(getMessage(exchange, "mesg.find.word"));
		result.setData(pquery == null ? null : pquery.getPagination(), dataList);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="banword-add")
	public void hanleAddProfan(HttpServerExchange exchange)throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		Principal principal = this.getPrincipal(exchange);
		
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.WRD_NEW);
	
		svcctx.addOperationPredicates(params);
		
		ActionResult result = null;

		BanWordInfo pinfo = new BanWordInfo();
		pinfo.setInfoId(IdKeys.newInfoId(NodeIdKey.BAN_WORD));
		pinfo.setBanName(Filters.filterString(params, "ban_name"));
		pinfo.setBanScope(Filters.filterString(params, "ban_scope"));
		pinfo.setDescription(Filters.filterString(params, "description"));
		pinfo.setState(Filters.filterString(params, "state"));
		pinfo.setWords(Filters.filterString(params, "words"));
		// check the validation of user information
		Set<ValidateMessage> vmsg = ValidateUtils.validate(principal.getLocale(), pinfo);
		if(Strings.isNullOrEmpty(pinfo.getBanScope()))
		{
			vmsg.add(new ValidateMessage("ban_scope","must not be null"));
		}
		if(!Iterables.isEmpty(vmsg)){ // fail pass validation
			ServiceException svcexcp = new ServiceException(principal.getLocale(), "excp.validate");
			svcexcp.addValidateMessages(vmsg);
			throw svcexcp;
		}
		profansvc.addBanWord(svcctx, pinfo);

		result = ActionResult.success(getMessage(exchange, "mesg.new.word"));

		this.sendResult(exchange, result);
	}
	
	@WebApi(path="banword-save")
	public void handleSaveWord(HttpServerExchange exchange)throws BaseException{
		
		Map<String, Object> params = this.getRequestBody(exchange);
		Principal principal = this.getPrincipal(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.WRD_UPD);
		svcctx.addOperationPredicates(params);
		
		ActionResult result = null;

		BanWordInfo pinfo = new BanWordInfo();
		pinfo.setInfoId(IdKeys.getInfoId(NodeIdKey.BAN_WORD, Filters.filterLong(params, "ban_id")));
		pinfo.setBanName(Filters.filterString(params, "ban_name"));
		pinfo.setBanScope(Filters.filterString(params, "ban_scope"));
		pinfo.setDescription(Filters.filterString(params, "description"));
		pinfo.setState(Filters.filterString(params, "state"));
		pinfo.setWords(Filters.filterString(params, "words"));
		// check the validation of user information
		Set<ValidateMessage> vmsg = ValidateUtils.validate(principal.getLocale(), pinfo);
		if(Strings.isNullOrEmpty(pinfo.getBanScope()))
		{
			vmsg.add(new ValidateMessage("ban_scope","not be null"));
		}
		if(!Iterables.isEmpty(vmsg)){ // fail pass validation
			ServiceException svcexcp = new ServiceException(principal.getLocale(), "excp.validate");
			svcexcp.addValidateMessages(vmsg);
			throw svcexcp;
		}
		
		svcctx.setOperationObject(pinfo.getInfoId());
		
		profansvc.updateBanWord(svcctx, pinfo);

		result = ActionResult.success(getMessage(exchange, "mesg.save.word"));
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="banword-remove")
	public void handleRemoveWord(HttpServerExchange exchange)throws BaseException{
		ActionResult result = null;
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ArgsValidator.newValidator(paramap)
			.require("ban_id")
			.validate(true);
		
		Principal principal = this.getPrincipal(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.WRD_RMV);
		
		Long banId = Filters.filterLong(paramap, "ban_id");
	
		if(banId == 0  ) {
			throw new CoreException(principal.getLocale(), "excp.miss.param");
		}
		InfoId wid = IdKeys.getInfoId(NodeIdKey.BAN_WORD, banId);
		svcctx.setOperationObject(wid);
		profansvc.removeBanWord( wid );

		result = ActionResult.success(getMessage(exchange, "mesg.remove.word"));

		this.sendResult(exchange, result);
	}
}
