package com.gp.core;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Enums;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.gp.bind.BindScanner;
import com.gp.common.GeneralContext.ExecState;
import com.gp.common.GroupUsers;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.Instruct;
import com.gp.common.Instructs;
import com.gp.common.KeyValuePair;
import com.gp.common.NodeIdKey;
import com.gp.common.Operations;
import com.gp.common.ServiceContext;
import com.gp.common.Synchronizes.MessageState;
import com.gp.common.Synchronizes.SyncOrigin;
import com.gp.dao.info.OperationInfo;
import com.gp.dao.info.SourceInfo;
import com.gp.dao.info.SyncMsgOutInfo;
import com.gp.dao.info.UserInfo;
import com.gp.exception.BaseException;
import com.gp.exception.CoreException;
import com.gp.info.BaseIdKey;
import com.gp.svc.CommonService;
import com.gp.svc.OperationService;
import com.gp.svc.master.SourceService;
import com.gp.svc.security.SecurityService;
import com.gp.svc.sync.SyncMsgService;
import com.gp.sync.SyncDataInfo;
import com.gp.sync.SyncServiceManager;
import com.gp.util.JsonUtils;
import com.gp.web.event.SyncEventLoad;
import com.gp.web.util.WebUtils;

/**
 * This class handle the operation events
 * 
 **/
public class OperSyncFacade {
	
	static Logger LOGGER = LoggerFactory.getLogger(OperSyncFacade.class);

	private OperationService operlogservice;
	private SecurityService securityservice;
	private SourceService sourceservice;
	private SyncMsgService syncservice;
	private CommonService commonService;
	
	private static OperSyncFacade Instance;
	
	private OperSyncFacade(){
		
		operlogservice = BindScanner.instance().getBean(OperationService.class);
		securityservice = BindScanner.instance().getBean(SecurityService.class);
		sourceservice = BindScanner.instance().getBean(SourceService.class);
		syncservice = BindScanner.instance().getBean(SyncMsgService.class);
		commonService = BindScanner.instance().getBean(CommonService.class);
	}
	
	public static OperSyncFacade instance() {
		
		if(null == Instance) {
			Instance = new OperSyncFacade();
		}
		
		return Instance;
	}
	
	/**
	 * Handle the core event payload
	 * 
	 * @param  coreload the payload of event
	 * 
	 * @return InfoId null ignore further processing, otherwise build sync event {@link SyncEventLoad}
	 **/
	public InfoId handleOperEventLoad(CoreEventload coreload){
		
		if(!coreload.isTraceable() || Instructs.UN_KWN.equals(coreload.getOperation())) {
			return null;
		}
		// prepare operation information
		OperationInfo operinfo = new OperationInfo();
		InfoId operid = IdKeys.newInfoId(BaseIdKey.OPERATION);
		operinfo.setInfoId(operid);		
		
		operinfo.setSubject(coreload.getOperator());
		operinfo.setOperation(coreload.getOperation().toString());
		
		if(IdKeys.isValidId(coreload.getObjectId()))
			operinfo.setObject(coreload.getObjectId().toString());
					
		operinfo.setOperationTime(new Date(coreload.getTimestamp()));
		operinfo.setAuditId(coreload.getAuditId().getId());
		
		InfoId wid = operlogservice.getRelatedWorkgroupId(coreload.getObjectId());
		if(null != wid) {
			operinfo.setWorkgroupId(wid.getId());
		}
		
		// set the predicates, json string
		operinfo.setPredicates(JsonUtils.toJson(coreload.getPredicates()));
		if(LOGGER.isDebugEnabled()) {
			LOGGER.debug("operation: {} - subject: {} - predicates: {}", 
					operinfo.getOperation(), 
					operinfo.getSubject(), 
					operinfo.getPredicates()
			);
		}

		KeyValuePair<String, String> kvLabels = operlogservice.getObjectPredicateLabel(coreload.getObjectId(), coreload.getOperation());
		if(kvLabels != null) {
			operinfo.setObjectLabel(kvLabels.getKey());
			operinfo.setSecondLabel(kvLabels.getValue());
		}
		
		ServiceContext svcctx = ServiceContext.getPseudoContext();
		InfoId operId = IdKeys.newInfoId(BaseIdKey.OPERATION);
		operinfo.setInfoId(operId);
		svcctx.setTraceInfo(operinfo);
		
		operlogservice.addOperation(operinfo);
		
		Optional<ExecState> option = Enums.getIfPresent(ExecState.class, coreload.getState());
		// Here we decide only [SUCCESS] operation trigger synchronization
		if(option.get() == ExecState.SUCCESS) {
			
			return operId;		
		} 
		
		return null;
			
	}
		
	/**
	 * Try to convert the {@link CoreEventload} into a {@link SyncEventLoad}
	 * If the SyncEventLoad is not null then it be attached to CoreEventLoad 
	 * as chain payload.
	 * 
	 * @param coreload the core event payload
	 * 
	 **/
	@SuppressWarnings("unchecked")
	public Collection<SyncEventLoad> buildSyncEventLoad(InfoId operId, CoreEventload coreload) {
		
		Collection<SyncEventLoad> rtv = Sets.newHashSet();
		
		// if operation is supported, then create a sync event load
		if(SyncServiceManager.getInstance().collectSupport(coreload.getOperation())) {
			// build child sync load or not
			SyncEventLoad syncload = new SyncEventLoad();
			
			syncload.setOperId(operId);
			syncload.setObjectId(coreload.getObjectId());
			syncload.setOperation(coreload.getOperation());
			syncload.setOperator(coreload.getOperator());
			syncload.setPredicates(coreload.getPredicates());
			 
			rtv.add(syncload);
			
		}
		
		// when following operations, notify to sync the workgroup's summary information
		if(Operations.CAB_FIL_NEW.equals(coreload.getOperation()) || 
		   Operations.TSK_NEW.equals(coreload.getOperation()) ||
		   Operations.TPC_NEW.equals(coreload.getOperation()) ||
		   Operations.CAB_FDR_NEW.equals(coreload.getOperation()) ||
		   Operations.WGRP_ADD_MBR.equals(coreload.getOperation()) ||
		   
		   Operations.CAB_FIL_RMV.equals(coreload.getOperation()) || 
		   Operations.TSK_RMV.equals(coreload.getOperation()) ||
		   Operations.TPC_RMV.equals(coreload.getOperation()) ||
		   Operations.CAB_FDR_RMV.equals(coreload.getOperation()) ||
		   Operations.WGRP_RMV_MBR.equals(coreload.getOperation())) { 
			
			InfoId wgrpId = operlogservice.getRelatedWorkgroupId(coreload.getObjectId());
			
			if(!IdKeys.isValidId(wgrpId)) {
				LOGGER.warn("Cannot find related workgroup id by resource id: {}", coreload.getObjectId());
				return null;
			}
			// build child sync load or not
			SyncEventLoad sumload = new SyncEventLoad();
			
			sumload.setObjectId(wgrpId);
			sumload.setOperId(operId);
			
			sumload.setOperation(Operations.WGRP_SUM);
			sumload.setOperator(coreload.getOperator());
			sumload.setPredicates(coreload.getPredicates());
			
			rtv.add(sumload);
		}
		
		/**
		 * CHNL_UPD/CHNL_BND/CHNL_UBND: sync to remote directly;
		 * CHNL_NEW: convert into CHNL_BND to remote
		 * CHNL_RMV: convert to CHNL_UBND to remote
		 **/
		if(Operations.CHNL_NEW.equals(coreload.getOperation())) {
			
			Collection<String> scopes = (Collection<String>)coreload.getPredicates().get("scopes");
			if(scopes != null && scopes.size() > 0) {
				// build child sync load or not
				SyncEventLoad syncload = new SyncEventLoad();
				
				syncload.setObjectId(coreload.getObjectId());
				syncload.setOperId(operId);
				
				syncload.setOperation(Operations.CHNL_BND);
				syncload.setOperator(coreload.getOperator());
				syncload.setPredicates(coreload.getPredicates());
				
				rtv.add(syncload);
			}
		}
		
		if(Operations.CHNL_RMV.equals(coreload.getOperation())) {
			
			// build child sync load or not
			SyncEventLoad syncload = new SyncEventLoad();
			
			syncload.setObjectId(coreload.getObjectId());
			syncload.setOperId(operId);
			
			syncload.setOperation(Operations.CHNL_UBND);
			syncload.setOperator(coreload.getOperator());
			syncload.setPredicates(coreload.getPredicates());
			
			rtv.add(syncload);
		}
		
		if(Operations.TPC_PUB.equals(coreload.getOperation()) ||
		   Operations.TPC_UPUB.equals(coreload.getOperation())) {
			
			Instruct oper = Operations.TPC_PUB.equals(coreload.getOperation()) ? Operations.CHNL_PUB : Operations.CHNL_UPUB;
			
			List<Map<String, String>> channels = (List<Map<String, String>>)coreload.getPredicates().get("channels");
			
			for(Map<String, String> channel : channels) {
				
				Map<String, Object> preds = Maps.newHashMap();
				preds.put("publish", channel); // channel includes: channel_code, scope
				preds.put("publisher_gid", coreload.getPredicates().get("publisher_gid"));
				
				// build child sync load or not
				SyncEventLoad syncload = new SyncEventLoad();
				
				syncload.setObjectId(coreload.getObjectId());
				syncload.setOperId(operId);
				
				syncload.setOperation(oper);
				syncload.setOperator(coreload.getOperator());
				
				syncload.setPredicates(preds);
				
				rtv.add(syncload);
			}
		}
		
		return rtv;
	}
	
	/**
	 * Build the synchronize message out 
	 * 
	 * @param syncload the SyncEventLoad
	 **/
	public void saveSyncMsgOut(SyncEventLoad syncload) throws BaseException{
		
		SyncDataInfo syncData = null;
		try(ServiceContext svcctx = new ServiceContext(GroupUsers.pseudo())){
			
			SyncMsgOutInfo msgOut = new SyncMsgOutInfo();
			
			msgOut.setOperId(syncload.getOperId().getId());

			msgOut.setOperTime(new Date(IdKeys.parseTime(syncload.getOperId().getId())));
			if(null != syncload.getOperator()) {
				UserInfo uinfo = securityservice.getUserInfo(null, syncload.getOperator(), null, null);
				msgOut.setOperatorGid(uinfo.getUserGid());
			}
			String nodeCode = null;
			
			SourceInfo srcInfo = sourceservice.getLocalSource();
			nodeCode = srcInfo.getNodeGid();
			if(IdKeys.isValidId(syncload.getObjectId())) {
				// first try to find in sync_bind table
				String objectCode = commonService.queryTrace(syncload.getObjectId());
				if(Strings.isNullOrEmpty(objectCode)) {
					// not found then generate local trace code
					objectCode = IdKeys.getTraceCode(nodeCode, syncload.getObjectId());
				}
				msgOut.setObjectCode(objectCode);
			}else {
				LOGGER.warn("Synchronize operation:{} not find object id ", syncload.getOperation());
			}
						
			// rebuild a new payload for sync-out message
			syncData = SyncServiceManager.getInstance().collect(
													syncload.getOperation(), 
													syncload.getObjectId(), 
													syncload.getPredicates()
												);
			
			if(syncData == null) {
				return ;
			}
			String json = JsonUtils.toJson(syncData.getSyncData());
			msgOut.setPayload(json);
			
			msgOut.setSyncCmd(syncload.getOperation().toString());
			msgOut.setState(MessageState.PENDING.name());
			
			if(syncData.isTargetDest(SyncOrigin.CENTER)) {

				InfoId outId = IdKeys.newInfoId(NodeIdKey.SYNC_MSG_OUT);
				msgOut.setInfoId(outId);

				String operCode = IdKeys.getTraceCode(nodeCode, outId);
				msgOut.setTraceCode(operCode);
				
				msgOut.setDestScope(SyncOrigin.CENTER.name());
				msgOut.setDestGid(srcInfo.getEntityGid());
				
				syncservice.newSyncMsgOut(svcctx, msgOut);
			}
			if(syncData.isTargetDest(SyncOrigin.GLOBAL)) {
				
				InfoId outId = IdKeys.newInfoId(NodeIdKey.SYNC_MSG_OUT);
				msgOut.setInfoId(outId);
				
				String operCode = IdKeys.getTraceCode(nodeCode, outId);
				msgOut.setTraceCode(operCode);
				
				msgOut.setDestScope(SyncOrigin.GLOBAL.name());
				msgOut.setDestGid(null);
				syncservice.newSyncMsgOut(svcctx, msgOut);
			}
			
		}catch(Exception se) {
			LOGGER.error("fail save sync out", se);
			throw new CoreException(se,"excp.sync.msg");
		}finally {
			
			if(null != syncData) {
				Collection<SyncOrigin> origins = syncData.getDestOrigins();
				SyncOrigin[] originAry = origins.toArray(new SyncOrigin[0]);
				
				String postData = JsonUtils.toJson(originAry);
				NodeApiAgent.instance().sendSyncPost(WebUtils.getAuthApiUri("sync-trigger"), postData);
			}
		}
	}
}
