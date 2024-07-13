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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.gp.bind.BindAutowired;
import com.gp.bind.BindComponent;
import com.gp.common.IdKeys;
import com.gp.common.InfoId;
import com.gp.common.MasterIdKey;
import com.gp.common.ServiceContext;
import com.gp.dao.GroupDAO;
import com.gp.dao.GroupUserDAO;
import com.gp.dao.info.GroupInfo;
import com.gp.dao.info.GroupUserInfo;
import com.gp.db.JdbiTran;
import com.gp.paging.PageQuery;
import com.gp.paging.Pagination;
import com.gp.paging.Paginator;
import com.gp.sql.BaseBuilder.SortOrder;
import com.gp.sql.SqlBuilder;
import com.gp.sql.delete.DeleteBuilder;
import com.gp.sql.select.SelectBuilder;
import com.gp.svc.BaseService;
import com.gp.svc.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@BindComponent(priority = BaseService.BASE_PRIORITY)
public class GroupService extends ServiceSupport implements BaseService {

	Logger LOGGER = LoggerFactory.getLogger(GroupService.class);

	@BindAutowired
	GroupDAO groupdao;

	@BindAutowired
	GroupUserDAO groupuserdao;
	
	@JdbiTran(readOnly = true)
	public GroupInfo getGroup(InfoId groupId) {

		return groupdao.row(groupId);

	}

	@JdbiTran(readOnly = true)
	public List<GroupInfo> getGroups(String groupName, String groupType, PageQuery pquery) {
		SelectBuilder builder = SqlBuilder.select(MasterIdKey.GROUP.schema());
		List<Object> params = Lists.newArrayList();

		if (!Strings.isNullOrEmpty(groupType)) {
			builder.and("group_type = ?");
			params.add(groupType);
		}
		if (!Strings.isNullOrEmpty(groupName)) {
			builder.and("group_name like '" + groupName + "%'");
		}

		if (Objects.nonNull(pquery)) {
			SelectBuilder countBuilder = builder.clone();
			countBuilder.column().column("count(" + MasterIdKey.GROUP.idColumn() + ")");
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
			LOGGER.debug("SQL : {} / params : {}", builder.build(), params);
		}
		
		return rows(builder.build(), GroupDAO.INFO_MAPPER, params);

	}

	@JdbiTran
	public boolean addGroup(ServiceContext svcctx, GroupInfo group) {

		svcctx.setTraceInfo(group);
		return groupdao.create(group) > 0;

	}

	@JdbiTran
	public boolean removeGroup(ServiceContext svcctx, InfoId groupId) {

		DeleteBuilder builder = SqlBuilder.delete(MasterIdKey.GROUP_USER.schema());
		builder.where("group_id = ?");
		
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : {} / PARAMS : {} ", builder.toString(), groupId);
		}
		update(builder.build(), groupId.getId());

		return groupdao.delete(groupId) > 0;

	}

	@JdbiTran
	public boolean updateGroup(ServiceContext svcctx, GroupInfo group) {

		svcctx.setTraceInfo(group);
		return groupdao.update(group) > 0;

	}

	@JdbiTran
	public boolean[] addGroupMember(ServiceContext svcctx, InfoId groupId, Long... members) {
		boolean[] cnts = new boolean[members.length];
		SelectBuilder check = SqlBuilder.select(MasterIdKey.GROUP_USER.schema());
		check.column("count(rel_id)");
		check.where("group_id = ?");
		check.and("member_uid = ?");

		for (int i = 0; i < members.length; i++) {

			Long member = members[i];
			List<Object> params = Arrays.asList(groupId.getId(), member);
			Integer exist = row(check.build(), Integer.class, params);
			if (exist > 0) {
				cnts[i] = false;
				continue;
			}
			GroupUserInfo guser = null;

			InfoId guid = IdKeys.newInfoId(MasterIdKey.GROUP_USER);

			guser = new GroupUserInfo();
			guser.setInfoId(guid);
			guser.setGroupId(groupId.getId());
			guser.setMemberUid(member);
			svcctx.setTraceInfo(guser);
			int cnt = groupuserdao.create(guser);
			cnts[i] = cnt > 0;

		}

		return cnts;
	}

	@JdbiTran
	public boolean[] removeGroupMember(ServiceContext svcctx, InfoId groupId, Long... members) {
		DeleteBuilder builder = SqlBuilder.delete(MasterIdKey.GROUP_USER.schema());
		builder.where("group_id = :gid");
		builder.and("member_uid in ( <accts> )");

		Map<String, Object> paraMap = Maps.newHashMap();
		paraMap.put("gid", groupId.getId());
		paraMap.put("accts", Lists.newArrayList(members));

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : {} / PARAMS : {} ", builder.toString(), paraMap);
		}

		update(builder.build(), paraMap);
		SelectBuilder select = SqlBuilder.select(MasterIdKey.GROUP_USER.schema());
		select.column("member_uid");
		select.where("group_id = :gid");
		select.and("member_uid in ( <accts> )");
		Set<Long> accts = Sets.newHashSet();
		query(select.build(), (rs) -> {
			accts.add(rs.getLong("member_uid"));
		}, paraMap);
		boolean[] cnts = new boolean[members.length];
		for (int i = 0; i < members.length; i++) {
			cnts[i] = !accts.contains(members[i]);
		}
		return cnts;

	}

	@JdbiTran(readOnly = true)
	public List<GroupUserInfo> getGroupMembers(InfoId groupId, String memberName, PageQuery pquery) {
		
		Map<String, Object> paramMap = Maps.newHashMap();

		SelectBuilder builder = SqlBuilder.select();
		builder.column("a.*");
		
		builder.column("b.username", "b.email", "b.full_name");
		builder.column("b.mobile", "b.category", "b.create_time", " b.state");
		builder.column("b.avatar_url");
		
		builder.column("s.source_id", " s.source_name", "s.abbr", "s.node_gid");
		builder.column("s.short_name", "s.entity_gid");
		
		builder.from((from) -> {
			from.table("gp_group_user a");
			from.leftJoin("gp_user b", "a.member_uid = b.user_id");
			from.leftJoin("gp_source s", "b.source_id = s.source_id");
		});

		builder.where("a.group_id = :gid");
		paramMap.put("gid", groupId.getId());
		if (!Strings.isNullOrEmpty(memberName)) {
			builder.and((cond) -> {
				cond.or("b.full_name like :name");
				cond.or("b.username like :name");
			});
			paramMap.put("name", memberName + "%");
		}
		builder.orderBy("b.full_name");

		if (Objects.nonNull(pquery)) {
			SelectBuilder countBuilder = builder.clone();
			countBuilder.column().column("count( b.user_id )");
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("SQL : {} / PARAMS : {}", countBuilder.build(), paramMap);
			}
			Integer total = row(countBuilder.toString(), Integer.class, paramMap);
			Paginator paginator = new Paginator(total, pquery);
			Pagination pagination = paginator.getPagination();
			pquery.setPagination(pagination);

			SortOrder orderType = SortOrder.valueOf(pquery.getOrder().toUpperCase());
			builder.orderBy(pquery.getOrderBy(), orderType);
			builder.limit(pagination.getPageStartRow(), pquery.getPageSize());
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : {} / PARAMS : {} ", builder.toString(), paramMap);
		}
		
		Set<String> strFlds = Sets.newHashSet("username", "email", "full_name", "mobile", "category", 
				"create_time", "state", "avatar_url", 
				"source_name", "abbr", "node_gid",
				"short_name", "entity_gid");
		
		return rows(builder.toString(), (rs, idx) -> {
			GroupUserInfo info = GroupUserDAO.INFO_MAPPER.map(rs, idx);
			
			for(String fld : strFlds) {
				info.setProperty(fld, rs.getString(fld));
			}
			info.setProperty("source_id", rs.getLong("source_id"));
			
			return info;
		}, paramMap);

	}

}
