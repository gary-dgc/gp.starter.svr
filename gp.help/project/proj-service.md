# 如何生成服务

在应用中创建服务首先需要明确以下思路：

1. 服务主要关联哪个业务，规划好服务中大概的方法范围

2. 服务是否需要开启事务管理，非事务服务继承BaseService，事务服务继承TranService

3. 服务中是否存在对其他服务类对象的调用，如果有考虑可否使用ExtendDAO或LinkerService优化

4. 服务中是否存在有复杂的业务逻辑，如果有考虑使用ActionService实现

## 创建普通Service

``` 
// 可以控制服务的Bean绑定处理顺序
@BindComponent( priority = BaseService.BASE_PRIORITY)
public class DataModelService extends ServiceSupport implements BaseService {

    static Logger LOGGER = LoggerFactory.getLogger(DataModelService.class);

    @BindAutowired
    private DataModelDAO dataModelDAO;

    public InfoId addDataModel(ServiceContext svcctx, DataModelInfo info){

        svcctx.setTraceInfo(info);
        dataModelDAO.create(info);

        return info.getInfoId();
    }

    public int saveDataModel(ServiceContext svcctx, DataModelInfo info){

        svcctx.setTraceInfo(info);
        return dataModelDAO.update(info);
    }

    public int removeDataModel(InfoId key){

        return dataModelDAO.delete(key);
    }
}
```

## 创建事务Service

``` 
// 可以控制服务的Bean绑定处理顺序
@BindComponent( priority = BaseService.BASE_PRIORITY)
public class DataModelService extends ServiceSupport implements TranService {

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
}

```

## 创建非事务的ExtendDAO

``` 
@BindComponent(type = GroupExt.class, priority = ExtendDAO.BASE_PRIORITY )
public class GroupExt extends DAOSupport implements ExtendDAO {

    static Logger LOGGER = LoggerFactory.getLogger(GroupExt.class);

    /**
     * Get user's group ids and it's ancestors, this query depends on
     * procedure function: func_group_ancestry
     *
     **/
    public Set<Long> getUserGroups(Long userId, Long workgroupId){
        SelectBuilder select = SqlBuilder.select();
        select.column("g.group_id", "func_group_ancestry(g.group_id) AS ancestry_ids");
        select.from(MasterIdKey.GROUP.schema() + " AS g", MasterIdKey.GROUP_USER.schema() + " AS gu");
        select.where("g.group_id = gu.group_id");
        select.and("gu.member_uid = ?");
        List<Object> params = Lists.newArrayList();
        params.add(userId);
        if(workgroupId != null && workgroupId > 0) {
            select.and("g.manage_id = ?");
            params.add(workgroupId);
        }
        if(LOGGER.isDebugEnabled()){
            LOGGER.debug("SQL: {} / PARAMS: {}", select, userId);
        }
        Set<Long> gids = Sets.newHashSet();
        Splitter splitter = Splitter.on(',');
        query(select.build(), rs -> {
            Long grpid = rs.getLong("group_id");
            gids.add(grpid);
            String _ancestry = rs.getString("ancestry_ids");
            if(!Strings.isNullOrEmpty(_ancestry)) {
                Iterable<String> ancestors = splitter.split(_ancestry);
                ancestors.forEach(a -> gids.add(Long.valueOf(a)));
            }
        }, params);

        return gids;
    }

    /**
     * Get org's group id by org id
     **/
    public Long getOrgGroupId(Long orgId){

        SelectBuilder select = SqlBuilder.select();
        select.column("g.group_id");
        select.from(
                "gp_org_hier o",
                "gp_dept_hier d",
                "gp_group g");
        select.and( "o.org_id = d.org_id");
        select.and( "d.dept_pid = 99");
        select.and( "g.manage_id = d.dept_id");
        select.and( "o.org_id = ?");

        if(LOGGER.isDebugEnabled()){
            LOGGER.debug("SQL: {} / PARAMS: {}", select, orgId);
        }
        return column(select.build(), Long.class, orgId);
    }
}

```

## 创建LinkerService

``` 
@BindComponent
public class DemoLinker extends LinkerSupport<OptionResult, DemoLink> implements BaseService {

    @BindAutowired
    AuditDAO auditDAO;

    public DemoLinker(){
        register();
    }
    
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
```

## 创建ActionService

``` 
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

        return null;
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

```