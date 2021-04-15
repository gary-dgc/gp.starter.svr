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

import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.NodeIdKey;
import com.gp.common.Operations;
import com.gp.common.ServiceContext;
import com.gp.dao.info.CabRecycleInfo;
import com.gp.exception.BaseException;
import com.gp.info.DataBuilder;
import com.gp.svc.cab.CabRecycleService;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;

import io.undertow.server.HttpServerExchange;

public class RepoRecycleHandler extends BaseApiSupport{

	private CabRecycleService cabRecycleService;
	
	public RepoRecycleHandler() {
		
		cabRecycleService = BindScanner.instance().getBean(CabRecycleService.class);
		
	}

	
	/**
	 * Identify the cabinet type 
	 **/
	@WebApi(path="cabinet-recycle-list", operation="cab:fnd-rec")
	public void handleRecycleList(HttpServerExchange exchange) throws BaseException{
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);

		svcctx.addOperationPredicates(params);
		
		ActionResult result = ActionResult.success("mesg.find.recycle");
		
		long cabinetId = Filters.filterLong(params, "cabinet_id");
		String keyword = Filters.filterString(params, "keyword");
		String entryType = Filters.filterString(params, "entry_type");
		
		InfoId manageId = null;
		if(cabinetId > 0){
			manageId = IdKeys.getInfoId(NodeIdKey.CABINET, cabinetId);
		}else {
			svcctx.abort("excp.miss.cab_id");
		}
		
		InfoId subject = svcctx.getPrincipal().getUserId();
		
		List<CabRecycleInfo> infos = cabRecycleService.getCabRecycles(subject, manageId, entryType, keyword, null);
		
		List<Map<String, Object>> data = infos.stream().map((CabRecycleInfo info) -> {
			
			DataBuilder builder = new DataBuilder();
			builder.set("recycle_id", info.getId().toString());
			builder.set("entry_id", info.getEntryId().toString());
			
			builder.set(info, "entry_type", "entry_name", "state", "description", "version", "format");
			
			if(null != info.getCreateTime()) {
				builder.set("create_time", info.getCreateTime().getTime());
			}
			return builder.build();
			
		}).collect(Collectors.toList());
			
		result.setData(data);
		
		this.sendResult(exchange, result);
		
	}

	/**
	 * Identify the cabinet type 
	 **/
	@WebApi(path="cabinet-file-restore")
	public void handleRestoreFile(HttpServerExchange exchange) throws BaseException{
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.CAB_REC_RST);
		
		ActionResult result = ActionResult.success("mesg.find.recycle");
		long fId = Filters.filterLong(params, "file_id");
		InfoId fileId = IdKeys.getInfoId(NodeIdKey.CAB_FILE, fId);
		
		svcctx.setOperationObject(fileId);
		
		cabRecycleService.restoreFile(svcctx, fileId);
		
		this.sendResult(exchange, result);
	}
	
	/**
	 * Identify the cabinet type 
	 **/
	@WebApi(path="cabinet-folder-restore")
	public void handleRestoreFolder(HttpServerExchange exchange) throws BaseException{
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.CAB_REC_RST);
		
		ActionResult result = ActionResult.success("mesg.find.recycle");
		long fId = Filters.filterLong(params, "folder_id");
		InfoId folderId = IdKeys.getInfoId(NodeIdKey.CAB_FOLDER, fId);
		
		svcctx.setOperationObject(folderId);
		cabRecycleService.restoreFolder(svcctx, folderId);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="cabinet-purge-recycle")
	public void handleRemoveRecycle(HttpServerExchange exchange) throws BaseException{
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.CAB_REC_RMV);
		
		ActionResult result = ActionResult.success("mesg.remove.recycle");
		long fId = Filters.filterLong(params, "recycle_id");
		InfoId recycleId = IdKeys.getInfoId(NodeIdKey.CAB_RECYCLE, fId);
		
		svcctx.setOperationObject(recycleId);
		cabRecycleService.purgeRecycle(svcctx, recycleId);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="cabinet-purge-all")
	public void handlePurgeRecycle(HttpServerExchange exchange) throws BaseException{
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.CAB_REC_PRG);
		ActionResult result = ActionResult.success("mesg.remove.recycle");
		long fId = Filters.filterLong(params, "cabinet_id");
		InfoId cabinetId = IdKeys.getInfoId(NodeIdKey.CABINET, fId);
		
		svcctx.setOperationObject(cabinetId);
		cabRecycleService.purgeAllRecycles(svcctx, cabinetId);
		
		this.sendResult(exchange, result);
	}
}
