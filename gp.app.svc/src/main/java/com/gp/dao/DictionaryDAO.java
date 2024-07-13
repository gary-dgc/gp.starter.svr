package com.gp.dao;

import com.google.common.collect.Lists;
import com.gp.bean.BeanAccessor;
import com.gp.bean.BeanMeta;
import com.gp.bean.BeanProxy;
import com.gp.bind.BindComponent;
import com.gp.common.*;
import com.gp.dao.info.DeptHierInfo;
import com.gp.dao.info.DictionaryInfo;
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

@BindComponent(type = DictionaryDAO.class, priority = BaseDAO.BASE_PRIORITY)
public class DictionaryDAO extends DAOSupport implements BaseDAO<DictionaryInfo> {

    private Identifier daoIdKey = BaseIdKey.BLIND;

	static Logger LOGGER = LoggerFactory.getLogger(DictionaryDAO.class);

	static final String[] COLUMNS = new String[]{
             "dict_group", "dict_key", "dict_value", "lbl_en_us", 
             "lbl_zh_cn", "lbl_ru_ru", "lbl_fr_fr", "lbl_de_de", 
             "modifier_uid", "modify_time", "del_flag"
    	};

	public static final RowParser<DictionaryInfo> INFO_PARSER = (rs, ctx) -> {
		DictionaryInfo info = new DictionaryInfo();
		InfoId id = rs.getInfoId(BaseIdKey.DICTIONARY);
		info.setInfoId(id);

		info.setDictGroup(rs.getString("dict_group"));
		info.setDictKey(rs.getString("dict_key"));
		info.setDictValue(rs.getString("dict_value"));

		for(String lang : Filters.LANGUAGES) {
			info.putLabel(lang, rs.getString("lbl_" + lang.toLowerCase()));
		}
		info.setDelFlag(rs.getBoolean("del_flag"));
		info.setModifierUid(rs.getLong("modifier_uid"));
		info.setModifyTime(rs.getTimestamp("modify_time"));

		return info;
	};

	public static final RowMapper<DictionaryInfo> INFO_MAPPER = (rs, ctx) -> {
		DictionaryInfo info = new DictionaryInfo();
		InfoId id = IdKeys.getInfoId(BaseIdKey.DICTIONARY, rs.getLong("dict_id"));
		info.setInfoId(id);
		
		info.setDictGroup(rs.getString("dict_group"));
		info.setDictKey(rs.getString("dict_key"));
		info.setDictValue(rs.getString("dict_value"));
	
		for(String lang : Filters.LANGUAGES) {
			info.putLabel(lang, rs.getString("lbl_" + lang.toLowerCase()));
		}
		info.setDelFlag(rs.getBoolean("del_flag"));
		info.setModifierUid(rs.getLong("modifier_uid"));
		info.setModifyTime(rs.getTimestamp("modify_time"));
		
		return info;
	};


	/**
	 * Default constructor
	 **/
	public DictionaryDAO(){
		this.daoIdKey = BaseIdKey.DICTIONARY;
	}

	/**
	 * Get the IdKey of DAO
	 **/
	@Override
	public Identifier getIdKey() {
		return this.daoIdKey;
	}

	@Override
	public RowParser<DictionaryInfo> getRowParser(){
		return INFO_PARSER;
	}

	@Override
	public RowMapper<DictionaryInfo> getRowMapper(){
		if(isCrossDB()){
			return new ParserBuilder<DictionaryInfo>().build(INFO_PARSER);
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
	public Object[] getValues(DictionaryInfo info, String ...columns){
		BeanMeta meta = BeanAccessor.getBeanMeta(DictionaryInfo.class);
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
	public int create(DictionaryInfo info) {
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
	public int update(DictionaryInfo info) {

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
	public DictionaryInfo row(InfoId id ){
		SelectBuilder builder = selectSql();
		builder.all();
		builder.where(getIdKey().idColumn() + " = ? ");

		if(LOGGER.isDebugEnabled()){
			LOGGER.debug("SQL : {} / PARAMS : {}", builder.toString(), id);
		}

		try (BaseHandle jhandle = this.getBaseHandle()){
			return jhandle.queryOne(builder.build(), new Object[]{id.getId()}, INFO_MAPPER);
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
