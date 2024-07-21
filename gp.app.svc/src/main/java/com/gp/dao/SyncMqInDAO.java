package com.gp.dao;

import com.google.common.collect.Lists;
import com.gp.bean.BeanAccessor;
import com.gp.bean.BeanMeta;
import com.gp.bean.BeanProxy;
import com.gp.bind.BindComponent;
import com.gp.common.*;
import com.gp.dao.info.SyncMqInInfo;
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

@BindComponent(type = SyncMqInDAO.class, priority = BaseDAO.BASE_PRIORITY)
public class SyncMqInDAO extends DAOSupport implements BaseDAO<SyncMqInInfo> {

    private Identifier daoIdKey = BaseIdKey.BLIND;

	static Logger LOGGER = LoggerFactory.getLogger(SyncMqInDAO.class);

	static final String[] COLUMNS = new String[]{
             "trace_id", "origin_topic", "origin_sys", "oper_time", 
             "operator_id", "oper_cmd", "payload", "state", 
             "result", "modifier_uid", "modify_time", "del_flag", 
    	};

	public static final RowParser<SyncMqInInfo> INFO_PARSER = (rp, ctx) -> {
		SyncMqInInfo info = new SyncMqInInfo();
		InfoId id = rp.getInfoId(AppIdKey.SYNC_MQ_IN);
		info.setInfoId(id);

		info.setTraceId(rp.getLong("trace_id"));
		info.setOriginTopic(rp.getString("origin_topic"));
		info.setOriginSys(rp.getBytes("origin_sys"));
		info.setOperTime(rp.getTimestamp("oper_time"));
		info.setOperatorId(rp.getLong("operator_id"));
		info.setOperCmd(rp.getString("oper_cmd"));
		info.setPayload(rp.getString("payload"));
		info.setState(rp.getString("state"));
		info.setResult(rp.getString("result"));
		info.setModifierUid(rp.getLong("modifier_uid"));
		info.setModifyTime(rp.getTimestamp("modify_time"));
		info.setDelFlag(rp.getBoolean("del_flag"));

		return info;
	};

	public static final RowMapper<SyncMqInInfo> INFO_MAPPER = (rs, ctx) -> {
		SyncMqInInfo info = new SyncMqInInfo();
		InfoId id = IdKeys.getInfoId(AppIdKey.SYNC_MQ_IN, rs.getLong("msg_id"));
		info.setInfoId(id);

		info.setTraceId(rs.getLong("trace_id"));
		info.setOriginTopic(rs.getString("origin_topic"));
		info.setOriginSys(rs.getBytes("origin_sys"));
		info.setOperTime(rs.getTimestamp("oper_time"));
		info.setOperatorId(rs.getLong("operator_id"));
		info.setOperCmd(rs.getString("oper_cmd"));
		info.setPayload(rs.getString("payload"));
		info.setState(rs.getString("state"));
		info.setResult(rs.getString("result"));
		info.setModifierUid(rs.getLong("modifier_uid"));
		info.setModifyTime(rs.getTimestamp("modify_time"));
		info.setDelFlag(rs.getBoolean("del_flag"));

		return info;
	};

	/**
	 * Default constructor
	 **/
	public SyncMqInDAO (){
		this.daoIdKey = AppIdKey.SYNC_MQ_IN;
	}

	/**
	 * Get the IdKey of DAO
	 **/
	@Override
	public Identifier getIdKey() {
		return this.daoIdKey;
	}

    @Override
	public RowParser<SyncMqInInfo> getRowParser(){
    	return INFO_PARSER;
    }

    @Override
	public RowMapper<SyncMqInInfo> getRowMapper(){
		if(isCrossDB()){
			return new ParserBuilder<SyncMqInInfo>().build(INFO_PARSER);
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
	public Object[] getValues(SyncMqInInfo info, String ...columns){
		BeanMeta meta = BeanAccessor.getBeanMeta(SyncMqInInfo.class);
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
	public int create(SyncMqInInfo info) {
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
	public int update(SyncMqInInfo info) {

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
	public SyncMqInInfo row( InfoId id ){
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
