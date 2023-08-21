package com.gp.web.api.master;

import com.google.common.collect.Maps;
import com.gp.bind.BindScanner;
import com.gp.common.*;
import com.gp.dao.info.SysSubInfo;
import com.gp.exception.BaseException;
import com.gp.info.DataBuilder;
import com.gp.paging.PageQuery;
import com.gp.svc.master.SysSubService;
import com.gp.validate.ArgsValidator;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SysSubHandler extends BaseApiSupport {

    static Logger LOGGER = LoggerFactory.getLogger(SysSubHandler.class);

    private SysSubService sysSubService;

    public SysSubHandler(){

        sysSubService = BindScanner.instance().getBean(SysSubService.class);
    }

    @WebApi(path="systems-query")
    public void handleSystemsQuery(HttpServerExchange exchange) {

        Map<String, Object> params = getRequestBody(exchange);
        ServiceContext svcctx = this.getServiceContext(exchange, Operations.STG_FND);
        svcctx.addOperationPredicates(params);

        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("params {}" , params);
        }
        ActionResult result = null;
        String keyword = Filters.filterString(params, "keyword");
        PageQuery pquery = Filters.filterPageQuery(params);

        List<SysSubInfo> infos = sysSubService.getSystems(keyword, pquery);

        List<Object> rows = infos.stream().map(info -> {

            DataBuilder row = new DataBuilder();
            row.set("sys_id", info.getId().toString());
            row.set(info, "sys_ecd", "sys_name", "sys_abbr", "description", "state");
            row.set(info, "public_url", "service_url");

            return row.build();
        }).collect(Collectors.toList());

        result = ActionResult.success(getMessage(exchange, "mesg.find.system"));
        result.setData(pquery == null ? null : pquery.getPagination(), rows);

        this.sendResult(exchange, result);
    }

    @WebApi(path="system-info")
    public void handleGetSystem(HttpServerExchange exchange)throws BaseException {
        Map<String, Object> params = getRequestBody(exchange);

        ArgsValidator validator = ArgsValidator.newValidator(params);
        validator.requireOne("sys_id", "sys_ecd")
                .validate(true);

        InfoId sysKey = Filters.filterInfoId(params, "sys_id", MasterIdKey.SYS_SUB);
        String sysEcd = Filters.filterString(params, "sys_ecd");

        SysSubInfo info = sysSubService.getSystem(sysKey, sysEcd);
        DataBuilder row = new DataBuilder();
        row.set("sys_id", info.getId().toString());
        row.set(info, "sys_ecd", "sys_name", "sys_abbr", "description", "state");
        row.set(info, "public_url", "service_url");

        ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.system"));
        result.setData(row.build());

        this.sendResult(exchange, result);
    }

    @WebApi(path="system-add")
    public void handleAddSystem(HttpServerExchange exchange)throws BaseException {
        Map<String, Object> params = getRequestBody(exchange);

        ArgsValidator validator = ArgsValidator.newValidator(params);
        validator.require("sys_name", "sys_ecd", "sys_abbr")
                .validate(true);

        ServiceContext svcctx = this.getServiceContext(exchange, Operations.STG_FND);

        SysSubInfo info = new SysSubInfo();
        info.setSysEcd(Filters.filterString(params, "sys_ecd"));
        info.setSysName(Filters.filterString(params, "sys_name"));
        info.setPublicUrl(Filters.filterString(params, "public_url"));
        info.setServiceUrl(Filters.filterString(params, "service_url"));
        info.setSysAbbr(Filters.filterString(params, "sys_abbr"));
        info.setDescription(Filters.filterString(params, "description"));
        info.setState(Filters.filterString(params, "state"));

        InfoId sKey = sysSubService.addSystem(svcctx, info);
        ActionResult result = ActionResult.success(getMessage(exchange, "mesg.find.system"));

        Map<String, Object> data = Maps.newHashMap();
        data.put("sys_id", sKey.getId());
        result.setData(data);

        this.sendResult(exchange, result);
    }

    @WebApi(path="system-save")
    public void handleSaveSystem(HttpServerExchange exchange)throws BaseException {
        Map<String, Object> params = getRequestBody(exchange);

        ArgsValidator validator = ArgsValidator.newValidator(params);
        validator.require("sys_name", "sys_ecd", "sys_abbr", "sys_id")
                .validate(true);
        InfoId sysKey = Filters.filterInfoId(params, "sys_id", MasterIdKey.SYS_SUB);
        ServiceContext svcctx = this.getServiceContext(exchange, Operations.STG_FND);

        SysSubInfo info = new SysSubInfo();
        info.setInfoId(sysKey);
        info.setSysEcd(Filters.filterString(params, "sys_ecd"));
        info.setSysName(Filters.filterString(params, "sys_name"));
        info.setPublicUrl(Filters.filterString(params, "public_url"));
        info.setServiceUrl(Filters.filterString(params, "service_url"));
        info.setSysAbbr(Filters.filterString(params, "sys_abbr"));
        info.setDescription(Filters.filterString(params, "description"));
        info.setState(Filters.filterString(params, "state"));

        boolean success = sysSubService.updateSystem(svcctx, info);
        ActionResult result = success ? ActionResult.success(getMessage(exchange, "mesg.find.system"))
                : ActionResult.failure(getMessage(exchange, "exce.find.system"));

        this.sendResult(exchange, result);
    }

    @WebApi(path="system-remove")
    public void handleRemoveSystem(HttpServerExchange exchange)throws BaseException {
        Map<String, Object> params = getRequestBody(exchange);

        ArgsValidator validator = ArgsValidator.newValidator(params);
        validator.requireOne("sys_id")
                .validate(true);

        InfoId sysKey = Filters.filterInfoId(params, "sys_id", MasterIdKey.SYS_SUB);

        boolean success = sysSubService.removeSystem(sysKey);
        ActionResult result = success ? ActionResult.success(getMessage(exchange, "mesg.find.system"))
                : ActionResult.failure(getMessage(exchange, "exce.find.system"));

        this.sendResult(exchange, result);
    }
}
