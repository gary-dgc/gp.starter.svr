/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.config;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.gp.bind.BindScanner;
import com.gp.common.*;
import com.gp.dao.info.DictionaryInfo;
import com.gp.exception.BaseException;
import com.gp.exception.ServiceException;
import com.gp.info.BaseIdKey;
import com.gp.info.DataBuilder;
import com.gp.info.Principal;
import com.gp.paging.PageQuery;
import com.gp.svc.master.DictionaryService;
import com.gp.validate.ArgsValidator;
import com.gp.validate.ValidateMessage;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

public class DictionaryHandler extends BaseApiSupport {

	static Logger LOGGER = LoggerFactory.getLogger(DictionaryHandler.class);
	
	static SimpleDateFormat MDF_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	private DictionaryService dictservice;
	
	public DictionaryHandler() {
		dictservice = BindScanner.instance().getBean(DictionaryService.class);
		
	}

	@WebApi(path="dicts-query", operation="dict:fnd")
	public void hanldeEntriesSearch(HttpServerExchange exchange)throws BaseException {
		
		Map<String, Object> paramap = this.getRequestBody(exchange);
				
		PageQuery pquery = Filters.filterPageQuery(paramap);

		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.dicts"));
		List<Map<String, Object>> list = Lists.newArrayList();

		String language = Filters.filterString(paramap, "language");
		
		if(Strings.isNullOrEmpty(language)) {
			language = "zh_CN";
		}
		
		List<DictionaryInfo> gresult = dictservice.getDictEntries( 
				Filters.filterAll(paramap, "group"), 
				Filters.filterAll(paramap, "keyword"), pquery);
		
		for(DictionaryInfo info: gresult){
			
			DataBuilder builder = new DataBuilder();
			
			builder.set("dict_id", info.getId().toString());
			builder.set(info, "dict_key", "dict_group", "dict_value", "language");
			builder.set("label", info.getLabel(language));
			
			builder.set("modifier_uid", info.getModifierUid().toString());
			builder.set("modify_time", String.valueOf(info.getModifyTime() == null? "0":info.getModifyTime().getTime()));
			
			list.add(builder.build());
			
		}

		result.setData(pquery == null ? null : pquery.getPagination(), list);

		this.sendResult(exchange, result);
	}
	
	@SuppressWarnings("unchecked")
	@WebApi(path="dict-save")
	public void handleEntrySave(HttpServerExchange exchange) throws BaseException{
		
		ActionResult result = null;

		Principal principal = this.getPrincipal(exchange);

		Map<String, Object> params = this.getRequestBody(exchange);
		
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.DICT_UPD);
			
		DictionaryInfo dinfo = new DictionaryInfo();
		InfoId did = IdKeys.getInfoId(BaseIdKey.DICTIONARY, Filters.filterLong(params, "dict_id"));
					
		dinfo.setInfoId(did);
		dinfo.setDictKey(Filters.filterString(params, "dict_key"));
		dinfo.setDictValue(Filters.filterString(params, "dict_value"));
		dinfo.setDictGroup(Filters.filterString(params, "dict_group"));

		dinfo.setLabelMap((Map<String, String>)params.get("labels"));
		
		if(!IdKeys.isValidId(dinfo.getInfoId())){
			ServiceException svcexcp = new ServiceException(principal.getLocale(), "excp.validate");
			svcexcp.addValidateMessage(ValidateMessage.newMessage("prop.dict_id", "mesg.prop.miss"));
			throw svcexcp;
		}

		svcctx.setOperationObject(dinfo.getInfoId());
		svcctx.addOperationPredicates(dinfo);
			
		dictservice.updateDictEntry(svcctx, dinfo);
		
		result = ActionResult.success(getMessage(exchange, "mesg.save.dict"));
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="dict-groups-query", operation="dict:grp-fnd")
	public void handleEntriesGroup(HttpServerExchange exchange) throws BaseException{
		
		ActionResult result = null;

		List<String> groups = dictservice.getDictGroups();
		
		result = ActionResult.success(getMessage(exchange, "mesg.find.dict.grp"));
		result.setData(groups);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="dict-info", operation="dict:fnd")
	public void handleEntryInfo(HttpServerExchange exchange) throws BaseException{
		
		ActionResult result = null;
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ArgsValidator.newValidator(paramap)
			.requireOne("dict_id", "dict_key")
			.validate(true);
		
		String dict = Filters.filterAll(paramap, "dict_key");
		long dictId = Filters.filterLong(paramap, "dict_id");
		
		DictionaryInfo dinfo = null;
		if(dictId > 0) {
			InfoId id = IdKeys.getInfoId(BaseIdKey.DICTIONARY, dictId);
			dinfo = dictservice.getDictEntry(id);
		}
		if(!Strings.isNullOrEmpty(dict)) {
			dinfo = dictservice.getDictEntry(dict);
		}
		DataBuilder entry = new DataBuilder();
		if(dinfo != null) {
			
			entry.set("dict_id", dinfo.getId().toString());
			entry.set(dinfo, "dict_group", "dict_key", "dict_value");
			entry.set("labels", dinfo.getLabelMap());
						
		}
	
		result = ActionResult.success(getMessage(exchange, "mesg.find.dict"));
		result.setData(entry.build());
		
		this.sendResult(exchange, result);
	}
}
