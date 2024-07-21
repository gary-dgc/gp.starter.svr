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
import com.gp.dao.info.DataPropInfo;

@BindComponent(type = DataPropDAO.class, priority = BaseDAO.BASE_PRIORITY)
public class DataPropDAO extends DAOSupport implements BaseDAO<DataPropInfo> {

    private Identifier daoIdKey = BaseIdKey.BLIND;

	static Logger LOGGER = LoggerFactory.getLogger(DataPropDAO.class);

	static final String[] COLUMNS = new String[]{
             "data_id", "prop_code", "prop_label", "prop_type", 
             "default_value", "enums", "format", "required", 
             "modifier_uid", "modify_time", "del_flag"
    	};

	public static final RowParser<DataPropInfo> INFO_PARSER = (rp, ctx) -> {
		DataPropInfo info = new DataPropInfo();
		InfoId id = rp.getInfoId(AppIdKey.DATA_PROP);
		info.setInfoId(id);

		info.setDataId(rp.getLong("data_id"));
		info.setPropCode(rp.getString("prop_code"));
		info.setPropLabel(rp.getString("prop_label"));
		info.setPropType(rp.getString("prop_type"));
		info.setDefaultValue(rp.getString("default_value"));
		info.setEnums(rp.getString("enums"));
		info.setFormat(rp.getString("format"));
		info.setRequired(rp.getBoolean("required"));
		info.setModifierUid(rp.getLong("modifier_uid"));
		info.setModifyTime(rp.getTimestamp("modify_time"));
		info.setDelFlag(rp.getBoolean("del_flag"));

		return info;
	};

	public static final RowMapper<DataPropInfo> INFO_MAPPER = (rs, ctx) -> {
		DataPropInfo info = new DataPropInfo();
		InfoId id = IdKeys.getInfoId(AppIdKey.DATA_PROP, rs.getLong("prop_id"));
		info.setInfoId(id);

		info.setDataId(rs.getLong("data_id"));
		info.setPropCode(rs.getString("prop_code"));
		info.setPropLabel(rs.getString("prop_label"));
		info.setPropType(rs.getString("prop_type"));
		info.setDefaultValue(rs.getString("default_value"));
		info.setEnums(rs.getString("enums"));
		info.setFormat(rs.getString("format"));
		info.setRequired(rs.getBoolean("required"));
		info.setModifierUid(rs.getLong("modifier_uid"));
		info.setModifyTime(rs.getTimestamp("modify_time"));
		info.setDelFlag(rs.getBoolean("del_flag"));

		return info;
	};

	/**
	 * Default constructor
	 **/
	public DataPropDAO (){
		this.daoIdKey = AppIdKey.DATA_PROP;
	}

	/**
	 * Get the IdKey of DAO
	 **/
	@Override
	public Identifier getIdKey() {
		return this.daoIdKey;
	}

    @Override
	public RowParser<DataPropInfo> getRowParser(){
    	return INFO_PARSER;
    }

    @Override
	public RowMapper<DataPropInfo> getRowMapper(){
		if(isCrossDB()){
			return new ParserBuilder<DataPropInfo>().build(INFO_PARSER);
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
	public Object[] getValues(DataPropInfo info, String ...columns){
		BeanMeta meta = BeanAccessor.getBeanMeta(DataPropInfo.class);
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
	public int create(DataPropInfo info) {
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
	public int update(DataPropInfo info) {

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
	public DataPropInfo row( InfoId id ){
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
