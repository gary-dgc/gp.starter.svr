package com.gp.dao;

import com.google.common.collect.Lists;
import com.gp.bean.BeanAccessor;
import com.gp.bean.BeanMeta;
import com.gp.bean.BeanProxy;
import com.gp.bind.BindComponent;
import com.gp.common.FlatColumns;
import com.gp.common.IdKeys;
import com.gp.common.Identifier;
import com.gp.common.InfoId;
import com.gp.dao.info.AuditInfo;
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

@BindComponent(type = AuditDAO.class, priority = BaseDAO.BASE_PRIORITY)
public class AuditDAO extends DAOSupport implements BaseDAO<AuditInfo> {

    private Identifier daoIdKey = BaseIdKey.BLIND;

	static Logger LOGGER = LoggerFactory.getLogger(AuditDAO.class);

	static final String[] COLUMNS = new String[]{
             "client", "host", "app", "path", 
             "version", "device", "subject", "operation", 
             "object_id", "predicates", "state", "message", 
             "audit_time", "elapsed_time", "modifier_uid", "modify_time", 
             "del_flag", "instance_id"
    	};

	public static final RowParser<AuditInfo> INFO_PARSER = (rp, ctx) -> {
		AuditInfo info = new AuditInfo();
		InfoId id = rp.getInfoId(BaseIdKey.AUDIT);
		info.setInfoId(id);

		info.setClient(rp.getString("client"));
		info.setHost(rp.getString("host"));
		info.setApp(rp.getString("app"));
		info.setPath(rp.getString("path"));
		info.setVersion(rp.getString("version"));
		info.setDevice(rp.getString("device"));
		info.setSubject(rp.getString("subject"));
		info.setOperation(rp.getString("operation"));
		info.setObjectId(rp.getString("object_id"));
		info.setPredicates(rp.getString("predicates"));
		info.setState(rp.getString("state"));
		info.setMessage(rp.getString("message"));
		info.setAuditTime(rp.getTimestamp("audit_time"));
		info.setElapsedTime(rp.getInt("elapsed_time"));
		info.setModifierUid(rp.getLong("modifier_uid"));
		info.setModifyTime(rp.getTimestamp("modify_time"));
		info.setDelFlag(rp.getBoolean("del_flag"));
		info.setInstanceId(rp.getInt("instance_id"));

		return info;
	};

	public static final RowMapper<AuditInfo> INFO_MAPPER = (rs, ctx) -> {
		AuditInfo info = new AuditInfo();
		InfoId id = IdKeys.getInfoId(BaseIdKey.AUDIT, rs.getLong("audit_id"));
		info.setInfoId(id);

		info.setClient(rs.getString("client"));
		info.setHost(rs.getString("host"));
		info.setApp(rs.getString("app"));
		info.setPath(rs.getString("path"));
		info.setVersion(rs.getString("version"));
		info.setDevice(rs.getString("device"));
		info.setSubject(rs.getString("subject"));
		info.setOperation(rs.getString("operation"));
		info.setObjectId(rs.getString("object_id"));
		info.setPredicates(rs.getString("predicates"));
		info.setState(rs.getString("state"));
		info.setMessage(rs.getString("message"));
		info.setAuditTime(rs.getTimestamp("audit_time"));
		info.setElapsedTime(rs.getInt("elapsed_time"));
		info.setModifierUid(rs.getLong("modifier_uid"));
		info.setModifyTime(rs.getTimestamp("modify_time"));
		info.setDelFlag(rs.getBoolean("del_flag"));
		info.setInstanceId(rs.getInt("instance_id"));

		return info;
	};

	/**
	 * Default constructor
	 **/
	public AuditDAO(){
		this.daoIdKey = BaseIdKey.AUDIT;
	}

	/**
	 * Get the IdKey of DAO
	 **/
	@Override
	public Identifier getIdKey() {
		return this.daoIdKey;
	}

    @Override
	public RowParser<AuditInfo> getRowParser(){
    	return INFO_PARSER;
    }

    @Override
	public RowMapper<AuditInfo> getRowMapper(){
		if(isCrossDB()){
			return new ParserBuilder<AuditInfo>().build(INFO_PARSER);
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
	public Object[] getValues(AuditInfo info, String ...columns){
		BeanMeta meta = BeanAccessor.getBeanMeta(AuditInfo.class);
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
	public int create(AuditInfo info) {
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
	public int update(AuditInfo info) {

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
	public AuditInfo row( InfoId id ){
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
