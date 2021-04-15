package com.gp.web.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gp.core.OperSyncFacade;
import com.gp.eventbus.EventListener;
import com.gp.eventbus.EventPayload;
import com.gp.eventbus.EventType;
import com.gp.exception.BaseException;

/**
 * This hooker monitor the SYNC {@link EventType}, then push a sync command to sync node
 * 
 *  @author gdiao 
 *  @version 0.1 2016-08-07
 *  
 **/
public class CoreSyncListener extends EventListener {

	// the logger of listener
	static Logger LOGGER = LoggerFactory.getLogger(CoreSyncListener.class);

	public CoreSyncListener() {
		super(EventType.SYNC);
	}
	
	@Override
	public void process(EventPayload payload) throws BaseException {

		SyncEventLoad syncload = (SyncEventLoad) payload;	
		// build and save the sync mssage out
		OperSyncFacade.instance().saveSyncMsgOut(syncload);
	}

}
