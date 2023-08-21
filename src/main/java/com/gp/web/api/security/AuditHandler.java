/*******************************************************************************
 * This project is released under the Apache License, version 2.0.
 * This is a commercial solution, requiring a valid paid-for license for 
 * commercial use. This product is free to use for non-commercial applications, 
 * like non-profits and educational usage.
 * Copyright (c) 2016 to Present
 * All rights reserved to Gary Diao(gary.diao@yahoo.com)
 ******************************************************************************/
package com.gp.web.api.security;

import com.google.common.base.Strings;
import com.gp.bind.BindScanner;
import com.gp.common.Filters;
import com.gp.dao.info.AuditInfo;
import com.gp.exception.BaseException;
import com.gp.info.DataBuilder;
import com.gp.paging.PageQuery;
import com.gp.sql.BaseBuilder.SortOrder;
import com.gp.svc.AuditService;
import com.gp.web.ActionResult;
import com.gp.web.BaseApiSupport;
import com.gp.web.anno.WebApi;
import io.undertow.server.HttpServerExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AuditHandler extends BaseApiSupport{

	static Logger LOGGER = LoggerFactory.getLogger(AuditHandler.class);
	
	private AuditService auditService = null;
	
	static SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy/MM/dd");  
	
	public AuditHandler() {
		auditService = BindScanner.instance().getBean(AuditService.class);
		
	}

	@WebApi(path="audits-query", auditable = false, traceable = false)
	public void handleAnnosQuery(HttpServerExchange exchange) throws BaseException {
		
		ActionResult result = null;
		Map<String, Object> params = this.getRequestBody(exchange);
		
		PageQuery pquery = Filters.filterPageQuery(params);
		String app = Filters.filterString(params, "app");
		String subject = Filters.filterString(params, "subject");
		String operation = Filters.filterString(params, "operation");
		String object = Filters.filterString(params, "object");
		String dtFrom = Filters.filterString(params, "date_from");
		String dtTo = Filters.filterString(params, "date_to");
		
		Date auditFrom = null;
		Date auditTo = null;
		try {
			auditFrom = Strings.isNullOrEmpty(dtFrom)? null : DATE_FMT.parse(dtFrom);
			auditTo = Strings.isNullOrEmpty(dtTo)? null : DATE_FMT.parse(dtTo);
		} catch (ParseException e) {
			
			result = ActionResult.failure("excp.illegal.param");
			this.sendResult(exchange, result);
		}
		
		if(null != pquery) {
			pquery.setOrderBy("audit_id");
			pquery.setOrder(SortOrder.DESC.name());
		}
		List<AuditInfo> infos = auditService.getAudits(app, subject, object, operation, auditFrom, auditTo, pquery);
		
		List<Object> data = infos.stream().map((info) -> {
			
			DataBuilder builder = new DataBuilder();
			builder.set("audit_id", info.getId().toString());
			
			builder.set(info, "client", "host", "app", "path", "version");
			builder.set(info, "device", "subject", "operation", "object_id", "predicates", "state");
			
			builder.set("audit_time", info.getAuditTime().getTime());
			return builder.build();
			
		}).collect(Collectors.toList());
		
		result = ActionResult.success("mesg.find.audits");
		result.setData(pquery == null ? null : pquery.getPagination(), data);
		
		this.sendResult(exchange, result);
	}

}
