/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.user;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.GroupUsers;
import com.gp.common.GroupUsers.InviteMode;
import com.gp.common.GroupUsers.InviteState;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.LocalDates;
import com.gp.common.NodeIdKey;
import com.gp.common.Operations;
import com.gp.common.ServiceContext;
import com.gp.common.Sources;
import com.gp.core.CoreConstants;
import com.gp.core.NodeApiAgent;
import com.gp.dao.info.SourceInfo;
import com.gp.dao.info.UserInfo;
import com.gp.dao.info.UserInviteInfo;
import com.gp.exception.BaseException;
import com.gp.info.BaseIdKey;
import com.gp.info.Principal;
import com.gp.info.TraceCode;
import com.gp.svc.CommonService;
import com.gp.svc.master.SourceService;
import com.gp.svc.user.FollowService;
import com.gp.svc.user.UserService;
import com.gp.svc.wgroup.JoinInviteService;
import com.gp.util.JsonUtils;
import com.gp.validate.ArgsValidator;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import io.undertow.server.HttpServerExchange;

public class UserExtHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(UserExtHandler.class);
	
	private JoinInviteService joinInviteService;
	
	private UserService userService;
	
	private FollowService followService;
	
	private SourceService sourceService;
	
	private CommonService commonService;
	
	public UserExtHandler() {
		
		userService = BindScanner.instance().getBean(UserService.class);
		followService = BindScanner.instance().getBean(FollowService.class);
		joinInviteService = BindScanner.instance().getBean(JoinInviteService.class);
		commonService = BindScanner.instance().getBean(CommonService.class);
		sourceService = BindScanner.instance().getBean(SourceService.class);		
	}

	/**
	 * Favorite work group
	 * 
	 * @param wgroup_id the work group to be added into fav list
	 **/
	@WebApi(path="follow-user")
	public void handleFollowUser(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.FOL_NEW);
		
		ArgsValidator.newValidator(paramap)
			.requireOne("user_id", "user_gid")
			.validate(true);
		
		InfoId userKey = Filters.filterInfoId(paramap, "user_id", BaseIdKey.USER);
		if(!IdKeys.isValidId(userKey)) {
			userKey = commonService.queryInfoId(Filters.filterString(paramap, "user_gid"));
		}
		
		int cnt = followService.followUser(svcctx, userKey);
		
		svcctx.addOperationPredicate("user_id", userKey.getId());
		InfoId followKey = svcctx.getPrincipal().getUserId();
		svcctx.addOperationPredicate("follower_uid", followKey.getId());
		
		ActionResult result = cnt == 0 ? ActionResult.failure("fail follow") : ActionResult.success("success follow");
		
		this.sendResult(exchange, result);
	}

	/**
	 * Favorite work group
	 * 
	 * @param wgroup_id the work group to be added into fav list
	 **/
	@WebApi(path="unfollow-user")
	public void handleUnfollowUser(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.FOL_RMV);
		ArgsValidator.newValidator(paramap)
		.requireOne("user_id", "user_gid")
		.validate(true);
	
		InfoId userKey = Filters.filterInfoId(paramap, "user_id", BaseIdKey.USER);
		if(!IdKeys.isValidId(userKey)) {
			userKey = commonService.queryInfoId(Filters.filterString(paramap, "user_gid"));
		}
		int cnt = followService.unfollowUser(svcctx, userKey);
		
		svcctx.addOperationPredicate("user_id", userKey.getId());
		InfoId followKey = svcctx.getPrincipal().getUserId();
		svcctx.addOperationPredicate("follower_uid", followKey.getId());
		
		ActionResult result = cnt == 0 ? ActionResult.failure("fail follow") : ActionResult.success("success follow");
		
		this.sendResult(exchange, result);
	}
	
	/**
	 * Favorite work group
	 * 
	 * @param wgroup_id the work group to be added into fav list
	 **/
	@WebApi(path="follow-check")
	public void handleCheckFollowUser(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.FOL_FND);
		ArgsValidator.newValidator(paramap)
			.requireOne("user_uid", "user_gid")
			.validate(true);
		
		InfoId userKey = Filters.filterInfoId(paramap, "user_uid", BaseIdKey.USER);
		if(!IdKeys.isValidId(userKey)) {
			userKey = commonService.queryInfoId(Filters.filterString(paramap, "user_gid"));
		}
		boolean followed = followService.checkfollowUser(svcctx, userKey);
		
		Map<String, Object> data = Maps.newHashMap();
		data.put("follow", followed);
		
		ActionResult result = ActionResult.success("success follow");
		result.setData(data);
		this.sendResult(exchange, result);
	}
	
	/**
	 * Favorite work group
	 * 
	 * @param wgroup_id the work group to be added into fav list
	 **/
	@WebApi(path="wgroup-invite")
	public void handleWorkgroupJoin(HttpServerExchange exchange) throws BaseException {
	
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.WGRP_JOI);
		String wgroupcode = Filters.filterString(paramap, "wgroup_code");
		
		InfoId wkey = commonService.queryInfoId(wgroupcode);
		if(!IdKeys.isValidId(wkey)) {
			svcctx.abort();
		}
		
		@SuppressWarnings("unchecked")
		List<Map<String, String>> bcards = (List<Map<String, String>>)paramap.get("bizcards");
		
		List<Map<String, String>> rtvs = Lists.newArrayList();
		Principal princ = svcctx.getPrincipal();
		for(Map<String, String> bcard: bcards) {
			
			String bcode = bcard.get("bizcard_code");
			String userGid = bcard.get("user_gid");
			
			// prepare return value
			Map<String, String> rtv = Maps.newHashMap();
			rtv.put("bizcard_code", bcode);
			
			UserInviteInfo invite = new UserInviteInfo();
			invite.setInviterUid(princ.getUserId().getId());
			invite.setBizcardCode(bcode);
			invite.setInviteeGid(userGid);
			invite.setInviteMode(InviteMode.WORKGROUP.name());
			
			invite.setInviteTime(LocalDates.now());

			invite.setWorkgroupId(wkey.getId());
			invite.setState(InviteState.PENDING.name());
			
			String state = processInvite( svcctx, invite);
			
			rtv.put("state", state);
			
			rtvs.add(rtv);
		
		}
		ActionResult result = ActionResult.success("success invite");
		result.setData(rtvs);
		
		this.sendResult(exchange, result);
	}
	
	/**
	 * Try to process the invite request
	 *  
	 **/
	private String processInvite(ServiceContext svcctx, UserInviteInfo invite)  throws BaseException{
		
		String nodeGid = TraceCode.parse(invite.getBizcardCode()).getNodeGid();
		InfoId userId = commonService.queryInfoId(invite.getBizcardCode());
		if(!IdKeys.isValidId(userId)) {
			
			Map<String, Object> data = Maps.newHashMap();
			data.put("bizcard_code", invite.getBizcardCode());
			LOGGER.debug("Try to detect the bizcard information");
			ActionResult gblResult = NodeApiAgent.instance().sendGlobalPost( CoreConstants.AUTH_API_PATH + "/bizcard-info", data);
			
			if(gblResult.isSuccess()) {
				LOGGER.debug("Success in fetching the bizcard information");
				try {
					JsonNode rootNode = JsonUtils.JSON_MAPPER.readTree((String)gblResult.getData());
					// find source in local db
					SourceInfo srcInfo = sourceService.getSource(nodeGid);
					
					if(null == srcInfo) {
						LOGGER.debug("Try to load source node information");
						JsonNode dataNode = rootNode.findPath("source");
						srcInfo = new SourceInfo();
						
						srcInfo.setEntityGid(dataNode.get("entity_gid").asText());
						srcInfo.setEntityName(dataNode.get("entity_name").asText());
						srcInfo.setNodeGid(nodeGid);
						srcInfo.setSourceName(dataNode.get("source_name").asText());
						srcInfo.setShortName(dataNode.get("short_name").asText());
						srcInfo.setDescription(dataNode.get("description").asText());
						srcInfo.setEmail(dataNode.get("email").asText());
						srcInfo.setState(Sources.State.ACTIVE.name());
						
						sourceService.addSource(svcctx, srcInfo);
					}
				
					UserInfo usrInfo = new UserInfo();
					
					usrInfo.setSourceId(srcInfo.getId());
					
					usrInfo.setUserGid(invite.getInviteeGid());
					usrInfo.setTraceCode(rootNode.get("bizcard_code").asText());
					usrInfo.setAvatarUrl(rootNode.get("avatar_url").asText());
					usrInfo.setUsername(rootNode.get("username").asText());
					usrInfo.setFullName(rootNode.get("full_name").asText());
					usrInfo.setNickname(rootNode.get("nickname").asText());
					usrInfo.setCategory(GroupUsers.UserCategory.GLOBAL.name());
					usrInfo.setClassification(GroupUsers.Classification.UNCLASSIFIED.name());
					usrInfo.setEmail(rootNode.get("email").asText());
					usrInfo.setMobile(rootNode.get("mobile").asText());
					usrInfo.setPhone(rootNode.get("phone").asText());
					
					usrInfo.setSupInfo(rootNode.get("sup_info").asText());
					usrInfo.setBiography(rootNode.get("biography").asText());
					usrInfo.setBiography(rootNode.get("avatar_url").asText());
					
					userService.newUserExt(svcctx, usrInfo, null);
					
				} catch (IOException e) {
					throw new BaseException("excp.parse.json");
				}
				
			}else {
				LOGGER.debug("Fail to fetch the bizcard information");
				// not find global node info, process next bizcard 
				return gblResult.getMeta().getState();
	
			}
		}
		
		// find user information
		invite.setInviterUid(svcctx.getPrincipal().getUserId().getId());
		int cnt = joinInviteService.submitInviteRequest(svcctx, invite);
		svcctx.setOperationObject(invite.getInfoId());
		
		return cnt == 0 ? CoreConstants.FAIL: CoreConstants.SUCCESS;
	}
	
	@WebApi(path="wgroup-join-review")
	public void handleReviewJoin(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> paramap = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.WGRP_JOI_RVW);
		Long joinId = Filters.filterLong(paramap, "join_id");
		String opinion = Filters.filterString(paramap, "opinion");
		String state = Filters.filterString(paramap, "state");

		InfoId jId = IdKeys.getInfoId(NodeIdKey.USER_JOIN, joinId);
		
		boolean success = joinInviteService.reviewJoinApply(svcctx, jId, opinion, state);
		
		svcctx.setOperationObject(jId);
		svcctx.addOperationPredicates(paramap);
		
		ActionResult result = success ? ActionResult.success("mesg.wgroup.joi.review"): ActionResult.failure("excp.wgroup.joi.review");
		
		this.sendResult(exchange, result);
	}
	
}
