/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.transfer;

import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.gp.cab.CabBinManager;
import com.gp.cab.CabBinMeta;
import com.gp.cache.CacheManager;
import com.gp.cache.ICache;
import com.gp.common.*;
import com.gp.exception.BaseException;
import com.gp.exception.ServiceException;
import com.gp.info.AccessPoint;
import com.gp.info.BaseIdKey;
import com.gp.info.Principal;
import com.gp.util.NumberUtils;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.api.ServiceApiHelper;
import com.gp.web.model.ResumableInfo;
import com.gp.web.model.ResumableInfo.ChunkNumber;
import com.gp.web.util.WebUtils;
import com.networknt.common.ContentType;
import io.undertow.server.BlockingHttpExchange;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import io.undertow.util.StatusCodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Deque;
import java.util.Map;

public class ResumeHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(ResumeHandler.class);
	public static final String UPLOAD_DIR = GeneralConfig.getStringByKeys("file", "resume.path");
	private static final String CACHE_PREFIX = "TSF";

	public ResumeHandler() {

		this.setPathMapping(WebUtils.getOpenApiUri("resume"), Methods.GET, this::handleGet);
		this.setPathMapping(WebUtils.getOpenApiUri("resume"), Methods.POST, this::handlePost);
	}

	public void handleGet(HttpServerExchange exchange)throws BaseException{
	    
		Map<String, Deque<String>> querys = exchange.getQueryParameters();
    	int resumableChunkNumber = NumberUtils.toInt(querys.get("chunk_number").getFirst(), -1);
    	
    	// Verify the token is valid
    	String symToken = querys.get("sym_token").getFirst();
    	if(null == symToken) {
    		ActionResult result = ActionResult.failure("miss sym_token");
    		this.sendResult(exchange, result, StatusCodes.PRECONDITION_FAILED);
    	
    		return; // not a valid token 
    	}
    	    	
        ResumableInfo info = getResumableInfo(exchange);

        if (info.chunks.contains(new ChunkNumber(resumableChunkNumber))) {
        	//This Chunk has been Uploaded.
//            exchange.setStatusCode(StatusCodes.OK);
//            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, ContentType.ANY_TYPE.toString());
//            exchange.getResponseSender().send("Uploaded");
            this.sendResult(exchange, ActionResult.success("Uploaded"));
        } else {
        	
        	exchange.setStatusCode(StatusCodes.NOT_FOUND);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, ContentType.ANY_TYPE.toString());
            exchange.getResponseSender().send("NotFound");
        }
		
	}
	
	public void handlePost(HttpServerExchange exchange)throws BaseException{
		
		Map<String, Deque<String>> queries = exchange.getQueryParameters();
		String chunknum = queries.get("chunk_number").getFirst();
		int chunkNumber        = NumberUtils.toInt(chunknum, -1);
		
		ServiceContext svcctx = this.getServiceContext(exchange);
    	// Verify the token is valid
		String symToken = queries.get("sym_token").getFirst();

    	if(null == symToken) {
    		ActionResult result = ActionResult.failure("miss sym_token");
    		this.sendResult(exchange, result, StatusCodes.PRECONDITION_FAILED);
    		return; // not a valid token 
    	}
    	
        BlockingHttpExchange bexchange = exchange.startBlocking();
        
        ResumableInfo info = getResumableInfo(exchange);
        try (RandomAccessFile raf = new RandomAccessFile(info.filePath, "rw")) {
	        
	        if(LOGGER.isDebugEnabled()) {
	        	LOGGER.debug("start trying to write: {}", info.filePath);
	        }
	        //Seek to position
	        raf.seek((chunkNumber - 1) * (long)info.chunkSize);
	       	        
	        //Save to file
	        InputStream is = bexchange.getInputStream();
	        long readed = 0;
	        String contentLen = exchange.getRequestHeaders().getFirst(Headers.CONTENT_LENGTH);
	        long content_length = NumberUtils.toLong(contentLen);
	        byte[] bytes = new byte[1024 * 100];
	        while(readed < content_length) {
	            int r = is.read(bytes);
	            if (r < 0)  {
	                break;
	            }
	            raf.write(bytes, 0, r);
	            readed += r;
	        }
	       
        } catch (IOException ioe) {
        	ActionResult result = ActionResult.failure(ioe.getMessage());
    		this.sendResult(exchange, result, StatusCodes.PRECONDITION_FAILED);
    		return;
        } 
        
        //Mark as uploaded.
        ChunkNumber chunk = new ChunkNumber(chunkNumber);
        
        int currentChunkSize   = NumberUtils.toInt(queries.get("current_chunk_size").getFirst(), -1);
        chunk.chunkSize = currentChunkSize;
        info.chunks.add(chunk);
        putResumableInfo(info);
        if (info.checkFinished()) {

        	//Check if all chunks uploaded, and change filename
        	ActionResult result = null;
        	Principal principal = this.getPrincipal(exchange);
        	Map<String, String> parts  = ServiceApiHelper.instance().getTokenOrigin(symToken);
        	long cabinetId = NumberUtils.toLong(parts.get("cabinet_id"));
			long folderPid = NumberUtils.toLong(parts.get("folder_pid"));

			InfoId binaryId = IdKeys.newInfoId(AppIdKey.BINARY);

			CabBinMeta binMeta = new CabBinMeta(binaryId);
			// reserve the cabinet id
			binMeta.setCabinetId(cabinetId);
			binMeta.setFormat(Files.getFileExtension(info.fileName));
			binMeta.setFileName(info.fileName);

			File srcFile = new File(info.filePath);
			binMeta.setSize(srcFile.length());

			if(IdKeys.isValidId(folderPid)){
				CabBinManager.instance().getCabinetService().newFileBinary(binMeta, folderPid);
			}else{
				CabBinManager.instance().getStorageService().newRawBinary(binMeta);
			}

			try (InputStream source = new FileInputStream(srcFile)){
				
				CabBinManager.instance().fillBinary(binaryId, source);
				
				result = ActionResult.success("All finished");
				Map<String, String> data = Maps.newHashMap();
				data.put("binary_id", binaryId.getId().toString());
				result.setData(data);
				
			} catch (IOException e) {
				LOGGER.error("Fail to write binary file", e);
				
				throw new ServiceException("excp.file.bin");
			}
			        	
            this.sendResult(exchange, result);
        } else {
        	this.sendResult(exchange, ActionResult.success("Chunk Uploaded"));
//        	exchange.setStatusCode(StatusCodes.OK);
//          exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, ContentType.ANY_TYPE.toString());
//          exchange.getResponseSender().send();
          
        }
	}
	
	public void putResumableInfo(ResumableInfo resumable){
		
//		String resumableId = CACHE_PREFIX + GeneralConstants.KEYS_SEPARATOR + resumable.identifier;
//		ICache fileCache = CacheManager.instance().getCache(Caches.FILE_CACHE);
		
	}
	
	/**
	 * Get the cached resumable info  
	 **/
    private ResumableInfo getResumableInfo(HttpServerExchange exchange) {
    	
    	Map<String, Deque<String>> queries = exchange.getQueryParameters();
    	
    	String symToken = queries.get("sym_token").getFirst();
        String identifier = queries.get("identifier").getFirst();
        String totalsize = queries.get("total_size").getFirst();
        String chunksize = queries.get("chunk_size").getFirst();
        
         String filename        = queries.get("file_name").getFirst();
        //Here we add a ".temp" to every upload file to indicate NON-FINISHED
        String base_dir = getUserTempPath(symToken);
        new File(base_dir).mkdirs();
        String filePath        = new File(base_dir, filename).getAbsolutePath() + ".temp";

        //ResumableInfo info = UploadHelper.getInstance().getResumableInfo(identifier);
        
		String resumableId = CACHE_PREFIX + GeneralConsts.KEYS_SEPARATOR + identifier;
		ICache fileCache = CacheManager.instance().getCache(Caches.FILE_CACHE);
		ResumableInfo rinfo = null;
		
		try {
			rinfo = (ResumableInfo)fileCache.fetch(resumableId, (String key) -> {
				
				ResumableInfo info = new ResumableInfo();
	                       
	            info.chunkSize     = NumberUtils.toInt(chunksize, -1);
	            info.totalSize     = NumberUtils.toLong(totalsize, -1);
	            info.identifier    = key;
	            info.fileName      = filename;
	            info.filePath      = filePath;
	            
	            return info;
			});
		} catch (Exception e) {
			// ignore
		}
        AccessPoint accesspoint = WebUtils.getAccessPoint(exchange);
        rinfo.accessPoint = accesspoint;
        
        return rinfo;
    }
    
    /**
     * Get the users's hash path under file cache directory base on user id
     * 
     * @param symToken the symmetric token
     * 
     **/
    private String getUserTempPath(String symToken) {
    	// {cabinet id}.{folder id}.{user id}.{user name}
    	Map<String, String> parts = ServiceApiHelper.instance().getTokenOrigin(symToken);
    	HashCode hcode  = Hashing.murmur3_32_fixed().hashLong(NumberUtils.toLong(parts.get("user_id")));
    	
    	StringBuilder hraw = new StringBuilder(hcode.toString());
		for(int i = 3; i > 0; i --) {
			hraw.insert( i * 2, File.separatorChar);
		}
		
		if(UPLOAD_DIR.endsWith(File.separator)) {
			hraw.insert(0, UPLOAD_DIR);
		}else {
			hraw.insert(0, File.separatorChar);
			hraw.insert(0, UPLOAD_DIR);
		}
		return hraw.toString();
    }
}
