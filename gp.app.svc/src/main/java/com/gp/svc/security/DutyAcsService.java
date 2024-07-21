package com.gp.svc.security;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.gp.bind.BindAutowired;
import com.gp.bind.BindComponent;
import com.gp.common.AppIdKey;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.ServiceContext;
import com.gp.dao.DutyAcsDAO;
import com.gp.dao.DutyHierDAO;
import com.gp.dao.info.DutyAcsInfo;
import com.gp.db.JdbiTran;
import com.gp.exception.ServiceException;
import com.gp.sql.SqlBuilder;
import com.gp.sql.select.SelectBuilder;
import com.gp.svc.BaseService;
import com.gp.svc.ServiceSupport;
import com.gp.svc.TranService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@BindComponent( priority = BaseService.BASE_PRIORITY)
public class DutyAcsService extends ServiceSupport implements TranService {

    static Logger LOGGER = LoggerFactory.getLogger(DutyAcsService.class);

    @BindAutowired
    private DutyHierDAO dutyHierDAO;

    @BindAutowired
    private DutyAcsDAO dutyAcsDAO;

    @JdbiTran(readOnly = true)
    public List<DutyAcsInfo> getDutyAcses(InfoId dutyKey, InfoId sysId, InfoId dataKey){

        SelectBuilder builder = SqlBuilder.select();
        builder.column("a.*");
        builder.column("s.sys_name");
        builder.column("d.data_name");

        builder.from( from -> {
           from.table(AppIdKey.DUTY_ACS.schema("a"));
           from.leftJoin(AppIdKey.SYS_SUB.schema("s"), "s.sys_id = a.sys_id");
           from.leftJoin(AppIdKey.DATA_MODEL.schema("d"), "d.data_id = a.data_id");
        });

        Map<String, Object> params = Maps.newHashMap();
        builder.where("a.duty_id = :duty");
        params.put("duty", dutyKey.getId());

        if(IdKeys.isValidId(sysId)){
            builder.and("a.sys_id = :sys");
            params.put("sys", sysId.getId());
        }

        if(IdKeys.isValidId(dataKey)){

            builder.and("a.data_id = :data");
            params.put("data", dataKey.getId());
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SQL : {} / PARAM : {}", builder.build(), params);
        }
        return rows(builder.build(), dutyAcsDAO.getRowMapper((info, rs) -> {
            info.setProperty("sys_name", rs.getString("sys_name"));
            info.setProperty("data_name", rs.getString("data_name"));
        }), params);

    }

    @JdbiTran(readOnly = true)
    public DutyAcsInfo getDutyAcs(InfoId acsKey){

        SelectBuilder builder = SqlBuilder.select();
        builder.column("a.*");
        builder.column("s.sys_name");
        builder.column("d.data_name");

        builder.from( from -> {
            from.table(AppIdKey.DUTY_ACS.schema("a"));
            from.leftJoin(AppIdKey.SYS_SUB.schema("s"), "s.sys_id = a.sys_id");
            from.leftJoin(AppIdKey.DATA_MODEL.schema("d"), "d.data_id = a.data_id");
        });

        Map<String, Object> params = Maps.newHashMap();
        builder.where("a.access_id = :acs");
        params.put("acs", acsKey.getId());

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SQL : {} / PARAM : {}", builder.build(), params);
        }
        return row(builder.build(), dutyAcsDAO.getRowMapper((info, rs) -> {
            info.setProperty("sys_name", rs.getString("sys_name"));
            info.setProperty("data_name", rs.getString("data_name"));
        }), params);

    }

    @JdbiTran
    public InfoId addDutyAcs(ServiceContext svcctx, DutyAcsInfo acs) throws ServiceException {

        SelectBuilder countSql = SqlBuilder.select(AppIdKey.SYS_SUB.schema());
        countSql.column("count(sys_id)");
        countSql.where("sys_id = ?");

        int exist = count(countSql.build(), Lists.newArrayList(acs.getSysId()));
        if(0 == exist){
            throw new ServiceException("system not exists");
        }

        countSql = SqlBuilder.select(AppIdKey.DATA_MODEL.schema());
        countSql.column("count(data_id)");
        countSql.where("data_id = ?").and("sys_id = ?");

        exist = count(countSql.build(), Lists.newArrayList(acs.getDataId(), acs.getSysId()));
        if(0 == exist){
            throw new ServiceException("data model not exists");
        }

        // duplicated access rule check
        exist = dutyAcsDAO.count(cond -> {
            cond.and("sys_id = ?").and("data_id = ?").and("duty_id = ?");
        }, acs.getSysId(), acs.getDataId(), acs.getDutyId() );
        if(exist > 0){
            throw new ServiceException("data acs exists already");
        }

        svcctx.setTraceInfo(acs);
        int cnt = dutyAcsDAO.create(acs);

        return cnt > 0 ? acs.getInfoId() : null;
    }

    @JdbiTran
    public int saveDutyAcs(ServiceContext svcctx, DutyAcsInfo acs) throws ServiceException{

        SelectBuilder countSql = SqlBuilder.select(AppIdKey.SYS_SUB.schema());
        countSql.column("count(sys_id)");
        countSql.where("sys_id = ?");

        int exist = count(countSql.build(), Lists.newArrayList(acs.getSysId()));
        if(0 == exist && acs.getFilter().check("sys_id")){
            throw new ServiceException("system not exists");
        }

        countSql = SqlBuilder.select(AppIdKey.DATA_MODEL.schema());
        countSql.column("count(data_id)");
        countSql.where("data_id = ?").and("sys_id = ?");

        exist = count(countSql.build(), Lists.newArrayList(acs.getDataId(), acs.getSysId()));
        if(0 == exist){
            throw new ServiceException("data model not exists");
        }

        svcctx.setTraceInfo(acs);
        int cnt = dutyAcsDAO.update(acs);

        return cnt;
    }

    @JdbiTran
    public int removeDutyAcs(InfoId acsKey){

        return dutyAcsDAO.delete(acsKey, true);
    }
}
