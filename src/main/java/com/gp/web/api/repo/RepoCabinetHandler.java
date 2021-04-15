/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.repo;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.InfoId;
import com.gp.common.NodeIdKey;
import com.gp.common.ServiceContext;
import com.gp.dao.info.CabinetInfo;
import com.gp.exception.BaseException;
import com.gp.info.DataBuilder;
import com.gp.pagination.PageQuery;
import com.gp.svc.cab.CabinetService;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;

import io.undertow.server.HttpServerExchange;

public class RepoCabinetHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(RepoCabinetHandler.class);
	
	private CabinetService cabinetSvc;
	
	public RepoCabinetHandler() {
		
		cabinetSvc = BindScanner.instance().getBean(CabinetService.class);
		
	}

	/**
	 * Query Cabinets Information List
	 * 
	 **/
	@WebApi(path="cabinet-info", operation="cab:loc")
	public void handleCabinetInfo(HttpServerExchange exchange) throws BaseException{
		
		Map<String, Object> params = this.getRequestBody(exchange);
		
		ActionResult result = null;
		InfoId cabKey = Filters.filterInfoId(params, "cabinet_id", NodeIdKey.CABINET);
		
		CabinetInfo sinfo = cabinetSvc.getCabinet(cabKey);
	
		DataBuilder builder = new DataBuilder();
		builder.set("cabinet_id", sinfo.getId().toString());
		
		builder.set(sinfo, "cabinet_name","cabinet_type","description");
		builder.set("version_enable", sinfo.getVersionEnable());
		
		long capacity = sinfo.getCapacity()/ (1024l * 1024l);
		builder.set("capacity", capacity);
		
		long used = sinfo.getUsed()/ (1024l * 1024l);
		builder.set("used", used);
		
		builder.set("storage",sbuilder -> {
			sbuilder.set("storage_id", sinfo.getStorageId().toString());
			sbuilder.set(sinfo, "storage_name", "storage_type");
		});
			
    
		result = ActionResult.success(getMessage(exchange, "mesg.find.cabinet"));
		result.setData(builder);

		this.sendResult(exchange, result);
	}
	
	/**
	 * Query Cabinets Information List
	 * 
	 **/
	@WebApi(path="cabinets-query", operation="cab:loc")
	public void handleCabinetQuery(HttpServerExchange exchange) throws BaseException{
		
		Map<String, Object> params = this.getRequestBody(exchange);
		
		ActionResult result = null;
		String keyword = Filters.filterString(params, "keyword");
		PageQuery pquery = Filters.filterPageQuery(params);
		
		List<CabinetInfo> infos = cabinetSvc.getCabinets(keyword, null, pquery);
		
		List<Map<String, Object>> rows = infos.stream().map((sinfo) -> {
			DataBuilder builder = new DataBuilder();
			builder.set("cabinet_id", sinfo.getId().toString());
			
			builder.set(sinfo, "cabinet_name","cabinet_type","description");
			builder.set("version_enable", sinfo.getVersionEnable());
			
			long capacity = sinfo.getCapacity()/ (1024l * 1024l);
			builder.set("capacity", capacity);
			
			long used = sinfo.getUsed()/ (1024l * 1024l);
			builder.set("used", used);
			
			builder.set("storage",sbuilder -> {
				sbuilder.set("storage_id", sinfo.getStorageId().toString());
				sbuilder.set(sinfo, "storage_name", "storage_type");
			});
			
			return builder.build();
		}).collect(Collectors.toList());
    
		result = ActionResult.success(getMessage(exchange, "mesg.find.cabinet"));
		result.setData(pquery == null ? null : pquery.getPagination(), rows);

		this.sendResult(exchange, result);
	}
	
	/**
	 * Query Cabinets Information List
	 * 
	 **/
	@WebApi(path="cabinet-save", operation="cab:loc")
	public void handleCabinetSave(HttpServerExchange exchange) throws BaseException{
		
		Map<String, Object> params = this.getRequestBody(exchange);
		
		ServiceContext svcctx = this.getServiceContext(exchange);
		ActionResult result = ActionResult.success("mesg.save.cab");
		InfoId cabKey = Filters.filterInfoId(params, "cabinet_id", NodeIdKey.CABINET);
		
		CabinetInfo cab = new CabinetInfo();
		cab.setInfoId(cabKey);
		cab.setCabinetName(Filters.filterString(params, "cabinet_name"));
		cab.setDescription(Filters.filterString(params, "description"));
		
		cab.setCapacity(Filters.filterLong(params, "capacity") * 1024l * 1024l);
		
		cabinetSvc.updateCabinet(svcctx, cab);
		this.sendResult(exchange, result);
	}
	
	
}
