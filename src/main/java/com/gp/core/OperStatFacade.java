/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.core;

import com.gp.bind.BindScanner;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.Instruct;
import com.gp.common.NodeIdKey;
import com.gp.common.Operations;
import com.gp.info.BaseIdKey;
import com.gp.svc.CommonService;
import com.gp.svc.task.TaskStatService;
import com.gp.svc.topic.TopicStatService;
import com.gp.svc.user.UserStatService;
import com.gp.svc.wgroup.WGroupStatService;

/**
 * Handle the operation events that cause the statistics change.
 * 
 * @author gdiao
 * 
 * @version 0.4
 * 
 **/
public class OperStatFacade {

	static private OperStatFacade Instance;
	
	private TopicStatService topicStatService;
	private TaskStatService taskStatService;
	private WGroupStatService wgroupStatService;
	private UserStatService userStatService;
	private CommonService commonService;
	
	private OperStatFacade() {
		commonService = BindScanner.instance().getBean(CommonService.class);
		topicStatService = BindScanner.instance().getBean(TopicStatService.class);
		taskStatService = BindScanner.instance().getBean(TaskStatService.class);
		userStatService = BindScanner.instance().getBean(UserStatService.class);
		wgroupStatService = BindScanner.instance().getBean(WGroupStatService.class);
	}

	/**
	 * Singleton instance 
	 **/
	public static OperStatFacade instance() {
		
		if(null == Instance) {
			Instance = new OperStatFacade();
		}		
		return Instance;
	}
	
	/**
	 * Update workgroup, topic and answer statistics when operation event emits. 
	 * 
	 * @param coreload the core event load
	 **/
	public void handleStatEventLoad(CoreEventload coreload){
		
		Instruct op = coreload.getOperation();
		InfoId objectKey = coreload.getObjectId();
		if(null == op) return;
		
		if((Operations.WGRP_ADD_MBR.equals(op) || Operations.WGRP_RMV_MBR.equals(op))&& IdKeys.isValidId(objectKey)) {
			wgroupStatService.collectSummary(objectKey, NodeIdKey.GROUP_USER);
			return;
		}
		
		// Topic related
		if(Operations.TPC_NEW.equals(op) && IdKeys.isValidId(objectKey)) {
			Long wgrpId = commonService.queryColumn(objectKey, "workgroup_id", Long.class);
			wgroupStatService.collectSummary(IdKeys.getInfoId(NodeIdKey.WORKGROUP, wgrpId), NodeIdKey.TOPIC);
			
			Long userid = commonService.queryColumn(objectKey, "owner_uid", Long.class);
			userStatService.updateUserStat(IdKeys.getInfoId(BaseIdKey.USER, userid), NodeIdKey.TOPIC);
			
			return;
		}
		
		if(Operations.TPC_RMV.equals(op) && IdKeys.isValidId(objectKey)) {
			Long wgrpId = (Long)coreload.getPredicates().get("workgroup_id");
			wgroupStatService.collectSummary(IdKeys.getInfoId(NodeIdKey.WORKGROUP, wgrpId), NodeIdKey.TOPIC);
			
			Long userid = (Long)coreload.getPredicates().get("owner_uid");
			userStatService.updateUserStat(IdKeys.getInfoId(BaseIdKey.USER, userid), NodeIdKey.TOPIC);
			return;
		}
		
		if((Operations.TPC_PUB.equals(op) || Operations.TPC_UPUB.equals(op)) && IdKeys.isValidId(objectKey)) {
			Long wgrpId = commonService.queryColumn(objectKey, "workgroup_id", Long.class);
			
			topicStatService.updateTopicStat(objectKey, NodeIdKey.TOPIC_PUBLISH);
			wgroupStatService.collectSummary(IdKeys.getInfoId(NodeIdKey.WORKGROUP, wgrpId), NodeIdKey.TOPIC_PUBLISH);
						
			return;
		}
		
		if(Operations.TPC_ADD_CMT.equals(op) && IdKeys.isValidId(objectKey)) {
			Long tpcId = commonService.queryColumn(objectKey, "target_id", Long.class);
			topicStatService.updateTopicStat(IdKeys.getInfoId(NodeIdKey.TOPIC, tpcId), NodeIdKey.TOPIC_COMMENT);
			return;
		}
		
		if(Operations.TPC_RMV_CMT.equals(op) && IdKeys.isValidId(objectKey)) {
			Long tpcId = (Long)coreload.getPredicates().get("topic_id");
			topicStatService.updateTopicStat(IdKeys.getInfoId(NodeIdKey.TOPIC, tpcId), NodeIdKey.TOPIC_COMMENT);
			return;
		}
		
		if(Operations.TPC_ADD_RPL.equals(op) && IdKeys.isValidId(objectKey)) {
			Long tpcId = commonService.queryColumn(objectKey, "target_id", Long.class);
			topicStatService.updateTopicStat(IdKeys.getInfoId(NodeIdKey.TOPIC, tpcId), NodeIdKey.TOPIC_COMMENT, NodeIdKey.TOPIC_REPLY);
			return;
		}
		
		if(Operations.TPC_RMV_RPL.equals(op) && IdKeys.isValidId(objectKey)) {
			Long tpcId = (Long)coreload.getPredicates().get("topic_id");
			topicStatService.updateTopicStat(IdKeys.getInfoId(NodeIdKey.TOPIC, tpcId), NodeIdKey.TOPIC_COMMENT, NodeIdKey.TOPIC_REPLY);
			return;
		}
		
		// Answer related
		if(Operations.ANS_NEW.equals(op) && IdKeys.isValidId(objectKey)) {
			Long wgrpId = commonService.queryColumn(objectKey, "workgroup_id", Long.class);
			Long tpcId = commonService.queryColumn(objectKey, "topic_id", Long.class);
			
			topicStatService.updateTopicStat(IdKeys.getInfoId(NodeIdKey.TOPIC, tpcId), NodeIdKey.TOPIC_ANSWER);			
			wgroupStatService.collectSummary(IdKeys.getInfoId(NodeIdKey.WORKGROUP, wgrpId), NodeIdKey.TOPIC_ANSWER);
			return;
		}
		
		if(Operations.ANS_RMV.equals(op) && IdKeys.isValidId(objectKey)) {
			Long wgrpId = (Long)coreload.getPredicates().get("workgroup_id");
			Long tpcId = (Long)coreload.getPredicates().get("topic_id");
			
			topicStatService.updateTopicStat(IdKeys.getInfoId(NodeIdKey.TOPIC, tpcId), NodeIdKey.TOPIC_ANSWER);	
			wgroupStatService.collectSummary(IdKeys.getInfoId(NodeIdKey.WORKGROUP, wgrpId), NodeIdKey.TOPIC_ANSWER);
			return;
		}
		
		if(Operations.ANS_ADD_CMT.equals(op) && IdKeys.isValidId(objectKey)) {
			Long answerId = commonService.queryColumn(objectKey, "target_id", Long.class);
			topicStatService.updateTopicStat(IdKeys.getInfoId(NodeIdKey.TOPIC_ANSWER, answerId), NodeIdKey.TOPIC_COMMENT);
			return;
		}
		
		if(Operations.ANS_RMV_CMT.equals(op) && IdKeys.isValidId(objectKey)) {
			Long answerId = (Long)coreload.getPredicates().get("answer_id");
			topicStatService.updateTopicStat(IdKeys.getInfoId(NodeIdKey.TOPIC_ANSWER, answerId), NodeIdKey.TOPIC_COMMENT);
			return;
		}
		
		if(Operations.ANS_ADD_RPL.equals(op) && IdKeys.isValidId(objectKey)) {
			Long answerId = commonService.queryColumn(objectKey, "target_id", Long.class);
			topicStatService.updateTopicStat(IdKeys.getInfoId(NodeIdKey.TOPIC_ANSWER, answerId), NodeIdKey.TOPIC_COMMENT, NodeIdKey.TOPIC_REPLY);
			return;
		}
		
		if(Operations.ANS_RMV_RPL.equals(op) && IdKeys.isValidId(objectKey)) {
			Long answerId = (Long)coreload.getPredicates().get("answer_id");
			topicStatService.updateTopicStat(IdKeys.getInfoId(NodeIdKey.TOPIC_ANSWER, answerId), NodeIdKey.TOPIC_COMMENT, NodeIdKey.TOPIC_REPLY);
			return;
		}
		
		// Task related
		if(Operations.TSK_NEW.equals(op) && IdKeys.isValidId(objectKey)) {
			Long wgrpId = commonService.queryColumn(objectKey, "workgroup_id", Long.class);
			wgroupStatService.collectSummary(IdKeys.getInfoId(NodeIdKey.WORKGROUP, wgrpId), NodeIdKey.TASK);
			
			Long userid = commonService.queryColumn(objectKey, "owner_uid", Long.class);
			userStatService.updateUserStat(IdKeys.getInfoId(BaseIdKey.USER, userid), NodeIdKey.TASK);
			
			taskStatService.updateTaskStat(objectKey, NodeIdKey.TASK_ATTACH);
			return;
		}
		
		if(Operations.TSK_RMV.equals(op) && IdKeys.isValidId(objectKey)) {
			Long wgrpId = (Long)coreload.getPredicates().get("workgroup_id");
			wgroupStatService.collectSummary(IdKeys.getInfoId(NodeIdKey.WORKGROUP, wgrpId), NodeIdKey.TASK);
			
			Long userid = (Long)coreload.getPredicates().get("owner_uid");
			userStatService.updateUserStat(IdKeys.getInfoId(BaseIdKey.USER, userid), NodeIdKey.TASK);
			return;
		}
		
		if(Operations.TSK_ADD_CMT.equals(op) && IdKeys.isValidId(objectKey)) {
			Long taskId = commonService.queryColumn(objectKey, "target_id", Long.class);
			taskStatService.updateTaskStat(IdKeys.getInfoId(NodeIdKey.TASK, taskId), NodeIdKey.TASK_COMMENT);
			return;
		}
		
		if(Operations.TSK_RMV_CMT.equals(op) && IdKeys.isValidId(objectKey)) {
			Long tpcId = (Long)coreload.getPredicates().get("task_id");
			taskStatService.updateTaskStat(IdKeys.getInfoId(NodeIdKey.TASK, tpcId), NodeIdKey.TASK_COMMENT);
			return;
		}
		
		if(Operations.TSK_ADD_RPL.equals(op) && IdKeys.isValidId(objectKey)) {
			Long tpcId = commonService.queryColumn(objectKey, "target_id", Long.class);
			topicStatService.updateTopicStat(IdKeys.getInfoId(NodeIdKey.TASK, tpcId), NodeIdKey.TASK_COMMENT, NodeIdKey.TASK_REPLY);
			return;
		}
		
		if(Operations.TSK_RMV_RPL.equals(op) && IdKeys.isValidId(objectKey)) {
			Long tpcId = (Long)coreload.getPredicates().get("task_id");
			topicStatService.updateTopicStat(IdKeys.getInfoId(NodeIdKey.TASK, tpcId), NodeIdKey.TASK_COMMENT, NodeIdKey.TASK_REPLY);
			return;
		}
		
		if(Operations.TSK_ADD_DLV.equals(op) && IdKeys.isValidId(objectKey)) {
			Long tpcId = commonService.queryColumn(objectKey, "task_id", Long.class);
			topicStatService.updateTopicStat(IdKeys.getInfoId(NodeIdKey.TASK, tpcId), NodeIdKey.TASK_COMMENT, NodeIdKey.TASK_REPLY);
			return;
		}
		
		if(Operations.TSK_RMV_DLV.equals(op) && IdKeys.isValidId(objectKey)) {
			Long tpcId = (Long)coreload.getPredicates().get("task_id");
			topicStatService.updateTopicStat(IdKeys.getInfoId(NodeIdKey.TASK, tpcId), NodeIdKey.TASK_COMMENT, NodeIdKey.TASK_REPLY);
			return;
		}
		
		if(Operations.CHK_NEW.equals(op) && IdKeys.isValidId(objectKey)) {
			Long tskId = commonService.queryColumn(objectKey, "target_id", Long.class);
			taskStatService.updateTaskStat(IdKeys.getInfoId(NodeIdKey.TASK, tskId), NodeIdKey.TASK_CHECK);
			
			taskStatService.updateTaskStat(objectKey, NodeIdKey.TASK_ATTACH);
			return;
		}
		
		if(Operations.CHK_RMV.equals(op) && IdKeys.isValidId(objectKey)) {
			Long tskId = (Long)coreload.getPredicates().get("task_id");
			taskStatService.updateTaskStat(IdKeys.getInfoId(NodeIdKey.TASK, tskId), NodeIdKey.TASK_CHECK);
			
			taskStatService.updateTaskStat(objectKey, NodeIdKey.TASK_ATTACH);
			return;
		}
		
		if(Operations.CHK_ADD_CMT.equals(op) && IdKeys.isValidId(objectKey)) {
			Long checkid = commonService.queryColumn(objectKey, "target_id", Long.class);
			taskStatService.updateTaskStat(IdKeys.getInfoId(NodeIdKey.TASK_CHECK, checkid), NodeIdKey.TASK_COMMENT);
			return;
		}
		
		if(Operations.CHK_RMV_CMT.equals(op) && IdKeys.isValidId(objectKey)) {
			Long checkid = (Long)coreload.getPredicates().get("check_id");
			taskStatService.updateTaskStat(IdKeys.getInfoId(NodeIdKey.TASK_CHECK, checkid), NodeIdKey.TASK_COMMENT);
			return;
		}
		
		if(Operations.CHK_ADD_RPL.equals(op) && IdKeys.isValidId(objectKey)) {
			Long checkid = commonService.queryColumn(objectKey, "target_id", Long.class);
			taskStatService.updateTaskStat(IdKeys.getInfoId(NodeIdKey.TASK_CHECK, checkid), NodeIdKey.TASK_COMMENT, NodeIdKey.TASK_REPLY);
			return;
		}
		
		if(Operations.CHK_RMV_RPL.equals(op) && IdKeys.isValidId(objectKey)) {
			Long checkid = (Long)coreload.getPredicates().get("check_id");
			taskStatService.updateTaskStat(IdKeys.getInfoId(NodeIdKey.TASK_CHECK, checkid), NodeIdKey.TASK_COMMENT, NodeIdKey.TASK_REPLY);
			return;
		}
		
		// Favorite related
		if(Operations.FAV_NEW.equals(op) || Operations.FAV_RMV.equals(op)) {
			Long userid = (Long)coreload.getPredicates().get("collector_uid");
			userStatService.updateUserStat(IdKeys.getInfoId(BaseIdKey.USER, userid), NodeIdKey.USER_FAVORITE);
		}
		
		if(Operations.FOL_NEW.equals(op) || Operations.FOL_RMV.equals(op)) {
			Long userid = (Long)coreload.getPredicates().get("user_id");
			userStatService.updateUserStat(IdKeys.getInfoId(BaseIdKey.USER, userid), NodeIdKey.USER_FOLLOW);
			
			Long followuid = (Long)coreload.getPredicates().get("follower_uid");
			userStatService.updateUserStat(IdKeys.getInfoId(BaseIdKey.USER, followuid), NodeIdKey.USER_FOLLOW);
		}
	}
}
