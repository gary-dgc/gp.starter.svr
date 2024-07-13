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
import com.google.common.collect.Maps;
import com.gp.bind.BindAutowired;
import com.gp.bind.BindComponent;
import com.gp.common.InfoId;
import com.gp.common.MasterIdKey;
import com.gp.common.ServiceContext;
import com.gp.dao.EndpointDAO;
import com.gp.dao.info.EndpointInfo;
import com.gp.db.JdbiTran;
import com.gp.paging.PageQuery;
import com.gp.paging.Pagination;
import com.gp.paging.Paginator;
import com.gp.sql.BaseBuilder.SortOrder;
import com.gp.sql.SqlBuilder;
import com.gp.sql.select.SelectBuilder;
import com.gp.svc.BaseService;
import com.gp.svc.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@BindComponent(priority = BaseService.BASE_PRIORITY)
public class EndpointService extends ServiceSupport implements BaseService {

	public static Logger LOGGER = LoggerFactory.getLogger(EndpointService.class);

	@BindAutowired
	EndpointDAO endpointDao;
	
	/**
	 * Get endpoints by name and module
	 *  
	 **/
	@JdbiTran(readOnly = true)
	public List<EndpointInfo> getEndpoints(String module, String name, PageQuery pquery) {
		
		SelectBuilder builder = SqlBuilder.select(MasterIdKey.ENDPOINT.schema());
		builder.all();
		
		Map<String, Object> paramap = Maps.newHashMap();
		
		if(!Strings.isNullOrEmpty(module)) {
			builder.and("module = :module");
			paramap.put("module", module);
		}
		
		if(!Strings.isNullOrEmpty(name)) {
			builder.and( cond -> {
				cond.or("endpoint_name LIKE :name");
				cond.or("endpoint_abbr LIKE :name");
			});
			
			paramap.put("name", "%" + name + "%");
		}
		
		if (Objects.nonNull(pquery)) {
			
			SelectBuilder countBuilder = builder.clone();
			countBuilder.column().column("count(" + MasterIdKey.ENDPOINT.idColumn() + ")");
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
		
		return rows(builder.build(), EndpointDAO.INFO_MAPPER, paramap);
	}

	@JdbiTran
	public boolean addEndpoint(ServiceContext svcctx, EndpointInfo info) {
		
		svcctx.setTraceInfo(info);
		
		return endpointDao.create(info) > 0;
	}
	
	@JdbiTran
	public boolean updateEndpoint(ServiceContext svcctx, EndpointInfo info) {
		
		svcctx.setTraceInfo(info);

		return endpointDao.update(info) > 0;
	}
	
	@JdbiTran
	public boolean removeEndpoint(InfoId epKey) {
		
		return endpointDao.delete(epKey, true) > 0;
	}
}
