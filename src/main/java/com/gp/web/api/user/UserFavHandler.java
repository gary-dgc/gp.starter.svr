/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.user;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.IdKeys;
import com.gp.common.LocalDates;
import com.gp.common.NodeIdKey;
import com.gp.common.Operations;
import com.gp.common.ServiceContext;
import com.gp.dao.info.UserFavoriteInfo;
import com.gp.exception.BaseException;
import com.gp.info.DataBuilder;
import com.gp.pagination.PageQuery;
import com.gp.svc.user.FavoriteService;
import com.gp.validate.ArgsValidator;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;

import io.undertow.server.HttpServerExchange;

public class UserFavHandler extends BaseApiSupport{
	
	private FavoriteService favService;

	public UserFavHandler() {
		favService = BindScanner.instance().getBean(FavoriteService.class);
	}

	@WebApi(path="cabinet-fav")
	public void handleCabFavAdd(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.FAV_NEW);
		
		Long rescId = Filters.filterLong(params, "entry_id");
		String rescType = Filters.filterString(params, "entry_type");
		UserFavoriteInfo fav = new UserFavoriteInfo();
		fav.setCollectorUid(svcctx.getPrincipal().getUserId().getId());
		fav.setCollectTime(LocalDates.now());
		fav.setResourceId(rescId);
				
		if(Objects.equal(rescType, "FILE")) {
			
			fav.setResourceType(NodeIdKey.CAB_FILE.name());
		}else if(Objects.equal(rescType, "FOLDER")) {
			
			fav.setResourceType(NodeIdKey.CAB_FOLDER.name());
		}
		
		
		if(Strings.isNullOrEmpty(fav.getResourceType())) {
			svcctx.abort("excp.resc.unexist");
		}
		boolean success = favService.addFavorite(svcctx, fav);
		
		svcctx.setOperationObject(fav.getInfoId());
		svcctx.addOperationPredicates(params);
		svcctx.addOperationPredicate("collector_uid", fav.getCollectorUid());
		ActionResult result = success ? ActionResult.success("mesg.fav"): ActionResult.failure("excp.fav");
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="cabinet-unfav")
	public void handleCabFavRemove(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.FAV_RMV);
		
		Long resourceId = Filters.filterLong(params, "entry_id");
		Long collectorUid = svcctx.getPrincipal().getUserId().getId();
				
		if(!IdKeys.isValidId(resourceId)) {
			svcctx.abort("excp.resc.unexist");
		}
		boolean success = favService.removeFavorite(svcctx, collectorUid, resourceId);
		
		svcctx.addOperationPredicates(params);
		svcctx.addOperationPredicate("collector_uid", collectorUid);
		ActionResult result = success ? ActionResult.success("mesg.unfav"): ActionResult.failure("excp.unfav");
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="topic-fav")
	public void handleTopicFavAdd(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.FAV_NEW);
		ArgsValidator.newValidator(params)
			.require("topic_id")
			.validate(true);
		
		Long rescId = Filters.filterLong(params, "topic_id");
		UserFavoriteInfo fav = new UserFavoriteInfo();
		fav.setCollectorUid(svcctx.getPrincipal().getUserId().getId());
		fav.setCollectTime(LocalDates.now());
		fav.setResourceId(rescId);
		fav.setResourceType(NodeIdKey.TOPIC.name());
		
		boolean success = favService.addFavorite(svcctx, fav);
		
		svcctx.setOperationObject(fav.getInfoId());
		svcctx.addOperationPredicates(params);
		svcctx.addOperationPredicate("collector_uid", fav.getCollectorUid());
		ActionResult result = success ? ActionResult.success("mesg.fav"): ActionResult.failure("excp.fav");
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="topic-unfav")
	public void handleTopicFavRemove(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.FAV_RMV);
		ArgsValidator.newValidator(params)
			.require("topic_id")
			.validate(true);
		Long resourceId = Filters.filterLong(params, "topic_id");
		Long collectorUid = svcctx.getPrincipal().getUserId().getId();
				
		boolean success = favService.removeFavorite(svcctx, collectorUid, resourceId);
		
		svcctx.addOperationPredicates(params);
		svcctx.addOperationPredicate("collector_uid", collectorUid);
		ActionResult result = success ? ActionResult.success("mesg.unfav"): ActionResult.failure("excp.unfav");
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="answer-fav")
	public void handleAnswerFavAdd(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.FAV_NEW);
		ArgsValidator.newValidator(params)
			.require("answer_id")
			.validate(true);
		
		Long rescId = Filters.filterLong(params, "answer_id");
		UserFavoriteInfo fav = new UserFavoriteInfo();
		fav.setCollectorUid(svcctx.getPrincipal().getUserId().getId());
		fav.setCollectTime(LocalDates.now());
		fav.setResourceId(rescId);
		fav.setResourceType(NodeIdKey.TOPIC_ANSWER.name());
		
		boolean success = favService.addFavorite(svcctx, fav);
		
		svcctx.setOperationObject(fav.getInfoId());
		svcctx.addOperationPredicates(params);
		svcctx.addOperationPredicate("collector_uid", fav.getCollectorUid());
		ActionResult result = success ? ActionResult.success("mesg.fav"): ActionResult.failure("excp.fav");
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="answer-unfav")
	public void handleAnswerFavRemove(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.FAV_RMV);
		ArgsValidator.newValidator(params)
			.require("answer_id")
			.validate(true);
		Long resourceId = Filters.filterLong(params, "answer_id");
		Long collectorUid = svcctx.getPrincipal().getUserId().getId();
				
		boolean success = favService.removeFavorite(svcctx, collectorUid, resourceId);
		
		svcctx.addOperationPredicates(params);
		svcctx.addOperationPredicate("collector_uid", collectorUid);
		ActionResult result = success ? ActionResult.success("mesg.unfav"): ActionResult.failure("excp.unfav");
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="favorites-query")
	public void handleFavQuery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.FAV_FND);
		
		String favType = Filters.filterString(params, "fav_type");
		Long cabinetId = Filters.filterLong(params, "cabinet_id");
		Long collectorUid = svcctx.getPrincipal().getUserId().getId();
		
		PageQuery pquery = Filters.filterPageQuery(params);
		if(null != pquery) {
			pquery.setOrderBy("favorite_id");
		}
		if(Strings.isNullOrEmpty(favType)) {
			svcctx.abort("excp.fav.query");
		}
		
		String[] types = new String[0];
		if(!Objects.equal(favType, "CABINET")) {
					
			types = new String[] {favType};
		}
		
		List<UserFavoriteInfo> infos = Objects.equal(favType, "CABINET") ? 
				favService.getCabFavorites(collectorUid, cabinetId, pquery) :
				favService.getFavorites(collectorUid, types, pquery);
		
		List<Object> data = infos.stream().map(info -> {
			DataBuilder builder = new DataBuilder();
			builder.set("favorite_id", info.getId().toString());
			builder.set("resource_id", info.getResourceId().toString());
			
			builder.set(info, "resource_type", "resource_name");
			builder.set("collect_time", info.getCollectTime().getTime());
			
			return builder.build();
		}).collect(Collectors.toList());
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.wgroup"));
		result.setData(pquery == null ? null : pquery.getPagination(), data);
		
		this.sendResult(exchange, result);
				
	}
}
