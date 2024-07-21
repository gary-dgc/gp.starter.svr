# 如何实现业务设计

服务的开发设计过程中需要重点考虑的是数据库事务的管理

较为复杂的服务开发有如下例子：

``` 
@BindComponent( priority = BaseService.BASE_PRIORITY)
public class DataModelService extends ServiceSupport implements TranService {

    static Logger LOGGER = LoggerFactory.getLogger(DataModelService.class);

    @BindAutowired
    private DataModelDAO dataModelDAO;

	@JdbiTran
	public int advanceDemoMethod(InfoId key) throws ServiceException {

		UpdateBuilder update = SqlBuilder.update();
		update.set("a", "?");
		update.where("id = ?");

		List<Object> params = Lists.newArrayList();

		BaseUtils.reset(params, "a", 1234L);
		
		// 在服务中嵌入Linker处理
		DemoLink link = new DemoLink();
		link.setVar1("a");

		DemoLinker linker = getLinkerService(DemoLinker.class);

		OptionResult result = linker.perform(link, linkCtx -> {
			update(update.build(), params);
		});

		// 在服务中嵌入Action处理
		DemoParam param = new DemoParam();
		// 设定可变参数
		param.setVar1("1");
		param.addArg(OptionArg.newArg("varg1", 111));
		
		DemoAction action = getActionService(DemoAction.class);
		
		result = action.perform(param);
		
		// 获取返回结果
		OptionArg<Integer> rtv = result.getArg("cnt");
		
		return rtv.getValue();
	}
}

```

## 在服务中执行分页处理

``` 
    @JdbiTran(readOnly = true)
	public List<UserInfo> getUsers(String username, Long sourceId, UserCategory[] category, UserState[] states,
			Boolean boundOnly, PageQuery pquery) {

		SelectBuilder builder = SqlBuilder.select();
		builder.column("a.*", "b.*");
		
		builder.from((from) -> {
			from.table("gp_user a");
			from.leftJoin("(SELECT source_id, node_gid, source_name,short_name, abbr FROM gp_source) b",
					"a.source_id = b.source_id");
		});

		Map<String, Object> params = new HashMap<String, Object>();
		// account or name condition
		if (!Strings.isNullOrEmpty(username)) {

			builder.and("(a.username like :uname or a.full_name like :fname)");
			params.put("uname", "%" + username.trim() + "%");
			params.put("fname", "%" + username.trim() + "%");
		}
		// entity condition
		
		if (Objects.nonNull(pquery)) {
			SelectBuilder countBuilder = builder.clone();
			countBuilder.column().column("count(" + BaseIdKey.USER.idColumn() + ")");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("SQL : {} / PARAMS : {}", countBuilder.build(), params);
			}
			Integer total = row(countBuilder.toString(), Integer.class, params);
			Paginator paginator = new Paginator(total, pquery);
			Pagination pagination = paginator.getPagination();
			pquery.setPagination(pagination);

			SortOrder orderType = SortOrder.valueOf(pquery.getOrder().toUpperCase());
			builder.orderBy(pquery.getOrderBy(), orderType);
			builder.limit(pagination.getPageStartRow(), pquery.getPageSize());
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : " + builder.toString() + " / PARAMS : " + params.toString());
		}
		List<String> ExtFields = Lists.newArrayList("abbr", "short_name", "node_gid", "source_name",
				"title", "department");

		return rows(builder.toString(), userdao.getRowMapper((info, rs) -> {
			for (String field : ExtFields) {
				if (DAOSupport.isExistColumn(rs, field)) {
					info.setProperty(field, rs.getString(field));
				}
			}
		}), params);

	}


```


``` 
    @JdbiTran(readOnly = true)
	public List<UserInfo> getUsers(String username, Long sourceId, UserCategory[] category, UserState[] states,
			Boolean boundOnly, PageQuery pquery) {

		SelectBuilder builder = SqlBuilder.select();
		builder.column("a.*", "b.*");
		
		builder.from((from) -> {
			from.table("gp_user a");
			from.leftJoin("(SELECT source_id, node_gid, source_name,short_name, abbr FROM gp_source) b",
					"a.source_id = b.source_id");
		});

		Map<String, Object> params = new HashMap<String, Object>();
		// account or name condition
		if (!Strings.isNullOrEmpty(username)) {

			builder.and("(a.username like :uname or a.full_name like :fname)");
			params.put("uname", "%" + username.trim() + "%");
			params.put("fname", "%" + username.trim() + "%");
		}
		// entity condition

		// paginate the query
		paginate("count(" + BaseIdKey.USER.idColumn() + ")", builder, params, pquery);
		
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : " + builder.toString() + " / PARAMS : " + params.toString());
		}
		List<String> ExtFields = Lists.newArrayList("abbr", "short_name", "node_gid", "source_name",
				"title", "department");

		return rows(builder.toString(), userdao.getRowMapper((info, rs) -> {
			for (String field : ExtFields) {
				if (DAOSupport.isExistColumn(rs, field)) {
					info.setProperty(field, rs.getString(field));
				}
			}
		}), params);

	}


```