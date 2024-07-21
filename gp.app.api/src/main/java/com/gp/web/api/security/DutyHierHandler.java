package com.gp.web.api.security;

import com.gp.bind.BindScanner;
import com.gp.common.*;
import com.gp.dao.info.DutyHierInfo;
import com.gp.exception.BaseException;
import com.gp.info.DataBuilder;
import com.gp.info.FilterMode;
import com.gp.info.Principal;
import com.gp.svc.CommonService;
import com.gp.svc.security.DutyHierService;
import com.gp.validate.ArgsValidator;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DutyHierHandler extends BaseApiSupport {

    static Logger LOGGER = LoggerFactory.getLogger(DutyHierHandler.class);

    private DutyHierService dutyService;
    private CommonService commonService;

    public DutyHierHandler() {
        dutyService = BindScanner.instance().getBean(DutyHierService.class);
        commonService = BindScanner.instance().getBean(CommonService.class);
    }

    @WebApi(path="duty-node-add")
    public void handleAddDutyHier(HttpServerExchange exchange)throws BaseException {
        ActionResult result = ActionResult.success(getMessage(exchange, "mesg.new.orghier"));

        Map<String, Object> params = this.getRequestBody(exchange);

        ArgsValidator.newValidator(params)
                .require("duty_name", "duty_ecd", "org_id")
                .validate(true);

        long parentId = Filters.filterLong(params, "duty_pid");
        if(parentId <= 0)
            parentId = GeneralConsts.HIER_ROOT;

        ServiceContext svcctx = this.getServiceContext(exchange, Operations.ORG_NEW);

        DutyHierInfo dutyhier = new DutyHierInfo();

        dutyhier.setDutyPid(parentId);
        dutyhier.setInfoId(IdKeys.newInfoId(AppIdKey.DUTY_HIER));
        dutyhier.setDutyEcd(Filters.filterString(params, "duty_ecd"));
        dutyhier.setDutyName(Filters.filterString(params, "duty_name"));
        dutyhier.setHeadcount(Filters.filterInt(params, "headcount"));
        dutyhier.setDutyLvl(Filters.filterString(params, "duty_lvl"));
        dutyhier.setDutyCate(Filters.filterString(params, "duty_cate"));
        dutyhier.setState(Filters.filterString(params, "state"));
        dutyhier.setOrgId(Filters.filterLong(params, "org_id"));
        dutyhier.setDescription(Filters.filterString(params, "description"));

        svcctx.setOperationObject(dutyhier.getInfoId());
        svcctx.addOperationPredicates(dutyhier);

        dutyService.newDutyHierNode(svcctx, dutyhier);

        this.sendResult(exchange, result);
    }


    @WebApi(path="duty-node-save")
    public void handleSaveDutyHier(HttpServerExchange exchange)throws BaseException{

        ActionResult result = ActionResult.success(getMessage(exchange, "mesg.target.orghier"));

        Principal principal = this.getPrincipal(exchange);
        Map<String, Object> params = this.getRequestBody(exchange);
        ServiceContext svcctx = this.getServiceContext(exchange, Operations.ORG_UPD);

        ArgsValidator.newValidator(params)
                .require("duty_id")
                .validate(true);
        InfoId nodeId = Filters.filterInfoId(params, "duty_id", AppIdKey.DUTY_HIER);

        svcctx.setOperationObject(nodeId);
        DutyHierInfo dutyhier =  new DutyHierInfo();

        dutyhier.setInfoId(nodeId);
        dutyhier.setDutyEcd(Filters.filterString(params, "duty_ecd"));
        dutyhier.setDutyName(Filters.filterString(params, "duty_name"));
        dutyhier.setHeadcount(Filters.filterInt(params, "headcount"));
        dutyhier.setDutyLvl(Filters.filterString(params, "duty_lvl"));
        dutyhier.setDutyCate(Filters.filterString(params, "duty_cate"));
        dutyhier.setState(Filters.filterString(params, "state"));
        dutyhier.setDescription(Filters.filterString(params, "description"));

        Set<String> keys = params.keySet();
        keys.remove("duty_id");
        keys.remove("duty_pid");
        keys.remove("org_id");
        dutyhier.setFilter(FilterMode.include(keys));
        dutyService.saveDutyHierNode(svcctx, dutyhier);

        this.sendResult(exchange, result);
    }

    @WebApi(path="duty-node-remove")
    public void handleRemoveDutyHier(HttpServerExchange exchange)throws BaseException{

        ActionResult result = ActionResult.success(getMessage(exchange, "mesg.remove.orghier"));
        ServiceContext svcctx = this.getServiceContext(exchange, Operations.ORG_RMV);

        Map<String, Object> params = this.getRequestBody(exchange);

        InfoId id = Filters.filterInfoId(params, "duty_id", AppIdKey.DUTY_HIER);

        svcctx.setOperationObject(id);
        if(!dutyService.removeDutyHierNode(id)){

            result = ActionResult.failure(getMessage(exchange, "excp.remove.orghier"));
        }

        this.sendResult(exchange, result);
    }

    @WebApi(path="duty-hier-query", operation="org:fnd")
    public void handleGetOrgHierNodes(HttpServerExchange exchange)throws BaseException {

        ActionResult result = null;

        Map<String, Object > paramap = this.getRequestBody(exchange);
        ArgsValidator.newValidator(paramap)
                .require("duty_pid", "org_id")
                .validate(true);

        InfoId oid = Filters.filterInfoId(paramap, "duty_pid", AppIdKey.DUTY_HIER);
        Long orgid = Filters.filterLong(paramap, "org_id");
        String keyword = Filters.filterString(paramap, "keyword");

        List<DutyHierInfo> infos =  dutyService.getDutyHierNodes(keyword, oid, orgid);
        List<Map<String, Object>> list =  infos.stream().map((info)->{
            DataBuilder builder = new DataBuilder();
            builder.set("duty_id", info.getId().toString());
            if(GeneralConsts.HIER_ROOT != info.getDutyPid()){
                builder.set("duty_pid", info.getDutyPid().toString());
            } else {
                builder.set("duty_pid", GeneralConsts.HIER_ROOT.toString());
            }
            builder.set(info, "duty_name", "description", "duty_lvl", "duty_cate", "duty_ecd");

            int childCnt = info.getProperty("child_count", Integer.class);
            builder.set("has_child", childCnt > 0);

            return builder.build();
        }).collect(Collectors.toList());

        result = ActionResult.success(getMessage(exchange, "mesg.find.orgs"));
        result.setData(list);

        this.sendResult(exchange, result);
    }

    @WebApi(path="duty-node-info", operation="org:fnd")
    public void handleGetDutyHierNode(HttpServerExchange exchange)throws BaseException {

        ActionResult result = null;

        Map<String, Object > paramap = this.getRequestBody(exchange);
        ArgsValidator.newValidator(paramap)
                .requireOne("duty_id")
                .validate(true);

        InfoId oid = Filters.filterInfoId(paramap, "duty_id", AppIdKey.DUTY_HIER);

        DutyHierInfo info =  dutyService.getDutyHierNode(oid);

        DataBuilder builder = new DataBuilder();
        builder.set("duty_id", info.getId().toString());
        if(GeneralConsts.HIER_ROOT != info.getDutyPid()){
            builder.set("duty_pid", info.getDutyPid().toString());
        } else {
            builder.set("duty_pid", GeneralConsts.HIER_ROOT.toString());
        }
        builder.set(info, "duty_name", "description", "duty_lvl", "duty_cate", "duty_ecd", "state", "headcount");

        result = ActionResult.success(getMessage(exchange, "mesg.find.orgs"));
        result.setData(builder);

        this.sendResult(exchange, result);
    }
}
