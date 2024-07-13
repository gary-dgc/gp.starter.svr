package com.gp.web.api.security;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.gp.bind.BindScanner;
import com.gp.common.*;
import com.gp.dao.info.DataModelInfo;
import com.gp.dao.info.DutyAcsInfo;
import com.gp.exception.BaseException;
import com.gp.info.DataBuilder;
import com.gp.info.FilterMode;
import com.gp.paging.PageQuery;
import com.gp.svc.CommonService;
import com.gp.svc.security.DataModelService;
import com.gp.validate.ArgsValidator;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DataModelHandler extends BaseApiSupport {

    static Logger LOGGER = LoggerFactory.getLogger(DataModelHandler.class);

    private DataModelService dataModelService ;
    private CommonService commonService;

    public DataModelHandler() {
        dataModelService = BindScanner.instance().getBean(DataModelService.class);
        commonService = BindScanner.instance().getBean(CommonService.class);
    }

    @WebApi(path="data-model-add")
    public void handleAddDataModel(HttpServerExchange exchange)throws BaseException {
        ActionResult result = ActionResult.success(getMessage(exchange, "mesg.new.orghier"));

        Map<String, Object> params = this.getRequestBody(exchange);

        ArgsValidator.newValidator(params)
                .require("sys_id", "data_name")
                .validate(true);

        ServiceContext svcctx = this.getServiceContext(exchange, Operations.ORG_NEW);

        DataModelInfo model = new DataModelInfo();
        model.setInfoId(IdKeys.newInfoId(MasterIdKey.DATA_MODEL));
        model.setDataName(Filters.filterString(params, "data_name"));
        model.setDescription(Filters.filterString(params, "description"));
        model.setSysId(Filters.filterLong(params, "sys_id"));

        svcctx.setOperationObject(model.getInfoId());
        svcctx.addOperationPredicates(params);

        dataModelService.addDataModel(svcctx, model);

        Map<String, Object> data = Maps.newHashMap();
        data.put("data_id", model.getId().toString());

        result.setData(data);

        this.sendResult(exchange, result);
    }

    @WebApi(path="data-model-save")
    public void handleSaveDataModel(HttpServerExchange exchange)throws BaseException {
        ActionResult result = ActionResult.success(getMessage(exchange, "mesg.new.orghier"));

        Map<String, Object> params = this.getRequestBody(exchange);

        ArgsValidator.newValidator(params)
                .require("data_id")
                .validate(true);

        ServiceContext svcctx = this.getServiceContext(exchange, Operations.ORG_NEW);

        InfoId dataKey = Filters.filterInfoId(params, "data_id", MasterIdKey.DATA_MODEL);
        DataModelInfo model = new DataModelInfo();
        model.setInfoId(dataKey);
        model.setDataName(Filters.filterString(params, "data_name"));
        model.setDescription(Filters.filterString(params, "description"));

        Collection<String> keys = Filters.filterKeys(params.keySet(), "data_id", "sys_id");
        model.setFilter(FilterMode.include(keys));

        svcctx.setOperationObject(model.getInfoId());
        svcctx.addOperationPredicates(params);

        dataModelService.saveDataModel(svcctx, model);

        Map<String, Object> data = Maps.newHashMap();
        data.put("data_id", model.getId().toString());

        result.setData(data);

        this.sendResult(exchange, result);
    }


    @WebApi(path="duty-model-remove")
    public void handleRemoveDataModel(HttpServerExchange exchange)throws BaseException {

        ActionResult result = ActionResult.success(getMessage(exchange, "mesg.remove.orghier"));
        ServiceContext svcctx = this.getServiceContext(exchange, Operations.ORG_RMV);

        Map<String, Object> params = this.getRequestBody(exchange);

        ArgsValidator.newValidator(params)
                .require("data_id")
                .validate(true);

        InfoId id = Filters.filterInfoId(params, "data_id", MasterIdKey.DATA_MODEL);

        svcctx.setOperationObject(id);
        if(dataModelService.removeDataModel(id) == 0){

            result = ActionResult.failure(getMessage(exchange, "excp.remove.orghier"));
        }

        this.sendResult(exchange, result);
    }

    @WebApi(path="data-model-query")
    public void handleQueryModels(HttpServerExchange exchange)throws BaseException {
        ActionResult result = ActionResult.success(getMessage(exchange, "mesg.remove.orghier"));
        ServiceContext svcctx = this.getServiceContext(exchange, Operations.ORG_RMV);

        Map<String, Object> params = this.getRequestBody(exchange);

        String keyword = Filters.filterString(params, "keyword");
        Long sysid = Filters.filterLong(params, "sys_id");

        PageQuery pquery = Filters.filterPageQuery(params);

        svcctx.addOperationPredicates(params);

        List<DataModelInfo> infos = dataModelService.getDataModels(keyword, sysid, pquery);
        List<Object> rows = infos.stream().map( info -> {

            DataBuilder builder = new DataBuilder();

            builder.set("data_id", info.getId().toString());
            builder.set("sys_id", info.getSysId().toString());
            builder.set("data_name", info.getDataName());
            builder.set("description", info.getDescription());
            builder.set(info, "sys_name", "modify_time");

            return builder.build();

        }).collect(Collectors.toList());

        result.setData(pquery == null ? null : pquery.getPagination(), rows);

        this.sendResult(exchange, result);
    }

    @WebApi(path="data-model-info")
    public void handleDutyAcsInfo(HttpServerExchange exchange)throws BaseException {
        ActionResult result = ActionResult.success(getMessage(exchange, "mesg.remove.orghier"));
        ServiceContext svcctx = this.getServiceContext(exchange, Operations.ORG_RMV);

        Map<String, Object> params = this.getRequestBody(exchange);

        ArgsValidator.newValidator(params)
                .require("data_id")
                .validate(true);

        InfoId dataKey = Filters.filterInfoId(params, "data_id", MasterIdKey.DATA_MODEL);

        svcctx.addOperationPredicates(params);

        DataModelInfo info = dataModelService.getDataModel(dataKey);

        DataBuilder builder = new DataBuilder();
        builder.set("data_id", info.getId().toString());
        builder.set("sys_id", info.getSysId().toString());
        builder.set("data_name", info.getDataName());
        builder.set("description", info.getDescription());
        builder.set(info, "sys_name");

        result.setData(builder);

        this.sendResult(exchange, result);
    }
}
