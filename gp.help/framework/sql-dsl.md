# SQL代码的DSL开发风格设计及实现

熟悉数据库开发的工程师对mybatis、hibernate、DbUtils、Spring的JdbcTemplate一定都十分熟悉。
GP框架采用Jdbi作为ORM层基础框架，相比于前述的几个框架，jdbi提供了充分的性能保证，它提供了注解
SQL嵌入的功能，mybatis的注解SQL类似，但从最初的GP框架目标考虑，我不推荐在应用中使用此特性。

DbUtil提供了DSL风格的SQL语句编写功能，mybatis在xml中嵌入SQL和result mapping，参考二者在GP
框架中组件了更加偏向传统风格DSL操作。

```
    // 事务及SQL编写实例
	@JdbiTran(readOnly = true)
	public List<GroupUserInfo> demoMethod(InfoId groupId, String memberName) {
		
		Map<String, Object> paramMap = Maps.newHashMap();

		SelectBuilder builder = SqlBuilder.select();
		builder.column("a.*");
		
		builder.column("b.username", "b.email", "b.full_name");
		builder.column("b.mobile", "b.category", "b.create_time", " b.state");
		builder.column("b.avatar_url");
		
		builder.column("s.source_id", " s.source_name", "s.abbr", "s.node_gid");
		builder.column("s.short_name", "s.entity_gid");
		
		builder.from((from) -> {
			from.table("gp_group_user a");
			from.leftJoin("gp_user b", "a.member_uid = b.user_id");
			from.leftJoin("gp_source s", "b.source_id = s.source_id");
		});

		builder.where("a.group_id = :gid");
		paramMap.put("gid", groupId.getId());
		
		// 通过入参直接进行SQL拼接
		if (!Strings.isNullOrEmpty(memberName)) {
			builder.and((cond) -> {
				cond.or("b.full_name like :name");
				cond.or("b.username like :name");
			});
			paramMap.put("name", memberName + "%");
		}
		builder.orderBy("b.full_name");

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : {} / PARAMS : {} ", builder.toString(), paramMap);
		}
		
		Set<String> strFlds = Sets.newHashSet("username", "email", "full_name", "mobile", "category", 
				"create_time", "state", "avatar_url", 
				"source_name", "abbr", "node_gid",
				"short_name", "entity_gid");
		
		return rows(builder.toString(), (rs, idx) -> {
			GroupUserInfo info = GroupUserDAO.INFO_MAPPER.map(rs, idx);
			
			for(String fld : strFlds) {
				info.setProperty(fld, rs.getString(fld));
			}
			info.setProperty("source_id", rs.getLong("source_id"));
			
			return info;
		}, paramMap);

	}

```

## 总结

通过DSL方法进行SQL编写可以最大程度避免SQL的拼写错误，而且最大程度保证性能，框架对基本的语法做了支持：

* SelectBuilder select = SqlBuilder.select();
* DeleteBuilder delete = SqlBuilder.delete();
* UpdateBuilder update = SqlBuilder.update();
* InsertBuilder insert = SqlBuilder.insert();

其他使用的支持细节可以参考starter项目了解
