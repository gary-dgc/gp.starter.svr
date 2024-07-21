package com.gp.dao;

import com.google.common.collect.Lists;
import com.gp.bean.BeanAccessor;
import com.gp.bean.BeanMeta;
import com.gp.bean.BeanProxy;
import com.gp.bind.BindComponent;
import com.gp.common.*;
import com.gp.dao.info.DutyAcsInfo;
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

@BindComponent(type = DutyAcsDAO.class, priority = BaseDAO.BASE_PRIORITY)
public class DutyAcsDAO extends DAOSupport implements BaseDAO<DutyAcsInfo> {

    private Identifier daoIdKey = BaseIdKey.BLIND;

	static Logger LOGGER = LoggerFactory.getLogger(DutyAcsDAO.class);

	static final String[] COLUMNS = new String[]{
             "duty_id", "sys_id", "data_id", "role_ids", 
             "reside_dept", "direct_dept", "any_sub_dept", "direct_subord", 
             "any_subord", "modifier_uid", "modify_time", "del_flag", 
    	};

	public static final RowParser<DutyAcsInfo> INFO_PARSER = (rp, ctx) -> {
		DutyAcsInfo info = new DutyAcsInfo();
		InfoId id = rp.getInfoId(AppIdKey.DUTY_ACS);
		info.setInfoId(id);

		info.setDutyId(rp.getLong("duty_id"));
		info.setSysId(rp.getLong("sys_id"));
		info.setDataId(rp.getLong("data_id"));
		info.setRoleIds(rp.getString("role_ids"));
		info.setResideDept(rp.getBoolean("reside_dept"));
		info.setDirectDept(rp.getBoolean("direct_dept"));
		info.setAnySubDept(rp.getBoolean("any_sub_dept"));
		info.setDirectSubord(rp.getBoolean("direct_subord"));
		info.setAnySubord(rp.getBoolean("any_subord"));
		info.setModifierUid(rp.getLong("modifier_uid"));
		info.setModifyTime(rp.getTimestamp("modify_time"));
		info.setDelFlag(rp.getBoolean("del_flag"));

		return info;
	};

	public static final RowMapper<DutyAcsInfo> INFO_MAPPER = (rs, ctx) -> {
		DutyAcsInfo info = new DutyAcsInfo();
		InfoId id = IdKeys.getInfoId(AppIdKey.DUTY_ACS, rs.getLong("access_id"));
		info.setInfoId(id);

		info.setDutyId(rs.getLong("duty_id"));
		info.setSysId(rs.getLong("sys_id"));
		info.setDataId(rs.getLong("data_id"));
		info.setRoleIds(rs.getString("role_ids"));
		info.setResideDept(rs.getBoolean("reside_dept"));
		info.setDirectDept(rs.getBoolean("direct_dept"));
		info.setAnySubDept(rs.getBoolean("any_sub_dept"));
		info.setDirectSubord(rs.getBoolean("direct_subord"));
		info.setAnySubord(rs.getBoolean("any_subord"));
		info.setModifierUid(rs.getLong("modifier_uid"));
		info.setModifyTime(rs.getTimestamp("modify_time"));
		info.setDelFlag(rs.getBoolean("del_flag"));

		return info;
	};

	/**
	 * Default constructor
	 **/
	public DutyAcsDAO(){
		this.daoIdKey = AppIdKey.DUTY_ACS;
	}

	/**
	 * Get the IdKey of DAO
	 **/
	@Override
	public Identifier getIdKey() {
		return this.daoIdKey;
	}

    @Override
	public RowParser<DutyAcsInfo> getRowParser(){
    	return INFO_PARSER;
    }

    @Override
	public RowMapper<DutyAcsInfo> getRowMapper(){
		if(isCrossDB()){
			return new ParserBuilder<DutyAcsInfo>().build(INFO_PARSER);
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
	public Object[] getValues(DutyAcsInfo info, String ...columns){
		BeanMeta meta = BeanAccessor.getBeanMeta(DutyAcsInfo.class);
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
	public int create(DutyAcsInfo info) {
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
	public int update(DutyAcsInfo info) {

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
	public DutyAcsInfo row( InfoId id ){
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
