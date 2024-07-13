package com.gp.svc.sync;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.gp.bind.BindAutowired;
import com.gp.bind.BindComponent;
import com.gp.common.InfoId;
import com.gp.dao.SyncTraceDAO;
import com.gp.dao.info.SyncTraceInfo;
import com.gp.db.JdbiTran;
import com.gp.paging.PageQuery;
import com.gp.paging.Pagination;
import com.gp.paging.Paginator;
import com.gp.sql.BaseBuilder;
import com.gp.sql.select.SelectBuilder;
import com.gp.svc.BaseService;
import com.gp.svc.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@BindComponent(priority = BaseService.BASE_PRIORITY)
public class SyncTraceService extends ServiceSupport implements BaseService {

    static Logger LOGGER = LoggerFactory.getLogger(SyncTraceService.class);

    @BindAutowired
    private SyncTraceDAO traceDAO;

    @JdbiTran(readOnly = true)
    public List<SyncTraceInfo> getSyncTraces(String hierKey, String op, PageQuery pquery){

        SelectBuilder builder = traceDAO.selectSql();

        Map<String, Object> params= Maps.newHashMap();
        params.put("hier_key", "%" + Strings.nullToEmpty(hierKey).trim() + "%");

        builder.all();
        builder.where("hier_key LIKE :hier_key");

        if(!Strings.isNullOrEmpty(op)){
            params.put("trace_op", op);
            builder.and("trace_op = :trace_op");
        }

        if (Objects.nonNull(pquery)) {
            SelectBuilder countBuilder = builder.clone();
            countBuilder.column().column("count(trace_id)");
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("SQL : {} / PARAMS : {}", countBuilder.build(), params);
            }
            Integer total = row(countBuilder.toString(), Integer.class, params);
            Paginator paginator = new Paginator(total, pquery);
            Pagination pagination = paginator.getPagination();
            pquery.setPagination(pagination);

            BaseBuilder.SortOrder orderType = BaseBuilder.SortOrder.valueOf(pquery.getOrder().toUpperCase());

            builder.orderBy(pquery.getOrderBy(), orderType);
            builder.limit(pagination.getPageStartRow(), pquery.getPageSize());
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SQL : {} / PARAMS : {}", builder.toString(), params);
        }

        return rows(builder.toString(), SyncTraceDAO.INFO_MAPPER, params);
    }

    @JdbiTran(readOnly = true)
    public List<SyncTraceInfo> getSyncTraces(Long chronicalId){
        SelectBuilder builder = traceDAO.selectSql();

        Map<String, Object> params= Maps.newHashMap();
        params.put("chronical_id", chronicalId);

        builder.all();
        builder.where("chronical_id = :chronical_id");

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SQL : {} / PARAMS : {}", builder.toString(), params);
        }

        return rows(builder.toString(), SyncTraceDAO.INFO_MAPPER, params);
    }

    @JdbiTran(readOnly = true)
    public SyncTraceInfo getSyncTrace(InfoId trcId){

        return traceDAO.row(trcId);
    }
}
