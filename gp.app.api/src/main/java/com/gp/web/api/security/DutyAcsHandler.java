package com.gp.web.api.security;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.gp.bind.BindScanner;
import com.gp.common.*;
import com.gp.dao.info.DutyAcsInfo;
import com.gp.exception.BaseException;
import com.gp.info.DataBuilder;
import com.gp.info.FilterMode;
import com.gp.svc.CommonService;
import com.gp.svc.security.DutyAcsService;
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
import java.util.stream.Collectors;

public class DutyAcsHandler extends BaseApiSupport {

    static Logger LOGGER = LoggerFactory.getLogger(DutyAcsHandler.class);

    private DutyAcsService dutyAcsService;
    private CommonService commonService;

    public DutyAcsHandler() {
        dutyAcsService = BindScanner.instance().getBean(DutyAcsService.class);
        commonService = BindScanner.instance().getBean(CommonService.class);
    }

    @WebApi(path="duty-acs-add")
    public void handleAddDutyAcs(HttpServerExchange exchange)throws BaseException {
        ActionResult result = ActionResult.success(getMessage(exchange, "mesg.new.orghier"));

        Map<String, Object> params = this.getRequestBody(exchange);

        ArgsValidator.newValidator(params)
                .require("duty_id", "sys_id", "data_id")
                .validate(true);

        ServiceContext svcctx = this.getServiceContext(exchange, Operations.ORG_NEW);

        DutyAcsInfo acs = new DutyAcsInfo();
        acs.setInfoId(IdKeys.newInfoId(AppIdKey.DUTY_ACS));
        acs.setDutyId(Filters.filterLong(params, "duty_id"));
        acs.setSysId(Filters.filterLong(params, "sys_id"));
        acs.setDataId(Filters.filterLong(params, "data_id"));

        List<Object> rids = Filters.filterList(params, "role_ids");
        String _rids = Joiner.on(',').join(rids);
        acs.setRoleIds(_rids);

        acs.setResideDept(Filters.filterBoolean(params, "reside_dept"));
        acs.setDirectDept(Filters.filterBoolean(params, "direct_dept"));
        acs.setAnySubDept(Filters.filterBoolean(params, "any_sub_dept"));
        acs.setDirectSubord(Filters.filterBoolean(params, "direct_subord"));
        acs.setAnySubord(Filters.filterBoolean(params, "any_subord"));

        svcctx.setOperationObject(acs.getInfoId());
        svcctx.addOperationPredicates(params);

        dutyAcsService.addDutyAcs(svcctx, acs);

        Map<String, Object> data = Maps.newHashMap();
        data.put("access_id", acs.getId().toString());

        result.setData(data);

        this.sendResult(exchange, result);
    }

    @WebApi(path="duty-acs-save")
    public void handleSaveDutyAcs(HttpServerExchange exchange)throws BaseException {
        ActionResult result = ActionResult.success(getMessage(exchange, "mesg.upd.acs"));

        Map<String, Object> params = this.getRequestBody(exchange);

        ArgsValidator.newValidator(params)
                .require("access_id")
                .validate(true);

        ServiceContext svcctx = this.getServiceContext(exchange, Operations.ORG_NEW);

        InfoId acsKey = Filters.filterInfoId(params, "access_id", AppIdKey.DUTY_ACS);
        DutyAcsInfo acs = new DutyAcsInfo();
        acs.setInfoId(acsKey);
        acs.setDutyId(Filters.filterLong(params, "duty_id"));
        acs.setSysId(Filters.filterLong(params, "sys_id"));

        List<Object> rids = Filters.filterList(params, "role_ids");
        String _rids = Joiner.on(',').join(rids);
        acs.setRoleIds(_rids);

        acs.setResideDept(Filters.filterBoolean(params, "reside_dept"));
        acs.setDirectDept(Filters.filterBoolean(params, "direct_dept"));
        acs.setAnySubDept(Filters.filterBoolean(params, "any_sub_dept"));
        acs.setDirectSubord(Filters.filterBoolean(params, "direct_subord"));
        acs.setAnySubord(Filters.filterBoolean(params, "any_subord"));

        // extract keys to update
        Collection<String> keys = Filters.filterKeys(params.keySet(), "access_id", "data_id");
        acs.setFilter(FilterMode.include(keys));

        svcctx.setOperationObject(acs.getInfoId());
        svcctx.addOperationPredicates(params);

        dutyAcsService.saveDutyAcs(svcctx, acs);

        this.sendResult(exchange, result);
    }

    @WebApi(path="duty-acs-remove")
    public void handleRemoveDutyAcs(HttpServerExchange exchange)throws BaseException {

        ActionResult result = ActionResult.success(getMessage(exchange, "mesg.remove.orghier"));
        ServiceContext svcctx = this.getServiceContext(exchange, Operations.ORG_RMV);

        Map<String, Object> params = this.getRequestBody(exchange);

        ArgsValidator.newValidator(params)
                .require("access_id")
                .validate(true);

        InfoId id = Filters.filterInfoId(params, "access_id", AppIdKey.DUTY_ACS);

        svcctx.setOperationObject(id);
        if(dutyAcsService.removeDutyAcs(id) == 0){

            result = ActionResult.failure(getMessage(exchange, "excp.remove.orghier"));
        }

        this.sendResult(exchange, result);
    }

    @WebApi(path="duty-acs-query")
    public void handleQueryDutyAcs(HttpServerExchange exchange)throws BaseException {
        ActionResult result = ActionResult.success(getMessage(exchange, "mesg.remove.orghier"));
        ServiceContext svcctx = this.getServiceContext(exchange, Operations.ORG_RMV);

        Map<String, Object> params = this.getRequestBody(exchange);

        ArgsValidator.newValidator(params)
                .require("duty_id")
                .validate(true);

        InfoId dutyKey = Filters.filterInfoId(params, "duty_id", AppIdKey.DUTY_HIER);
        InfoId sysKey = Filters.filterInfoId(params, "sys_id", AppIdKey.SYS_SUB);
        InfoId dataKey = Filters.filterInfoId(params, "data_id", AppIdKey.DATA_MODEL);

        svcctx.addOperationPredicates(params);

        List<DutyAcsInfo> infos = dutyAcsService.getDutyAcses(dutyKey, sysKey, dataKey);
        List<Object> data = infos.stream().map( info -> {

            DataBuilder builder = new DataBuilder();
            builder.set("access_id", info.getId().toString());

            builder.set("data_id", info.getDataId().toString());
            builder.set("sys_id", info.getSysId().toString());

            String rids = info.getRoleIds();
            List<String> _rids = Splitter.on(',').splitToList(rids);
            builder.set("role_ids", _rids);
            builder.set(info, "reside_dept", "direct_dept", "any_sub_dept", "direct_subord", "any_subord");
            builder.set(info, "sys_name", "data_name");
            return builder.build();

        }).collect(Collectors.toList());

        result.setData(data);

        this.sendResult(exchange, result);
    }

    @WebApi(path="duty-acs-info")
    public void handleDutyAcsInfo(HttpServerExchange exchange)throws BaseException {
        ActionResult result = ActionResult.success(getMessage(exchange, "mesg.remove.orghier"));
        ServiceContext svcctx = this.getServiceContext(exchange, Operations.ORG_RMV);

        Map<String, Object> params = this.getRequestBody(exchange);

        ArgsValidator.newValidator(params)
                .require("access_id")
                .validate(true);

        InfoId acsKey = Filters.filterInfoId(params, "access_id", AppIdKey.DUTY_ACS);

        svcctx.addOperationPredicates(params);

        DutyAcsInfo info = dutyAcsService.getDutyAcs(acsKey);


        DataBuilder builder = new DataBuilder();
        builder.set("access_id", info.getId().toString());

        builder.set("data_id", info.getDataId().toString());
        builder.set("sys_id", info.getSysId().toString());

        String rids = info.getRoleIds();
        List<String> _rids = Splitter.on(',').splitToList(rids);
        builder.set("role_ids", _rids);
        builder.set(info, "reside_dept", "direct_dept", "any_sub_dept", "direct_subord", "any_subord");

        result.setData(builder);

        this.sendResult(exchange, result);
    }
}
