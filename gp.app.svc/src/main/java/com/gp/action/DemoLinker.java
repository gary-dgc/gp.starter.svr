package com.gp.action;

import com.gp.action.param.DemoLink;
import com.gp.bind.BindAutowired;
import com.gp.bind.BindComponent;
import com.gp.dao.AuditDAO;
import com.gp.dao.info.AuditInfo;
import com.gp.exception.ServiceException;
import com.gp.exec.OptionResult;
import com.gp.svc.BaseService;
import com.gp.svc.LinkerSupport;

@BindComponent
public class DemoLinker extends LinkerSupport<OptionResult, DemoLink> implements BaseService {

    @BindAutowired
    AuditDAO auditDAO;

    @Override
    protected OptionResult before() throws ServiceException {

        ContextVars vars = resetVars(ContextVars::new);
        DemoLink param = getParameter();
        AuditInfo info = auditDAO.row(pair("lane_id", param.getVar1()));

        vars.shot = info;

        return null;
    }

    @Override
    protected OptionResult after(boolean before) throws ServiceException {
        DemoLink param = getParameter();
        ContextVars vars = getVars();
        AuditInfo info = auditDAO.row(pair("lane_id", param.getVar1()));

        if(vars.shot == null && before && info != null){

            insertRow(info);
        }else if(vars.shot != null && before && info != null){

            updateRow(vars.shot, info);
        }else if(vars.shot != null && before && info== null){

            deleteRow(vars.shot);
        }

        return OptionResult.success("success");
    }


    void insertRow(AuditInfo info){

    }

    void updateRow(AuditInfo shot, AuditInfo info){

    }

    void deleteRow(AuditInfo shot){

    }

    static class ContextVars{

        AuditInfo shot;
    }
}
