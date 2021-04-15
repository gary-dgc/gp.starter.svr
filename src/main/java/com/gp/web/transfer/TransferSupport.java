/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.transfer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.protobuf.ServiceException;
import com.gp.acl.Acl;
import com.gp.common.Cabinets;
import com.gp.common.GroupUsers;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.KeyValuePair;
import com.gp.common.MimeTypes;
import com.gp.common.NodeIdKey;
import com.gp.common.ServiceContext;
import com.gp.common.Sources;
import com.gp.common.VersionEvolver.Part;
import com.gp.dao.info.BinaryInfo;
import com.gp.dao.info.CabFileInfo;
import com.gp.dao.info.CabinetInfo;
import com.gp.exception.CoreException;
import com.gp.exception.BaseException;
import com.gp.info.BaseIdKey;
import com.gp.storage.BinaryManager;
import com.gp.storage.ContentRange;
import com.gp.svc.cab.CabFileService;
import com.gp.svc.cab.CabinetService;
import com.gp.svc.master.StorageService;
import com.gp.util.NumberUtils;
import com.gp.web.BaseApiSupport;
import com.gp.web.api.ServiceApiHelper;
import com.networknt.httpstring.ContentType;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.DateUtils;
import io.undertow.util.ETag;
import io.undertow.util.ETagUtils;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

public abstract class TransferSupport extends BaseApiSupport {

	static Logger LOGGER = LoggerFactory.getLogger(TransferSupport.class);
	
	private static final int DEFAULT_BUFFER_SIZE = 10240; // ..bytes = 10KB.
	private static final long DEFAULT_EXPIRE_TIME = 604800000L; // ..ms = 1 week.
	private static final String MULTIPART_BOUNDARY = "MULTIPART_BYTERANGES";
	private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

	/**
	 * Date formats as specified in the HTTP RFC.
	 * @see <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.1">Section 7.1.1.1 of RFC 7231</a>
	 */
	private static final String[] DATE_FORMATS = new String[] {
			"EEE, dd MMM yyyy HH:mm:ss zzz",
			"EEE, dd-MMM-yy HH:mm:ss zzz",
			"EEE MMM dd HH:mm:ss yyyy"
	};
	
	abstract CabFileService getCabFileService();
	abstract StorageService getStorageService();
	abstract CabinetService getCabinetService();
	
	/**
	 * Create binary record under cabinet without file entry record
	 * 
	 * @param symToken the symmetric token
	 * @param filename the filename of binary
	 * @param filePath the file cache path of binary data
	 * 
	 **/
	public InfoId createBinary(String symToken, String filename, String filePath) throws Exception{
		//{cabinet id}.{user id}.{user name}
		Map<String, String> parts  = ServiceApiHelper.instance().getTokenOrigin(symToken);
		
		String extension = Files.getFileExtension(filename);
		
		long cabinetId = NumberUtils.toLong(parts.get("cabinet_id"));
		InfoId binaryId = IdKeys.newInfoId(BaseIdKey.BINARY);
		BinaryInfo binfo = new BinaryInfo();
		
		InfoId cabid = IdKeys.getInfoId(NodeIdKey.CABINET, cabinetId);
		CabinetInfo cabinfo = getCabinetService().getCabinet(cabid);
		
		File srcFile = new File(filePath);
		
		binfo.setInfoId(binaryId);
		binfo.setSourceId(Sources.LOCAL_INST_ID.getId());		
		binfo.setStorageId(cabinfo.getStorageId());		
		binfo.setFormat(extension);
		binfo.setSize(Math.toIntExact(srcFile.length()));
		binfo.setState(Cabinets.FileState.READY.name());
		binfo.setCreatorUid(GroupUsers.ANONY_UID.getId());
		binfo.setCreateTime(new Date());
		
		ServiceContext svcctx  = ServiceContext.getPseudoContext();
		
		getStorageService().newBinary(svcctx, binfo);
		
		try (InputStream source = new FileInputStream(srcFile)){
			
			BinaryManager.instance().fillBinary(binfo.getInfoId(), source);
		} catch (IOException e) {
			LOGGER.error("Fail to write binary file", e);
			throw new ServiceException("excp.file.bin");
		}
		
		return binaryId;
	}
	
	/**
	 * Create a file in cabinet folder, at this time the binary is uploaded to temporary folder.
	 * here a binary and a cab file records are create in database. then remove the temporary file 
	 * to storage folder.
	 * 
	 * @param symToken the symmetric token
	 * @param filename the filename of binary
	 * @param filePath the file cache path of binary data
	 * 
	 **/
	public KeyValuePair<InfoId, InfoId> createFile(String symToken, String fileName, String filePath) throws Exception{
		
		KeyValuePair<InfoId, InfoId> rtv = KeyValuePair.newPair();

		// {cabinet id}.{folder id}.{user id}.{user name}
		Map<String, String> parts  = ServiceApiHelper.instance().getTokenOrigin(symToken);
	
		long cabinetId = NumberUtils.toLong(parts.get("cabinet_id"));
		long folderPid = NumberUtils.toLong(parts.get("folder_pid"));
		
		InfoId binaryId = IdKeys.newInfoId(BaseIdKey.BINARY);
		rtv.setValue(binaryId);
		BinaryInfo binfo = new BinaryInfo();
		
		CabFileInfo fileinfo = new CabFileInfo();
	
		InfoId cabid = IdKeys.getInfoId(NodeIdKey.CABINET,cabinetId);
		CabinetInfo cabinfo = getCabinetService().getCabinet(cabid);
		
		File srcFile = new File(filePath);
		
		binfo.setInfoId(binaryId);
		binfo.setSourceId(Sources.LOCAL_INST_ID.getId());		
		binfo.setStorageId(cabinfo.getStorageId());		
		binfo.setFormat(Files.getFileExtension(fileName));
		binfo.setSize(Math.toIntExact(srcFile.length()));
		binfo.setState(Cabinets.FileState.READY.name());
		binfo.setCreatorUid(GroupUsers.ANONY_UID.getId());
		binfo.setCreateTime(new Date());
		
		fileinfo.setCabinetId(cabinetId);
		fileinfo.setSourceId(Sources.LOCAL_INST_ID.getId());
		fileinfo.setBinaryId(binfo.getId()); // Set the binary id
		fileinfo.setCommentOn(false);
		fileinfo.setParentId(folderPid);
		fileinfo.setFormat(binfo.getFormat());
		fileinfo.setOwnerUid(GroupUsers.ANONY_UID.getId());
		fileinfo.setState(Cabinets.FileState.READY.name());
		fileinfo.setSize(Math.toIntExact(srcFile.length()));
		fileinfo.setEntryName(fileName);
		
		fileinfo.setVersion("1.0");
		fileinfo.setCreateTime(new Date());
		ServiceContext svcctx  = ServiceContext.getPseudoContext();
		getStorageService().newBinary(svcctx, binfo);
		
		try (InputStream source = new FileInputStream(srcFile)){
			
			BinaryManager.instance().fillBinary(binfo.getInfoId(), source);
		} catch (IOException e) {
			LOGGER.error("Fail to write binary file", e);
			throw new ServiceException("excp.file.bin");
		}
		
		Acl acl =  Cabinets.getDefaultAcl();
		acl.setAclId(IdKeys.newInfoId(NodeIdKey.CAB_ACL));
		
		getCabFileService().newFile(svcctx, fileinfo, acl, Part.MAJOR);
		rtv.setKey(fileinfo.getInfoId());
	
		return rtv;
	}
	
	/**
	 * Process the actual request.
	 * 
	 * @param request
	 *            The request to be processed.
	 * @param response
	 *            The response to be created.
	 * @param content
	 *            Whether the request body should be written (GET) or not
	 *            (HEAD).
	 * @throws IOException
	 *             If something fails at I/O level.
	 */
	public void processBinary(HttpServerExchange exchange, String fileName, InfoId binaryId) throws Exception{

		BinaryInfo binfo = getStorageService().getBinary(binaryId);
		// Prepare some variables. The ETag is an unique identifier of the file.
		fileName = encodeFileName(exchange, fileName);
		long length = binfo.getSize();
		/*****last modified time should be retrieved from binary record***/
		long lastModified = binfo.getModifyTime().getTime();
		ETag etag = new ETag(false, fileName + "_" + length + "_" + lastModified);
		
		// Validate request headers for caching
		// ---------------------------------------------------

		// If-None-Match header should contain "*" or ETag. If so, then return
		// 304.
		if (!ETagUtils.handleIfNoneMatch(exchange, etag, false)) {
			exchange.getResponseHeaders().add(Headers.ETAG, etag.toString()); // Required in 304.
			this.send(exchange, StatusCodes.NOT_MODIFIED, null);
			return;
		}
		
		// If-Modified-Since header should be greater than LastModified. If so,
		// then return 304.
		// This header is ignored if any If-None-Match header is specified.
		if (!DateUtils.handleIfModifiedSince(exchange, binfo.getModifyTime())) {
			exchange.getResponseHeaders().add(Headers.ETAG, etag.toString()); // Required in 304.
			this.send(exchange, StatusCodes.NOT_MODIFIED, null);
			return;
		}

		// Validate request headers for resume
		// ----------------------------------------------------
		// If-Match header should contain "*" or ETag. If not, then return 412.
		if (!ETagUtils.handleIfMatch(exchange, etag, false)) {
			this.send(exchange, StatusCodes.PRECONDITION_FAILED, null);
			return;
		}

		// If-Unmodified-Since header should be greater than LastModified. If
		// not, then return 412.
		if (!DateUtils.handleIfUnmodifiedSince(exchange, binfo.getModifyTime())) {
			this.send(exchange, StatusCodes.PRECONDITION_FAILED, null);
			return;
		}

		// Validate and process range
		// -------------------------------------------------------------
		// Prepare some variables. The full Range represents the complete file.
		ContentRange full = new ContentRange(0l, length - 1, length);
		List<ContentRange> ranges = Lists.newArrayList();

		// Validate and process Range and If-Range headers.
		String range = exchange.getRequestHeaders().getFirst(Headers.RANGE);
		if (range != null) {

			// Range header should match format "bytes=n-n,n-n,n-n...". If not, then return 416.
			if (!range.matches("^bytes=\\d*-\\d*(,\\d*-\\d*)*$")) {
				exchange.getResponseHeaders().add(Headers.CONTENT_RANGE, "bytes */" + length); // Required in 416.
			
				this.send(exchange, StatusCodes.REQUEST_RANGE_NOT_SATISFIABLE, null);
				return;
			}

			// If-Range header should either match ETag or be greater then LastModified. If not, then return full file.
			String ifRange = exchange.getRequestHeaders().getFirst(Headers.IF_RANGE);
			if (ifRange != null && !ifRange.equals(etag.toString())) {
				try {
				
					long ifRangeTime = parseDateHeader(ifRange); // Throws IAE if invalid.
					if (ifRangeTime != -1 && ifRangeTime + 1000 < lastModified) {
						ranges.add(full);
					}
				} catch (IllegalArgumentException ignore) {
					ranges.add(full);
				}
			}

			// If any valid If-Range header, then process each part of byte range.
			if (ranges.isEmpty()) {
				for (String part : range.substring(6).split(",")) {
					// Assuming a file with length of 100, the following
					// examples returns bytes at: 50-80 (50 to 80), 40- (40 to length=100), -20 (length-20=80 to length=100).
					long start = sublong(part, 0, part.indexOf("-"));
					long end = sublong(part, part.indexOf("-") + 1, part.length());

					if (start == -1) {
						start = length - end;
						end = length - 1;
					} else if (end == -1 || end > length - 1) {
						end = length - 1;
					}

					// Check if Range is syntactically valid. If not, then return 416.
					if (start > end) {
						exchange.getResponseHeaders().add(Headers.CONTENT_RANGE, "bytes */" + length); // Required in 416.
						this.send(exchange, StatusCodes.REQUEST_RANGE_NOT_SATISFIABLE, null);
						return;
					}

					// Add range.
					ranges.add(new ContentRange(start, end, length));
				}
			}
		}

		// Prepare and initialize response
		// --------------------------------------------------------

		// Get content type by file name and set default GZIP support and
		// content disposition.
		// String contentType = request.getServletContext().getMimeType(fileName);
		String contentType = MimeTypes.getMimeType(fileName);
		boolean acceptsGzip = false;
		String disposition = "inline";

		// If content type is unknown, then set the default value.
		// For all content types, see:
		// http://www.w3schools.com/media/media_mimeref.asp
		// To add new content types, add new mime-mapping entry in web.xml.
		if (contentType == null) {
			contentType = "application/octet-stream";
		}

		// If content type is text, then determine whether GZIP content encoding
		// is supported by
		// the browser and expand content type with the one and right character
		// encoding.
		if (contentType.startsWith("text")) {
			
			String acceptEncoding = exchange.getRequestHeaders().getFirst(Headers.ACCEPT_ENCODING);
			acceptsGzip = acceptEncoding != null && accepts(acceptEncoding, "gzip");
			contentType += ";charset=UTF-8";
		}

		// Else, expect for images, determine content disposition. If content
		// type is supported by
		// the browser, then set to inline, else attachment which will pop a
		// 'save as' dialogue.
		else if (!contentType.startsWith("image")) {
			String accept = exchange.getRequestHeaders().getFirst(Headers.ACCEPT);
			disposition = accept != null && accepts(accept, contentType) ? "inline" : "attachment";
		}

		// Initialize response.
		// response.reset();
		// response.setBufferSize(DEFAULT_BUFFER_SIZE);
		exchange.getResponseHeaders().add(Headers.CONTENT_DISPOSITION, disposition + ";filename=\"" + fileName + "\"");
		exchange.getResponseHeaders().add(Headers.ACCEPT_RANGES, "bytes");
		exchange.getResponseHeaders().add(Headers.ETAG, etag.toString());
		exchange.getResponseHeaders().add(Headers.LAST_MODIFIED, DateUtils.toDateString(new Date(lastModified)));
		exchange.getResponseHeaders().add(Headers.EXPIRES, DateUtils.toDateString(new Date(System.currentTimeMillis() + DEFAULT_EXPIRE_TIME)));
		// Send requested file (part(s)) to client
		// ------------------------------------------------
		exchange.startBlocking();
		// Prepare streams.
		OutputStream output = null;

		// Open streams.
		// input = new RandomAccessFile(new File(""), "r");
		output = exchange.getOutputStream();
		if (ranges.isEmpty() || ranges.get(0) == full) {

			// Return full file.
			ContentRange r = full;
			exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, contentType);
			exchange.getResponseHeaders().put(Headers.CONTENT_RANGE, "bytes " + r.getStartPos() + "-" + r.getEndPos() + "/" + r.getFileSize());
			
			if (acceptsGzip) {
				// The browser accepts GZIP, so GZIP the content.
				exchange.getResponseHeaders().put(Headers.CONTENT_ENCODING, "gzip");
				output = new GZIPOutputStream(output, DEFAULT_BUFFER_SIZE);
			} else {
				// Content length is not directly predictable in case of
				// GZIP.
				// So only add it if there is no means of GZIP, else
				// browser will hang.
				exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, String.valueOf(r.getRangeLength()));
			}

			// Copy full range.
			// copy(input, output, r.getStartPos(), r.getRangeLength());
			try {
				BinaryManager.instance().dumpBinary(binaryId, output);
			} catch (BaseException e) {
				LOGGER.error("fail dump whole binary", e);
				throw new CoreException(e, "excp.fail.dump");
			}
			

		} else if (ranges.size() == 1) {

			// Return single part of file.
			ContentRange r = ranges.get(0);
			exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, contentType);
			exchange.getResponseHeaders().put(Headers.CONTENT_RANGE,  "bytes " + r.getStartPos() + "-" + r.getEndPos() + "/" + r.getFileSize());
			exchange.getResponseHeaders().put(Headers.CONTENT_LENGTH, String.valueOf(r.getRangeLength()));
			exchange.setStatusCode(StatusCodes.PARTIAL_CONTENT); // 206.
			
			// Copy single part range.
			// copy(input, output, r.getStartPos(), r.getRangeLength());
			try {
				BinaryManager.instance().dumpBinary(binaryId, output);
			} catch (BaseException e) {
				LOGGER.error("fail dump whole binary", e);
				throw new CoreException(e, "excp.fail.dump");
			}
			
		} else {

			// Return multiple parts of file.
			exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "multipart/byteranges; boundary=" + MULTIPART_BOUNDARY);
			exchange.setStatusCode(StatusCodes.PARTIAL_CONTENT); // 206.
			
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(output));
			// Copy multi part range.
			for (ContentRange r : ranges) {
				// Add multipart boundary and header fields for every
				// range.
				bw.newLine();
				bw.write( "--" + MULTIPART_BOUNDARY);
				bw.newLine();
				bw.write( "Content-Type: " + contentType);
				bw.newLine();
				bw.write( "Content-Range: bytes " + r.getStartPos() + "-" + r.getEndPos() + "/" + r.getFileSize());
				bw.newLine();
				// Copy single part range of multi part range.
				// copy(input, output, r.getStartPos(), r.getRangeLength());
				try {
					BinaryManager.instance().dumpBinary(binaryId, r,  output);
				} catch (BaseException e) {
					LOGGER.error("fail dump multi-parts binary", e);
					throw new CoreException(e, "excp.fail.dump");
				}
			}

			// End with multipart boundary.
			bw.newLine();
			bw.write(  "--" + MULTIPART_BOUNDARY + "--");
			bw.newLine();
			
		}

	}
	
	private void send(HttpServerExchange exchange, int status, String body) {
		exchange.setStatusCode(status);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, ContentType.ANY_TYPE.toString());
        if(Strings.isNullOrEmpty(body)) {
        	exchange.endExchange();
        }else {
        	exchange.getResponseSender().send(body);
        }
	}
	
	private String encodeFileName(HttpServerExchange exchange, String fileName) {
		
		String browser = getBrowserName(exchange);
		if (browser.startsWith("ie")) {  
			// IE & EDGE browser
			try {
				fileName = URLEncoder.encode(fileName, Charsets.UTF_8.name());
			} catch (UnsupportedEncodingException e) {
				LOGGER.error("fail encode file name", e);
			}  
		} else {  
			// other browsers
			fileName = new String(fileName.getBytes(Charsets.UTF_8), Charsets.ISO_8859_1);  
		}
		return fileName;
	}
	
	private long parseDateHeader(String value) {
		for (String dateFormat : DATE_FORMATS) {
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat, Locale.US);
			simpleDateFormat.setTimeZone(GMT);
			try {
				return simpleDateFormat.parse(value).getTime();
			}
			catch (ParseException ex) {
				// ignore
			}
		}
		return -1;
	}
	
	/**
	  * Get the browser name 
	  *
	  * @param agent
	  * @return
	  */
	private String getBrowserName(HttpServerExchange exchange) {
		
		String agent=exchange.getRequestHeaders().getFirst(Headers.USER_AGENT_STRING).toLowerCase();
		
		if (agent.indexOf("msie 7") > 0) {
			return "ie7";
		} else if (agent.indexOf("msie 8") > 0) {
			return "ie8";
		} else if (agent.indexOf("msie 9") > 0) {
			return "ie9";
		} else if (agent.indexOf("msie 10") > 0) {
			return "ie10";
		} else if (agent.indexOf("msie") > 0) {
			return "ie";
		} else if (agent.indexOf("opera") > 0) {
			return "opera";
		} else if (agent.indexOf("opera") > 0) {
			return "opera";
		} else if (agent.indexOf("firefox") > 0) {
			return "firefox";
		} else if (agent.indexOf("webkit") > 0) {
			return "webkit";
		} else if (agent.indexOf("gecko") > 0 && agent.indexOf("rv:11") > 0) {
			return "ie11";
		} else {
			return "Others";
		}
	}

	/**
	 * Returns a substring of the given string value from the given begin index
	 * to the given end index as a long. If the substring is empty, then -1 will
	 * be returned
	 * 
	 * @param value
	 *            The string value to return a substring as long for.
	 * @param beginIndex
	 *            The begin index of the substring to be returned as long.
	 * @param endIndex
	 *            The end index of the substring to be returned as long.
	 * @return A substring of the given string value as long or -1 if substring
	 *         is empty.
	 */
	private long sublong(String value, int beginIndex, int endIndex) {
		String substring = value.substring(beginIndex, endIndex);
		return (substring.length() > 0) ? Long.parseLong(substring) : -1;
	}

	/**
	 * Returns true if the given accept header accepts the given value.
	 * 
	 * @param acceptHeader
	 *            The accept header.
	 * @param toAccept
	 *            The value to be accepted.
	 * @return True if the given accept header accepts the given value.
	 */
	private boolean accepts(String acceptHeader, String toAccept) {
		String[] acceptValues = acceptHeader.split("\\s*(,|;)\\s*");
		Arrays.sort(acceptValues);
		return Arrays.binarySearch(acceptValues, toAccept) > -1
				|| Arrays.binarySearch(acceptValues, toAccept.replaceAll("/.*$", "/*")) > -1
				|| Arrays.binarySearch(acceptValues, "*/*") > -1;
	}

}
