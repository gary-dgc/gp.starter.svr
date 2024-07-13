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
import com.gp.dao.info.SysOptionInfo;
import com.gp.dao.info.TokenInfo;
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

@BindComponent(type = TokenDAO.class, priority = BaseDAO.BASE_PRIORITY)
public class TokenDAO extends DAOSupport implements BaseDAO<TokenInfo> {

    private Identifier daoIdKey = BaseIdKey.BLIND;

	static Logger LOGGER = LoggerFactory.getLogger(TokenDAO.class);

	static final String[] COLUMNS = new String[]{
             "issuer", "audience", "expire_time", "not_before", 
             "subject", "device", "issue_at", "claims", 
             "jwt_token", "scope", "refresh_token", "modifier_uid", 
             "modify_time", "del_flag"
    	};

	public static final RowParser<TokenInfo> INFO_PARSER = (rs, ctx) -> {
		TokenInfo info = new TokenInfo();
		InfoId id = rs.getInfoId(BaseIdKey.TOKEN);
		info.setInfoId(id);

		info.setIssuer(rs.getString("issuer"));
		info.setAudience(rs.getString("audience"));
		info.setExpireTime(rs.getTimestamp("expire_time"));
		info.setNotBefore(rs.getTimestamp("not_before"));
		info.setSubject(rs.getString("subject"));
		info.setDevice(rs.getString("device"));
		info.setIssueAt(rs.getTimestamp("issue_at"));
		info.setClaims(rs.getString("claims"));
		info.setJwtToken(rs.getString("jwt_token"));
		info.setScope(rs.getString("scope"));
		info.setRefreshToken(rs.getString("refresh_token"));
		info.setModifierUid(rs.getLong("modifier_uid"));
		info.setModifyTime(rs.getTimestamp("modify_time"));
		info.setDelFlag(rs.getBoolean("del_flag"));

		return info;
	};

	public static final RowMapper<TokenInfo> INFO_MAPPER = (rs, ctx) -> {
		TokenInfo info = new TokenInfo();
		InfoId id = IdKeys.getInfoId(BaseIdKey.TOKEN, rs.getLong("token_id"));
		info.setInfoId(id);
		
		info.setIssuer(rs.getString("issuer"));
		info.setAudience(rs.getString("audience"));
		info.setExpireTime(rs.getTimestamp("expire_time"));
		info.setNotBefore(rs.getTimestamp("not_before"));
		info.setSubject(rs.getString("subject"));
		info.setDevice(rs.getString("device"));
		info.setIssueAt(rs.getTimestamp("issue_at"));
		info.setClaims(rs.getString("claims"));
		info.setJwtToken(rs.getString("jwt_token"));
		info.setScope(rs.getString("scope"));
		info.setRefreshToken(rs.getString("refresh_token"));
		info.setModifierUid(rs.getLong("modifier_uid"));
		info.setModifyTime(rs.getTimestamp("modify_time"));
		info.setDelFlag(rs.getBoolean("del_flag"));
		
		return info;
	};


	/**
	 * Default constructor
	 **/
	public TokenDAO(){
		this.daoIdKey = BaseIdKey.TOKEN;
	}

	/**
	 * Get the IdKey of DAO
	 **/
	@Override
	public Identifier getIdKey() {
		return this.daoIdKey;
	}

	@Override
	public RowParser<TokenInfo> getRowParser(){
		return INFO_PARSER;
	}

	@Override
	public RowMapper<TokenInfo> getRowMapper(){
		if(isCrossDB()){
			return new ParserBuilder<TokenInfo>().build(INFO_PARSER);
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
	public Object[] getValues(TokenInfo info, String ...columns){
		BeanMeta meta = BeanAccessor.getBeanMeta(TokenInfo.class);
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
	public int create(TokenInfo info) {
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
	public int update(TokenInfo info) {

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
	public TokenInfo row(InfoId id ){
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
