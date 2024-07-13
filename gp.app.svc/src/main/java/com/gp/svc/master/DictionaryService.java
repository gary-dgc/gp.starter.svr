/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.svc.master;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.gp.bind.BindAutowired;
import com.gp.bind.BindComponent;
import com.gp.cache.CacheManager;
import com.gp.cache.ICache;
import com.gp.common.Caches;
import com.gp.common.GeneralConsts;
import com.gp.common.InfoId;
import com.gp.common.ServiceContext;
import com.gp.dao.DictionaryDAO;
import com.gp.dao.info.DictionaryInfo;
import com.gp.db.JdbiTran;
import com.gp.info.BaseIdKey;
import com.gp.paging.PageQuery;
import com.gp.paging.Pagination;
import com.gp.paging.Paginator;
import com.gp.sql.BaseBuilder.SortOrder;
import com.gp.sql.SqlBuilder;
import com.gp.sql.select.SelectBuilder;
import com.gp.svc.BaseService;
import com.gp.svc.ServiceSupport;
import org.jdbi.v3.core.mapper.RowMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@BindComponent(priority = BaseService.BASE_PRIORITY)
public class DictionaryService extends ServiceSupport implements BaseService {

	public static Logger LOGGER = LoggerFactory.getLogger(DictionaryService.class);
	
	public static final String PROP_PREFIX = "prop.";
	
	public static final String LANG_EN_US = "en_US";
	public static final String LANG_FR_FR = "fr_FR";
	public static final String LANG_ZH_CN = "zh_CN";
	public static final String LANG_DE_DE = "de_DE";
	public static final String LANG_RU_RU = "ru_RU";
	
	@BindAutowired
	private DictionaryDAO dictionarydao;

	private ICache dictCache;
	
	public DictionaryService() {
		
		this.dictCache = CacheManager.instance().getCache(Caches.DICT_CACHE);
	}
	
	@JdbiTran(readOnly = true)
	public List<DictionaryInfo> getDictEntries(String dictGroup, String keyFilter, PageQuery pquery) {

		SelectBuilder builder = SqlBuilder.select(BaseIdKey.DICTIONARY.schema());

		Map<String, Object> paramap = new HashMap<String, Object>();

		if (!Strings.isNullOrEmpty(dictGroup)) {
			builder.and("dict_group = :group");
			paramap.put("group", dictGroup);
		}

		builder.and("dict_key LIKE :key");
		paramap.put("key", keyFilter == null ? "%" : keyFilter + "%");

		RowMapper<DictionaryInfo> rmapper = DictionaryDAO.INFO_MAPPER;

		if (Objects.nonNull(pquery)) {
			SelectBuilder countBuilder = builder.clone();
			countBuilder.column().column("count(" + BaseIdKey.DICTIONARY.idColumn() + ")");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("SQL : {} / PARAMS : {}", countBuilder.build(), paramap);
			}
			Integer total = row(countBuilder.toString(), Integer.class, paramap);
			Paginator paginator = new Paginator(total, pquery);
			Pagination pagination = paginator.getPagination();
			pquery.setPagination(pagination);

			SortOrder orderType = SortOrder.valueOf(pquery.getOrder().toUpperCase());
			builder.orderBy(pquery.getOrderBy(), orderType);
			builder.limit(pagination.getPageStartRow(), pquery.getPageSize());
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : {} / PARAM : {}", builder.build(), paramap.toString());
		}
		
		return rows(builder.build(), rmapper, paramap);

	}

	@JdbiTran
	public boolean updateDictEntry(ServiceContext svcctx, DictionaryInfo dictinfo) {

		svcctx.setTraceInfo(dictinfo);
		// evict cache entries
		String dkey = dictinfo.getDictGroup() + GeneralConsts.KEYS_SEPARATOR + dictinfo.getDictKey();
		dictCache.evict(dkey, dictinfo.getDictKey(), dictinfo.getInfoId().toString());
		
		return dictionarydao.update(dictinfo) > 0;

	}

	@JdbiTran
	public DictionaryInfo getDictEntry(InfoId dictId){
		
		return (DictionaryInfo)dictCache.fetch(dictId.toString(), key -> {
			return dictionarydao.row(dictId);
		});
	
	}

	public DictionaryInfo getDictEntry(String dictKey) {
		
		return this.getDictEntry(dictKey, false);
	
	}

	@JdbiTran(readOnly = true)
	public DictionaryInfo getDictEntry(String dictKey, boolean property) {

		return (DictionaryInfo)dictCache.fetch(dictKey, key -> {
			
			SelectBuilder builder = SqlBuilder.select(BaseIdKey.DICTIONARY.schema());
			builder.where("dict_key = ?");
			if (property) {
				builder.and("dict_group = 'info_prop'");
			}
			
			List<Object> parms = Arrays.asList( dictKey );
	
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("SQL : {} / PARAM : {}", builder.toString(), parms);
			}
			
			return row(builder.toString(), DictionaryDAO.INFO_MAPPER, parms);
		});
	}

	@JdbiTran(readOnly = true)
	public List<DictionaryInfo> getDictGroupEntries(String dictGroup) {
		SelectBuilder builder = SqlBuilder.select(BaseIdKey.DICTIONARY.schema());
		builder.where("dict_group = ?");

		List<Object> parms = Arrays.asList( dictGroup );

		List<DictionaryInfo> result = rows(builder.toString(),  DictionaryDAO.INFO_MAPPER, parms);
		
		return result;

	}

	@JdbiTran(readOnly = true)
	public DictionaryInfo getDictEntry(String dictGroup, String dictKey) {
		
		SelectBuilder builder = SqlBuilder.select(BaseIdKey.DICTIONARY.schema());
		builder.where("dict_group = ?");
		builder.and("dict_key = ?");

		List<Object> parms = Arrays.asList( dictGroup, dictKey );

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : {} / PARAM : {}", builder.toString(), parms);
		}
		DictionaryInfo dictinfo = row(builder.toString(), DictionaryDAO.INFO_MAPPER, parms);
		
		String dkey = dictinfo.getDictGroup() + GeneralConsts.KEYS_SEPARATOR + dictinfo.getDictKey();
		dictCache.put(dkey, dictinfo);
		dictCache.put(dictinfo.getDictKey(), dictinfo);
		dictCache.put(dictinfo.getInfoId().toString(), dictinfo);
	
		return dictinfo ;

	}

	@JdbiTran(readOnly = true)
	public List<String> getDictGroups() {
		SelectBuilder builder = SqlBuilder.select(BaseIdKey.DICTIONARY.schema());
		builder.column("dict_group");
		builder.distinct();

		List<String> rtv = Lists.newArrayList();

		rtv = rows(builder.toString(), String.class);

		return rtv;
	}

}
