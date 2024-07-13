package com.gp.svc.security;


import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.gp.bind.BindAutowired;
import com.gp.bind.BindComponent;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.MasterIdKey;
import com.gp.common.ServiceContext;
import com.gp.dao.DutyFlatDAO;
import com.gp.dao.DutyHierDAO;
import com.gp.dao.ext.DutyExt;
import com.gp.dao.info.DutyHierInfo;
import com.gp.db.JdbiTran;
import com.gp.sql.SqlBuilder;
import com.gp.sql.delete.DeleteBuilder;
import com.gp.sql.select.SelectBuilder;
import com.gp.svc.BaseService;
import com.gp.svc.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@BindComponent( priority = BaseService.BASE_PRIORITY)
public class DutyHierService extends ServiceSupport implements BaseService {

    static Logger LOGGER = LoggerFactory.getLogger(DutyHierService.class);

    @BindAutowired
    private DutyHierDAO dutyHierDAO;

    @BindAutowired
    private DutyFlatDAO dutyFlatDAO;

    @BindAutowired
    private DutyExt dutyExt;

    @JdbiTran
    public InfoId newDutyHierNode(ServiceContext svcctx, DutyHierInfo info) {

        svcctx.setTraceInfo(info);

        InfoId dutyKey = IdKeys.newInfoId(MasterIdKey.DEPT_HIER);
        info.setInfoId(dutyKey);

        int cnt = dutyHierDAO.create(info);

        dutyExt.processDutyFlat(dutyKey);

        return cnt > 0 ? info.getInfoId() : null;

    }

    @JdbiTran(readOnly = true)
    public List<DutyHierInfo> getDutyHierNodes(String keyword, InfoId parentNodeId, Long orgid) {

        List<DutyHierInfo> list = null;
        SelectBuilder builder = SqlBuilder.select();
        builder.column("d.*");
        builder.column("cd.child_count");
        builder.from( from -> {
           from.table(MasterIdKey.DUTY_HIER.schema("d"));
           from.leftJoin("(SELECT duty_pid, count(duty_id) AS child_count FROM gp_duty_hier GROUP BY duty_pid) AS cd", "d.duty_id = cd.duty_pid");
        });
        Map<String, Object> params = Maps.newHashMap();
        if (!Strings.isNullOrEmpty(keyword)) {
            builder.and("d.duty_name like :name");
            params.put("name", keyword + "%");
        }
        if (IdKeys.isValidId(parentNodeId)) {
            builder.and("d.duty_pid = :pid");
            params.put("pid", parentNodeId.getId());
        }
        if(IdKeys.isValidId(orgid)){
            builder.and("d.org_id = :org");
            params.put("org", orgid);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SQL: {} / PARAM: {}", builder.toString(), params);
        }

        list = rows(builder.toString(), dutyHierDAO.getRowMapper((info, rs) -> {
            info.setProperty("child_count", rs.getInt("child_count"));
        }), params);

        return list;
    }

    @JdbiTran
    public boolean saveDutyHierNode(ServiceContext svcctx, DutyHierInfo info) {

        svcctx.setTraceInfo(info);

        return dutyHierDAO.update(info) > 0;

    }

    @JdbiTran(readOnly = true)
    public DutyHierInfo getDutyHierNode(InfoId orgid) {

        return dutyHierDAO.row(orgid);

    }

    @JdbiTran(readOnly = true)
    public boolean removeDutyHierNode(InfoId dutyKey) {

        // remove sub-dept
        DeleteBuilder builder = dutyHierDAO.deleteSql();
        builder.or("duty_id IN ("
                + " SELECT f.duty_leaf_id FROM gp_duty_flat f "
                + " WHERE f.duty_pid = ?"
                + ")");
        List<Object> params = Arrays.asList(dutyKey.getId());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SQL : " + builder.toString() + " / params : " + params);
        }
        update(builder.toString(), params);

        // remove the flat table rows
        params = Arrays.asList(dutyKey.getId(), dutyKey.getId());
        builder = dutyFlatDAO.deleteSql();
        builder.where("duty_pid = ?");
        builder.or("duty_leaf_id = ?");
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SQL : " + builder.toString() + " / params : " + params);
        }
        update(builder.toString(), params);

        return dutyHierDAO.delete(dutyKey) > 0;

    }

}
