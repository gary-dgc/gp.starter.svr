/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.repo;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.gp.bind.BindScanner;
import com.gp.common.Binaries;
import com.gp.common.Binaries.BinaryMode;
import com.gp.common.Cabinets;
import com.gp.common.Filters;
import com.gp.common.GeneralConstants;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.NodeIdKey;
import com.gp.common.Operations;
import com.gp.common.ServiceContext;
import com.gp.common.VersionEvolver.Part;
import com.gp.dao.info.CabEntryInfo;
import com.gp.dao.info.CabFileInfo;
import com.gp.dao.info.CabFolderInfo;
import com.gp.exception.BaseException;
import com.gp.info.BaseIdKey;
import com.gp.info.DataBuilder;
import com.gp.info.Principal;
import com.gp.svc.CommonService;
import com.gp.svc.cab.CabFileService;
import com.gp.svc.cab.CabFolderService;
import com.gp.svc.cab.CabinetService;
import com.gp.svc.user.FavoriteService;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import com.gp.web.api.ServiceApiHelper;

import io.undertow.server.HttpServerExchange;

public class RepoEntryHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(RepoEntryHandler.class);
	
	private CabinetService cabinetSvc;
	private CabFileService fileSvc;
	private CabFolderService folderSvc;
	private CommonService commonSvc;
	private FavoriteService favoriteSvc;
	
	public RepoEntryHandler() {
		
		cabinetSvc = BindScanner.instance().getBean(CabinetService.class);
		fileSvc = BindScanner.instance().getBean(CabFileService.class);
		folderSvc = BindScanner.instance().getBean(CabFolderService.class);
		commonSvc = BindScanner.instance().getBean(CommonService.class);
		favoriteSvc = BindScanner.instance().getBean(FavoriteService.class);
	}

	/**
	 * Identify the cabinet type 
	 **/
	@WebApi(path="cabinet-locate", operation="cab:loc")
	public void handleCabinetLocate(HttpServerExchange exchange) throws BaseException{
		
		Map<String, Object> params = this.getRequestBody(exchange);
		
		ActionResult result = null;
		
		long wgroupId = Filters.filterLong(params, "workgroup_id");
		long userId = Filters.filterLong(params, "user_id");
		long manageId = Filters.filterLong(params, "manage_id");
	
		Map<String, String> map = Maps.newHashMap();
	
		if(wgroupId > 0) {
			InfoId wId = IdKeys.getInfoId(NodeIdKey.WORKGROUP, wgroupId);
			Long cabId = commonSvc.queryColumn(wId, "cabinet_id", Long.class);
			map.put("cabinet_type", Cabinets.CabinetType.WORKGROUP.name());
			map.put("cabinet_id", String.valueOf(cabId));
		}
		if(userId > 0) {
			InfoId uId = IdKeys.getInfoId(BaseIdKey.USER, userId);
			Long cabId = commonSvc.queryColumn(uId, "cabinet_id", Long.class);
			map.put("cabinet_type", Cabinets.CabinetType.PERSONAL.name());
			map.put("cabinet_id", String.valueOf(cabId));
		}
		if( manageId > 0) {
			InfoId wId = IdKeys.getInfoId(NodeIdKey.WORKGROUP,manageId);
			Long cabId = commonSvc.queryColumn(wId, "cabinet_id", Long.class);
			if(null != cabId) {
				map.put("cabinet_type", Cabinets.CabinetType.WORKGROUP.name());
				map.put("cabinet_id", String.valueOf(cabId));
			
			}else {
				InfoId uId = IdKeys.getInfoId(BaseIdKey.USER, userId);
				cabId = commonSvc.queryColumn(uId, "cabinet_id", Long.class);
				map.put("cabinet_type", Cabinets.CabinetType.PERSONAL.name());
				map.put("cabinet_id", String.valueOf(cabId));
			}
		}
		
		result = ActionResult.success("success find the cabinet id");
		result.setData(map);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="cabinet-folder-add")
	public void handleFolderAdd(HttpServerExchange exchange) throws BaseException{
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.CAB_FDR_NEW);
		svcctx.addOperationPredicates(params);
		
		ActionResult result = null;
		
		long cabinetId = Filters.filterLong(params, "cabinet_id");
		String folder = Filters.filterString(params, "folder_name");
		long pId = Filters.filterLong(params, "folder_pid");
		String descr = Filters.filterString(params, "description");
		CabFolderInfo finfo = new CabFolderInfo();
		finfo.setEntryName(Strings.isNullOrEmpty(folder) ? "New folder" : folder);
		finfo.setTotalSize(0);
		finfo.setState(Cabinets.FolderState.READY.name());
		finfo.setDescription(descr);
					
		if(cabinetId > 0){
			finfo.setCabinetId(cabinetId);
		}else {
			svcctx.abort();
		}
		
		if(pId > 0) {
			finfo.setParentId(pId);
		}
		
		InfoId fileId =  folderSvc.newFolder(svcctx, finfo, Cabinets.getDefaultAcl());
		
		if(IdKeys.isValidId(fileId)) {
			result = ActionResult.success("success create folder") ;
			DataBuilder builder = new DataBuilder();
			builder.set("entry_id", fileId.getId().toString());
			builder.set("parent_id", finfo.getParentId().toString());
			builder.set("entry_name", finfo.getEntryName());
			
			result.setData(builder.build());
			
		}else {
			result = ActionResult.failure("fail to create folder");
		}
		
		this.sendResult(exchange, result);
		
	}
	
	@WebApi(path="cabinet-file-list")
	public void handleFileList(HttpServerExchange exchange) throws BaseException{
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.CAB_FND);
		svcctx.addOperationPredicates(params);
		
		ActionResult result = null;
	
		long cabinetId = Filters.filterLong(params, "cabinet_id");
		long fldrId = Filters.filterLong(params, "folder_id");
	
		InfoId manageId = null;
		if(cabinetId > 0){
			manageId = IdKeys.getInfoId(NodeIdKey.CABINET, cabinetId);
		}else {
			svcctx.abort();
		}
		InfoId folderId = null;
		if(fldrId > 0) {
			folderId = IdKeys.getInfoId(NodeIdKey.CAB_FOLDER, fldrId);
		}
		else {
			folderId = IdKeys.getInfoId(NodeIdKey.CAB_FOLDER, GeneralConstants.FOLDER_ROOT);
		}
		
		/**
		 * Check the user is a workgroup member
		 **/
		List<CabFileInfo> infos = cabinetSvc.getCabFiles(svcctx.getPrincipal().getUserId(), manageId, folderId, "", null);
	
		List<Map<String, Object>> data = infos.stream().map((CabFileInfo info) -> {
			
			DataBuilder builder = new DataBuilder();
			builder.set("entry_id", info.getId().toString());
			builder.set("entry_type", info.isFolder() ? Cabinets.EntryType.FOLDER.name() : Cabinets.EntryType.FILE.name());
			builder.set(info, "entry_name", "state", "description", "classification", "format");
			
			if(!info.isFolder()) {
				CabFileInfo finfo = (CabFileInfo) info;
				String accessFile = Binaries.getBinaryHashPath(finfo.getInfoId(), finfo.getFormat());
				accessFile = ServiceApiHelper.absoluteBinaryUrl(accessFile);
				builder.set("access_url", accessFile);
			}
			builder.set("parent_id", info.getParentId().toString());
			builder.set("create_time", info.getCreateTime().getTime());
	
			return builder.build();
		}).collect(Collectors.toList());
			
		result = ActionResult.success("excp.find.entries");
		result.setData(data);
		
		this.sendResult(exchange, result);
		
	}
	
	@WebApi(path="cabinet-folder-list", operation="cab:fnd")
	public void handleFolderList(HttpServerExchange exchange) throws BaseException{
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);

		svcctx.addOperationPredicates(params);
		
		ActionResult result = null;
	
		long cabinetId = Filters.filterLong(params, "cabinet_id");
		long fldrId = Filters.filterLong(params, "folder_id");
	
		InfoId manageId = null;
		if(cabinetId > 0){
			manageId = IdKeys.getInfoId(NodeIdKey.CABINET, cabinetId);
		}else {
			svcctx.abort();
		}
		InfoId folderId = null;
		if(fldrId > 0) {
			folderId = IdKeys.getInfoId(NodeIdKey.CAB_FOLDER, fldrId);
		}
		else {
			folderId = IdKeys.getInfoId(NodeIdKey.CAB_FOLDER, GeneralConstants.FOLDER_ROOT);
		}
		
		/**
		 * Check the user is a workgroup member
		 **/
		List<CabFolderInfo> infos = cabinetSvc.getCabFolders(svcctx.getPrincipal().getUserId(), manageId, folderId, "", null);
	
		List<Map<String, Object>> data = infos.stream().map((CabEntryInfo info) -> {
			
			DataBuilder builder = new DataBuilder();
			builder.set("entry_id", info.getId().toString());
			builder.set("entry_type", info.isFolder() ? Cabinets.EntryType.FOLDER.name() : Cabinets.EntryType.FILE.name());
			builder.set(info, "entry_name", "state", "description", "classification");
			builder.set("parent_id", info.getParentId().toString());
			builder.set("create_time", info.getCreateTime().getTime());
	
			return builder.build();
		}).collect(Collectors.toList());
			
		result = ActionResult.success("excp.find.entries");
		result.setData(data);
		
		this.sendResult(exchange, result);
		
	}

	@WebApi(path="cabinet-entry-list", operation="cab:fnd")
	public void handleEntryList(HttpServerExchange exchange) throws BaseException{
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);

		svcctx.addOperationPredicates(params);
		
		ActionResult result = null;
	
		long cabinetId = Filters.filterLong(params, "cabinet_id");
		long fldrId = Filters.filterLong(params, "folder_id");
	
		InfoId manageId = null;
		if(cabinetId > 0){
			manageId = IdKeys.getInfoId(NodeIdKey.CABINET, cabinetId);
		}else {
			svcctx.abort("excp.miss.cab_id");
		}
		InfoId folderId = null;
		if(fldrId > 0) {
			folderId = IdKeys.getInfoId(NodeIdKey.CAB_FOLDER, fldrId);
		}
		else {
			folderId = IdKeys.getInfoId(NodeIdKey.CAB_FOLDER, GeneralConstants.FOLDER_ROOT);
		}
		
		/**
		 * Check the user is a workgroup member
		 **/
		List<CabEntryInfo> infos = cabinetSvc.getCabEntries(svcctx.getPrincipal().getUserId(), manageId, folderId, "", null);
		
		Set<InfoId> rescIds = Sets.newHashSet();
		List<Map<String, Object>> data = infos.stream().map((CabEntryInfo info) -> {
			
			DataBuilder builder = new DataBuilder();
			builder.set("entry_id", info.getId().toString());
			builder.set("entry_type", info.isFolder() ? Cabinets.EntryType.FOLDER.name() : Cabinets.EntryType.FILE.name());
			builder.set(info, "entry_name", "state", "description", "classification");
			
			
			if(!info.isFolder()) {
				CabFileInfo finfo = (CabFileInfo) info;
				
				String accessFile = ServiceApiHelper.instance().absoluteBinaryUrl(BinaryMode.FILE, finfo.getInfoId(), finfo.getFormat());
				builder.set("access_url", accessFile);
				
				builder.set("format", finfo.getFormat());
			}
			builder.set("parent_id", info.getParentId().toString());
			builder.set("create_time", info.getCreateTime().getTime());
			
			// reserve the resource id
			rescIds.add(info.getInfoId());
			
			builder.set("info_id", info.getInfoId());
			
			return builder.build();
		}).collect(Collectors.toList());
		
		// detect favorites
		Set<InfoId> favResIds = favoriteSvc.detectFavorites(svcctx.getPrincipal().getUserId().getId(), rescIds);
		
		// set favorite property
		data.forEach(e -> {
			// fetch info id
			InfoId infoId = (InfoId)e.remove("info_id");
			
			// check favorite or not
			e.put("favorite", favResIds.contains(infoId));
		});
		
		result = ActionResult.success("excp.find.entries");
		result.setData(data);
		
		this.sendResult(exchange, result);
		
	}
	
	@WebApi(path="cabinet-entry-info", operation="cab:fnd")
	public void handleEntryInfo(HttpServerExchange exchange) throws BaseException{
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);

		svcctx.addOperationPredicates(params);
		
		ActionResult result = null;
	
		long cabinetId = Filters.filterLong(params, "cabinet_id");
		long fldrId = Filters.filterLong(params, "entry_id");
	
		InfoId manageId = null;
		if(cabinetId > 0){
			manageId = IdKeys.getInfoId(NodeIdKey.CABINET, cabinetId);
		}else {
			svcctx.abort("excp.miss.cab_id");
		}
		InfoId folderId = null;
		if(fldrId > 0) {
			folderId = IdKeys.getInfoId(NodeIdKey.CAB_FOLDER, fldrId);
		}
		else {
			folderId = IdKeys.getInfoId(NodeIdKey.CAB_FOLDER, GeneralConstants.FOLDER_ROOT);
		}
		
		/**
		 * Check the user is a workgroup member
		 **/
		CabEntryInfo info = cabinetSvc.getCabEntry(svcctx.getPrincipal().getUserId(), manageId, folderId);
	
		Object data = null;
		if(null != info) {
			DataBuilder builder = new DataBuilder();
			builder.set("entry_id", info.getId().toString());
			builder.set("entry_type", info.isFolder() ? Cabinets.EntryType.FOLDER.name() : Cabinets.EntryType.FILE.name());
			builder.set(info, "entry_name", "state", "description", "classification");
			
			
			if(!info.isFolder()) {
				CabFileInfo finfo = (CabFileInfo) info;
				
				String accessFile = ServiceApiHelper.instance().absoluteBinaryUrl(BinaryMode.FILE, finfo.getInfoId(), finfo.getFormat());
				builder.set("access_url", accessFile);
				
				builder.set("format", finfo.getFormat());
			}
			builder.set("parent_id", info.getParentId().toString());
			builder.set("create_time", info.getCreateTime().getTime());
	
			data = builder.build();
		}
		
		result = ActionResult.success("mesg.find.entry");
		result.setData(data);
		
		this.sendResult(exchange, result);
		
	}
	
	@WebApi(path="cabinet-file-add")
	public void handleFileAdd(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.CAB_FIL_NEW);
		svcctx.addOperationPredicates(params);
		
		ActionResult result = null;
		Principal principal = this.getPrincipal(exchange);
		
		long folderPid = Filters.filterLong(params, "folder_pid");
		long binaryId = Filters.filterLong(params, "binary_id");
		long cabinetId = Filters.filterLong(params, "cabinet_id");
		String name = Filters.filterString(params, "file_name");
		Part versionPart = Filters.filterEnum(params, "version_part", Part.class);
		if(cabinetId <= 0 || Strings.isNullOrEmpty(name)) {
			result = ActionResult.failure("excp.illegal.param");
			this.sendResult(exchange, result);
			return;
		}
		Long folderId = null;
		if(folderPid >0) {
			folderId = folderPid;
		}
		else {
			folderId = GeneralConstants.FOLDER_ROOT;
		}
		
		CabFileInfo file = new CabFileInfo();
		file.setEntryName(name);
		file.setParentId(folderId);
		file.setCabinetId(cabinetId);
		
		file.setSourceId(principal.getSourceId());
		
		file.setAuthorUid(Filters.filterLong(params, "author_uid"));
		file.setOwnerUid(svcctx.getPrincipal().getUserId().getId());
		file.setBinaryId(binaryId); // Set the binary id
		file.setCommentOn(false);
		file.setDescription(Filters.filterString(params, "description"));
		file.setFormat(Files.getFileExtension(name));
		
		// binary id is valid means the binary data is available, otherwise means file is blank
		if(binaryId > 99L) {
			file.setState(Cabinets.FileState.READY.name());
		}else {
			file.setState(Cabinets.FileState.BLANK.name());
		}
		String version = Filters.filterString(params, "version");
		file.setVersion(version);
		
		String versionLabel = Filters.filterString(params, "version_label");
		file.setVersion(versionLabel);
		
		InfoId fId =  fileSvc.newFile(svcctx, file, Cabinets.getDefaultAcl(), versionPart);
			
		if(IdKeys.isValidId(fId) ) {
			result = ActionResult.success(getMessage(exchange, "mesg.new.file"));
			DataBuilder builder = new DataBuilder();
			builder.set("entry_id", fId.getId().toString());
			builder.set("parent_id", folderId.toString());
			builder.set("entry_name", name);
			
			result.setData(builder.build());
		}else {
			
			ActionResult.success(getMessage(exchange, "excp.new.file"));
		}
				
		this.sendResult(exchange, result);
		
	}
	
	@WebApi(path="cabinet-file-remove")
	public void handleFileRemove(HttpServerExchange exchange) throws BaseException {
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.remove.file"));
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.CAB_FIL_RMV);
		
		Map<String, Object> params = this.getRequestBody(exchange);
		long fileId = Filters.filterLong(params, "file_id");
		if(fileId <= 0){
			result = ActionResult.failure("miss parameters");
			
			this.sendResult(exchange, result);
			return;
		}
		
		InfoId fid = IdKeys.getInfoId(NodeIdKey.CAB_FILE, fileId);
		svcctx.setOperationObject(fid);
		InfoId recycleId = fileSvc.removeFile(svcctx, fid);
		Map<String, Object> data = Maps.newHashMap();
		data.put("recycle_id", recycleId.getId().toString());
		
		result.setData(data);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="cabinet-folder-remove")
	public void handleFolderRemove(HttpServerExchange exchange) throws BaseException {
	
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.remove.folder"));
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.CAB_FDR_RMV);
		
		Map<String, Object> params = this.getRequestBody(exchange);
		long fileId = Filters.filterLong(params, "folder_id");
		if(fileId <= 0){
			result = ActionResult.failure("miss parameters");
			
			this.sendResult(exchange, result);
			return;
		} 
		
		InfoId fid = IdKeys.getInfoId(NodeIdKey.CAB_FOLDER, fileId);
		svcctx.setOperationObject(fid);
		InfoId recycleId = folderSvc.removeFolder(svcctx, fid);
		Map<String, Object> data = Maps.newHashMap();
		data.put("recycle_id", recycleId.getId().toString());
		
		result.setData(data);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="cabinet-file-move")
	public void handleFileMove(HttpServerExchange exchange) throws BaseException {
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.file.move"));
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.CAB_FDR_RMV);
		
		Map<String, Object> params = this.getRequestBody(exchange);
		
		InfoId fileId = Filters.filterInfoId(params, "file_id", NodeIdKey.CAB_FILE);
		InfoId folderId = Filters.filterInfoId(params, "folder_pid", NodeIdKey.CAB_FOLDER);
		
		this.fileSvc.moveFile(svcctx, fileId, folderId);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="cabinet-folder-move")
	public void handleFolderMove(HttpServerExchange exchange) throws BaseException {
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.folder.move"));
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.CAB_FDR_RMV);
		
		Map<String, Object> params = this.getRequestBody(exchange);
		
		InfoId fileId = Filters.filterInfoId(params, "folder_id", NodeIdKey.CAB_FOLDER);
		InfoId folderId = Filters.filterInfoId(params, "folder_pid", NodeIdKey.CAB_FOLDER);
		
		this.folderSvc.moveFolder(svcctx, fileId, folderId);
		
		this.sendResult(exchange, result);
		
	}
	
	@WebApi(path="cabinet-file-copy")
	public void handleFileCopy(HttpServerExchange exchange) throws BaseException {
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.file.move"));
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.CAB_FDR_RMV);
		
		Map<String, Object> params = this.getRequestBody(exchange);
		
		InfoId fileId = Filters.filterInfoId(params, "file_id", NodeIdKey.CAB_FILE);
		InfoId folderId = Filters.filterInfoId(params, "folder_pid", NodeIdKey.CAB_FOLDER);
		
		this.fileSvc.copyFile(svcctx, fileId, folderId);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="cabinet-folder-copy")
	public void handleFolderCopy(HttpServerExchange exchange) throws BaseException {
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.folder.move"));
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.CAB_FDR_RMV);
		
		Map<String, Object> params = this.getRequestBody(exchange);
		
		InfoId fileId = Filters.filterInfoId(params, "folder_id", NodeIdKey.CAB_FOLDER);
		InfoId folderId = Filters.filterInfoId(params, "folder_pid", NodeIdKey.CAB_FOLDER);
		
		this.folderSvc.copyFolder(svcctx, fileId, folderId);
		
		this.sendResult(exchange, result);
		
	}
	
	@WebApi(path="cabinet-folder-paths")
	public void handleFolderPaths(HttpServerExchange exchange) throws BaseException {
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.folder.paths"));
		
		Map<String, Object> params = this.getRequestBody(exchange);
		
		long fldrId = Filters.filterLong(params, "folder_id");
	
		InfoId folderId = null;
		if(fldrId > 0) {
			folderId = IdKeys.getInfoId(NodeIdKey.CAB_FOLDER, fldrId);
		}
		else {
			folderId = IdKeys.getInfoId(NodeIdKey.CAB_FOLDER, GeneralConstants.FOLDER_ROOT);
		}
		
		List<CabFolderInfo> infos = folderSvc.getFolerPaths(folderId);
		Collections.reverse(infos);
		
		List<Object> data = infos.stream().map(info -> {
			
			DataBuilder builder = new DataBuilder();
			
			builder.set("entry_id", info.getId().toString());
			
			builder.set("parent_id", info.getParentId().toString());
			builder.set("entry_name", info.getEntryName());
					
			return builder.build();
		}).collect(Collectors.toList());
		
		result.setData(data);
		
		this.sendResult(exchange, result);
	}
	
	
}
