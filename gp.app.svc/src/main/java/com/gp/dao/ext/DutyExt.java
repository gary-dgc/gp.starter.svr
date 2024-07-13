package com.gp.dao.ext;

import com.gp.bind.BindComponent;
import com.gp.common.InfoId;
import com.gp.dao.BaseDAO;
import com.gp.dao.DAOSupport;
import com.gp.dao.ExtendDAO;
import com.gp.db.BaseHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@BindComponent(type = DutyExt.class, priority = ExtendDAO.BASE_PRIORITY )
public class DutyExt  extends DAOSupport implements ExtendDAO {

    static Logger LOGGER = LoggerFactory.getLogger(DutyExt.class);

    public void processDutyFlat(InfoId dutyId) {

        try(BaseHandle jhandle = this.getBaseHandle()){

            jhandle.call("call proc_duty_flat(?)", new Object[]{dutyId});
        }
    }
}
