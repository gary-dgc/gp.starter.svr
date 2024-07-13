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
import com.gp.dao.info.SeqNoInfo;

@BindComponent(type = SeqNoDAO.class, priority = BaseDAO.BASE_PRIORITY)
public class SeqNoDAO extends DAOSupport implements BaseDAO<SeqNoInfo> {

    private Identifier daoIdKey = BaseIdKey.BLIND;

	static Logger LOGGER = LoggerFactory.getLogger(SeqNoDAO.class);

	static final String[] COLUMNS = new String[]{
             "seq_key", "prefix", "length", "curr_val", 
             "step_intvl", "description", "init_val", "modifier_uid", 
             "modify_time", "del_flag"
    	};

	public static final RowParser<SeqNoInfo> INFO_PARSER = (rp, ctx) -> {
		SeqNoInfo info = new SeqNoInfo();
		InfoId id = rp.getInfoId(BaseIdKey.SEQ_NO);
		info.setInfoId(id);

		info.setSeqKey(rp.getString("seq_key"));
		info.setPrefix(rp.getString("prefix"));
		info.setLength(rp.getInt("length"));
		info.setCurrVal(rp.getInt("curr_val"));
		info.setStepIntvl(rp.getInt("step_intvl"));
		info.setDescription(rp.getString("description"));
		info.setInitVal(rp.getInt("init_val"));
		info.setModifierUid(rp.getLong("modifier_uid"));
		info.setModifyTime(rp.getTimestamp("modify_time"));
		info.setDelFlag(rp.getBoolean("del_flag"));

		return info;
	};

	public static final RowMapper<SeqNoInfo> INFO_MAPPER = (rs, ctx) -> {
		SeqNoInfo info = new SeqNoInfo();
		InfoId id = IdKeys.getInfoId(BaseIdKey.SEQ_NO, rs.getLong("seq_id"));
		info.setInfoId(id);

		info.setSeqKey(rs.getString("seq_key"));
		info.setPrefix(rs.getString("prefix"));
		info.setLength(rs.getInt("length"));
		info.setCurrVal(rs.getInt("curr_val"));
		info.setStepIntvl(rs.getInt("step_intvl"));
		info.setDescription(rs.getString("description"));
		info.setInitVal(rs.getInt("init_val"));
		info.setModifierUid(rs.getLong("modifier_uid"));
		info.setModifyTime(rs.getTimestamp("modify_time"));
		info.setDelFlag(rs.getBoolean("del_flag"));

		return info;
	};

	/**
	 * Default constructor
	 **/
	public SeqNoDAO (){
		this.daoIdKey = BaseIdKey.SEQ_NO;
	}

	/**
	 * Get the IdKey of DAO
	 **/
	@Override
	public Identifier getIdKey() {
		return this.daoIdKey;
	}

    @Override
	public RowParser<SeqNoInfo> getRowParser(){
    	return INFO_PARSER;
    }

    @Override
	public RowMapper<SeqNoInfo> getRowMapper(){
		if(isCrossDB()){
			return new ParserBuilder<SeqNoInfo>().build(INFO_PARSER);
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
	public Object[] getValues(SeqNoInfo info, String ...columns){
		BeanMeta meta = BeanAccessor.getBeanMeta(SeqNoInfo.class);
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
	public int create(SeqNoInfo info) {
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
	public int update(SeqNoInfo info) {

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
	public SeqNoInfo row( InfoId id ){
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
