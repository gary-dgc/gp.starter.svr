package com.gp.dao;

import com.google.common.collect.Lists;
import com.gp.bean.BeanAccessor;
import com.gp.bean.BeanMeta;
import com.gp.bean.BeanProxy;
import com.gp.bind.BindComponent;
import com.gp.common.*;
import com.gp.db.BaseHandle;
import com.gp.info.BaseIdKey;
import com.gp.info.FilterMode;
import com.gp.sql.delete.DeleteBuilder;
import com.gp.sql.insert.InsertBuilder;
import com.gp.sql.select.SelectBuilder;
import com.gp.sql.update.UpdateBuilder;
import org.jdbi.v3.core.mapper.RowMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import com.gp.dao.info.UserRoleInfo;

@BindComponent(type = UserRoleDAO.class, priority = BaseDAO.BASE_PRIORITY)
public class UserRoleDAO extends DAOSupport implements BaseDAO<UserRoleInfo> {

    private Identifier daoIdKey = BaseIdKey.BLIND;

	static Logger LOGGER = LoggerFactory.getLogger(UserRoleDAO.class);

	static final String[] COLUMNS = new String[]{
             "user_id", "role_id", "modifier_uid", "modify_time", 
             "del_flag"
    	};

	public static final RowParser<UserRoleInfo> INFO_PARSER = (rp, ctx) -> {
		UserRoleInfo info = new UserRoleInfo();
		InfoId id = rp.getInfoId(AppIdKey.USER_ROLE);
		info.setInfoId(id);

		info.setUserId(rp.getLong("user_id"));
		info.setRoleId(rp.getLong("role_id"));
		info.setModifierUid(rp.getLong("modifier_uid"));
		info.setModifyTime(rp.getTimestamp("modify_time"));
		info.setDelFlag(rp.getBoolean("del_flag"));

		return info;
	};

	public static final RowMapper<UserRoleInfo> INFO_MAPPER = (rs, ctx) -> {
		UserRoleInfo info = new UserRoleInfo();
		InfoId id = IdKeys.getInfoId(AppIdKey.USER_ROLE, rs.getLong("rel_id"));
		info.setInfoId(id);

		info.setUserId(rs.getLong("user_id"));
		info.setRoleId(rs.getLong("role_id"));
		info.setModifierUid(rs.getLong("modifier_uid"));
		info.setModifyTime(rs.getTimestamp("modify_time"));
		info.setDelFlag(rs.getBoolean("del_flag"));

		return info;
	};

	/**
	 * Default constructor
	 **/
	public UserRoleDAO (){
		this.daoIdKey = AppIdKey.USER_ROLE;
	}

	/**
	 * Get the IdKey of DAO
	 **/
	@Override
	public Identifier getIdKey() {
		return this.daoIdKey;
	}

    @Override
	public RowParser<UserRoleInfo> getRowParser(){
    	return INFO_PARSER;
    }

    @Override
	public RowMapper<UserRoleInfo> getRowMapper(){
		if(isCrossDB()){
			return new ParserBuilder<UserRoleInfo>().build(INFO_PARSER);
		}
		return INFO_MAPPER;
    }

	@Override
	public String[] getColumns(){
		return COLUMNS;
	}

	/**
	 * Get values of specified columns
	 **/
	public Object[] getValues(UserRoleInfo info, String ...columns){
		BeanMeta meta = BeanAccessor.getBeanMeta(UserRoleInfo.class);
		BeanProxy proxy = meta.getBeanProxy();

		if(null == columns || columns.length == 0){

			Object[] rtv = new Object[COLUMNS.length];
			for(int i = 0; i < COLUMNS.length; i++){
				rtv[i] = proxy.getProperty(info, COLUMNS[i]);
			}

			return rtv;
		} else {

			Object[] rtv = new Object[columns.length];

			for(int i = 0; i < columns.length; i++){
				rtv[i] = proxy.getProperty(info, columns[i]);
			}

			return rtv;
		}
	}


	@Override
	public int create(UserRoleInfo info) {
		InsertBuilder builder = insertSql();

		builder.column(getColumns(FilterMode.blind()));

		IdKeys.setInfoIdAsNeed(info, getIdKey());

		builder.set(getIdKey().idColumn(), info.getId());

		Object[] params = getValues(info);

		if(LOGGER.isDebugEnabled()){
			LOGGER.debug("SQL : {} / PARAMS : {}", builder, Arrays.toString(params));
		}

		try (BaseHandle jhandle = this.getBaseHandle()){
			return jhandle.update(builder.build(), params);
		}
	}

	@Override
	public int update(UserRoleInfo info) {

		UpdateBuilder builder = updateSql();

		// use FilterMode to get columns
		String[] columns = getColumns(info.getFilter());
		// get values by columns
		Object[] values = getValues(info, columns);

		for(int i = 0; i < columns.length; i++){
			builder.set(columns[i], "?");
		}

		List<Object> params = Lists.newArrayList(values);
		builder.where(getIdKey().idColumn()+ " = ? ");
		params.add(info.getId());

		if(LOGGER.isDebugEnabled()){
			LOGGER.debug("SQL : {} / PARAMS : {}", builder, params.toString());
		}

		try (BaseHandle jhandle = this.getBaseHandle()){
			return jhandle.update(builder.build(), params.toArray());
		}
	}

	@Override
	public UserRoleInfo row( InfoId id ){
		SelectBuilder builder = selectSql();
		builder.all();
		builder.where(getIdKey().idColumn() + " = ? ");

		if(LOGGER.isDebugEnabled()){
			LOGGER.debug("SQL : {} / PARAMS : {}", builder.toString(), id);
		}

		try (BaseHandle jhandle = this.getBaseHandle()){
			return jhandle.queryOne(builder.build(), new Object[]{id.getId()}, getRowMapper());
		}
	}

	@Override
	public int delete( InfoId id, boolean purge ){
		String Sql = null;
		if(purge) {

			DeleteBuilder builder = deleteSql();
			builder.from(getIdKey().schema());
			builder.where(getIdKey().idColumn() + " = ? ");
			Sql = builder.build();

		}else {

			UpdateBuilder builder = updateSql();
			builder.set(FlatColumns.DEL_FLAG.getColumn(), Boolean.TRUE);
			builder.where(getIdKey().idColumn() + " = ? ");
			Sql = builder.build();

		}
		if(LOGGER.isDebugEnabled()){
			LOGGER.debug("SQL : {} / PARAMS : {}", Sql, id);
		}

		try (BaseHandle jhandle = this.getBaseHandle()){
			return jhandle.update(Sql,  new Object[]{id.getId()});
		}

	}
}
