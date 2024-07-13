/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.svc;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.gp.bind.BindAutowired;
import com.gp.bind.BindComponent;
import com.gp.common.InfoId;
import com.gp.common.KVPair;
import com.gp.common.ServiceContext;
import com.gp.common.Sources;
import com.gp.dao.BaseDAO;
import com.gp.dao.SeqNoDAO;
import com.gp.dao.SysOptionDAO;
import com.gp.dao.UserDAO;
import com.gp.dao.info.SeqNoInfo;
import com.gp.dao.info.SysOptionInfo;
import com.gp.db.JdbiTran;
import com.gp.info.BaseIdKey;
import com.gp.info.FilterMode;
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
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@BindComponent( priority = BaseService.BASE_PRIORITY - 20)
public class SystemService extends ServiceSupport implements BaseService {

	Logger LOGGER = LoggerFactory.getLogger(SystemService.class);

	@BindAutowired
	SysOptionDAO sysoptiondao;

	@BindAutowired
	UserDAO userdao;

	@JdbiTran(readOnly = true)
	public List<SysOptionInfo> getOptions() {

		SelectBuilder builder = SqlBuilder.select(BaseIdKey.SYS_OPTION.schema());

		if (LOGGER.isDebugEnabled()) {

			LOGGER.debug("SQL : " + builder.toString());
		}

		return rows(builder.toString(), SysOptionDAO.INFO_MAPPER);

	}

	@JdbiTran(readOnly = true)
	public List<SysOptionInfo> getOptions(String groupKey[], String keywords, PageQuery pquery) {

		SelectBuilder builder = SqlBuilder.select(BaseIdKey.SYS_OPTION.schema());

		if (groupKey != null && groupKey.length > 0) {

			String allKeys = Joiner.on("','").join(groupKey);
			builder.and("opt_group in ('" + allKeys + "') ");

		}
		if (!Strings.isNullOrEmpty(keywords)) {

			builder.and((cond) -> {
				cond.or("opt_key like '" + keywords + "%'");
				cond.or("description like '" + keywords + "%'");
			});
		}
		
		if (Objects.nonNull(pquery)) {
			SelectBuilder countBuilder = builder.clone();
			countBuilder.column().column("count(" + BaseIdKey.SYS_OPTION.idColumn() + ")");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("SQL : {} / PARAMS : {}", countBuilder.build());
			}
			Integer total = row(countBuilder.toString(), Integer.class);
			Paginator paginator = new Paginator(total, pquery);
			Pagination pagination = paginator.getPagination();
			pquery.setPagination(pagination);

			SortOrder orderType = SortOrder.valueOf(pquery.getOrder().toUpperCase());
			builder.orderBy(pquery.getOrderBy(), orderType);
			builder.limit(pagination.getPageStartRow(), pquery.getPageSize());
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : {} / PARAMS : {}", builder.build());
		}
		
		return rows(builder.toString(), SysOptionDAO.INFO_MAPPER);

	}

	@JdbiTran
	public boolean updateOption(ServiceContext svcctx, String optKey, String value) {

		SysOptionInfo opt = null;

		SelectBuilder builder = SqlBuilder.select(BaseIdKey.SYS_OPTION.schema());
		builder.where("opt_key = ?");

		List<Object> params = Arrays.asList(optKey);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL: {} / PARAMS: {}", builder.toString(), params);
		}
		opt = row(builder.toString(), SysOptionDAO.INFO_MAPPER, params);
		opt.setOptValue(value);
		svcctx.setTraceInfo(opt);
		opt.setFilter(FilterMode.include("opt_value"));
		return sysoptiondao.update(opt) > 0;
		
	}

	@JdbiTran
	public boolean updateOption(ServiceContext svcctx, InfoId optKey, String value) {

		SysOptionInfo opt = sysoptiondao.row(optKey);
		opt.setOptValue(value);
		svcctx.setTraceInfo(opt);
		
		return sysoptiondao.update(opt) > 0;

	}

	@JdbiTran(readOnly = true)
	public SysOptionInfo getOption(String optKey) {
		SelectBuilder builder = SqlBuilder.select(BaseIdKey.SYS_OPTION.schema());
		builder.where("opt_key = ?");
		List<Object> params = Arrays.asList(optKey);
	
		return row(builder.toString(), SysOptionDAO.INFO_MAPPER, params);

	}

	@JdbiTran(readOnly = true)
	public SysOptionInfo getOption(InfoId oKey) {

		return sysoptiondao.row(oKey);

	}

	@JdbiTran(readOnly = true)
	public List<String> getOptionGroups() {

		StringBuffer SQL = new StringBuffer("select distinct opt_group from gp_sys_option");
		
		return rows(SQL.toString(), (rs, index) -> {
			return rs.getString("opt_group");
		});

	}

	/**
	 * The reentrant lock
	 **/
	private Lock codeLock = new ReentrantLock();

	@JdbiTran
	public int getPrecedence(String seqKey) {

		try {
			codeLock.lock();
			SelectBuilder builder = SqlBuilder.select(BaseIdKey.SEQ_NO.schema());
			builder.where("seq_key = '" + seqKey + "'");
			
			SeqNoInfo seqInfo = row(builder.build(), SeqNoDAO.INFO_MAPPER);

			int val = seqInfo.getCurrVal() + seqInfo.getStepIntvl();

			int cnt = update(seqInfo.getInfoId(), "curr_val", val);
			if (cnt > 0) {

				return val;
			} else {
				return -1;
			}
		} finally {
			codeLock.unlock();
		}
	}

	@JdbiTran(readOnly = true)
	public String getLocalNodeGid() {

		InfoId srcId = Sources.LOCAL_INST_ID;
		String nodeGid = column(srcId, "node_gid", String.class);
		return nodeGid;
	}

	@JdbiTran(readOnly = true)
	public String getLocalCenterGid() {

		SelectBuilder builder = SqlBuilder.select(BaseIdKey.SYS_OPTION.schema());
		builder.where("opt_key = ?");
		List<Object> params = Arrays.asList("center.gid");

		KVPair<String, String> pholder = KVPair.newPair("center.gid");
		query(builder.build(), (rs) -> {
			pholder.setValue(rs.getString("opt_value"));
		}, params);

		return pholder.getValue();
	}

	@JdbiTran(readOnly = true)
	public String getLocalEntityGid() {

		InfoId srcId = Sources.LOCAL_INST_ID;
		String centerGid = column(srcId, "entity_gid", String.class);

		return centerGid;
	}
}
