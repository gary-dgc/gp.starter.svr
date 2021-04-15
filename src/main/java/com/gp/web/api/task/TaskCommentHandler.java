/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.task;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.NodeIdKey;
import com.gp.common.Operations;
import com.gp.common.ServiceContext;
import com.gp.common.Tasks;
import com.gp.dao.info.TaskCommentInfo;
import com.gp.dao.info.TaskReplyInfo;
import com.gp.exception.BaseException;
import com.gp.info.DataBuilder;
import com.gp.info.DataWeaver;
import com.gp.info.DataWeaver.DataWrapper;
import com.gp.info.DataWeaver.LeafResolver;
import com.gp.info.Principal;
import com.gp.svc.CommonService;
import com.gp.svc.task.TaskCommentService;
import com.gp.validate.ArgsValidator;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import com.gp.web.api.ServiceApiHelper;

import io.undertow.server.HttpServerExchange;

public class TaskCommentHandler extends BaseApiSupport {

	static final int MAX_LEN = 256;
	static Logger LOGGER = LoggerFactory.getLogger(TaskCommentHandler.class);
	
	private TaskCommentService taskCommentService;
	private CommonService commonService;
	
	public TaskCommentHandler() {
		
		taskCommentService = BindScanner.instance().getBean(TaskCommentService.class);
		commonService = BindScanner.instance().getBean(CommonService.class);
				
	}

	@WebApi(path="task-comments-query")
	public void handleTaskCommentsQuery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);
		svcctx.setOperation(Operations.TSK_FND_CMT);
		svcctx.addOperationPredicates(params);
		
		svcctx.addOperationPredicates(params);
		long taskid = Filters.filterLong(params, "task_id");
		String taskcode = Filters.filterString(params, "task_code");
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.comments"));
		
		if(taskid <= 0 && Strings.isNullOrEmpty(taskcode)){
			result = ActionResult.failure("miss parameters");
			this.sendResult(exchange, result);
			return ;
		}
		
		InfoId taskKey = IdKeys.getInfoId(NodeIdKey.TASK, taskid);
		LeafResolver replyResolver = (context) -> {
			
			@SuppressWarnings("unchecked")
			Map<String,Object> cmnt = context.getHolder(Map.class);
			InfoId cmntId = IdKeys.getInfoId(NodeIdKey.TASK_COMMENT, Filters.filterLong(cmnt, "comment_id"));
			
			List<TaskReplyInfo> replies = taskCommentService.getReplies( taskKey, cmntId);
			
			List<Map<String,Object>> rows = replies.stream().map(info -> {
				DataBuilder builder = new DataBuilder();
				builder.set("reply_id", info.getId().toString());
				builder.set("workgroup_id", info.getWorkgroupId().toString());
				builder.set("task_id", info.getTargetId().toString());
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
			LOGGER.debug("reply => find {} - {}", taskKey, cmntId);
		
		};
		
		Collection<Map<String,Object>> cmntlist = DataWeaver.resolveCollect((context) -> {
			
			List<TaskCommentInfo> cmnts = taskCommentService.getComments(taskKey);
			
			List<Map<String,Object>> rows = cmnts.stream().map(info -> {
				DataBuilder builder = new DataBuilder();
				builder.set("comment_id", info.getId().toString());
				builder.set("workgroup_id", info.getWorkgroupId().toString());
				builder.set("task_id", info.getTargetId().toString());
				
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
			
			LOGGER.debug("comment => find {} ", taskKey);
			context.setNextResolver(replyResolver);
			
			return DataWrapper.wrapCollect(rows);
			
		});
		
		result.setData(cmntlist);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-comment-info")
	public void handleTaskCommentInfo(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange);
		svcctx.setOperation(Operations.TSK_FND_CMT);
		svcctx.addOperationPredicates(params);
		long commentid = Filters.filterLong(params, "comment_id");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.comment"));
		
		if(commentid <= 0){
			result = ActionResult.failure("miss parameters");
			this.sendResult(exchange, result);
			return;
		}
		InfoId cmntId = IdKeys.getInfoId(NodeIdKey.TASK_COMMENT, commentid);
		TaskCommentInfo info = taskCommentService.getComment(cmntId);
		if(null != info) {
			
			DataBuilder builder = new DataBuilder();
			builder.set("comment_id", info.getId().toString());
			builder.set("workgroup_id", info.getWorkgroupId().toString());
			builder.set("task_id", info.getTargetId().toString());
			
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
			
			List<TaskReplyInfo> replies = taskCommentService.getReplies( null, cmntId);
			
			List<Map<String,Object>> rows = replies.stream().map(info1 -> {
				DataBuilder builder1 = new DataBuilder();
				builder1.set("reply_id", info1.getId().toString());
				builder1.set("workgroup_id", info1.getWorkgroupId().toString());
				builder1.set("task_id", info1.getTargetId().toString());
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

	@WebApi(path="task-comment-add")
	public void handleTaskAddComment(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_ADD_CMT);
		svcctx.addOperationPredicates(params);
		ArgsValidator.newValidator(params)
		.requireOne("task_id", "content")
		.validate(true);
		
		long taskid = Filters.filterLong(params, "task_id");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.add.comment"));
		
		TaskCommentInfo comment = new TaskCommentInfo();
		comment.setTargetId(taskid);
		comment.setContent(Filters.filterString(params, "content"));
		comment.setState(Tasks.State.PENDING.name());
		Principal princ = svcctx.getPrincipal();
	
		comment.setOwnerUid(princ.getUserId().getId());
		comment.setAuthorUid(princ.getUserId().getId());
		
		taskCommentService.newComment(svcctx, comment, NodeIdKey.TASK);
		// set object id into context
		svcctx.setOperationObject(comment.getInfoId());
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-comment-save")
	public void handleTaskSaveComment(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_UPD_CMT);
		svcctx.addOperationPredicates(params);
		ArgsValidator.newValidator(params)
			.requireId("comment_id")
			.require("content", "state")
			.validate(true);
		
		long commentid = Filters.filterLong(params, "comment_id");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.add.comment"));
	
		TaskCommentInfo comment = new TaskCommentInfo();
		comment.setInfoId(IdKeys.getInfoId(NodeIdKey.TASK_COMMENT, commentid));
		comment.setContent(Filters.filterString(params, "content"));
		comment.setState(Filters.filterString(params, "state"));
		Principal princ = svcctx.getPrincipal();
	
		comment.setAuthorUid(princ.getUserId().getId());
		comment.setPropertyFilter(Sets.newHashSet("content", "state", "author_uid"));
		
		taskCommentService.updateComment(svcctx, comment);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-comment-remove")
	public void handleTaskRemoveComment(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_RMV_CMT);
		svcctx.addOperationPredicates(params);
		ArgsValidator.newValidator(params)
			.requireId("comment_id")
			.validate(true);
			
		long commentid = Filters.filterLong(params, "comment_id");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.remove.comment"));
		
		InfoId commentId = IdKeys.getInfoId(NodeIdKey.TASK_COMMENT, commentid);
		
		// set object id into context
		Long topicid = commonService.queryColumn(commentId, "target_id", Long.class);
		svcctx.addOperationPredicate("task_id", topicid);
		svcctx.setOperationObject(commentId);
				
		taskCommentService.removeComment(svcctx, commentId);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-replies-query")
	public void handleTaskRepliesQuery(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_FND_RPL);
		svcctx.addOperationPredicates(params);
		ArgsValidator.newValidator(params)
			.requireOne("comment_code", "comment_id")
			.validate(true);
		
		long commentid = Filters.filterLong(params, "comment_id");
		String commentcode = Filters.filterString(params, "comment_code");
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.replies"));

		InfoId cmntId = null;
		if(commentid > 0) {
			cmntId = IdKeys.getInfoId(NodeIdKey.TASK_COMMENT, commentid);
		}else {
			cmntId = commonService.queryInfoId(commentcode);
		}
		
		List<TaskReplyInfo> replies = taskCommentService.getReplies( null, cmntId);
		
		List<Map<String,Object>> rows = replies.stream().map(info -> {
			
			DataBuilder builder = new DataBuilder();
			builder.set("reply_id", info.getId().toString());
			builder.set("workgroup_id", info.getWorkgroupId().toString());
			builder.set("task_id", info.getTargetId().toString());
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
	
	@WebApi(path="task-reply-add")
	public void handleTaskReplyAdd(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_ADD_RPL);
		svcctx.addOperationPredicates(params);
		ArgsValidator.newValidator(params)
			.requireId("comment_pid")
			.require("content")
			.validate(true);
		
		long commentid = Filters.filterLong(params, "comment_pid");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.add.reply"));
	
		TaskReplyInfo reply = new TaskReplyInfo();
		
		reply.setCommentPid(commentid);
		reply.setContent(Filters.filterString(params, "content"));
		reply.setState(Tasks.State.PENDING.name());
		Principal princ = svcctx.getPrincipal();
	
		reply.setOwnerUid(princ.getUserId().getId());
		reply.setAuthorUid(princ.getUserId().getId());
		
		taskCommentService.newReply(svcctx, reply);
		
		// set object id into context
		svcctx.setOperationObject(reply.getInfoId());
				
		this.sendResult(exchange, result);
	}

	@WebApi(path="task-reply-save")
	public void handleTaskReplySave(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_UPD_RPL);
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
		.requireId("reply_id")
		.validate(true);
		
		long replyid = Filters.filterLong(params, "reply_id");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.update.reply"));
			
		TaskReplyInfo reply = new TaskReplyInfo();
		
		reply.setInfoId(IdKeys.getInfoId(NodeIdKey.TASK_REPLY, replyid));
		reply.setContent(Filters.filterString(params, "content"));
		reply.setState(Filters.filterString(params, "state"));
		Principal princ = svcctx.getPrincipal();
	
		reply.setAuthorUid(princ.getUserId().getId());
		reply.setPropertyFilter(Sets.newHashSet("content", "state", "author_uid"));
		
		taskCommentService.updateReply(svcctx, reply);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-reply-update")
	public void handleTaskReplyUpdate(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_UPD_RPL);
		svcctx.addOperationPredicates(params);
		ArgsValidator.newValidator(params)
			.requireId("reply_id")
			.validate(true);
		
		long replyid = Filters.filterLong(params, "reply_id");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.update.reply"));
			
		TaskReplyInfo reply = new TaskReplyInfo();
		
		reply.setInfoId(IdKeys.getInfoId(NodeIdKey.TASK_REPLY, replyid));
		if(params.containsKey("content")) {
			reply.setContent(Filters.filterString(params, "content"));
		}
		if(params.containsKey("state")) {
			reply.setState(Filters.filterString(params, "state"));
		}
		Set<String> keys = params.keySet();
		keys.remove("reply_id"); // reserve property keys only
		
		reply.setPropertyFilter(keys);
		
		taskCommentService.updateReply(svcctx, reply);
		
		this.sendResult(exchange, result);
	}
	
	@WebApi(path="task-reply-remove")
	public void handleTaskRemoveReply(HttpServerExchange exchange) throws BaseException {
		
		Map<String, Object> params = this.getRequestBody(exchange);
		ServiceContext svcctx = this.getServiceContext(exchange, Operations.TSK_RMV_RPL);
		svcctx.addOperationPredicates(params);
		
		ArgsValidator.newValidator(params)
			.requireId("reply_id")
			.validate(true);
		
		long replyid = Filters.filterLong(params, "reply_id");
		
		ActionResult result = ActionResult.success(getMessage(exchange, "mesg.remove.reply"));
		
		InfoId replyId = IdKeys.getInfoId(NodeIdKey.TASK_REPLY, replyid);
		
		// set object id into context
		Long taskid = commonService.queryColumn(replyId, "target_id", Long.class);
		svcctx.addOperationPredicate("task_id", taskid);
		svcctx.setOperationObject(replyId);
				
		taskCommentService.removeReply(svcctx, replyId);
		
		this.sendResult(exchange, result);
	}
}
