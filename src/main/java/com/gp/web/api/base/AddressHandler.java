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
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.gp.bind.BindScanner;
import com.gp.common.Addresses;
import com.gp.common.Filters;
import com.gp.common.GeneralConstants;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.NodeIdKey;
import com.gp.common.Operations;
import com.gp.common.ServiceContext;
import com.gp.dao.info.AddressInfo;
import com.gp.exception.BaseException;
import com.gp.exception.ServiceException;
import com.gp.info.BaseIdKey;
import com.gp.info.DataBuilder;
import com.gp.info.Principal;
import com.gp.svc.CommonService;
import com.gp.svc.master.AddressService;
import com.gp.util.NumberUtils;
import com.gp.validate.ArgsValidator;
import com.gp.validate.ValidateMessage;
import com.gp.validate.ValidateUtils;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;

import io.undertow.server.HttpServerExchange;

public class AddressHandler extends BaseApiSupport{
	
	static Logger LOGGER = LoggerFactory.getLogger(AddressHandler.class);
	
	AddressService addressService;
	
	CommonService commonService;
	
	public AddressHandler() {
		
		addressService = BindScanner.instance().getBean(AddressService.class);
		commonService = BindScanner.instance().getBean(CommonService.class);
		
	}

	@WebApi(path="address-query")
	public void handleQueryAddresses(HttpServerExchange exchange)throws BaseException {
		
		this.getServiceContext(exchange, Operations.ADR_FND);
		Map<String,Object> paramap = this.getRequestBody(exchange);
		String sidStr = Filters.filterAll(paramap, "source_id");
		String uidStr = Filters.filterAll(paramap, "user_id");
		
		ActionResult result = null;
	
		Long addrId = GeneralConstants.FAKE_BLIND_ID;
		List<AddressInfo> infos = Lists.newArrayList();
		if(!Strings.isNullOrEmpty(sidStr)) {
			long sid = Strings.isNullOrEmpty(sidStr) ? GeneralConstants.LOCAL_SOURCE: NumberUtils.toLong(sidStr);
			infos = addressService.getSourceAddresses(IdKeys.getInfoId(BaseIdKey.SOURCE, sid));
			addrId = commonService.queryColumn(IdKeys.getInfoId(BaseIdKey.SOURCE, sid), "addr_id", Long.class);
		}else {
			long uid = NumberUtils.toLong(uidStr);
			infos = addressService.getUserAddresses(IdKeys.getInfoId(BaseIdKey.USER, uid));
		}
		final Long fAddrId = addrId;
		List<Map<String, Object>> data = infos.stream().map((info) -> {
			
			DataBuilder builder = new DataBuilder();
			
			builder.set("is_primary", Objects.equals(info.getId(), fAddrId));
			builder.set("addr_id", info.getId().toString());
			builder.set(info, "zip_code", "trace_code", "tag", "province", "city");
			builder.set(info, "county", "street", "detail");
						
			return builder.build();
			
		}).collect(Collectors.toList());
		
		result = ActionResult.success(this.getMessage(exchange, "mesg.success"));
		result.setData(data);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="address-save")
	public void handleNewAddress(HttpServerExchange exchange)throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		
		ActionResult result = null;
	
		Principal principal = this.getPrincipal(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ADR_NEW);
		svcctx.addOperationPredicates(params);
		
		AddressInfo info = new AddressInfo();
		
		info.setTag(Filters.filterString(params, "tag"));
		info.setRefType(Filters.filterString(params, "ref_type"));
		info.setRefId(Filters.filterLong(params, "ref_id"));
		info.setProvince(Filters.filterString(params, "province"));
		info.setCity(Filters.filterString(params, "city"));
		info.setCountry("zh_CN");
		info.setCounty(Filters.filterString(params, "county"));
		info.setZipCode(Filters.filterString(params, "zip_code"));
		info.setStreet(Filters.filterString(params, "street"));
		info.setDetail(Filters.filterString(params, "detail"));
		// check the validation of user information
		Set<ValidateMessage> vmsg = ValidateUtils.validate(principal.getLocale(), info);
		if(Strings.isNullOrEmpty((String)params.get("ref_type")))
		{
			vmsg.add(new ValidateMessage("ref_type","must not be null"));
		}
		if(!Iterables.isEmpty(vmsg)){ // fail pass validation
			ServiceException svcexcp = new ServiceException(principal.getLocale(), "excp.validate");
			svcexcp.addValidateMessages(vmsg);
			throw svcexcp;
		}
		String refType = (String)params.get("ref_type");
		if(Addresses.ReferType.SOURCE.name().equals(refType)) {
			
			info.setRefId(GeneralConstants.LOCAL_SOURCE);
			addressService.addSourceAddress(svcctx, info, Filters.filterBoolean(params, "is_primary"));
		}else if(Addresses.ReferType.USER.name().equals(refType)) {
			
			addressService.addUserAddress(svcctx, info, Filters.filterBoolean(params, "is_primary"));
		}
		
		result = ActionResult.success(getMessage(exchange, "mesg.new.addr"));
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="address-remove")
	public void handleRemoveAddress(HttpServerExchange exchange)throws BaseException {
		
		Map<String, Object> paramap = this.getRequestBody(exchange);
		
		ArgsValidator.newValidator(paramap)
			.require("addr_id")
			.validate(true);
		
		String addrIdStr = Filters.filterAll(paramap, "addr_id");
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ADR_RMV);
	
		ActionResult result = null;
		
		InfoId addrId = IdKeys.getInfoId(NodeIdKey.ADDRESS, NumberUtils.toLong(addrIdStr));
		addressService.removeAddress(svcctx, addrId);
		svcctx.setOperationObject(addrId);
		
		result = ActionResult.success(getMessage(exchange, "mesg.remove.addr"));
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="address-primary")
	public void handlePrimaryAddress(HttpServerExchange exchange)throws BaseException {
		
		Map<String, Object> paramap = this.getRequestBody(exchange);
		
		ArgsValidator.newValidator(paramap)
			.require("addr_id")
			.validate(true);
		
		String addrIdStr = Filters.filterAll(paramap, "addr_id");
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ADR_PRM);
	
		ActionResult result = null;
		
		InfoId addrId = IdKeys.getInfoId(NodeIdKey.ADDRESS, NumberUtils.toLong(addrIdStr));
		svcctx.setOperationObject(addrId);
		
		addressService.setPrimaryAddress(svcctx, addrId);
			
		result = ActionResult.success(getMessage(exchange, "mesg.primary.addr"));
		
		this.sendResult(exchange, result);
	}
}
