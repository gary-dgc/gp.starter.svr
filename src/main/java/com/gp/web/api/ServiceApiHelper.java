package com.gp.web.api;

import com.google.common.base.*;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.gp.bind.BindScanner;
import com.gp.common.*;
import com.gp.common.Binaries.BinaryMode;
import com.gp.dao.info.StorageInfo;
import com.gp.dao.info.SysOptionInfo;
import com.gp.exception.BaseException;
import com.gp.cab.CabBinManager;
import com.gp.svc.SystemService;
import com.gp.svc.master.StorageService;
import com.gp.util.ImageUtils;
import com.gp.web.InterimToken;

import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

public class ServiceApiHelper {
	
	public static String FILE_CACHE_DIR = GeneralConfig.getStringByKeys("file", "cache.path");

	public static String FILE_ACCESS_URL   = "";
	
	private static ServiceApiHelper Instance;
	
	private static InterimToken SYM_TOKEN;

	private SystemService systemService;
	private StorageService storageService;
	
	private ServiceApiHelper(){
		this.initial();
		Instance = this;
	}

	private void initial() {
	
		systemService = BindScanner.instance().getBean(SystemService.class);
		storageService = BindScanner.instance().getBean(StorageService.class);
		
		SysOptionInfo fileOptInfo = systemService.getOption("file.access");
		FILE_ACCESS_URL = fileOptInfo.getOptValue();

	}
	
	/**
	 * The singleton instance 
	 **/
	public static ServiceApiHelper instance() {
		
		if(null == Instance) {
			new ServiceApiHelper();
		}
		return Instance;
	}
	
	/**
	 * cache the base64 image into the cache path, eg. {cache dir}/{img path}/123-20160201-123213.jpg
	 * 
	 * @return String the file name in cache folder, which include hash path and binary id.
	 **/
	public String cacheAvatar(String base64Img) throws BaseException{
		
		StorageInfo defaultStg = Instance.storageService.getDefaultStorage();
		return cacheAvatar(defaultStg.getInfoId(), base64Img);
		
	}
	
	/**
	 * cache the base64 image into the cache path, eg. {cache dir}/{img path}/123-20160201-123213.jpg
	 * 
	 * @param base64Img the base64 image string, data:image/png;base64,seYsxdYJJLddxd....
	 * 
	 * @return String the file name in cache folder, which include hash path and binary id.
	 **/
	public String cacheAvatar(InfoId storageId, String base64Img) throws BaseException{
		
		int pos = base64Img.indexOf(';');
		
		String prefix = base64Img.substring(0, pos);
		pos = prefix.indexOf(GeneralConsts.SLASH_SEPARATOR);
		String ext = prefix.substring(pos + 1); // get file extension
    	
		InfoId binaryId = Instance.storageService.newBinary(ServiceContext.getPseudoContext(), storageId, ext);
		
    	String cacheFileName = Binaries.getBinaryHashPath(binaryId, ext);
    	BufferedImage bufImg = ImageUtils.read(base64Img);
    	String cacheFile = Paths.get(FILE_CACHE_DIR , cacheFileName).toString();
    	ImageUtils.write(bufImg, cacheFile, ext);
    	
    	InputStream source;
		try {
			source = new FileInputStream(cacheFile);
			CabBinManager.instance().fillBinary(binaryId, source);
			
		} catch (FileNotFoundException e) {
			throw new BaseException("excp.cache.avatar");
		}
		
		return BinaryMode.AVATAR.uri().substring(1) + "/" + binaryId.getId().toString() + "." + ext;

	}
	
	/**
	 * Convert the relative URL into absolute one. 
	 * 
	 * @param relativeUrl the relative url
	 **/
	public static String absoluteBinaryUrl(String relativeUrl) {
		
		String rtvUrl = Strings.nullToEmpty(relativeUrl);
		if(rtvUrl.startsWith("http") || Strings.isNullOrEmpty(relativeUrl)) 
			return rtvUrl;
		
		return FILE_ACCESS_URL + "/" + rtvUrl;
	}
	
	/**
	 * Convert the relative URL into absolute one. 
	 * 
	 * @param mode the mode of url : /binary; /avatar ; /file
	 * @param binaryId the binary id
	 * @param extension the file extension
	 * 
	 **/
	public String absoluteBinaryUrl(BinaryMode mode, InfoId binaryId, String extension) {
	
		return FILE_ACCESS_URL + mode.uri() + "/" + binaryId.getId().toString() 
				+ (Strings.isNullOrEmpty(extension) ? "" : "." + extension);
	}
	
	/**
	 * Convert the absolute URL into relative one. 
	 * 
	 * http:lslsl.com/image/3412341234.png -> image/3412341234.png
	 * 
	 * @param absoluteUrl the image relative url path. http:lslsl.com/image/3412341234.png
	 **/
	public String relativeBinaryUrl(String absoluteUrl) {
		
		if(Strings.isNullOrEmpty(absoluteUrl)) 
			return "";
		
		EnumSet<BinaryMode> set = EnumSet.allOf(BinaryMode.class);
		
		KVPair<String, String> urlHolder = KVPair.newPair("url");
		set.forEach(mode -> {
			if(!Strings.isNullOrEmpty(urlHolder.getValue())) {
				return;
			}
			int pos = 0;
			pos = absoluteUrl.indexOf(mode.uri());
			if(pos >= 0) {
				urlHolder.setValue(absoluteUrl.substring(pos + 1));
			}
		});
		
		return Strings.isNullOrEmpty(urlHolder.getValue()) ? absoluteUrl : urlHolder.getValue();
	}
	
	/**
	 * Encrypt the string contents with symmetric algorithm
	 * 
	 * @param mode  the token mode
	 * @param parts the token parts to be encrypted
	 * 
	 * @return String the interim token
	 * 
	 **/
	public String getInterimToken(BinaryMode mode, String ... parts) {
		Preconditions.checkArgument(parts != null && parts.length > 0, "Token content is required");

		if(null == SYM_TOKEN) {
			SYM_TOKEN = new InterimToken(GroupUsers.anonymous(), GeneralConfig.getStringByKeys("web", "interim.key"));
		}
		List<String> items = Lists.newArrayList(parts);
		items.add(0, mode.name());
		String partsStr = Joiner.on(GeneralConsts.NAMES_SEPARATOR).join(items);
		
		return SYM_TOKEN.encrypt(partsStr);
	}

	/**
	 * Get the original value of symmetric token string
	 * 
	 * @param token the symmetric token string
	 * 
	 * @return String the original value, with keys: mode, cabinet_id, folder_pid, user_id, username
	 * 
	 **/
	public Map<String, String> getTokenOrigin(String token) {
		
		Preconditions.checkArgument(!Strings.isNullOrEmpty(token), "Token content is required");
		if(null == SYM_TOKEN) {
			SYM_TOKEN = new InterimToken(GroupUsers.anonymous(), GeneralConfig.getStringByKeys("web", "interim.key"));
		}
		String partsStr = SYM_TOKEN.decrypt(token);
		
		List<String> parts = Splitter.on(GeneralConsts.NAMES_SEPARATOR).splitToList(partsStr);
		Map<String, String> rtv = Maps.newHashMap();
		Optional<BinaryMode> optional = Enums.getIfPresent(BinaryMode.class, Strings.nullToEmpty(parts.get(0)));
		if(BinaryMode.FILE == optional.get()) {
			
			rtv.put("mode", parts.get(0));
			rtv.put("cabinet_id", parts.get(1));
			rtv.put("folder_pid", parts.get(2));
			rtv.put("user_id", parts.get(3));
			rtv.put("username", parts.get(4));
		} else {
			
			rtv.put("mode", parts.get(0));
			rtv.put("cabinet_id", parts.get(1));
			rtv.put("user_id", parts.get(2));
			rtv.put("username", parts.get(3));
		}
		return rtv;
	}

}