/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.master;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.gp.bind.BindScanner;
import com.gp.common.*;
import com.gp.common.Storages.SettingKeys;
import com.gp.common.Storages.StorageState;
import com.gp.common.Storages.StorageType;
import com.gp.dao.info.StorageInfo;
import com.gp.exception.BaseException;
import com.gp.exception.ServiceException;
import com.gp.info.DataBuilder;
import com.gp.info.Principal;
import com.gp.oss.StorageSetting;
import com.gp.paging.PageQuery;
import com.gp.svc.master.StorageService;
import com.gp.validate.ValidateMessage;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StorageHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(StorageHandler.class);
	
	private StorageService storageService;
	
	public StorageHandler() {
		storageService = BindScanner.instance().getBean(StorageService.class);

	}

	@WebApi(path="storages-query")
	public void handleStoragesQuery(HttpServerExchange exchange) throws BaseException {

		// the model and view
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.STG_FND);
		svcctx.addOperationPredicates(paramap);
		
		if(LOGGER.isDebugEnabled()) {
			LOGGER.debug("params {}" , paramap);
		}
		ActionResult result = null;
		String namecond = Filters.filterString(paramap, "keyword");
		String type = Filters.filterAll(paramap, "type");
		String state = Filters.filterAll(paramap, "state");
		
		
		PageQuery pquery = Filters.filterPageQuery(paramap);
		if(pquery != null && pquery.getOrderBy().indexOf("name") > 0){
			pquery.setOrderBy("storage_name");
		}
		if(pquery != null && pquery.getOrderBy().indexOf("type") > 0){
			pquery.setOrderBy("storage_type");
		}

		String[] types = null;
		if(Strings.isNullOrEmpty(type)){
			types = new String[]{
					StorageType.DISK.name(),
					StorageType.MINIO.name()
				};
		}else{
			types = new String[]{type};
		}
		String[] states = null;
		if(Strings.isNullOrEmpty(state)){
			states = new String[]{
					StorageState.CLOSE.name(),
					StorageState.FULL.name(),
					StorageState.OPEN.name()};
		}else{
			states = new String[]{state};
		}
		Map<String,String> parmap = Maps.newHashMap();
		parmap.put("storagename",namecond);		
		svcctx.addOperationPredicates(parmap);
		
		List<StorageInfo> infoList =  storageService.getStorages( namecond, types, states, pquery);	
		List<Map<String, Object>> rows = infoList.stream().map((sinfo) -> {
			DataBuilder builder = new DataBuilder();
			builder.set("storage_id", sinfo.getId().toString());
			
			builder.set(sinfo, "storage_name","state","storage_type","description", "is_default");
			
			builder.set("capacity", String.valueOf(sinfo.getCapacity()/(1024L*1024L)));
			builder.set("used", String.valueOf(sinfo.getUsed()/(1024L*1024L)));
			int percent = (int)((double)sinfo.getUsed()/(double)sinfo.getCapacity()*100);
			builder.set("percent", percent);
			
			Map<String, Object> setting = StorageSetting.parseSetting(sinfo.getSettingJson()).asMap();
			builder.set("store_path", Filters.filterString(setting, SettingKeys.StorePath));
			builder.set("hdfs_host", Filters.filterString(setting, SettingKeys.AccessKey));
			builder.set("hdfs_port", Filters.filterString(setting, SettingKeys.AccessSecret));
			
			return builder.build();
		}).collect(Collectors.toList());
    
		result = ActionResult.success(getMessage(exchange, "mesg.find.storage"));
		result.setData(pquery == null ? null : pquery.getPagination(), rows);

		this.sendResult(exchange, result);
	}

	@WebApi(path="storage-add")
	public void handleNewStorage(HttpServerExchange exchange)throws BaseException{
		
		Map<String, Object> params = this.getRequestBody(exchange);
		// read trace information
		Principal principal = this.getPrincipal(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.STG_NEW);
		svcctx.addOperationPredicates(params);
		
		// prepare result
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.new.storage"));
		
		StorageInfo stgInfo = new StorageInfo();
		long capacity = Filters.filterLong(params, "capacity");
		stgInfo.setCapacity(capacity*1024L*1024L);
		stgInfo.setDescription(Filters.filterString(params, "description"));
		// convert setting into json string
		Map<String, Object> setting = new HashMap<String, Object>();
		setting.put(SettingKeys.StorePath, Filters.filterString(params, "store_path"));
		setting.put(SettingKeys.AccessKey, Filters.filterString(params, "hdfs_host"));
		setting.put(SettingKeys.AccessSecret, Filters.filterString(params, "hdfs_port"));

		StorageSetting storageSetting = new StorageSetting(StorageType.MINIO, setting);
		// try to save setting
		stgInfo.setSettingJson(storageSetting.asJson());
		stgInfo.setState(Filters.filterString(params, "state"));
		stgInfo.setStorageType(Filters.filterString(params, "storage_type"));
		stgInfo.setStorageName(Filters.filterString(params, "storage_name"));
		stgInfo.setUsed(0l);

		stgInfo.setIsDefault(Filters.filterBoolean(params, "is_default"));

		InfoId stgId = null;
		svcctx.addOperationPredicates(stgInfo);
		if(!IdKeys.isValidId(stgInfo.getInfoId())){
			stgId = IdKeys.newInfoId(MasterIdKey.STORAGE);
			stgInfo.setInfoId(stgId);
		}else{
			stgId = stgInfo.getInfoId();
		}
		svcctx.setOperationObject(stgId);
		storageService.newStorage(svcctx, stgInfo);
			
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="storage-save")
	public void handleSaveStorage(HttpServerExchange exchange)throws BaseException{

		// read parameter
		Map<String, Object> params = this.getRequestBody(exchange);
		
		// read trace information
		Principal principal = this.getPrincipal(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.STG_UPD);
		svcctx.addOperationPredicates(params);
		
		// prepare result
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.save.storage"));
		
		StorageInfo sinfo = new StorageInfo();
		
		InfoId infoid = IdKeys.getInfoId(MasterIdKey.STORAGE, Filters.filterLong(params, "storage_id"));
		sinfo.setInfoId(infoid);
		long capacity = Filters.filterLong(params, "capacity");
		sinfo.setCapacity(capacity*1024L*1024L);
		sinfo.setDescription(Filters.filterString(params, "description"));
		// convert setting into json string
		Map<String, Object> setting = new HashMap<String, Object>();
		setting.put(SettingKeys.StorePath, Filters.filterString(params, "store_path"));
		setting.put(SettingKeys.AccessKey, Filters.filterString(params, "hdfs_host"));
		setting.put(SettingKeys.AccessSecret, Filters.filterString(params, "hdfs_port"));

		StorageSetting storageSetting = new StorageSetting(StorageType.MINIO, setting);
		// try to save setting
		sinfo.setSettingJson(storageSetting.asJson());
		
		sinfo.setState(Filters.filterString(params, "state"));
		sinfo.setStorageType(Filters.filterString(params, "storage_type"));
		sinfo.setStorageName(Filters.filterString(params, "storage_name"));
		sinfo.setIsDefault(Filters.filterBoolean(params, "is_default"));

		if(!IdKeys.isValidId(sinfo.getInfoId())){
			ServiceException cexcp = new ServiceException(principal.getLocale(), "excp.save.storage");
			cexcp.addValidateMessage(ValidateMessage.newMessage("storageid", "mesg.prop.miss"));
			throw cexcp;
		}
		
		svcctx.setOperationObject(sinfo.getInfoId());
		svcctx.addOperationPredicates(sinfo);
		storageService.updateStorage(svcctx, sinfo);

		this.sendResult(exchange, result);
	}
	
	@WebApi(path="storage-remove")
	public void handleRemoveStorage(HttpServerExchange exchange)throws BaseException {
		
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.STG_RMV);
		svcctx.addOperationPredicates(paramap);
		
		long storageId = Filters.filterLong(paramap, "storage_id");
		
		ActionResult result = new ActionResult();
		
		InfoId sid = IdKeys.getInfoId(MasterIdKey.STORAGE, storageId);
		if(Storages.DEFAULT_STORAGE_ID == sid.getId()){
			
			result = ActionResult.failure(getMessage(exchange, "mesg.rsrv.storage"));
			this.sendResult(exchange, result);
			return ;
		}
	
		svcctx.setOperationObject(sid);
		storageService.removeStorage( sid);
			
		result = ActionResult.success(getMessage(exchange, "mesg.remove.storage"));

		this.sendResult(exchange, result);
	}
	
	@WebApi(path="storage-info")
	public void handleGetStorage(HttpServerExchange exchange)throws BaseException {
			
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);
		svcctx.setOperation(Operations.STG_INF);
		svcctx.addOperationPredicates(paramap);
		
		long storageId = Filters.filterLong(paramap, "storage_id");
	
		ActionResult result = new ActionResult();
		
		InfoId sid = IdKeys.getInfoId(MasterIdKey.STORAGE , storageId);

		svcctx.setOperationObject(sid);
		StorageInfo sinfo =  storageService.getStorage( sid);
		DataBuilder builder = new DataBuilder();
		builder.set("storage_id", sinfo.getId().toString());
		
		builder.set(sinfo, "storage_name","state","storage_type","description", "is_default");

		builder.set("capacity", String.valueOf(sinfo.getCapacity()/(1024L*1024L)));
		builder.set("used", String.valueOf(sinfo.getUsed()/(1024L*1024L)));
		int percent = (int)((double)sinfo.getUsed()/(double)sinfo.getCapacity()*100);
		builder.set("percent", percent);
		
		Map<String, Object> setting = StorageSetting.parseSetting(sinfo.getSettingJson()).asMap();
		builder.set("store_path", Filters.filterString(setting, SettingKeys.StorePath));
		builder.set("hdfs_host", Filters.filterString(setting, SettingKeys.AccessKey));
		builder.set("hdfs_port", Filters.filterString(setting, SettingKeys.AccessSecret));
		
		result = ActionResult.success(getMessage(exchange, "mesg.find.storage"));
		result.setData(builder.build());
		
		this.sendResult(exchange, result);
	}
}