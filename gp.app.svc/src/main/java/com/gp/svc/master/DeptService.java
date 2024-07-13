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
import com.google.common.collect.Iterables;
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
import com.gp.db.BaseHandle;
import com.gp.db.JdbiTran;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@BindComponent(priority = BaseService.BASE_PRIORITY)
public class DeptService extends ServiceSupport implements BaseService {

	Logger LOGGER = LoggerFactory.getLogger(OrgHierService.class);

	@BindAutowired
	private DeptHierDAO deptHierDao;
	
	@BindAutowired
	private DeptFlatDAO deptFlatDao;
	
	@BindAutowired
	private GroupDAO groupDao;

	@BindAutowired
	private GroupExt groupExt;

	@BindAutowired
	private GroupUserDAO groupUserDao;
	
	@JdbiTran(readOnly = true)
	public List<DeptHierInfo> getDeptHierNodes(String deptName, InfoId parentNodeId) {

		List<DeptHierInfo> orglist = null;
		SelectBuilder builder = SqlBuilder.select(MasterIdKey.DEPT_HIER.schema());
		Map<String, Object> params = Maps.newHashMap();
		if (!Strings.isNullOrEmpty(deptName)) {
			builder.and("dept_name LIKE :name");
			params.put("name", deptName + "%");
		}
		if (IdKeys.isValidId(parentNodeId)) {
			builder.and("dept_pid = :pid");
			params.put("pid", parentNodeId.getId());
		}
		
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL: {} / PARAM: {}", builder.toString(), params);
		}

		orglist = rows(builder.toString(), DeptHierDAO.INFO_MAPPER, params);

		return orglist;
	}
	
	@JdbiTran(readOnly = true)
	public DeptHierInfo getDeptHierNode(InfoId deptId) {

		return  deptHierDao.row(deptId);
	
	}
	
	@JdbiTran(readOnly = true)
	public DeptHierInfo getDeptHierRoot(InfoId orgId) {

		List<DeptHierInfo> infos = deptHierDao.query(cond -> {
			if(IdKeys.isValidId(orgId)) {
				cond.and("org_id = " + orgId.getId());
			}
			cond.and("dept_pid = " + GeneralConsts.HIER_ROOT);
			
		});
		
		return Iterables.getFirst(infos, null);

	}
	
	@JdbiTran(readOnly = true)
	public List<DeptHierInfo> getDeptHierChildNodes(boolean countGrandNode, InfoId orgid, InfoId... pids) {

		List<Long> oids = new ArrayList<Long>();
		if(null != pids && pids.length > 0) {
			for (InfoId id : pids) {
				if(!IdKeys.isValidId(id)) continue;
				oids.add(id.getId());
			}
		}
		if(oids.size() == 0) {
			oids.add(GeneralConsts.HIER_ROOT);
		}
		final Map<Long, Integer> countMap = new HashMap<Long, Integer>();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("dept_ids", oids);

		// count the sub nodes of parent node
		SelectBuilder builder = SqlBuilder.select(MasterIdKey.DEPT_HIER.schema());
		builder.column("count(dept_id) as grand_cnt", "dept_pid");
		builder.where("dept_pid in (SELECT dept_id FROM gp_dept_hier WHERE dept_pid IN ( <dept_ids> ) )");
		// only query under certain org
		if(IdKeys.isValidId(orgid)) {
			builder.and("org_id = :org_id");
			params.put("org_id", orgid.getId());
		}
		builder.groupBy("dept_pid");
		
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : " + builder.toString() + " / PARAMS : " + oids.toString());
		}
		query(builder.toString(), (rs) -> {

			countMap.put(rs.getLong("dept_pid"), rs.getInt("grand_cnt"));
		}, params);

		builder = SqlBuilder.select(MasterIdKey.DEPT_HIER.schema());
		builder.where("dept_pid IN ( <dept_ids> ) ");
		if(IdKeys.isValidId(orgid)) {
			builder.and("org_id = :org_id");
			params.put("org_id", orgid.getId());
		}
		builder.and("(del_flag IS NULL OR del_flag = 0)");
		builder.orderBy("dept_id", SortOrder.ASC);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : " + builder.toString() + " / params : " + oids.toString());
		}
		
		return rows(builder.toString(), (rs, i) -> {

			DeptHierInfo info = DeptHierDAO.INFO_MAPPER.map(rs, i);

			info.setProperty("child_count", countMap.containsKey(info.getId()) ? countMap.get(info.getId()) : 0);

			return info;
		}, params);

	}
	
	@JdbiTran
	public InfoId newDeptHierNode(ServiceContext svcctx, DeptHierInfo nodeinfo) {
		
		svcctx.setTraceInfo(nodeinfo);
		
		InfoId deptId = IdKeys.newInfoId(MasterIdKey.DEPT_HIER);
		nodeinfo.setInfoId(deptId);
		
		GroupInfo group = new GroupInfo();
		group.setGroupName(nodeinfo.getDeptName() +"'s user group");
		group.setGroupType(MasterIdKey.DEPT_HIER.name());
		group.setManageId(nodeinfo.getId());
		
		svcctx.setTraceInfo(group);
		groupDao.create(group);

		// if parent id not set then set it with org's root dept id
		if(null == nodeinfo.getDeptPid() || 0 == nodeinfo.getDeptPid()) {
			
			Long rootDeptId = deptHierDao.query(cond -> {
				cond.and("org_id = " + nodeinfo.getOrgId() );
				cond.and("dept_pid = " + GeneralConsts.HIER_ROOT);
			}).get(0).getId();
			
			nodeinfo.setDeptPid(rootDeptId);
		}
		
		int cnt = deptHierDao.create(nodeinfo);
		
		// generate the flat table rows
		if(cnt > 0) {
			processDeptFlat(deptId);
		}
		
		return cnt > 0 ? nodeinfo.getInfoId() : null;
	}
	
	@JdbiTran
	public boolean saveDeptHierNode(ServiceContext svcctx, DeptHierInfo nodeinfo) {
	
		svcctx.setTraceInfo(nodeinfo);
		return deptHierDao.update(nodeinfo) > 0;
		
	}
	
	@JdbiTran
	public boolean removeDeptHierNode(InfoId deptid) {

		// remove dept group and sub-group users
		DeleteBuilder builder = SqlBuilder.delete(MasterIdKey.GROUP_USER.schema());
		builder.where("group_id IN (SELECT group_id FROM gp_group WHERE manage_id = ?)");
		builder.or("group_in IN ("
				+ "SELECT g.group_id FROM gp_group g, gp_dept_flat f " +
				" WHERE g.manage_id = f.dept_leaf_id AND f.dept_pid = ?"
				+ ")");
		List<Object> params = Arrays.asList(deptid.getId(), deptid.getId());
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : " + builder.toString() + " / params : " + params);
		}
		update(builder.toString(), params);

		// remove sub-group
		builder = groupDao.deleteSql();
		builder.where("manage_id = ?");
		builder.or("manage_id IN ("
				+ " SELECT f.dept_leaf_id FROM gp_dept_flat f "
				+ " WHERE f.dept_pid = ?"
				+ ")");
		params = Arrays.asList(deptid.getId(), deptid.getId());
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : " + builder.toString() + " / params : " + params);
		}
		update(builder.toString(), params);

		// remove sub-dept
		builder = deptHierDao.deleteSql();
		builder.or("dept_id IN ("
				+ " SELECT f.dept_leaf_id FROM gp_dept_flat f "
				+ " WHERE f.dept_pid = ?"
				+ ")");
		params = Arrays.asList(deptid.getId());
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : " + builder.toString() + " / params : " + params);
		}
		update(builder.toString(), params);

		// remove the flat table rows
		params = Arrays.asList(deptid.getId(), deptid.getId());
		builder = deptFlatDao.deleteSql();
		builder.where("dept_pid = ?");
		builder.or("dept_leaf_id = ?");
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("SQL : " + builder.toString() + " / params : " + params);
		}
		update(builder.toString(), params);
		
		int cnt = deptHierDao.delete(deptid) ;

		return cnt > 0;
	}
	
	private void processDeptFlat(InfoId deptId) {
		
		try(BaseHandle jhandle = this.getBaseHandle()){
			
			jhandle.call("call proc_dept_flat(?)", new Object[]{deptId});
		}
	}
	
	@JdbiTran
	public void addDeptHierMember(ServiceContext svcctx, InfoId deptId, Long... members) {

		// find root member group id
		Long groupId = groupDao.column("group_id", Long.class, KVPair.newPair("manage_id", deptId.getId()));
		Long orgId = deptHierDao.row(deptId).getOrgId();
		Long rootDeptId = deptHierDao.row(KVPair.newPair("org_id", orgId), KVPair.newPair("dept_pid", 99)).getId();
		// check user exists in groups under organization
		SelectBuilder builder = SqlBuilder.select(MasterIdKey.GROUP_USER.schema());
		builder.where("group_id IN (" +
				"select " +
					"g.group_id " +
				"from gp_group g, gp_dept_flat df " +
				"where g.manage_id = df.dept_leaf_id and " +
					"df.dept_pid = ?" +
				")");
		builder.and("member_uid = ?");
		
		for (Long member : members) {
			
			List<Object> params = Arrays.asList(rootDeptId, member);
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("SQL: {} / PARAMS: {}", builder.toString(),params);
			}
			InfoId mbrid = IdKeys.getInfoId(MasterIdKey.GROUP_USER, -1l);
			query(builder.toString(), (rs) -> {
				mbrid.setId(rs.getLong("rel_id"));
			}, params);

			// Member already exists then assign member to target group id
			if (IdKeys.isValidId(mbrid)) {
				update(mbrid, "group_id", groupId);
				continue;
			}
			GroupUserInfo guinfo = new GroupUserInfo();
			InfoId rid = IdKeys.newInfoId(MasterIdKey.GROUP_USER);
			guinfo.setInfoId(rid);
			guinfo.setGroupId(groupId);
			guinfo.setType(GroupUsers.MemberType.USER.name());
			guinfo.setRole(GroupUsers.MemberRole.MEMBER.name());
			guinfo.setMemberUid(member);
			svcctx.setTraceInfo(guinfo);

			groupUserDao.create(guinfo);

		}

	}

	@JdbiTran
	public void removeDeptHierMember(ServiceContext svcctx, InfoId deptId, Long... members) {

		for (Long member : members) {
			
			DeleteBuilder builder = SqlBuilder.delete(MasterIdKey.GROUP_USER.schema());
			builder.where("group_id IN (SELECT group_id FROM gp_group WHERE manage_id = ?)");
			builder.and("member_uid = ?");

			List<Object> params = Arrays.asList( deptId, member );
			if (LOGGER.isDebugEnabled()) {

				LOGGER.debug("SQL : " + builder.toString() + " / params : " + params);
			}
			update(builder.toString(), params);
		}

	}

	@JdbiTran
	public List<GroupUserInfo> getDeptHierMembers(ServiceContext svcctx, String keyword, InfoId deptid, Collection<String> features, PageQuery pquery) {

		List<GroupUserInfo> rtv = null;

		SelectBuilder builder = SqlBuilder.select();
		builder.column("a.*");
		builder.column("b.username", "b.email", "b.full_name", "b.avatar_url");
		builder.column("b.mobile", "b.category", "b.create_time", " b.state");
		builder.column("s.source_id", "s.source_name", "s.abbr", "s.node_gid");

		builder.from((from) -> {
			from.table("gp_group_user a");
			from.leftJoin("gp_user b", "a.member_uid = b.user_id");
			from.leftJoin("gp_source s", "b.source_id = s.source_id");
		});

		Map<String, Object> params = Maps.newHashMap();
		Principal princ = svcctx.getPrincipal();		
		params.put("op_uid", princ.getUserId().getId());
		
		if( null != features && features.contains("DIRECT")){
			// member under department directly	
			builder.where("a.group_id IN (select group_id from gp_group where manage_id = :dept_id)");
			params.put("dept_id", deptid.getId());
			
		} else {
			// all members under department
			builder.and( cond -> {
				cond.or("a.group_id IN (select g.group_id from gp_dept_flat df, gp_group g where df.dept_leaf_id = g.manage_id and df.dept_pid = :dept_id )");
				cond.or("a.group_id IN (select group_id from gp_group where manage_id = :dept_id)");
			});
			params.put("dept_id", deptid.getId());		
		} 

		if (!Strings.isNullOrEmpty(keyword)) {
			builder.and( cond -> {
				cond.or("b.full_name LIKE :keyword");
				cond.or("b.username LIKE :keyword");
			});
			params.put("keyword", keyword + "%");
		}
		
		builder.orderBy("b.full_name");
		
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
			
			info.setProperty("create_time", rs.getTimestamp("create_time"));
			info.setProperty("source_id", rs.getLong("source_id"));
			return info;
		}, params);

		return rtv;

	}
}
