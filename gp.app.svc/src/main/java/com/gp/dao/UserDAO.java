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
import com.gp.dao.info.UserInfo;

@BindComponent(type = UserDAO.class, priority = BaseDAO.BASE_PRIORITY)
public class UserDAO extends DAOSupport implements BaseDAO<UserInfo> {

    private Identifier daoIdKey = BaseIdKey.BLIND;

	static Logger LOGGER = LoggerFactory.getLogger(UserDAO.class);

	static final String[] COLUMNS = new String[]{
             "source_id", "username", "user_gid", "trace_code", 
             "category", "mobile", "phone", "full_name", 
             "nickname", "email", "id_card", "state", 
             "crypto_key", "extra_info", "timezone", "language", 
             "cabinet_id", "classification", "score", "biography", 
             "sup_info", "avatar_url", "retry_times", "last_logon", 
             "remark", "create_time", "modifier_uid", "modify_time", 
             "del_flag"
    	};

	public static final RowParser<UserInfo> INFO_PARSER = (rp, ctx) -> {
		UserInfo info = new UserInfo();
		InfoId id = rp.getInfoId(BaseIdKey.USER);
		info.setInfoId(id);

		info.setSourceId(rp.getLong("source_id"));
		info.setUsername(rp.getString("username"));
		info.setUserGid(rp.getString("user_gid"));
		info.setTraceCode(rp.getString("trace_code"));
		info.setCategory(rp.getString("category"));
		info.setMobile(rp.getString("mobile"));
		info.setPhone(rp.getString("phone"));
		info.setFullName(rp.getString("full_name"));
		info.setNickname(rp.getString("nickname"));
		info.setEmail(rp.getString("email"));
		info.setIdCard(rp.getString("id_card"));
		info.setState(rp.getString("state"));
		info.setCryptoKey(rp.getString("crypto_key"));
		info.setExtraInfo(rp.getString("extra_info"));
		info.setTimezone(rp.getString("timezone"));
		info.setLanguage(rp.getString("language"));
		info.setCabinetId(rp.getLong("cabinet_id"));
		info.setClassification(rp.getString("classification"));
		info.setScore(rp.getInt("score"));
		info.setBiography(rp.getString("biography"));
		info.setSupInfo(rp.getString("sup_info"));
		info.setAvatarUrl(rp.getString("avatar_url"));
		info.setRetryTimes(rp.getInt("retry_times"));
		info.setLastLogon(rp.getTimestamp("last_logon"));
		info.setRemark(rp.getString("remark"));
		info.setCreateTime(rp.getTimestamp("create_time"));
		info.setModifierUid(rp.getLong("modifier_uid"));
		info.setModifyTime(rp.getTimestamp("modify_time"));
		info.setDelFlag(rp.getBoolean("del_flag"));

		return info;
	};

	public static final RowMapper<UserInfo> INFO_MAPPER = (rs, ctx) -> {
		UserInfo info = new UserInfo();
		InfoId id = IdKeys.getInfoId(BaseIdKey.USER, rs.getLong("user_id"));
		info.setInfoId(id);

		info.setSourceId(rs.getLong("source_id"));
		info.setUsername(rs.getString("username"));
		info.setUserGid(rs.getString("user_gid"));
		info.setTraceCode(rs.getString("trace_code"));
		info.setCategory(rs.getString("category"));
		info.setMobile(rs.getString("mobile"));
		info.setPhone(rs.getString("phone"));
		info.setFullName(rs.getString("full_name"));
		info.setNickname(rs.getString("nickname"));
		info.setEmail(rs.getString("email"));
		info.setIdCard(rs.getString("id_card"));
		info.setState(rs.getString("state"));
		info.setCryptoKey(rs.getString("crypto_key"));
		info.setExtraInfo(rs.getString("extra_info"));
		info.setTimezone(rs.getString("timezone"));
		info.setLanguage(rs.getString("language"));
		info.setCabinetId(rs.getLong("cabinet_id"));
		info.setClassification(rs.getString("classification"));
		info.setScore(rs.getInt("score"));
		info.setBiography(rs.getString("biography"));
		info.setSupInfo(rs.getString("sup_info"));
		info.setAvatarUrl(rs.getString("avatar_url"));
		info.setRetryTimes(rs.getInt("retry_times"));
		info.setLastLogon(rs.getTimestamp("last_logon"));
		info.setRemark(rs.getString("remark"));
		info.setCreateTime(rs.getTimestamp("create_time"));
		info.setModifierUid(rs.getLong("modifier_uid"));
		info.setModifyTime(rs.getTimestamp("modify_time"));
		info.setDelFlag(rs.getBoolean("del_flag"));

		return info;
	};

	/**
	 * Default constructor
	 **/
	public UserDAO (){
		this.daoIdKey = BaseIdKey.USER;
	}

	/**
	 * Get the IdKey of DAO
	 **/
	@Override
	public Identifier getIdKey() {
		return this.daoIdKey;
	}

    @Override
	public RowParser<UserInfo> getRowParser(){
    	return INFO_PARSER;
    }

    @Override
	public RowMapper<UserInfo> getRowMapper(){
		if(isCrossDB()){
			return new ParserBuilder<UserInfo>().build(INFO_PARSER);
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
	public Object[] getValues(UserInfo info, String ...columns){
		BeanMeta meta = BeanAccessor.getBeanMeta(UserInfo.class);
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
	public int create(UserInfo info) {
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
	public int update(UserInfo info) {

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
	public UserInfo row( InfoId id ){
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
