package com.gp.dao.ext;

import com.google.common.collect.Lists;
import com.gp.bind.BindComponent;
import com.gp.dao.DAOSupport;
import com.gp.dao.ExtendDAO;
import com.gp.info.BaseIdKey;
import com.gp.sql.SqlBuilder;
import com.gp.sql.select.SelectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@BindComponent(type = Sync1Ext.class, priority = ExtendDAO.BASE_PRIORITY )
public class Sync1Ext extends DAOSupport implements ExtendDAO {

    static Logger LOGGER = LoggerFactory.getLogger(Sync1Ext.class);

    public Sync1Ext(){
        this.setDataSource("secondary");
    }

    public void testCount(){

        SelectBuilder select = SqlBuilder.select(BaseIdKey.AUDIT);
        select.column("COUNT(1)");

        int cnt = count(select.build(), Lists.newArrayList());

        LOGGER.debug("count {}", cnt);
    }
}
