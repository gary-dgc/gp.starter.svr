/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.topic;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.gp.bind.BindScanner;
import com.gp.common.Channels.BindScope;
import com.gp.common.Channels.BindState;
import com.gp.common.Filters;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.LocalDates;
import com.gp.common.NodeIdKey;
import com.gp.common.Operations;
import com.gp.common.ServiceContext;
import com.gp.dao.info.ChannelBindInfo;
import com.gp.dao.info.ChannelInfo;
import com.gp.dao.info.StorageInfo;
import com.gp.exception.BaseException;
import com.gp.info.DataBuilder;
import com.gp.info.Principal;
import com.gp.pagination.PageQuery;
import com.gp.svc.ChannelService;
import com.gp.svc.CommonService;
import com.gp.svc.master.StorageService;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import com.gp.web.api.ServiceApiHelper;

import io.undertow.server.HttpServerExchange;

public class ChannelHandler extends BaseApiSupport {

	static Logger LOGGER = LoggerFactory.getLogger(ChannelHandler.class);
	
	private CommonService commonService;
	private ChannelService channelService;
	private StorageService storageService;
	
	public ChannelHandler() {
		
		commonService = BindScanner.instance().getBean(CommonService.class);
		channelService = BindScanner.instance().getBean(ChannelService.class);
		storageService = BindScanner.instance().getBean(StorageService.class);
		
	}

	@WebApi(path="channels-query", operation="chnl:fnd")
	public void handleChannelsQuery(HttpServerExchange exchange) throws BaseException {
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.channel"));
		
		Map<String, Object> params = this.getRequestBody(exchange);
		PageQuery pquery = Filters.filterPageQuery(params);
		LOGGER.debug("params {}" , params);
		String keyword = Filters.filterString(params, "keyword");
		String state = Filters.filterString(params, "state");
		String type = Filters.filterAll(params, "type");
		List<String> scope = Filters.filterList(params, "scope", String.class);
		
		List<ChannelInfo> channels = channelService.getChannels(keyword, state, type, scope, pquery );
		
		List<Object> data = Lists.newArrayList();
		for(ChannelInfo info : channels) {
			
			DataBuilder builder = new DataBuilder();
			builder.set("channel_id", info.getId().toString());
			
			builder.set(info, "channel_name", "channel_type", "description", "state");
			
			String avatar = info.getAvatarUrl();
			avatar = ServiceApiHelper.absoluteBinaryUrl(avatar);
			builder.set("avatar_url", avatar);
			
			builder.set("channel_code", info.getTraceCode());
			
			builder.set("contribute_on", info.getPublishOn());
			
			builder.set("create_time", info.getCreateTime().getTime());
			
			builder.set("manager", sbuilder -> {
				sbuilder.set("user_id", String.valueOf(info.getManagerUid()));
				
				sbuilder.set("user_gid", info.getProperty("manager_gid"));
				sbuilder.set("username", info.getProperty("manager_username"));
				
				sbuilder.set("full_name", info.getProperty("manager_full_name"));
				sbuilder.set("nickname", info.getProperty("manager_nickname"));
				
				String avatarUrl = info.getProperty("manager_avatar_url", String.class);
				avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
				sbuilder.set("avatar_url", avatarUrl);
			});
			
			List<ChannelBindInfo> pubs = channelService.getChannelBinds(info.getInfoId());
			
			List<Map<String, Object>> scopes = pubs.stream().map(p -> { 
				
				Map<String, Object> bind = Maps.newHashMap();
				bind.put("scope", p.getBindScope());
				bind.put("state", p.getState());
				bind.put("publish_on", p.getPublishOn());
				
				return bind;
			}).collect(Collectors.toList());
			
			builder.set("scopes", scopes);
			
			data.add(builder.build());
		}
		
		result.setData(pquery == null ? null : pquery.getPagination(), data);
		
		this.sendResult(exchange, result);
		
	}

	/**
	 * channel add the scopes parameter indicates if bind it externally or not.
	 *  
	 **/
	@WebApi(path="channel-add")
	public void handleChannelAdd(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		LOGGER.debug("params {}" , params);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.CHNL_NEW);
		
		ChannelInfo info = new ChannelInfo();
		
		info.setChannelName(Filters.filterString(params, "channel_name"));
		info.setChannelType(Filters.filterString(params, "channel_type"));
		info.setDescription(Filters.filterString(params, "description"));
		
		List<String> scopes = Filters.filterList(params, "scopes", String.class);
		
		if(null != scopes) {
			for(String s: scopes) {
				if(Objects.equals(s, BindScope.CENTER.name())) {
					
					info.setProperty("center_bind", true);
				} else if(Objects.equals(s, BindScope.GLOBAL.name())) {
					
					info.setProperty("global_bind", true);
				}
			}
		}
		
		String avatarUrl = Filters.filterString(params, "avatar_url");
		if(!Strings.isNullOrEmpty(avatarUrl) && avatarUrl.startsWith("data:image/")){
			StorageInfo defaultStg = storageService.getDefaultStorage();
			// process the avatar base64 image
			avatarUrl = ServiceApiHelper.instance().cacheAvatar(defaultStg.getInfoId(), avatarUrl);
			
			info.setAvatarUrl(avatarUrl);
			
		}else {
			String relativeUrl = ServiceApiHelper.instance().relativeBinaryUrl(avatarUrl);
			
			info.setAvatarUrl(relativeUrl);
		}
		
		info.setCreateTime(LocalDates.now());
		Principal princ = svcctx.getPrincipal();
		info.setManagerUid(princ.getUserId().getId());
		
		channelService.newChannel(svcctx, info);
		InfoId cId = info.getInfoId();
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.new.channel"));
		Map<String, Object> data = Maps.newHashMap();
		data.put("channel_id", cId.getId().toString());
		result.setData(data);
		
		svcctx.setOperationObject(cId);
		svcctx.addOperationPredicates(info);
		svcctx.addOperationPredicate("scopes", scopes);
		
		this.sendResult(exchange, result);
		
	}
	
	/**
	 * Bind the the channel to external channels in center or global scope. 
	 **/
	@WebApi(path="channel-bind")
	public void handleChannelBind(HttpServerExchange exchange) throws BaseException {
		// the model and view
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.CHNL_BND);
		
		String channelCode = Filters.filterString(params, "channel_code");
		Long chnlId = Filters.filterLong(params, "channel_id");
		
		BindScope scope = Filters.filterEnum(params, "scope", BindScope.class);
		BindState state = Filters.filterEnum(params, "state", BindState.class);
		Boolean publishOn = Filters.filterBoolean(params, "publish_on");
		
		InfoId chnlKey = null;
		if(chnlId > 0) {
			chnlKey = IdKeys.getInfoId(NodeIdKey.CHANNEL, chnlId);
		}else {
			chnlKey = commonService.queryInfoId(channelCode);
		}
		
		channelService.bindChannel(svcctx, chnlKey, scope, state, publishOn);
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.save.channel"));
		
		// prepare the sync payload data
		svcctx.setOperationObject(chnlKey);
		params.put("scopes", Lists.newArrayList(scope.name()));
		svcctx.addOperationPredicates(params);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="channel-save")
	public void handleChannelSave(HttpServerExchange exchange) throws BaseException {
		// the model and view
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.CHNL_UPD);
		
		String channelCode = Filters.filterString(params, "channel_code");
		long mgruid = Filters.filterLong(params, "manager_uid");
		InfoId chnlId = commonService.queryInfoId(channelCode);
		
		ChannelInfo info = new ChannelInfo();
		info.setInfoId(chnlId);
		info.setChannelName(Filters.filterString(params, "channel_name"));
		info.setChannelType(Filters.filterString(params, "channel_type"));
		info.setDescription(Filters.filterString(params, "description"));
			
		String avatarUrl = Filters.filterString(params, "avatar_url");
		if(!Strings.isNullOrEmpty(avatarUrl) && avatarUrl.startsWith("data:image/")){
			StorageInfo defaultStg = storageService.getDefaultStorage();
			// process the avatar base64 image
			avatarUrl = ServiceApiHelper.instance().cacheAvatar(defaultStg.getInfoId(), avatarUrl);
			
			info.setAvatarUrl(avatarUrl);
			
		}else if(!Strings.isNullOrEmpty(avatarUrl)) {
			String relativeUrl = ServiceApiHelper.instance().relativeBinaryUrl(avatarUrl);
			
			info.setAvatarUrl(relativeUrl);
		}
		
		if(mgruid > 0) {
			info.setManagerUid(mgruid);
		}
		channelService.updateChannel(svcctx, info);
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.save.channel"));
		Map<String, Object> data = Maps.newHashMap();
		data.put("channel_id", chnlId.getId().toString());
		
		result.setData(data);
		svcctx.setOperationObject(chnlId);
		svcctx.addOperationPredicates(info);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="channel-remove")
	public void handleChannelRemove(HttpServerExchange exchange) throws BaseException {
		// the model and view
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.CHNL_RMV);
		
		Long chnlid = Filters.filterLong(params, "channel_id");
		InfoId chnlKey = IdKeys.getInfoId(NodeIdKey.CHANNEL, chnlid);
		
		if(!IdKeys.isValidId(chnlKey)) {
			String channelCode = Filters.filterString(params, "channel_code");
			chnlKey = commonService.queryInfoId(channelCode);
		}
				
		channelService.removeChannel(svcctx, chnlKey);
		svcctx.setOperationObject(chnlKey);
		svcctx.addOperationPredicates(params);
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.remove.channel"));
		
		this.sendResult(exchange, result);
	}
}
