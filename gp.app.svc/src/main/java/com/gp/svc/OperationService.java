/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.svc;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.gp.bind.BindAutowired;
import com.gp.bind.BindComponent;
import com.gp.cache.CacheManager;
import com.gp.cache.ICache;
import com.gp.common.*;
import com.gp.dao.DAOSupport;
import com.gp.dao.DictionaryDAO;
import com.gp.dao.OperationDAO;
import com.gp.dao.UserDAO;
import com.gp.dao.info.DictionaryInfo;
import com.gp.dao.info.OperationInfo;
import com.gp.db.JdbiTran;
import com.gp.info.BaseIdKey;
import com.gp.paging.PageQuery;
import com.gp.paging.Pagination;
import com.gp.paging.Paginator;
import com.gp.sql.BaseBuilder.SortOrder;
import com.gp.sql.SqlBuilder;
import com.gp.sql.select.SelectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@BindComponent(priority = BaseService.BASE_PRIORITY)
public class OperationService extends ServiceSupport implements BaseService {

	static Logger LOGGER = LoggerFactory.getLogger(OperationService.class);

	@BindAutowired
	OperationDAO operationDao;
	
	@BindAutowired
	DictionaryDAO dictDao;

	@BindAutowired
	UserDAO userdao;

	private ICache dictCache;
	
	public OperationService(){
		
		this.dictCache = CacheManager.instance().getCache(Caches.DICT_CACHE);
	}

	@JdbiTran(readOnly = true)
	public List<OperationInfo> getNodeOperations(PageQuery pquery) {

		SelectBuilder builder = SqlBuilder.select();
		builder.column("a.*");
		builder.column("u.user_gid", "u.avatar_url");
		builder.from(from -> {
			from.table(BaseIdKey.OPERATION.schema() + " a");
			from.leftJoin(BaseIdKey.USER.schema() + " u", "a.subject_uid = u.user_id");
		});
		
		List<Object> params = Arrays.asList( );
	
		if (Objects.nonNull(pquery)) {
			SelectBuilder countBuilder = builder.clone();
			countBuilder.column().column("count(" + BaseIdKey.OPERATION.idColumn() + ")");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("SQL : {} / PARAMS : {}", countBuilder.build(), params);
			}
			Integer total = row(countBuilder.toString(), Integer.class, params);
			Paginator paginator = new Paginator(total, pquery);
			Pagination pagination = paginator.getPagination();
			pquery.setPagination(pagination);

			SortOrder orderType = SortOrder.valueOf(pquery.getOrder().toUpperCase());
			builder.orderBy(pquery.getOrderBy(), orderType);
			builder.limit(pagination.getPageStartRow(), pquery.getPageSize());
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : " + builder.build() + " / params : " + params);
		}
		
		return rows(builder.build(), (rs, idx) -> {

			OperationInfo oper = OperationDAO.INFO_MAPPER.map(rs, idx);

			DAOSupport.setInfoProperty(rs, oper, String.class, "user_gid", "avatar_url");

			return oper;
		}, params);
	}
	
	@JdbiTran(readOnly = true)
	public List<OperationInfo> getWorkgroupOperations(InfoId wid, PageQuery pquery) {

		SelectBuilder builder = SqlBuilder.select();
		builder.column("a.*");
		builder.column("u.user_gid", "u.avatar_url");
		builder.from(from -> {
			from.table(BaseIdKey.OPERATION.schema() + " a");
			from.leftJoin(BaseIdKey.USER.schema() + " u", "a.subject_uid = u.user_id");
		});

		builder.where("a.workgroup_id = ?");
		
		builder.orderBy("oper_id", SortOrder.DESC);
		
		List<Object> params = Arrays.asList(wid.getId());
	
		List<OperationInfo> result = null;

		if (Objects.nonNull(pquery)) {
			SelectBuilder countBuilder = builder.clone();
			countBuilder.column().column("count(" + BaseIdKey.OPERATION.idColumn() + ")");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("SQL : {} / PARAMS : {}", countBuilder.build(), params);
			}
			Integer total = row(countBuilder.toString(), Integer.class, params);
			Paginator paginator = new Paginator(total, pquery);
			Pagination pagination = paginator.getPagination();
			pquery.setPagination(pagination);

			SortOrder orderType = SortOrder.valueOf(pquery.getOrder().toUpperCase());
			builder.orderBy(pquery.getOrderBy(), orderType);
			builder.limit(pagination.getPageStartRow(), pquery.getPageSize());
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : " + builder.build() + " / params : " + params);
		}
		result = rows(builder.build(), (rs, idx) -> {

			OperationInfo oper = OperationDAO.INFO_MAPPER.map(rs, idx);

			DAOSupport.setInfoProperty(rs, oper, String.class, "user_gid", "avatar_url");

			return oper;
		}, params);

		return result;
	}

	@JdbiTran(readOnly = true)
	public List<OperationInfo> getAccountOperations(String account, PageQuery pquery) {

		SelectBuilder builder = SqlBuilder.select();
		builder.column("a.*");
		builder.column("u.user_gid", "u.avatar_url");
		builder.from(from -> {
			from.table(BaseIdKey.OPERATION.schema() + " a");
			from.leftJoin(BaseIdKey.USER.schema() + " u", "a.subject_uid = u.user_id");
		});
		builder.where("a.subject = ?");

		List<Object> params = Arrays.asList(account);
	
		if (Objects.nonNull(pquery)) {
			SelectBuilder countBuilder = builder.clone();
			countBuilder.column().column("count(" + BaseIdKey.OPERATION.idColumn() + ")");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("SQL : {} / PARAMS : {}", countBuilder.build(), params);
			}
			Integer total = row(countBuilder.toString(), Integer.class, params);
			Paginator paginator = new Paginator(total, pquery);
			Pagination pagination = paginator.getPagination();
			pquery.setPagination(pagination);

			SortOrder orderType = SortOrder.valueOf(pquery.getOrder().toUpperCase());
			builder.orderBy(pquery.getOrderBy(), orderType);
			builder.limit(pagination.getPageStartRow(), pquery.getPageSize());
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : " + builder.build() + " / params : " + params.toString());
		}
		
		return rows(builder.build(), (rs, idx) -> {

			OperationInfo oper = OperationDAO.INFO_MAPPER.map(rs, idx);

			DAOSupport.setInfoProperty(rs, oper, String.class, "user_gid", "avatar_url");

			return oper;
		}, params);

	}

	@JdbiTran(readOnly = true)
	public List<OperationInfo> getObjectOperations(InfoId objectId, PageQuery pquery) {

		SelectBuilder builder = SqlBuilder.select();
		builder.column("a.*");
		builder.column("u.user_gid", "u.avatar_url");
		builder.from(from -> {
			from.table(BaseIdKey.OPERATION.schema() + " a");
			from.leftJoin(BaseIdKey.USER.schema() + " u", "a.subject_uid = u.user_id");
		});
		builder.where("a.object = ?");

		List<Object> params = Arrays.asList( objectId.toString());
	
		if (Objects.nonNull(pquery)) {
			SelectBuilder countBuilder = builder.clone();
			countBuilder.column().column("count(" + BaseIdKey.OPERATION.idColumn() + ")");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("SQL : {} / PARAMS : {}", countBuilder.build(), params);
			}
			Integer total = row(countBuilder.toString(), Integer.class, params);
			Paginator paginator = new Paginator(total, pquery);
			Pagination pagination = paginator.getPagination();
			pquery.setPagination(pagination);

			SortOrder orderType = SortOrder.valueOf(pquery.getOrder().toUpperCase());
			builder.orderBy(pquery.getOrderBy(), orderType);
			builder.limit(pagination.getPageStartRow(), pquery.getPageSize());
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : " + builder.build() + " / params : " + params);
		}
		
		return rows(builder.build(), (rs, idx) -> {

			OperationInfo oper = OperationDAO.INFO_MAPPER.map(rs, idx);

			DAOSupport.setInfoProperty(rs, oper, String.class, "user_gid", "avatar_url");

			return oper;
		}, params);
	}

	@SuppressWarnings("unchecked")
	@JdbiTran
	public void addOperation(OperationInfo operlog) {

		KVPair<String, String> langHolder = KVPair.newPair("language");
		
		String userCacheKey = "oper.user:" + operlog.getSubject();		
		Object cachedUser = this.dictCache.fetch(userCacheKey);
		// Fetch object from cache
		if(cachedUser != null) {
			LOGGER.debug("find cached operator - {}", userCacheKey);
			
			Map<String, Object> _user = (Map<String, Object> )cachedUser;
			
			operlog.setSubjectLabel((String)_user.get("full_name"));
			operlog.setSubjectUid((Long)_user.get("user_id"));
			langHolder.setValue((String)_user.get("language"));
			
		}else {
			
			SelectBuilder builder = SqlBuilder.select(BaseIdKey.USER.schema());
			builder.where("user_id = ?").or("username = ?");
			
			List<Object> params = Arrays.asList( operlog.getSubjectUid(), operlog.getSubject());
			
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("SQL : " + builder.toString() + " / PARAMS : " + params);
			}
						
			query(builder.toString(), (rs) -> {
				
				operlog.setSubjectLabel(rs.getString("full_name"));
				operlog.setSubjectUid(rs.getLong("user_id"));
				langHolder.setValue(rs.getString("language"));
				
				Map<String, Object> _user = Maps.newHashMap();
				_user.put("full_name", operlog.getSubjectLabel());
				_user.put("user_id", operlog.getSubjectUid());
				_user.put("language", langHolder.getValue());
				
				this.dictCache.put(userCacheKey, _user);
			}, params);
			
		}
		
		// dictionary entry start with: oper.[...]
		String dcitCacheKey = "oper." + operlog.getOperation().toLowerCase();
		// Fetch object from cache
		Object cachedDict = this.dictCache.fetch(dcitCacheKey);
		if(cachedDict != null) {
			
			DictionaryInfo info = (DictionaryInfo) cachedDict;
			operlog.setOperationLabel(info.getLabel(langHolder.getValue()));
			
		}else {
			
			SelectBuilder dictBuilder = dictDao.selectSql();
			
			dictBuilder.where("dict_key = '" + dcitCacheKey + "'");
			List<DictionaryInfo> dinfos = rows(dictBuilder.toString(), DictionaryDAO.INFO_MAPPER);
			
			if(!Iterables.isEmpty(dinfos)) {
				operlog.setOperationLabel(dinfos.get(0).getLabel(langHolder.getValue()));
				
				this.dictCache.put(dcitCacheKey, dinfos.get(0));
			}
		}
		
		operationDao.create(operlog);

	}

	@JdbiTran(readOnly = true)
	public KVPair<String, String> getObjectPredicateLabel(InfoId objectId, Instruct oper) {
		
		if (null == objectId)
			return null;

		String objectLabel = null;
		String predicateLabel = null;

		return null;
	}
}
