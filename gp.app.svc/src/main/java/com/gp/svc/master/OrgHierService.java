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
import com.google.common.collect.Sets;
import com.gp.bind.BindAutowired;
import com.gp.bind.BindComponent;
import com.gp.common.*;
import com.gp.dao.*;
import com.gp.dao.ext.GroupExt;
import com.gp.dao.info.DeptHierInfo;
import com.gp.dao.info.GroupInfo;
import com.gp.dao.info.GroupUserInfo;
import com.gp.dao.info.OrgHierInfo;
import com.gp.db.JdbiTran;
import com.gp.info.BaseIdKey;
import com.gp.info.Principal;
import com.gp.paging.PageQuery;
import com.gp.paging.Pagination;
import com.gp.paging.Paginator;
import com.gp.sql.BaseBuilder.SortOrder;
import com.gp.sql.SqlBuilder;
import com.gp.sql.delete.DeleteBuilder;
import com.gp.sql.select.SelectBuilder;
import com.gp.svc.BaseService;
import com.gp.svc.ServiceSupport;
import com.gp.svc.SystemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@BindComponent(priority = BaseService.BASE_PRIORITY)
public class OrgHierService extends ServiceSupport implements BaseService {

	Logger LOGGER = LoggerFactory.getLogger(OrgHierService.class);

	@BindAutowired
	private OrgHierDAO orghierdao;

	@BindAutowired
	private GroupDAO groupdao;

	@BindAutowired
	private GroupUserDAO orguserdao;

	@BindAutowired
	private DeptHierDAO deptHierDao;
	
	@BindAutowired
	private SystemService systemService;
	
	@JdbiTran(readOnly = true)
	public List<OrgHierInfo> getOrgHierNodes(String orgName, InfoId parentNodeId) {

		List<OrgHierInfo> orglist = null;
		SelectBuilder builder = SqlBuilder.select(MasterIdKey.ORG_HIER.schema());
		Map<String, Object> params = Maps.newHashMap();
		if (!Strings.isNullOrEmpty(orgName)) {
			builder.and("org_name like :name");
			params.put("name", orgName + "%");
		}
		if (IdKeys.isValidId(parentNodeId)) {
			builder.and("org_pid = :pid");
			params.put("pid", parentNodeId.getId());
		}
		
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL: {} / PARAM: {}", builder.toString(), params);
		}

		orglist = rows(builder.toString(), OrgHierDAO.INFO_MAPPER, params);

		return orglist;
	}

	@JdbiTran
	public boolean newOrgHierNode(ServiceContext svcctx, OrgHierInfo orginfo) {

		svcctx.setTraceInfo(orginfo);
				
		InfoId orgId = IdKeys.newInfoId(MasterIdKey.ORG_HIER);
		orginfo.setInfoId(orgId);
				
		InfoId grpId = IdKeys.newInfoId(MasterIdKey.GROUP);
		InfoId deptId = IdKeys.newInfoId(MasterIdKey.DEPT_HIER);
		
		// create default dept record with parent id: 99
		DeptHierInfo dept = new DeptHierInfo();
		dept.setInfoId(deptId);
		dept.setDeptName(orginfo.getOrgName() +"'s root dept.");
		dept.setDeptPid(GeneralConsts.HIER_ROOT);
		dept.setOrgId(orgId.getId());

		svcctx.setTraceInfo(dept);		
		deptHierDao.create(dept);
		
		GroupInfo group = new GroupInfo();
		group.setInfoId(grpId);
		group.setGroupName(orginfo.getOrgName() +"'s user group");
		group.setGroupType(MasterIdKey.DEPT_HIER.name());
		group.setManageId(deptId.getId());
		
		svcctx.setTraceInfo(group);
		groupdao.create(group);
		
		return orghierdao.create(orginfo) > 0;

	}

	@JdbiTran
	public boolean saveOrgHierNode(ServiceContext svcctx, OrgHierInfo orginfo) {

		svcctx.setTraceInfo(orginfo);
		return orghierdao.update(orginfo) > 0;

	}

	@JdbiTran(readOnly = true)
	public OrgHierInfo getOrgHierNode(InfoId orgid) {

		return orghierdao.row(orgid);

	}

	@JdbiTran
	public boolean removeOrgHierNode(InfoId orgid) {

		// delete group  user
		DeleteBuilder builder = SqlBuilder.delete(MasterIdKey.GROUP_USER.schema());
		builder.where("group_id IN ("
				+ "select g.group_id "
				+ "from gp_dept_hier d, gp_group g "
				+ "where d.dept_id = g.manage_id and d.org_id = ?"
				+ ")");

		List<Object> params = Arrays.asList(orgid.getId());
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : " + builder.toString() + " / params : " + params);
		}
		update(builder.toString(), params);

		// delete group under organization
		builder = SqlBuilder.delete(MasterIdKey.GROUP.schema());
		builder.where("manage_id IN ("
					+ "select dept_id "
					+ "from gp_dept_hier "
					+ "where org_id = ?"
					+ ")");
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : " + builder.toString() + " / params : " + params);
		}
		update(builder.toString(), params);
		
		// delete deptartment under organization
		builder = SqlBuilder.delete(MasterIdKey.DEPT_HIER.schema());
			builder.where("org_id = ?");
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : " + builder.toString() + " / params : " + params);
		}		
		update(builder.toString(), params);
		
		return orghierdao.delete(orgid) > 0;

	}

	@JdbiTran
	public void addOrgHierMember(ServiceContext svcctx, InfoId orgId, Long... members) {

		// find root member group id
		Long rootGroupId = null;
		List<DeptHierInfo> infos = deptHierDao.query(cond -> {
			cond.and("org_id = " + orgId.getId());
			cond.and("dept_pid = " + GeneralConsts.HIER_ROOT);
		});
		
		if(infos.size()> 0) {
			rootGroupId = groupdao.column("group_id", Long.class, KVPair.newPair("manage_id", infos.get(0).getId()));
		}
		
		// check user exists in groups under organization
		SelectBuilder builder = SqlBuilder.select(MasterIdKey.GROUP_USER.schema());
		builder.where("group_id IN ("
				+ "select g.group_id "
				+ "from gp_dept_hier d, gp_group g "
				+ "where d.dept_id = g.manage_id and d.org_id = ?"
				+ ")");
		builder.and("member_uid = ?");
		
		for (Long member : members) {
			
			List<Object> params = Arrays.asList(orgId, member);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("SQL: {} / PARAMS: {}", builder.toString(),params);
			}
			InfoId mbrid = IdKeys.getInfoId(MasterIdKey.GROUP_USER, -1l);
			query(builder.toString(), (rs) -> {
				mbrid.setId(rs.getLong("rel_id"));
			}, params);

			// Member already exists then ignore
			if (IdKeys.isValidId(mbrid)) {
				continue;
			}
			GroupUserInfo guinfo = new GroupUserInfo();
			InfoId rid = IdKeys.newInfoId(MasterIdKey.GROUP_USER);
			guinfo.setInfoId(rid);
			guinfo.setGroupId(rootGroupId);
			guinfo.setType(GroupUsers.MemberType.USER.name());
			guinfo.setRole(GroupUsers.MemberRole.MEMBER.name());
			guinfo.setMemberUid(member);
			svcctx.setTraceInfo(guinfo);

			orguserdao.create(guinfo);

		}

	}

	@JdbiTran
	public void removeOrgHierMember(ServiceContext svcctx, InfoId orgId, Long... members) {

		for (Long member : members) {
			
			DeleteBuilder builder = SqlBuilder.delete(MasterIdKey.GROUP_USER.schema());
			builder.where("group_id IN ("
					+ "select g.group_id "
					+ "from gp_dept_hier d, gp_group g "
					+ "where d.dept_id = g.manage_id and d.org_id = ?"
					+ ")");
			builder.and("member_uid = ?");

			List<Object> params = Arrays.asList( orgId, member );
			if (LOGGER.isDebugEnabled()) {

				LOGGER.debug("SQL : " + builder.toString() + " / params : " + params);
			}
			update(builder.toString(), params);
		}

	}

	@JdbiTran
	public List<GroupUserInfo> getOrgHierMembers(ServiceContext svcctx, String keyword, InfoId orgid, Collection<String> features, PageQuery pquery) {

		List<GroupUserInfo> rtv = null;

		SelectBuilder builder = SqlBuilder.select();
		builder.column("a.*");
		builder.column("b.username", "b.email", "b.full_name", "b.avatar_url");
		builder.column("b.mobile", "b.category", "b.create_time", " b.state");
		builder.column("s.source_id", "s.source_name", "s.abbr", "s.node_gid");

		builder.from((from) -> {
			from.table(MasterIdKey.GROUP_USER.schema("a"));
			from.join(MasterIdKey.GROUP.schema("g"), "g.group_id = a.group_id");
			from.leftJoin(BaseIdKey.USER.schema("b"), "a.member_uid = b.user_id");
			from.leftJoin(BaseIdKey.SOURCE.schema("s"), "b.source_id = s.source_id");
		});

		builder.orderBy("b.full_name");

		Map<String, Object> params = Maps.newHashMap();
		Principal princ = svcctx.getPrincipal();		
		params.put("op_uid", princ.getUserId().getId());
		
		if( null != features && features.contains("DIRECT")){
			
			builder.and("g.manage_id IN (select dept_id from gp_dept_hier where org_id = :org_id and dept_pid = 99)");
			params.put("org_id", orgid.getId());
		} else {
			
			builder.and("g.manage_id IN (select dept_id from gp_dept_hier where org_id = :org_id)");
			params.put("org_id", orgid.getId());
		}

		if (!Strings.isNullOrEmpty(keyword)) {
			builder.and( cond -> {
				cond.or("b.full_name LIKE :keyword");
				cond.or("b.username LIKE :keyword");
			});
			params.put("keyword", keyword + "%");
		}
		
		if (Objects.nonNull(pquery)) {
			SelectBuilder countBuilder = builder.clone();
			countBuilder.column().column("count(" + MasterIdKey.GROUP_USER.idColumn() + ")");
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
			LOGGER.debug("SQL : " + builder.toString() + " / PARAMS : " + params);
		}

		Set<String> fields = Sets.newHashSet("username", "email", "full_name", "avatar_url",
				"mobile", "category", "state", "source_name", "abbr", "node_gid");
		
		rtv = rows(builder.toString(), (rs, idx) -> {
			
			GroupUserInfo info = GroupUserDAO.INFO_MAPPER.map(rs, idx);
			
			DAOSupport.setInfoProperty(rs, info, String.class, fields.toArray(new String[0]));
			
			info.setProperty("create_time", rs.getTimestamp("create_time").getTime());
			info.setProperty("source_id", rs.getLong("source_id"));

			return info;
		}, params);

		return rtv;

	}

	@JdbiTran(readOnly = true)
	public List<OrgHierInfo> getOrgHierNodes(InfoId... orgids) {

		List<Long> oids = new ArrayList<Long>();
		Map<String, Object> params = new HashMap<String, Object>();
		SelectBuilder builder = SqlBuilder.select(MasterIdKey.ORG_HIER.schema());
		if (orgids != null && orgids.length > 0) {

			for (InfoId id : orgids) {
				oids.add(id.getId());
			}
			builder.where("org_id IN ( <org_ids> ) ");
			params.put("org_ids", oids);
		}

		builder.orderBy("org_id", SortOrder.ASC);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : " + builder.toString() + " / params : " + oids.toString());
		}

		return rows(builder.toString(), OrgHierDAO.INFO_MAPPER, params);

	}

	@JdbiTran(readOnly = true)
	public List<OrgHierInfo> getOrgHierAllNodes(InfoId orgNodeId) {

		List<OrgHierInfo> all = new ArrayList<OrgHierInfo>();
		List<InfoId> ids = new ArrayList<InfoId>();

		SelectBuilder builder = SqlBuilder.select(MasterIdKey.ORG_HIER.schema());
		builder.where("org_pid = ?");
		builder.orderBy("org_id", SortOrder.ASC);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : " + builder.toString() + " / params : " + orgNodeId.toString());
		}

		List<OrgHierInfo> lvl1 = rows(builder.toString(),
				OrgHierDAO.INFO_MAPPER, orgNodeId.getId());
		all.addAll(lvl1);
		for (OrgHierInfo oinfo : lvl1) {
			ids.add(oinfo.getInfoId());
		}
		List<OrgHierInfo> subs = getSubNodes(ids.toArray(new InfoId[0]));
		if (!subs.isEmpty())
			all.addAll(subs);
		return all;

	}

	@JdbiTran(readOnly = true)
	private List<OrgHierInfo> getSubNodes(InfoId... orgNodeIds) {

		List<OrgHierInfo> all = new ArrayList<OrgHierInfo>();
		List<InfoId> ids = new ArrayList<InfoId>();

		List<Long> oids = new ArrayList<Long>();
		Map<String, Object> params = new HashMap<String, Object>();
		SelectBuilder builder = SqlBuilder.select(MasterIdKey.ORG_HIER.schema());
		if (orgNodeIds != null && orgNodeIds.length > 0) {

			for (InfoId id : orgNodeIds) {
				oids.add(id.getId());
			}
			builder.where("org_pid IN ( <org_ids> ) ");
			params.put("org_ids", oids);
		}
		builder.orderBy("org_id", SortOrder.ASC);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : " + builder.toString() + " / params : " + oids.toString());
		}

		List<OrgHierInfo> lvl1 = rows(builder.toString(), OrgHierDAO.INFO_MAPPER, params);

		all.addAll(lvl1);
		for (OrgHierInfo oinfo : lvl1) {
			ids.add(oinfo.getInfoId());
		}
		if (!ids.isEmpty()) {
			List<OrgHierInfo> subs = getSubNodes(ids.toArray(new InfoId[0]));
			if (!subs.isEmpty())
				all.addAll(subs);
		}
		return all;

	}

	@JdbiTran(readOnly = true)
	public List<OrgHierInfo> getOrgHierChildNodes(boolean countGrandNode, InfoId... pids) {

		List<Long> oids = new ArrayList<Long>();
		for (InfoId id : pids) {
			oids.add(id.getId());
		}

		final Map<Long, Integer> countMap = new HashMap<Long, Integer>();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("org_ids", oids);

		SelectBuilder builder = SqlBuilder.select(MasterIdKey.ORG_HIER.schema());
		builder.column("count(org_id) as grand_cnt", "org_pid");

		builder.where("org_pid in (select org_id from gp_org_hier where org_pid IN ( <org_ids> ) )");
		builder.groupBy("org_pid");
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : " + builder.toString() + " / params : " + oids.toString());
		}
		query(builder.toString(), (rs) -> {

			countMap.put(rs.getLong("org_pid"), rs.getInt("grand_cnt"));
		}, params);

		builder = SqlBuilder.select(MasterIdKey.ORG_HIER.schema());
		builder.where("org_pid IN ( <org_ids> ) ");
		builder.and("(del_flag IS NULL OR del_flag = 0)");
		builder.orderBy("org_id", SortOrder.ASC);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : " + builder.toString() + " / params : " + oids.toString());
		}
		
		return rows(builder.toString(), (rs, i) -> {

			OrgHierInfo info = OrgHierDAO.INFO_MAPPER.map(rs, i);

			info.setProperty("child_count", countMap.containsKey(info.getId()) ? countMap.get(info.getId()) : 0);

			return info;
		}, params);

	}

	/**
	 * Get organization information by node global id
	 **/
	@JdbiTran(readOnly = true)
	public OrgHierInfo getOrgHierRoot() {
	
		String nodeGid = systemService.getLocalNodeGid();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("node_gid", nodeGid);
		
		SelectBuilder builder = SqlBuilder.select(MasterIdKey.ORG_HIER.schema());
		builder.where("node_gid = :node_gid ");
		builder.and("(del_flag IS NULL OR del_flag = 0)");
		
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : " + builder.toString() + " / PARAMS : " + params);
		}
		
		return row(builder.toString(), OrgHierDAO.INFO_MAPPER, params);
	}
}
