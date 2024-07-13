package com.gp.dao.ext;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.gp.bind.BindComponent;
import com.gp.common.MasterIdKey;
import com.gp.dao.BaseDAO;
import com.gp.dao.DAOSupport;
import com.gp.dao.ExtendDAO;
import com.gp.sql.SqlBuilder;
import com.gp.sql.select.SelectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

@BindComponent(type = GroupExt.class, priority = ExtendDAO.BASE_PRIORITY )
public class GroupExt extends DAOSupport implements ExtendDAO {

    static Logger LOGGER = LoggerFactory.getLogger(GroupExt.class);

    /**
     * Get user's group ids and it's ancestors, this query depends on
     * procedure function: func_group_ancestry
     *
     **/
    public Set<Long> getUserGroups(Long userId, Long workgroupId){
        SelectBuilder select = SqlBuilder.select();
        select.column("g.group_id", "func_group_ancestry(g.group_id) AS ancestry_ids");
        select.from(MasterIdKey.GROUP.schema() + " AS g", MasterIdKey.GROUP_USER.schema() + " AS gu");
        select.where("g.group_id = gu.group_id");
        select.and("gu.member_uid = ?");
        List<Object> params = Lists.newArrayList();
        params.add(userId);
        if(workgroupId != null && workgroupId > 0) {
            select.and("g.manage_id = ?");
            params.add(workgroupId);
        }
        if(LOGGER.isDebugEnabled()){
            LOGGER.debug("SQL: {} / PARAMS: {}", select, userId);
        }
        Set<Long> gids = Sets.newHashSet();
        Splitter splitter = Splitter.on(',');
        query(select.build(), rs -> {
            Long grpid = rs.getLong("group_id");
            gids.add(grpid);
            String _ancestry = rs.getString("ancestry_ids");
            if(!Strings.isNullOrEmpty(_ancestry)) {
                Iterable<String> ancestors = splitter.split(_ancestry);
                ancestors.forEach(a -> gids.add(Long.valueOf(a)));
            }
        }, params);

        return gids;
    }

    /**
     * Get org's group id by org id
     **/
    public Long getOrgGroupId(Long orgId){

        SelectBuilder select = SqlBuilder.select();
        select.column("g.group_id");
        select.from(
                "gp_org_hier o",
                "gp_dept_hier d",
                "gp_group g");
        select.and( "o.org_id = d.org_id");
        select.and( "d.dept_pid = 99");
        select.and( "g.manage_id = d.dept_id");
        select.and( "o.org_id = ?");

        if(LOGGER.isDebugEnabled()){
            LOGGER.debug("SQL: {} / PARAMS: {}", select, orgId);
        }
        return column(select.build(), Long.class, orgId);
    }
}
