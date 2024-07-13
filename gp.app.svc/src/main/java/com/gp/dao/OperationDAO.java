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
import com.gp.dao.info.OperationInfo;

@BindComponent(type = OperationDAO.class, priority = BaseDAO.BASE_PRIORITY)
public class OperationDAO extends DAOSupport implements BaseDAO<OperationInfo> {

    private Identifier daoIdKey = BaseIdKey.BLIND;

	static Logger LOGGER = LoggerFactory.getLogger(OperationDAO.class);

	static final String[] COLUMNS = new String[]{
             "audit_id", "workgroup_id", "subject_uid", "subject", 
             "subject_label", "operation_time", "operation", "operation_label", 
             "object", "object_label", "second", "second_label", 
             "predicates", "modifier_uid", "modify_time", "del_flag", 
    	};

	public static final RowParser<OperationInfo> INFO_PARSER = (rp, ctx) -> {
		OperationInfo info = new OperationInfo();
		InfoId id = rp.getInfoId(BaseIdKey.OPERATION);
		info.setInfoId(id);

		info.setAuditId(rp.getLong("audit_id"));
		info.setWorkgroupId(rp.getLong("workgroup_id"));
		info.setSubjectUid(rp.getLong("subject_uid"));
		info.setSubject(rp.getString("subject"));
		info.setSubjectLabel(rp.getString("subject_label"));
		info.setOperationTime(rp.getTimestamp("operation_time"));
		info.setOperation(rp.getString("operation"));
		info.setOperationLabel(rp.getString("operation_label"));
		info.setObject(rp.getString("object"));
		info.setObjectLabel(rp.getString("object_label"));
		info.setSecond(rp.getString("second"));
		info.setSecondLabel(rp.getString("second_label"));
		info.setPredicates(rp.getString("predicates"));
		info.setModifierUid(rp.getLong("modifier_uid"));
		info.setModifyTime(rp.getTimestamp("modify_time"));
		info.setDelFlag(rp.getBoolean("del_flag"));

		return info;
	};

	public static final RowMapper<OperationInfo> INFO_MAPPER = (rs, ctx) -> {
		OperationInfo info = new OperationInfo();
		InfoId id = IdKeys.getInfoId(BaseIdKey.OPERATION, rs.getLong("oper_id"));
		info.setInfoId(id);

		info.setAuditId(rs.getLong("audit_id"));
		info.setWorkgroupId(rs.getLong("workgroup_id"));
		info.setSubjectUid(rs.getLong("subject_uid"));
		info.setSubject(rs.getString("subject"));
		info.setSubjectLabel(rs.getString("subject_label"));
		info.setOperationTime(rs.getTimestamp("operation_time"));
		info.setOperation(rs.getString("operation"));
		info.setOperationLabel(rs.getString("operation_label"));
		info.setObject(rs.getString("object"));
		info.setObjectLabel(rs.getString("object_label"));
		info.setSecond(rs.getString("second"));
		info.setSecondLabel(rs.getString("second_label"));
		info.setPredicates(rs.getString("predicates"));
		info.setModifierUid(rs.getLong("modifier_uid"));
		info.setModifyTime(rs.getTimestamp("modify_time"));
		info.setDelFlag(rs.getBoolean("del_flag"));

		return info;
	};

	/**
	 * Default constructor
	 **/
	public OperationDAO (){
		this.daoIdKey = BaseIdKey.OPERATION;
	}

	/**
	 * Get the IdKey of DAO
	 **/
	@Override
	public Identifier getIdKey() {
		return this.daoIdKey;
	}

    @Override
	public RowParser<OperationInfo> getRowParser(){
    	return INFO_PARSER;
    }

    @Override
	public RowMapper<OperationInfo> getRowMapper(){
		if(isCrossDB()){
			return new ParserBuilder<OperationInfo>().build(INFO_PARSER);
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
	public Object[] getValues(OperationInfo info, String ...columns){
		BeanMeta meta = BeanAccessor.getBeanMeta(OperationInfo.class);
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
	public int create(OperationInfo info) {
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
	public int update(OperationInfo info) {

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
	public OperationInfo row( InfoId id ){
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
