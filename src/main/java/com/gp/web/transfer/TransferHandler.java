/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.transfer;

import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.gp.bind.BindScanner;
import com.gp.cab.CabBinManager;
import com.gp.common.*;
import com.gp.common.Binaries.BinaryMode;
import com.gp.dao.info.CabFileInfo;
import com.gp.exception.BaseException;
import com.gp.info.BaseIdKey;
import com.gp.svc.cab.CabFileService;
import com.gp.svc.cab.CabinetService;
import com.gp.svc.master.StorageService;
import com.gp.util.ImageUtils;
import com.gp.util.NumberUtils;
import com.gp.web.ActionResult;
import com.gp.web.api.ServiceApiHelper;
import com.gp.web.util.WebUtils;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormData.FormValue;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static io.undertow.server.handlers.form.FormDataParser.FORM_DATA;

public class TransferHandler extends TransferSupport{

	public static final String FILE_UPLOAD_PATH = "/upload";
	public static final String FILE_CACHE_DIR = GeneralConfig.getStringByKeys("file", "cache.path");
	public static final String FILE_UPLOAD_DIR = GeneralConfig.getStringByKeys("file", "upload.path");
	
	InputStream DefaultImg = null;
	
	private CabFileService fileSvc;
	private StorageService storageSvc;
	private CabinetService cabinetSvc;
	
	@Override
	public CabFileService getCabFileService() {
		return fileSvc;
	}
	
	@Override
	public StorageService getStorageService(){
		return this.storageSvc;
	}
	
	@Override
	public CabinetService getCabinetService(){
		return this.cabinetSvc;
	}
	
	public TransferHandler() {
		
		fileSvc = BindScanner.instance().getBean(CabFileService.class);
		storageSvc = BindScanner.instance().getBean(StorageService.class);
		cabinetSvc = BindScanner.instance().getBean(CabinetService.class);
		
		this.setPathMapping(BinaryMode.FILE.uri() + "/*", Methods.GET, this::handleGet);
		this.setPathMapping(BinaryMode.BINARY.uri() + "/*", Methods.GET, this::handleGet);
		this.setPathMapping(BinaryMode.IMAGE.uri() + "/*", Methods.GET, this::handleGet);
		this.setPathMapping(BinaryMode.AVATAR.uri() + "/*", Methods.GET, this::handleGet);
		this.setPathMapping(BinaryMode.ATTACH.uri() + "/*", Methods.GET, this::handleGet);
		
		this.setPathMapping(FILE_UPLOAD_PATH + "/*", Methods.POST, this::handlePost);
		this.setPathMapping(FILE_UPLOAD_PATH , Methods.POST, this::handlePost);
		
		DefaultImg = this.getClass().getResourceAsStream("/static/default.gif");
	}
	
	/**
	 * Process GET request.
	 * request example
	 * <ul>
	 * 	<li>http://xxx.com/file/928282727272766225.doc</li>
	 * 	<li>http://xxx.com/image/928282727272766225.png</li>
	 * 	<li>http://xxx.com/binary/928282727272766225.doc</li>
	 * 	<li>http://xxx.com/avatar/928282727272766225.jpg</li>
	 * 	<li>http://xxx.com/attach/928282727272766225.doc?category=topic</li>
	 * </ul>
	 * @see HttpServlet#doGet(HttpServletRequest, HttpServletResponse).
	 */
	public void handleGet(HttpServerExchange exchange)throws Exception{

		String reqUri = exchange.getRequestPath();
		String extension = Files.getFileExtension(reqUri);
		
		if(reqUri.startsWith(BinaryMode.FILE.uri())) {
			String fileIdStr = reqUri.substring(reqUri.lastIndexOf('/') + 1, reqUri.lastIndexOf('.'));
			Long fileId = NumberUtils.toLong(fileIdStr);
			InfoId fileid = IdKeys.getInfoId( MasterIdKey.CAB_FILE, fileId);
			
			//GeneralResult<CabFileInfo> gresult = CabinetFacade.findCabinetFile(accesspoint, principal, sourceId,fileid);
			CabFileInfo cabfile = fileSvc.getFile( fileid);
			// Check if file is actually supplied to the request URL.
			if (cabfile == null) {
				// Do your thing if the file is not supplied to the request URL.
				// Throw an exception, or send 404, or show default/warning page, or
				// just ignore it.
				exchange.setStatusCode(StatusCodes.NOT_FOUND);
				exchange.endExchange();
				return;
			}
			
			InfoId binaryId = IdKeys.getInfoId(BaseIdKey.BINARY, cabfile.getBinaryId());
			
			processBinary(exchange, cabfile.getEntryName(), binaryId);
		
		} else if(reqUri.startsWith(BinaryMode.BINARY.uri()) || reqUri.startsWith(BinaryMode.ATTACH.uri())) {
			
			String binIdStr = reqUri.substring(reqUri.lastIndexOf('/') + 1, reqUri.lastIndexOf('.'));
			Long binaryId = NumberUtils.toLong(binIdStr);
			InfoId binId = IdKeys.getInfoId(BaseIdKey.BINARY, binaryId);
			
			processBinary(exchange, 
					reqUri.substring( reqUri.lastIndexOf('/')+1, reqUri.length() ), 
					binId);
			
		
		} else if(reqUri.startsWith(BinaryMode.IMAGE.uri()) || reqUri.startsWith(BinaryMode.AVATAR.uri())) {
			
			String binIdStr = reqUri.substring(reqUri.lastIndexOf('/') + 1, reqUri.lastIndexOf('.'));
			InfoId binaryId = IdKeys.getInfoId(BaseIdKey.BINARY, NumberUtils.toLong(binIdStr));
			String cacheFileName = Binaries.getBinaryHashPath(binaryId, extension);
			
			try {
				File cache = new File(FILE_CACHE_DIR + File.separator + cacheFileName);
				if(!cache.exists()) {
					
					cache.getParentFile().mkdirs();
					
					OutputStream output = new FileOutputStream(cache);
					CabBinManager.instance().dumpBinary(binaryId, output);
				}
				
				WebUtils.writeImage(exchange, cache, false);
			
			} catch (BaseException e) {
				LOGGER.error("Fail process[GET] method", e);
				// when error case, we try to write default image back to client
				WebUtils.writeImage(exchange, cacheFileName, DefaultImg, false);
			}
			
		} else {
			
			ActionResult result = ActionResult.error("target binary resource not exist");
			sendResult(exchange, result, StatusCodes.NOT_FOUND);
		}
	}
	
	/**
	 * Handle the post request with form data which include the file.
	 * This method handle one file per time.
	 * 
	 * @param sym_token the symmetric token
	 * @param mode the mode the data: avatar / binary
	 * 
	 **/
	public void handlePost(HttpServerExchange exchange)throws Exception{

		FormData formData = exchange.getAttachment(FORM_DATA);
		// Iterate through form data
		List<FormValue> fileItems = Lists.newLinkedList();
        for (String data : formData) {
            for (FormValue formValue : formData.get(data)) {
                if (formValue.isFileItem()) {
                    // Process file here
                	fileItems.add(formValue);
                } 
            }
        }
        Map<String, Object> params = this.getRequestBody(exchange);
		String symToken = (String)params.get("token");
		if(null == symToken ) {
    		ActionResult result = ActionResult.error("interim token is required");
			sendResult(exchange, result, StatusCodes.PRECONDITION_FAILED);
    		return; // not a valid token 
    	}
		if(fileItems.isEmpty() || fileItems.size() > 1) {
			
			ActionResult result = ActionResult.error("binary data miss or support only one");
			sendResult(exchange, result, StatusCodes.BAD_REQUEST);
    		return; // not a valid token 
		}
		
		FormValue fileValue = fileItems.get(0);
		String fileName = fileValue.getFileName();
		String mimeType = MimeTypes.getMimeType(fileName);
		Map<String, String> partMap  = ServiceApiHelper.instance().getTokenOrigin(symToken);
		Optional<BinaryMode> optional = Enums.getIfPresent(BinaryMode.class, partMap.get("mode"));
		String extension = Files.getFileExtension(fileName);
	
		if(BinaryMode.AVATAR == optional.get()) {

			if(!mimeType.startsWith("image") || (!mimeType.contains("png") && !mimeType.contains("jpeg"))) {
				ActionResult result = ActionResult.error("avatar need image file:" + fileName);
				sendResult(exchange, result, StatusCodes.BAD_REQUEST);
	    		return; // not a valid image
			}
	
			BufferedImage srcimg = ImageIO.read(fileValue.getFileItem().getInputStream());
			
			// generate an image id and save it to cache.
			InfoId blindId = IdKeys.newBlindInfoId();
			
			String cacheFileName = Binaries.getBinaryHashPath(blindId, extension);
			ImageUtils.write(srcimg, Paths.get(FILE_UPLOAD_DIR, cacheFileName).toString(), extension);
			
			ActionResult result = ActionResult.success("success cache the file");
		
			InfoId binaryId = createBinary(symToken, fileName, Paths.get(FILE_UPLOAD_DIR, cacheFileName).toString());
			
			Map<String, String> data = Maps.newHashMap();
			data.put("binary_uri", ServiceApiHelper.instance().absoluteBinaryUrl(BinaryMode.AVATAR, binaryId, extension));
			data.put("binary_id", binaryId.getId().toString());
			
			result.setData(data);
			
			sendResult(exchange, result, StatusCodes.OK);
			
		} else if(BinaryMode.BINARY == optional.get()) {
		
			ActionResult result = ActionResult.success("success cache the file");
			
			InfoId binaryId = createBinary(symToken, fileName, fileValue.getFileItem().getFile().toString());
			
			Map<String, String> data = Maps.newHashMap();
			data.put("binary_uri", ServiceApiHelper.instance().absoluteBinaryUrl(BinaryMode.BINARY, binaryId, extension));
			data.put("binary_id", binaryId.getId().toString());
			
			result.setData(data);
			
			sendResult(exchange, result, StatusCodes.OK);
			
		} else if(BinaryMode.IMAGE == optional.get()) {
		    
			ActionResult result = ActionResult.success("success cache the file");
		
			InfoId binaryId = createBinary(symToken, fileName, fileValue.getFileItem().getFile().toString());
			
			Map<String, String> data = Maps.newHashMap();
			data.put("image_uri", ServiceApiHelper.instance().absoluteBinaryUrl(BinaryMode.IMAGE, binaryId, extension));
			data.put("image_id", binaryId.getId().toString());
			
			result.setData(data);
		
			sendResult(exchange, result, StatusCodes.OK);
			
		}else if(BinaryMode.FILE == optional.get()) {

			ActionResult result = ActionResult.success("success cache the file");
		
			KVPair<InfoId, InfoId> rtv = createFile(symToken, fileName, fileValue.getFileItem().getFile().toString());

			Map<String, String> data = Maps.newHashMap();
			data.put("file_uri", ServiceApiHelper.instance().absoluteBinaryUrl(BinaryMode.FILE, rtv.getKey(), extension));
			data.put("file_id", rtv.getKey().getId().toString());
			
			data.put("binary_uri", ServiceApiHelper.instance().absoluteBinaryUrl(BinaryMode.BINARY, rtv.getValue(), extension));
			data.put("binary_id", rtv.getValue().getId().toString());
			
			result.setData(data);
			
			sendResult(exchange, result, StatusCodes.OK);
			
		}
	}
	
}
