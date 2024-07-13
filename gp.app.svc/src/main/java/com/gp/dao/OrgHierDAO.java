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
import com.gp.dao.info.OrgHierInfo;

@BindComponent(type = OrgHierDAO.class, priority = BaseDAO.BASE_PRIORITY)
public class OrgHierDAO extends DAOSupport implements BaseDAO<OrgHierInfo> {

    private Identifier daoIdKey = BaseIdKey.BLIND;

	static Logger LOGGER = LoggerFactory.getLogger(OrgHierDAO.class);

	static final String[] COLUMNS = new String[]{
             "org_pid", "org_ecd", "org_name", "org_type", 
             "avatar_url", "email", "description", "fax", 
             "phone", "liaison", "tax_no", "state", 
             "mobile", "modifier_uid", "modify_time", "del_flag", 
    	};

	public static final RowParser<OrgHierInfo> INFO_PARSER = (rp, ctx) -> {
		OrgHierInfo info = new OrgHierInfo();
		InfoId id = rp.getInfoId(MasterIdKey.ORG_HIER);
		info.setInfoId(id);

		info.setOrgPid(rp.getLong("org_pid"));
		info.setOrgEcd(rp.getString("org_ecd"));
		info.setOrgName(rp.getString("org_name"));
		info.setOrgType(rp.getString("org_type"));
		info.setAvatarUrl(rp.getString("avatar_url"));
		info.setEmail(rp.getString("email"));
		info.setDescription(rp.getString("description"));
		info.setFax(rp.getString("fax"));
		info.setPhone(rp.getString("phone"));
		info.setLiaison(rp.getString("liaison"));
		info.setTaxNo(rp.getString("tax_no"));
		info.setState(rp.getString("state"));
		info.setMobile(rp.getString("mobile"));
		info.setModifierUid(rp.getLong("modifier_uid"));
		info.setModifyTime(rp.getTimestamp("modify_time"));
		info.setDelFlag(rp.getBoolean("del_flag"));

		return info;
	};

	public static final RowMapper<OrgHierInfo> INFO_MAPPER = (rs, ctx) -> {
		OrgHierInfo info = new OrgHierInfo();
		InfoId id = IdKeys.getInfoId(MasterIdKey.ORG_HIER, rs.getLong("org_id"));
		info.setInfoId(id);

		info.setOrgPid(rs.getLong("org_pid"));
		info.setOrgEcd(rs.getString("org_ecd"));
		info.setOrgName(rs.getString("org_name"));
		info.setOrgType(rs.getString("org_type"));
		info.setAvatarUrl(rs.getString("avatar_url"));
		info.setEmail(rs.getString("email"));
		info.setDescription(rs.getString("description"));
		info.setFax(rs.getString("fax"));
		info.setPhone(rs.getString("phone"));
		info.setLiaison(rs.getString("liaison"));
		info.setTaxNo(rs.getString("tax_no"));
		info.setState(rs.getString("state"));
		info.setMobile(rs.getString("mobile"));
		info.setModifierUid(rs.getLong("modifier_uid"));
		info.setModifyTime(rs.getTimestamp("modify_time"));
		info.setDelFlag(rs.getBoolean("del_flag"));

		return info;
	};

	/**
	 * Default constructor
	 **/
	public OrgHierDAO (){
		this.daoIdKey = MasterIdKey.ORG_HIER;
	}

	/**
	 * Get the IdKey of DAO
	 **/
	@Override
	public Identifier getIdKey() {
		return this.daoIdKey;
	}

    @Override
	public RowParser<OrgHierInfo> getRowParser(){
    	return INFO_PARSER;
    }

    @Override
	public RowMapper<OrgHierInfo> getRowMapper(){
		if(isCrossDB()){
			return new ParserBuilder<OrgHierInfo>().build(INFO_PARSER);
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
	public Object[] getValues(OrgHierInfo info, String ...columns){
		BeanMeta meta = BeanAccessor.getBeanMeta(OrgHierInfo.class);
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
	public int create(OrgHierInfo info) {
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
	public int update(OrgHierInfo info) {

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
	public OrgHierInfo row( InfoId id ){
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
