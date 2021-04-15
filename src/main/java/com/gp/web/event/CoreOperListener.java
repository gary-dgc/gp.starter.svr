package com.gp.web.event;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gp.common.InfoId;
import com.gp.core.CoreEventload;
import com.gp.core.OperStatFacade;
import com.gp.core.OperSyncFacade;
import com.gp.eventbus.EventListener;
import com.gp.eventbus.EventPayload;
import com.gp.eventbus.EventType;
import com.gp.exception.BaseException;

/**
 * Here generate the operation log for resource, member, workgroup
 * the trace logs are content of time line.
 * 
 *  @author gdiao 
 *  @version 0.1 2016-08-07
 *  
 **/
public class CoreOperListener extends EventListener {

	// the logger of listener
	static Logger LOGGER = LoggerFactory.getLogger(CoreOperListener.class);
	
	/**
	 * Constructor with event type 
	 **/
	public CoreOperListener() {
		super(EventType.CORE);
	}

	/**
	 * Process the event payload and populate the sync event payload as per 
	 * the continue sync flag. <br>
	 * sync check: if operator is not local user means it's a operation from 
	 * remote node. then don't sync.
	 * 
	 * @author gdiao
	 **/
	@Override
	public void process(EventPayload payload) throws BaseException {

		CoreEventload coreload = (CoreEventload) payload;
		// handle the operation processing
		InfoId operId = OperSyncFacade.instance().handleOperEventLoad(coreload);
		
		// hand over the sync event load to SyncEventHooker
		if(operId != null) {
			
			Collection<SyncEventLoad> events = OperSyncFacade.instance().buildSyncEventLoad( operId,  coreload);
			
			for(SyncEventLoad event: events) {
				
				coreload.addChainPayload(event);
			}
						
		}
		
		// try to handle the system statistics data
		OperStatFacade.instance().handleStatEventLoad(coreload);
	}

}
