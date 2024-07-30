package com.gp.core;

import com.gp.audit.SyncEventload;
import com.gp.bind.BindScanner;
import com.gp.bind.IBeanBinder;
import com.gp.common.*;
import com.gp.dao.info.AuditInfo;
import com.gp.dao.info.OperationInfo;
import com.gp.dao.info.SyncMqOutInfo;
import com.gp.dao.info.UserInfo;
import com.gp.exception.BaseException;
import com.gp.info.BaseIdKey;
import com.gp.info.InfoCopier;
import com.gp.mq.IProducer;
import com.gp.mq.MQMesg;
import com.gp.svc.AuditService;
import com.gp.svc.OperationService;
import com.gp.svc.SystemService;
import com.gp.svc.security.SecurityService;
import com.gp.svc.sync.SyncInternService;
import com.gp.sync.SyncProcesses;
import com.gp.sync.SyncRoute;
import com.gp.util.JsonUtils;
import com.gp.web.model.Audit;
import com.gp.web.model.Operation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Objects;
import java.util.Set;

/**
 * This class handle the operation events
 * 
 **/
public class PersistFacade implements IBeanBinder {

	static Logger LOGGER = LoggerFactory.getLogger(PersistFacade.class);

	private final AuditService auditService;
	private OperationService operationService;
	private SecurityService securityservice;
	private SystemService systemService;

	private SyncInternService syncService;

	private IProducer<?> producer ;

	private static PersistFacade Instance;

	private PersistFacade(){

		auditService = getBean(AuditService.class);

		operationService = getBean(OperationService.class);
		securityservice = getBean(SecurityService.class);
		systemService = getBean(SystemService.class);

		syncService =  BindScanner.instance().getBean(SyncInternService.class);

		//producer = MQManager.instance().getProducer(MQManager.TYPE_ROCKET);
	}

	public static PersistFacade instance() {

		if(null == Instance) {
			Instance = new PersistFacade();
		}

		return Instance;
	}


	public void persistAudit(Audit audit)  {

		if(Objects.isNull(audit))
			return ;

		AuditInfo info = new AuditInfo();
		InfoCopier.copy(audit, info);
		info.setPath(audit.getApi());
		info.setInfoId(audit.getAuditId());
		info.setModifierUid(GroupUsers.ADMIN_UID.getId());
		info.setModifyTime(new Date(System.currentTimeMillis()));

		auditService.addAudit( info );

	}

	/**
	 * Handle the core event payload
	 *
	 * @param  operation the payload of event
	 *
	 * @return InfoId null ignore further processing, otherwise build sync event {@link SyncEventload}
	 **/
	public InfoId persistOperation(Operation operation){

		if(LOGGER.isDebugEnabled()){
			LOGGER.debug("persist operation: {}", operation.getInstruct());
		}
		// prepare operation information
		OperationInfo operinfo = new OperationInfo();
		InfoId operid = IdKeys.newInfoId(BaseIdKey.OPERATION);
		operinfo.setInfoId(operid);

		operinfo.setSubject(operation.getOperator());
		operinfo.setOperation(operation.getInstruct().toString());

		if(IdKeys.isValidId(operation.getObjectId()))
			operinfo.setObject(operation.getObjectId().toString());

		operinfo.setOperationTime(operation.getOperateTime());
		operinfo.setAuditId(operation.getAuditId().getId());

		// set the predicates, json string
		operinfo.setPredicates(JsonUtils.toJson(operation.getPredicates()));
		if(LOGGER.isDebugEnabled()) {
			LOGGER.debug("operation: {} - subject: {} - predicates: {}",
					operinfo.getOperation(),
					operinfo.getSubject(),
					operinfo.getPredicates()
			);
		}

		KVPair<String, String> kvLabels = operationService.getObjectPredicateLabel(operation.getObjectId(), operation.getInstruct());
		if(kvLabels != null) {
			operinfo.setObjectLabel(kvLabels.getKey());
			operinfo.setSecondLabel(kvLabels.getValue());
		}

		ServiceContext svcctx = ServiceContext.getPseudoContext();
		svcctx.setTraceInfo(operinfo);

		operationService.addOperation(operinfo);
		operation.setOperId(operid);

		return operid;

	}

	/**
	 * Build the synchronize message out
	 *
	 * @param operation the SyncEventLoad
	 **/
	public void persistSync(Operation operation) throws BaseException {

		Instruct instruct = operation.getInstruct();
		if(!Instructs.SYN_INTN.equals(instruct)){
			LOGGER.debug("ignore operation as not {}", instruct);
			return;
		}
		try(ServiceContext svcctx = new ServiceContext(GroupUsers.pseudo())){


			SyncMqOutInfo msgOut = new SyncMqOutInfo();

			msgOut.setOperTime(new Date(IdKeys.parseTime(operation.getOperId().getId())));
			msgOut.setTraceId(operation.getObjectId() == null ? null : operation.getObjectId().getId());
			msgOut.setOperCmd(instruct.toString());

			if(null != operation.getOperator()) {
				UserInfo userInfo = securityservice.getUserInfo(null, operation.getOperator(), null, null);
				msgOut.setOperatorId(userInfo.getId());
			}

			// rebuild a new payload for sync-out message
			SyncRoute syncRoute = SyncProcesses.instance().assembly(operation.getInstruct(), operation.getObjectId(), operation.getPredicates());

			if(syncRoute == null) {
				return ;
			}

			String json = JsonUtils.toJson(syncRoute.getSyncData());
			msgOut.setPayload(json);

			msgOut.setState(Syncs.MessageState.PENDING.name());
			if(syncRoute.isTargetDest(Syncs.SyncOrigin.INTERN)) {

				InfoId outId = IdKeys.newInfoId(AppIdKey.SYNC_MQ_OUT);
				msgOut.setInfoId(outId);

				Set<String> destKey = syncRoute.getDestKey(Syncs.SyncOrigin.INTERN);
				destKey.forEach(k -> {
					String topic = k;
					String tag = "*";
					if(k.indexOf(GeneralConsts.KEYS_SEPARATOR) > 0){
						topic = k.substring(0, k.indexOf(GeneralConsts.KEYS_SEPARATOR));
						tag = k.substring(k.indexOf(GeneralConsts.KEYS_SEPARATOR) + 1);
					}
					msgOut.setDestTopic(k);

					msgOut.setDestSys(tag);

					if(LOGGER.isDebugEnabled()){
						LOGGER.debug("Save mq message out");
					}
					syncService.saveMesgOut(svcctx, msgOut);

					MQMesg mesg = syncService.convert(msgOut);
					if(LOGGER.isDebugEnabled()){
						LOGGER.debug("publish mq message out: {} - {}", topic, tag);
					}

					if(null == producer) {
						producer.publish(topic, tag, mesg);
					}
				});

			}

		}
	}
}
