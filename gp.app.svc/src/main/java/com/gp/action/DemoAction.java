package com.gp.action;

import com.gp.action.param.DemoParam;
import com.gp.bind.BindAutowired;
import com.gp.bind.BindComponent;
import com.gp.dao.AuditDAO;
import com.gp.dao.info.AuditInfo;
import com.gp.exception.ServiceException;
import com.gp.exec.OptionArg;
import com.gp.exec.OptionResult;
import com.gp.svc.ActionSupport;
import com.gp.svc.BaseService;

@BindComponent
public class DemoAction extends ActionSupport<OptionResult, DemoParam> implements BaseService {

    @BindAutowired
    AuditDAO auditDAO;

    public DemoAction(){
        register();
    }

    @Override
    protected OptionResult _perform(DemoParam param) throws ServiceException {

        System.out.println("demo action");

        OptionResult result = OptionResult.success("ok");
        result.addArg(OptionArg.newArg("cnt", 123));

        return result;
    }

    @Override
    protected boolean validate() throws ServiceException {

        resetVars(ContextVars::new);

        return super.validate();
    }

    static class ContextVars{

        AuditInfo shot;
    }
}
