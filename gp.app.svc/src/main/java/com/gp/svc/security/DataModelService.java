package com.gp.svc.security;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.gp.bind.BindAutowired;
import com.gp.bind.BindComponent;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.AppIdKey;
import com.gp.common.ServiceContext;
import com.gp.dao.DataModelDAO;
import com.gp.dao.info.DataModelInfo;
import com.gp.db.JdbiTran;
import com.gp.paging.PageQuery;
import com.gp.paging.Pagination;
import com.gp.paging.Paginator;
import com.gp.sql.BaseBuilder;
import com.gp.sql.SqlBuilder;
import com.gp.sql.select.SelectBuilder;
import com.gp.svc.BaseService;
import com.gp.svc.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@BindComponent( priority = BaseService.BASE_PRIORITY)
public class DataModelService extends ServiceSupport implements BaseService {

    static Logger LOGGER = LoggerFactory.getLogger(DataModelService.class);

    @BindAutowired
    private DataModelDAO dataModelDAO;

    @JdbiTran
    public InfoId addDataModel(ServiceContext svcctx, DataModelInfo info){

        svcctx.setTraceInfo(info);
        dataModelDAO.create(info);

        return info.getInfoId();
    }

    @JdbiTran
    public int saveDataModel(ServiceContext svcctx, DataModelInfo info){

        svcctx.setTraceInfo(info);
        return dataModelDAO.update(info);
    }

    @JdbiTran
    public int removeDataModel(InfoId key){

        return dataModelDAO.delete(key);
    }

    @JdbiTran(readOnly = true)
    public DataModelInfo getDataModel( InfoId dataKey){

        SelectBuilder builder = SqlBuilder.select();
        builder.column("d.*");
        builder.column("s.sys_name");

        builder.from(from -> {
           from.table("gp_data_model d");
           from.leftJoin("gp_sys_sub s", "d.sys_id = s.sys_id");
        });

        builder.where("d.data_id = ?");

        return row(builder.build(), dataModelDAO.getRowMapper((info, rs) -> {
            info.setProperty("sys_name", rs.getString("sys_name"));
        }), dataKey.getId());
    }

    @JdbiTran(readOnly = true)
    public List<DataModelInfo> getDataModels(String keyword, Long sysId, PageQuery pquery){
        SelectBuilder builder = SqlBuilder.select();
        builder.column("d.*");
        builder.column("s.sys_name");

        builder.from(from -> {
            from.table("gp_data_model d");
            from.leftJoin("gp_sys_sub s", "d.sys_id = s.sys_id");
        });

        Map<String, Object> params = Maps.newHashMap();
        if (!Strings.isNullOrEmpty(keyword)) {
            builder.and("d.data_name like :name");
            params.put("name", keyword + "%");
        }
        if (IdKeys.isValidId(sysId)) {
            builder.and("d.sys_id = :pid");
            params.put("pid", sysId);
        }

        if (Objects.nonNull(pquery)) {

            SelectBuilder countBuilder = builder.clone();
            countBuilder.column().column("count(d." + AppIdKey.DATA_MODEL.idColumn() + ")");
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
            LOGGER.debug("SQL : {} / PARAM : {}", builder.build(), params.toString());
        }

        return rows(builder.build(), dataModelDAO.getRowMapper((info, rs) -> {
            info.setProperty("sys_name", rs.getString("sys_name"));
        }), params);
    }
}
