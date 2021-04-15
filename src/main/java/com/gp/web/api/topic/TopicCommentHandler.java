/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.topic;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.NodeIdKey;
import com.gp.common.Operations;
import com.gp.common.ServiceContext;
import com.gp.common.Topics;
import com.gp.dao.info.TopicCommentInfo;
import com.gp.dao.info.TopicReplyInfo;
import com.gp.exception.BaseException;
import com.gp.info.DataBuilder;
import com.gp.info.DataWeaver;
import com.gp.info.DataWeaver.DataWrapper;
import com.gp.info.DataWeaver.LeafResolver;
import com.gp.info.Principal;
import com.gp.svc.CommonService;
import com.gp.svc.topic.TopicCommentService;
import com.gp.svc.topic.TopicExtraService;
import com.gp.validate.ArgsValidator;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import com.gp.web.api.ServiceApiHelper;

import io.undertow.server.HttpServerExchange;

@SuppressWarnings("unused")
public class TopicCommentHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(TopicCommentHandler.class);
	
	static final int MAX_LEN = 256;
	
	private TopicCommentService topicCommentService;
	private CommonService commonService;
	
	public TopicCommentHandler() {
		topicCommentService = BindScanner.instance().getBean(TopicCommentService.class);
		commonService = BindScanner.instance().getBean(CommonService.class);
	}

	@WebApi(path="topic-comments-query")
	public void handleTopicCommentsQuery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_FND_CMT);
		
		svcctx.addOperationPredicates(params);
		ArgsValidator.newValidator(params)
			.requireOne("topic_code", "topic_id")
			.validate(true);
			
		long topicid = Filters.filterLong(params, "topic_id");
		String topiccode = Filters.filterString(params, "topic_code");
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.comments"));
	
		InfoId topicKey = topicid > 0 ? IdKeys.getInfoId(NodeIdKey.TOPIC, topicid) : commonService.queryInfoId(topiccode);
		
		LeafResolver replyResolver = (context) -> {
			
			@SuppressWarnings("unchecked")
			Map<String,Object> cmnt = context.getHolder(Map.class);
			InfoId cmntId = IdKeys.getInfoId(NodeIdKey.TOPIC_COMMENT, Filters.filterLong(cmnt, "comment_id"));
			
			List<TopicReplyInfo> replies = topicCommentService.getReplies( topicKey, cmntId);
			
			List<Map<String,Object>> rows = replies.stream().map(info -> {
				DataBuilder builder = new DataBuilder();
				builder.set("reply_id", info.getId().toString());
				builder.set("workgroup_id", info.getWorkgroupId().toString());
				builder.set("target_id", info.getTargetId().toString());
				builder.set("comment_pid", info.getCommentPid().toString());
				builder.set("reply_time", info.getReplyTime().getTime());
				
				builder.set(info, "trace_code", "content", "state");
				
				builder.set("owner", sbuilder -> {
					sbuilder.set("user_gid", info.getProperty("owner_gid"));
					sbuilder.set("username", info.getProperty("owner_username"));
					
					sbuilder.set("full_name", info.getProperty("owner_full_name"));
					sbuilder.set("nickname", info.getProperty("owner_nickname"));
					String avatarUrl = info.getProperty("owner_avatar_url", String.class);
					avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
					sbuilder.set("avatar_url", avatarUrl);
				});
				builder.set("author", sbuilder -> {
					sbuilder.set("user_gid", info.getProperty("author_gid"));
					sbuilder.set("username", info.getProperty("author_username"));
					
					sbuilder.set("full_name", info.getProperty("author_full_name"));
					sbuilder.set("nickname", info.getProperty("author_nickname"));
					String avatarUrl = info.getProperty("author_avatar_url", String.class);
					avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
					sbuilder.set("avatar_url", avatarUrl);
				});
				return builder.build();
			}).collect(Collectors.toList());
			
			cmnt.put("replies", rows);
			LOGGER.debug("reply => find {} - {}", topicKey, cmntId);
		
		};
		
		Collection<Map<String,Object>> cmntlist = DataWeaver.resolveCollect((context) -> {
			
			List<TopicCommentInfo> cmnts = topicCommentService.getComments(topicKey);
			
			List<Map<String, Object>> rows = cmnts.stream().map(info -> {
				DataBuilder builder = new DataBuilder();
				builder.set("comment_id", info.getId().toString());
				builder.set("workgroup_id", info.getWorkgroupId().toString());
				builder.set("target_id", info.getTargetId().toString());
				
				builder.set("comment_time", info.getCommentTime().getTime());
				
				builder.set(info, "trace_code", "content", "state");
				
				builder.set("owner", sbuilder -> {
					sbuilder.set("user_gid", info.getProperty("owner_gid"));
					sbuilder.set("username", info.getProperty("owner_username"));
					
					sbuilder.set("full_name", info.getProperty("owner_full_name"));
					sbuilder.set("nickname", info.getProperty("owner_nickname"));
					String avatarUrl = info.getProperty("owner_avatar_url", String.class);
					avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
					sbuilder.set("avatar_url", avatarUrl);
				});
				builder.set("author", sbuilder -> {
					sbuilder.set("user_gid", info.getProperty("author_gid"));
					sbuilder.set("username", info.getProperty("author_username"));
					
					sbuilder.set("full_name", info.getProperty("author_full_name"));
					sbuilder.set("nickname", info.getProperty("author_nickname"));
					String avatarUrl = info.getProperty("author_avatar_url", String.class);
					avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
					sbuilder.set("avatar_url", avatarUrl);
				});
				return builder.build();
			}).collect(Collectors.toList());
			
			LOGGER.debug("comment => find {} ", topicKey);
			// find related 
			context.setNextResolver(replyResolver);
			
			return DataWrapper.wrapCollect(rows);
			
		});
		
		result.setData(cmntlist);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="answer-comments-query")
	public void handleAnswerCommentsQuery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_FND_CMT);
		
		svcctx.addOperationPredicates(params);
		ArgsValidator.newValidator(params)
			.requireOne("answer_code", "answer_id")
			.validate(true);
			
		long answerid = Filters.filterLong(params, "answer_id");
		String answercode = Filters.filterString(params, "answer_code");
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.comments"));
	
		InfoId answerKey = answerid > 0 ? IdKeys.getInfoId(NodeIdKey.TOPIC_ANSWER, answerid) : commonService.queryInfoId(answercode);
		
		LeafResolver replyResolver = (context) -> {
			
			@SuppressWarnings("unchecked")
			Map<String,Object> cmnt = context.getHolder(Map.class);
			InfoId cmntId = IdKeys.getInfoId(NodeIdKey.TOPIC_COMMENT, Filters.filterLong(cmnt, "comment_id"));
			
			List<TopicReplyInfo> replies = topicCommentService.getReplies( answerKey, cmntId);
			
			List<Map<String,Object>> rows = replies.stream().map(info -> {
				DataBuilder builder = new DataBuilder();
				builder.set("reply_id", info.getId().toString());
				builder.set("workgroup_id", info.getWorkgroupId().toString());
				builder.set("target_id", info.getTargetId().toString());
				builder.set("comment_pid", info.getCommentPid().toString());
				builder.set("reply_time", info.getReplyTime().getTime());
				
				builder.set(info, "trace_code", "content", "state");
				
				builder.set("owner", sbuilder -> {
					sbuilder.set("user_gid", info.getProperty("owner_gid"));
					sbuilder.set("username", info.getProperty("owner_username"));
					
					sbuilder.set("full_name", info.getProperty("owner_full_name"));
					sbuilder.set("nickname", info.getProperty("owner_nickname"));
					String avatarUrl = info.getProperty("owner_avatar_url", String.class);
					avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
					sbuilder.set("avatar_url", avatarUrl);
				});
				builder.set("author", sbuilder -> {
					sbuilder.set("user_gid", info.getProperty("author_gid"));
					sbuilder.set("username", info.getProperty("author_username"));
					
					sbuilder.set("full_name", info.getProperty("author_full_name"));
					sbuilder.set("nickname", info.getProperty("author_nickname"));
					String avatarUrl = info.getProperty("author_avatar_url", String.class);
					avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
					sbuilder.set("avatar_url", avatarUrl);
				});
				return builder.build();
			}).collect(Collectors.toList());
			
			cmnt.put("replies", rows);
			LOGGER.debug("reply => find {} - {}", answerKey, cmntId);
		
		};
		
		Collection<Map<String,Object>> cmntlist = DataWeaver.resolveCollect((context) -> {
			
			List<TopicCommentInfo> cmnts = topicCommentService.getComments(answerKey);
			
			List<Map<String, Object>> rows = cmnts.stream().map(info -> {
				DataBuilder builder = new DataBuilder();
				builder.set("comment_id", info.getId().toString());
				builder.set("workgroup_id", info.getWorkgroupId().toString());
				builder.set("target_id", info.getTargetId().toString());
				
				builder.set("comment_time", info.getCommentTime().getTime());
				
				builder.set(info, "trace_code", "content", "state");
				
				builder.set("owner", sbuilder -> {
					sbuilder.set("user_gid", info.getProperty("owner_gid"));
					sbuilder.set("username", info.getProperty("owner_username"));
					
					sbuilder.set("full_name", info.getProperty("owner_full_name"));
					sbuilder.set("nickname", info.getProperty("owner_nickname"));
					String avatarUrl = info.getProperty("owner_avatar_url", String.class);
					avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
					sbuilder.set("avatar_url", avatarUrl);
				});
				builder.set("author", sbuilder -> {
					sbuilder.set("user_gid", info.getProperty("author_gid"));
					sbuilder.set("username", info.getProperty("author_username"));
					
					sbuilder.set("full_name", info.getProperty("author_full_name"));
					sbuilder.set("nickname", info.getProperty("author_nickname"));
					String avatarUrl = info.getProperty("author_avatar_url", String.class);
					avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
					sbuilder.set("avatar_url", avatarUrl);
				});
				return builder.build();
			}).collect(Collectors.toList());
			
			LOGGER.debug("comment => find {} ", answerKey);
			// find related 
			context.setNextResolver(replyResolver);
			
			return DataWrapper.wrapCollect(rows);
			
		});
		
		result.setData(cmntlist);
		
		this.sendResult(exchange, result);
	}

	@WebApi(path="topic-comment-info")
	public void handleTopicCommentInfo(HttpServerExchange exchange) throws BaseException {

		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_FND_CMT);
		
		svcctx.addOperationPredicates(params);
		long commentid = Filters.filterLong(params, "comment_id");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.comment"));
	
		if(commentid <= 0){
			result = ActionResult.failure("miss parameters");
			this.sendResult(exchange, result);
			return ;
		}
		InfoId cmntId = IdKeys.getInfoId(NodeIdKey.TASK_COMMENT, commentid);
		TopicCommentInfo info = topicCommentService.getComment(cmntId);
		if(null != info) {
			
			DataBuilder builder = new DataBuilder();
			builder.set("comment_id", info.getId().toString());
			builder.set("workgroup_id", info.getWorkgroupId().toString());
			builder.set("target_id", info.getTargetId().toString());
			
			builder.set("comment_time", info.getCommentTime().getTime());
			
			builder.set(info, "trace_code", "content", "state");
			
			builder.set("owner", sbuilder -> {
				sbuilder.set("user_gid", info.getProperty("owner_gid"));
				sbuilder.set("username", info.getProperty("owner_username"));
				
				sbuilder.set("full_name", info.getProperty("owner_full_name"));
				sbuilder.set("nickname", info.getProperty("owner_nickname"));
				String avatarUrl = info.getProperty("owner_avatar_url", String.class);
				avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
				sbuilder.set("avatar_url", avatarUrl);
			});
			builder.set("author", sbuilder -> {
				sbuilder.set("user_gid", info.getProperty("author_gid"));
				sbuilder.set("username", info.getProperty("author_username"));
				
				sbuilder.set("full_name", info.getProperty("author_full_name"));
				sbuilder.set("nickname", info.getProperty("author_nickname"));
				String avatarUrl = info.getProperty("author_avatar_url", String.class);
				avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
				sbuilder.set("avatar_url", avatarUrl);
			});
			
			List<TopicReplyInfo> replies = topicCommentService.getReplies( null, cmntId);
			
			List<Map<String,Object>> rows = replies.stream().map(info1 -> {
				DataBuilder builder1 = new DataBuilder();
				builder1.set("reply_id", info1.getId().toString());
				builder1.set("workgroup_id", info1.getWorkgroupId().toString());
				builder1.set("target_id", info1.getTargetId().toString());
				builder1.set("comment_pid", info1.getCommentPid().toString());
				builder1.set("reply_time", info1.getReplyTime().getTime());
				
				builder1.set(info1, "trace_code", "content", "state");
				
				builder1.set("owner", sbuilder -> {
					sbuilder.set("user_gid", info1.getProperty("owner_gid"));
					sbuilder.set("username", info1.getProperty("owner_username"));
					
					sbuilder.set("full_name", info1.getProperty("owner_full_name"));
					sbuilder.set("nickname", info1.getProperty("owner_nickname"));
					String avatarUrl = info1.getProperty("owner_avatar_url", String.class);
					avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
					sbuilder.set("avatar_url", avatarUrl);
				});
				builder1.set("author", sbuilder -> {
					sbuilder.set("user_gid", info1.getProperty("author_gid"));
					sbuilder.set("username", info1.getProperty("author_username"));
					
					sbuilder.set("full_name", info1.getProperty("author_full_name"));
					sbuilder.set("nickname", info1.getProperty("author_nickname"));
					String avatarUrl = info1.getProperty("author_avatar_url", String.class);
					avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
					sbuilder.set("avatar_url", avatarUrl);
				});
				return builder1.build();
				
			}).collect(Collectors.toList());
			
			builder.set("replies", rows);
			
			result.setData(builder);
		}
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="topic-comment-add")
	public void handleTopicAddComment(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_ADD_CMT);
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.requireOne("topic_id", "content")
			.validate(true);
				
		Long topicid = Filters.filterLong(params, "topic_id");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.topic.comment.save"));
				
		TopicCommentInfo comment = new TopicCommentInfo();
		comment.setTargetId(topicid);
		comment.setContent(Filters.filterString(params, "content"));
		comment.setState(Topics.State.DRAFT.name());
		Principal princ = svcctx.getPrincipal();
		
		Long wgrpid = commonService.queryColumn(IdKeys.getInfoId(NodeIdKey.TOPIC, topicid), "workgroup_id", Long.class);
		comment.setWorkgroupId(wgrpid);
		
		comment.setOwnerUid(princ.getUserId().getId());
		comment.setAuthorUid(princ.getUserId().getId());
		
		topicCommentService.newComment(svcctx, comment, NodeIdKey.TOPIC);
		
		// set object id into context
		svcctx.setOperationObject(comment.getInfoId());
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="answer-comment-add")
	public void handleAnswerAddComment(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ANS_ADD_CMT);
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.require("answer_id", "content")
			.validate(true);
				
		Long answerid = Filters.filterLong(params, "answer_id");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.topic.comment.save"));
				
		TopicCommentInfo comment = new TopicCommentInfo();
		comment.setTargetId(answerid);
		comment.setContent(Filters.filterString(params, "content"));
		comment.setState(Topics.State.DRAFT.name());
		Principal princ = svcctx.getPrincipal();
		
		Long wgrpid = commonService.queryColumn(IdKeys.getInfoId(NodeIdKey.TOPIC_ANSWER, answerid), "workgroup_id", Long.class);
		comment.setWorkgroupId(wgrpid);
		
		comment.setOwnerUid(princ.getUserId().getId());
		comment.setAuthorUid(princ.getUserId().getId());
		
		topicCommentService.newComment(svcctx, comment, NodeIdKey.TOPIC_ANSWER);
		
		// set object id into context
		svcctx.setOperationObject(comment.getInfoId());
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="topic-comment-save")
	public void handleTopicSaveComment(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ArgsValidator.newValidator(params)
			.requireId("comment_id")
			.require("content", "state")
			.validate(true);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_UPD_CMT);
		
		svcctx.addOperationPredicates(params);
		long commentid = Filters.filterLong(params, "comment_id");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.save.comment"));
			
		TopicCommentInfo comment = new TopicCommentInfo();
		comment.setInfoId(IdKeys.getInfoId(NodeIdKey.TOPIC_COMMENT, commentid));
		comment.setContent(Filters.filterString(params, "content"));
		comment.setState(Filters.filterString(params, "state"));
		Principal princ = svcctx.getPrincipal();
	
		comment.setAuthorUid(princ.getUserId().getId());
		comment.setPropertyFilter(Sets.newHashSet("content", "state", "author_uid"));
		
		topicCommentService.updateComment(svcctx, comment, NodeIdKey.TOPIC);
		
		// set object id into context
		svcctx.setOperationObject(comment.getInfoId());
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="answer-comment-save")
	public void handleAnswerSaveComment(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ArgsValidator.newValidator(params)
			.requireId("comment_id")
			.require("content", "state")
			.validate(true);
		
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ANS_UPD_CMT);
		
		svcctx.addOperationPredicates(params);
		long commentid = Filters.filterLong(params, "comment_id");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.save.comment"));
				
		TopicCommentInfo comment = new TopicCommentInfo();
		comment.setInfoId(IdKeys.getInfoId(NodeIdKey.TOPIC_COMMENT, commentid));
		comment.setContent(Filters.filterString(params, "content"));
		comment.setState(Filters.filterString(params, "state"));
		Principal princ = svcctx.getPrincipal();
	
		comment.setAuthorUid(princ.getUserId().getId());
		comment.setPropertyFilter(Sets.newHashSet("content", "state", "author_uid"));
		
		topicCommentService.updateComment(svcctx, comment, NodeIdKey.TOPIC_ANSWER);
		
		// set object id into context
		svcctx.setOperationObject(comment.getInfoId());
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="topic-comment-remove")
	public void handleTopicRemoveComment(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ArgsValidator.newValidator(params)
			.requireId("comment_id")
			.validate(true);
		
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_RMV_CMT);
		svcctx.addOperationPredicates(params);
		long commentid = Filters.filterLong(params, "comment_id");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.remove.comment"));
		
		InfoId commentId = IdKeys.getInfoId(NodeIdKey.TOPIC_COMMENT, commentid);
				
		// set object id into context
		Long topicid = commonService.queryColumn(commentId, "target_id", Long.class);
		svcctx.addOperationPredicate("topic_id", topicid);
		svcctx.setOperationObject(commentId);
		
		topicCommentService.removeComment(svcctx, commentId, NodeIdKey.TOPIC);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="answer-comment-remove")
	public void handleAnswerRemoveComment(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ANS_RMV_CMT);
		ArgsValidator.newValidator(params)
			.requireId("comment_id")
			.validate(true);
			
		svcctx.addOperationPredicates(params);
		long commentid = Filters.filterLong(params, "comment_id");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.remove.comment"));
		
		InfoId commentId = IdKeys.getInfoId(NodeIdKey.TOPIC_COMMENT, commentid);
		
		// set object id into context
		Long answerid = commonService.queryColumn(commentId, "target_id", Long.class);
		svcctx.addOperationPredicate("answer_id", answerid);
		svcctx.setOperationObject(commentId);
				
		topicCommentService.removeComment(svcctx, commentId, NodeIdKey.TOPIC_ANSWER);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="topic-replies-query")
	public void handleTopicRepliesQuery(HttpServerExchange exchange) throws BaseException {
				
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_FND_RPL);
		
		ArgsValidator.newValidator(params)
			.requireOne("comment_code", "comment_id")
			.validate(true);
		
		svcctx.addOperationPredicates(params);
		long commentid = Filters.filterLong(params, "comment_id");
		String commentcode = Filters.filterString(params, "comment_code");
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.reply"));
		
		InfoId cmntId = null;
		if(commentid > 0) {
			cmntId = IdKeys.getInfoId(NodeIdKey.TOPIC_COMMENT, commentid);
		}else {
			cmntId = commonService.queryInfoId(commentcode);
		}
		
		List<TopicReplyInfo> replies = topicCommentService.getReplies( null, cmntId);
		
		List<Map<String,Object>> rows = replies.stream().map(info -> {
			
			DataBuilder builder = new DataBuilder();
			builder.set("reply_id", info.getId().toString());
			builder.set("workgroup_id", info.getWorkgroupId().toString());
			builder.set("target_id", info.getTargetId().toString());
			builder.set("comment_pid", info.getCommentPid().toString());
			builder.set("comment_time", info.getReplyTime().getTime());
			
			builder.set(info, "trace_code", "content", "state");
			
			builder.set("owner", sbuilder -> {
				sbuilder.set("user_gid", info.getProperty("owner_gid"));
				sbuilder.set("username", info.getProperty("owner_username"));
				
				sbuilder.set("full_name", info.getProperty("owner_full_name"));
				sbuilder.set("nickname", info.getProperty("owner_nickname"));
				String avatarUrl = info.getProperty("owner_avatar_url", String.class);
				avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
				sbuilder.set("avatar_url", avatarUrl);
			});
			builder.set("author", sbuilder -> {
				sbuilder.set("user_gid", info.getProperty("author_gid"));
				sbuilder.set("username", info.getProperty("author_username"));
				
				sbuilder.set("full_name", info.getProperty("author_full_name"));
				sbuilder.set("nickname", info.getProperty("author_nickname"));
				String avatarUrl = info.getProperty("author_avatar_url", String.class);
				avatarUrl = ServiceApiHelper.absoluteBinaryUrl(avatarUrl);
				sbuilder.set("avatar_url", avatarUrl);
			});
			
			return builder.build();
			
		}).collect(Collectors.toList());
		
		result.setData(rows);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="topic-reply-add")
	public void handleTopicReplyAdd(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_ADD_RPL);
		ArgsValidator.newValidator(params)
			.requireId("comment_pid")
			.require("content")
			.validate(true);
		
		svcctx.addOperationPredicates(params);
		long commentid = Filters.filterLong(params, "comment_pid");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.add.reply"));

		TopicReplyInfo reply = new TopicReplyInfo();
		
		reply.setCommentPid(commentid);
		reply.setContent(Filters.filterString(params, "content"));
		reply.setState(Topics.State.DRAFT.name());
		Principal princ = svcctx.getPrincipal();
	
		reply.setOwnerUid(princ.getUserId().getId());
		reply.setAuthorUid(princ.getUserId().getId());
		
		topicCommentService.newReply(svcctx, reply, NodeIdKey.TOPIC);
		
		// set object id into context
		svcctx.setOperationObject(reply.getInfoId());
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="answer-reply-add")
	public void handleAnswerReplyAdd(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ANS_ADD_RPL);
		ArgsValidator.newValidator(params)
			.requireId("comment_pid")
			.require("content")
			.validate(true);
		
		svcctx.addOperationPredicates(params);
		long commentid = Filters.filterLong(params, "comment_pid");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.add.reply"));
	
		TopicReplyInfo reply = new TopicReplyInfo();
		
		reply.setCommentPid(commentid);
		reply.setContent(Filters.filterString(params, "content"));
		reply.setState(Topics.State.DRAFT.name());
		Principal princ = svcctx.getPrincipal();
	
		reply.setOwnerUid(princ.getUserId().getId());
		reply.setAuthorUid(princ.getUserId().getId());
		
		topicCommentService.newReply(svcctx, reply, NodeIdKey.TOPIC_ANSWER);
		
		// set object id into context
		svcctx.setOperationObject(reply.getInfoId());
				
		this.sendResult(exchange, result);
	}
		
	@WebApi(path="topic-reply-update")
	public void handleTopicReplyUpdate(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ANS_UPD_RPL);
		ArgsValidator.newValidator(params)
			.requireId("reply_id")
			.validate(true);
		
		svcctx.addOperationPredicates(params);
		long replyid = Filters.filterLong(params, "reply_id");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.update.reply"));
		TopicReplyInfo reply = new TopicReplyInfo();
		
		reply.setInfoId(IdKeys.getInfoId(NodeIdKey.TOPIC_REPLY, replyid));
		if(params.containsKey("content")) {
			reply.setContent(Filters.filterString(params, "content"));
		}
		if(params.containsKey("state")) {
			reply.setState(Filters.filterString(params, "state"));
		}
		Set<String> keys = params.keySet();
		keys.remove("reply_id"); // reserve property keys only
		
		reply.setPropertyFilter(keys);
		
		topicCommentService.updateReply(svcctx, reply, NodeIdKey.TOPIC);
		
		// set object id into context
		svcctx.setOperationObject(reply.getInfoId());
				
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="answer-reply-update")
	public void handleAnswerReplyUpdate(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ANS_UPD_RPL);
		ArgsValidator.newValidator(params)
			.requireId("reply_id")
			.validate(true);
		
		svcctx.addOperationPredicates(params);
		long replyid = Filters.filterLong(params, "reply_id");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.update.reply"));
		
		TopicReplyInfo reply = new TopicReplyInfo();
		
		reply.setInfoId(IdKeys.getInfoId(NodeIdKey.TOPIC_REPLY, replyid));
		if(params.containsKey("content")) {
			reply.setContent(Filters.filterString(params, "content"));
		}
		if(params.containsKey("state")) {
			reply.setState(Filters.filterString(params, "state"));
		}
		Set<String> keys = params.keySet();
		keys.remove("reply_id"); // reserve property keys only
		
		reply.setPropertyFilter(keys);
		
		topicCommentService.updateReply(svcctx, reply, NodeIdKey.TOPIC_ANSWER);
		
		// set object id into context
		svcctx.setOperationObject(reply.getInfoId());
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="topic-reply-remove")
	public void handleTopicRemoveReply(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TPC_RMV_RPL);
		
		ArgsValidator.newValidator(params)
			.requireId("reply_id")
			.validate(true);
		
		svcctx.addOperationPredicates(params);
		long replyid = Filters.filterLong(params, "reply_id");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.remove.reply"));
		
		InfoId replyId = IdKeys.getInfoId(NodeIdKey.TASK_REPLY, replyid);
		
		// set object id into context
		Long topicid = commonService.queryColumn(replyId, "target_id", Long.class);
		svcctx.addOperationPredicate("topic_id", topicid);
		svcctx.setOperationObject(replyId);

		topicCommentService.removeReply(svcctx, replyId, NodeIdKey.TOPIC);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="answer-reply-remove")
	public void handleAnswerRemoveReply(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.ANS_RMV_RPL);
		
		ArgsValidator.newValidator(params)
			.requireId("reply_id")
			.validate(true);
		
		svcctx.addOperationPredicates(params);
		long replyid = Filters.filterLong(params, "reply_id");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.remove.reply"));
		
		InfoId replyId = IdKeys.getInfoId(NodeIdKey.TASK_REPLY, replyid);
		
		// set object id into context
		Long answerid = commonService.queryColumn(replyId, "target_id", Long.class);
		svcctx.addOperationPredicate("answer_id", answerid);
		svcctx.setOperationObject(replyId);
		
		topicCommentService.removeReply(svcctx, replyId, NodeIdKey.TOPIC_ANSWER);
		
		this.sendResult(exchange, result);
	}
	
}
