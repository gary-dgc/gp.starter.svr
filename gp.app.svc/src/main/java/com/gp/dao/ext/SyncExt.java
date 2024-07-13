package com.gp.dao.ext;

import com.gp.bind.BindComponent;
import com.gp.dao.BaseDAO;
import com.gp.dao.DAOSupport;
import com.gp.dao.ExtendDAO;
import com.gp.dao.info.SyncMqOutInfo;
import com.gp.mq.MQMesg;

@BindComponent(type = SyncExt.class, priority = BaseDAO.BASE_PRIORITY )
public class SyncExt extends DAOSupport implements ExtendDAO {

    public MQMesg getMQMessage(SyncMqOutInfo outInfo){

        MQMesg rtv = new MQMesg();
        rtv.setTraceId(outInfo.getTraceId());
        rtv.setCommand(outInfo.getOperCmd());
        rtv.setSystem(outInfo.getDestSys());
        rtv.setPayload(outInfo.getPayload());

        return rtv;
    }
}
