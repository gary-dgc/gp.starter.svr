/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.svc;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.gp.bind.BindAutowired;
import com.gp.bind.BindComponent;
import com.gp.common.GeneralConfig;
import com.gp.common.InfoId;
import com.gp.common.ServiceContext;
import com.gp.dao.AuditDAO;
import com.gp.dao.info.AuditInfo;
import com.gp.db.JdbiTran;
import com.gp.info.BaseIdKey;
import com.gp.paging.PageQuery;
import com.gp.paging.Pagination;
import com.gp.paging.Paginator;
import com.gp.sql.BaseBuilder.SortOrder;
import com.gp.sql.SqlBuilder;
import com.gp.sql.delete.DeleteBuilder;
import com.gp.sql.select.SelectBuilder;
import com.gp.util.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


@BindComponent(priority = BaseService.BASE_PRIORITY)
public class AuditService extends ServiceSupport implements BaseService {

	Logger LOGGER = LoggerFactory.getLogger(AuditService.class);

	@BindAutowired
	AuditDAO auditdao;

	@JdbiTran(readOnly = true)
	public List<AuditInfo> getAudits(String app, String subject, String object, String operation,
									 Date auditFrom, Date auditTo, PageQuery pquery) {

		SelectBuilder builder = auditdao.selectSql();
		builder.all();

		Map<String, Object> params = Maps.newHashMap();
		if (!Strings.isNullOrEmpty(subject)) {
			builder.and("app LIKE :app ");
			params.put("app", "%" + app + "%");
		}

		if (!Strings.isNullOrEmpty(subject)) {
			builder.and("subject LIKE :sub ");
			params.put("sub", "%" + subject + "%");
		}

		if (!Strings.isNullOrEmpty(object)) {
			builder.and("object_id like :obj ");
			params.put("obj", "%" + object + "%");
		}

		if (!Strings.isNullOrEmpty(operation)) {
			builder.and("operation like :op ");
			params.put("op", "%" + operation + "%");
		}

		if(auditFrom != null) {
			builder.and("audit_time >= :from");
			params.put("from", auditFrom);
		}
		if(auditTo != null) {
			builder.and("audit_time < :to");
			params.put("to", auditTo);
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : {} / PARAMS : {}", builder.toString(), params);
		}

		if (Objects.nonNull(pquery)) {
			SelectBuilder countBuilder = builder.clone();
			countBuilder.column().column("count(" + BaseIdKey.AUDIT.idColumn() + ")");
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

		return rows(builder.toString(), AuditDAO.INFO_MAPPER, params);

	}

	@JdbiTran
	public boolean deleteAudit(ServiceContext svcctx, InfoId id) {

		return auditdao.delete(id) > 0;

	}

	@JdbiTran
	public boolean addAudit(AuditInfo ainfo) {
		// set instance id
		ainfo.setInstanceId(NumberUtils.toInt(GeneralConfig.getStringByKeys("system", "instance")));
		return auditdao.create(ainfo) > 0;

	}

	@JdbiTran
	public boolean purgeAudits(String subject, String objectType, Date reservedate) {

		List<Object> parmlist = new ArrayList<Object>();
		DeleteBuilder builder = SqlBuilder.delete();
		builder.from(BaseIdKey.AUDIT.schema());

		if (!Strings.isNullOrEmpty(subject)) {

			builder.where("subject = ?");
			parmlist.add(subject);
		}

		if (!Strings.isNullOrEmpty(objectType)) {
			builder.where("object like ?");
			parmlist.add(objectType);
		}

		if (null != reservedate) {
			builder.where("audit_time < ?");
			parmlist.add(reservedate);
		}

		if (LOGGER.isDebugEnabled()) {

			LOGGER.debug("SQL : {} / params : {}", builder.toString(), parmlist);
		}

		return update(builder.toString(), parmlist) > 0;

	}

}

