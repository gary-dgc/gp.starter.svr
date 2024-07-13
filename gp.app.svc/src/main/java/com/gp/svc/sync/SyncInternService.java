package com.gp.svc.sync;

import com.gp.bind.BindAutowired;
import com.gp.bind.BindComponent;
import com.gp.common.InfoId;
import com.gp.common.ServiceContext;
import com.gp.dao.SyncMqInDAO;
import com.gp.dao.SyncMqOutDAO;
import com.gp.dao.SyncTraceDAO;
import com.gp.dao.ext.SyncExt;
import com.gp.dao.info.SyncMqInInfo;
import com.gp.dao.info.SyncMqOutInfo;
import com.gp.db.JdbiTran;
import com.gp.mq.MQMesg;
import com.gp.svc.BaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@BindComponent(priority = BaseService.BASE_PRIORITY)
public class SyncInternService {

    Logger LOGGER = LoggerFactory.getLogger(SyncInternService.class);

    @BindAutowired
    private SyncTraceDAO syncTraceDAO;

    @BindAutowired
    private SyncMqInDAO mqInDAO;

    @BindAutowired
    private SyncMqOutDAO mqOutDAO;

    @BindAutowired
    private SyncExt syncExt;

    @JdbiTran
    public InfoId saveMesgOut(ServiceContext context, SyncMqOutInfo outMesg){

        context.setTraceInfo(outMesg);
        int cnt = mqOutDAO.create(outMesg);
        return cnt > 0 ? outMesg.getInfoId() : null;
    }

    @JdbiTran
    public InfoId saveMesgIn(ServiceContext context, SyncMqInInfo inMesg){

        context.setTraceInfo(inMesg);
        int cnt = mqInDAO.create(inMesg);
        return cnt > 0 ? inMesg.getInfoId() : null;
    }


    public MQMesg convert(SyncMqOutInfo outMesg){

        return syncExt.getMQMessage(outMesg);
    }
}
