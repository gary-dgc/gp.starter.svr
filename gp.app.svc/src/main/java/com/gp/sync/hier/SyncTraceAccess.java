package com.gp.sync.hier;

import com.google.common.base.Enums;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.gp.bind.BindAutowired;
import com.gp.bind.BindComponent;
import com.gp.common.*;
import com.gp.dao.SyncTraceDAO;
import com.gp.dao.info.SyncTraceInfo;
import com.gp.db.JdbiTran;
import com.gp.info.TraceCode;
import com.gp.sql.BaseBuilder.SortOrder;
import com.gp.sql.select.SelectBuilder;
import com.gp.sql.update.UpdateBuilder;
import com.gp.svc.BaseService;
import com.gp.svc.ServiceSupport;
import com.gp.sync.trace.DataShot;
import com.gp.sync.trace.TraceAccess;
import com.gp.util.BaseUtils;
import com.gp.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@BindComponent(priority = BaseService.BASE_PRIORITY)
public class SyncTraceAccess extends ServiceSupport implements TraceAccess {

    static Logger LOGGER = LoggerFactory.getLogger(SyncTraceAccess.class);

    @BindAutowired
    private SyncTraceDAO syncTraceDAO;

    /**
     * Persist data shots generated locally
     **/
    public InfoId persistLocal(DataShot... traces) {

        if(null == traces || traces.length == 0) return null;
        String nodeGid = getLocalNodeGid();
        InfoId chronicalId = IdKeys.newInfoId(MasterIdKey.SYNC_TRACE);
        int cnt = 0;
        for(DataShot shot : traces) {

            NodeMeta nodeMeta = HierSchema.instance().getNodeMeta(shot.getHierKey());
            SyncTraceInfo trace = new SyncTraceInfo();

            trace.setInfoId(cnt == 0 ? chronicalId : IdKeys.newInfoId(MasterIdKey.SYNC_TRACE));

            trace.setChronicalId(chronicalId.getId());
            trace.setTraceOp(shot.getTraceOp().name());

            trace.setTraceCode(TraceCode.newNodeTrace(nodeGid, trace.getInfoId()));
            trace.setHierKey(shot.getHierKey().toString());
            trace.setOriginGid(nodeGid);
            trace.setTraceTime(new Date(shot.getTraceTime()));
            trace.setSyncTime(new Date(shot.getSyncTime()));

            // CREATE OR DELETE, Try to set all attributes
            Set<String> keys = (shot.getTraceOp() == Syncs.TraceOp.DELETE || shot.getTraceOp() == Syncs.TraceOp.CREATE) ?
                    nodeMeta.getAttrs() : shot.getData().keySet();

            String keyStr = Joiner.on(',').join(keys);
            trace.setAttrKeys(keyStr);

            trace.setData(JsonUtils.toJson(shot.getData()));
            trace.setState(Syncs.MessageState.PROCESSED.name());

            trace.setModifierUid(GroupUsers.ANONY_UID.getId());
            trace.setModifyTime(BaseUtils.now());

            cnt++;
            syncTraceDAO.create(trace);
            shot.setTraceId(trace.getInfoId());

        }

        return chronicalId;
    }

    /**
     * Persist data shots received from remote nodes
     **/
    public InfoId persistRemote(DataShot... traces) {

        if(null == traces || traces.length == 0) return null;
        InfoId chronicalId = IdKeys.newInfoId(MasterIdKey.SYNC_TRACE);
        int cnt = 0;
        for(DataShot shot : traces) {

            // if record exist, ignore and persist next shot.
            int count = syncTraceDAO.count(KVPair.newPair("trace_code", shot.getTraceCode()));
            if(count > 0) {
                continue;
            }
            NodeMeta nodeMeta = HierSchema.instance().getNodeMeta(shot.getHierKey());
            SyncTraceInfo trace = new SyncTraceInfo();

            trace.setInfoId(cnt == 0 ? chronicalId : IdKeys.newInfoId(MasterIdKey.SYNC_TRACE));

            trace.setChronicalId(chronicalId.getId());
            trace.setTraceOp(shot.getTraceOp().name());

            trace.setTraceCode(shot.getTraceCode());
            trace.setHierKey(shot.getHierKey().toString());
            trace.setOriginGid(shot.getOriginGid());
            trace.setTraceTime(new Date(shot.getTraceTime()));
            trace.setSyncTime(null);

            // CREATE OR DELETE, Try to set all attributes
            Set<String> keys = (shot.getTraceOp() == Syncs.TraceOp.DELETE || shot.getTraceOp() == Syncs.TraceOp.CREATE) ?
                    nodeMeta.getAttrs() : shot.getData().keySet();

            String keyStr = Joiner.on(',').join(keys);
            trace.setAttrKeys(keyStr);

            trace.setState(Syncs.MessageState.PENDING.name());

            trace.setModifierUid(GroupUsers.ANONY_UID.getId());
            trace.setModifyTime(BaseUtils.now());

            cnt++;
            syncTraceDAO.create(trace);
            shot.setTraceId(trace.getInfoId());

        }

        return chronicalId;
    }

    public int changeTraceState(InfoId chronicalId, Syncs.MessageState state){

        UpdateBuilder update = syncTraceDAO.updateSql();
        update.set("state", state.name());
        update.where("chronical_id = ?");

        List<Object> params = Lists.newArrayList(chronicalId.getId());

        return update(update.build(), params.toArray());
    }

    @JdbiTran(readOnly = true)
    public String getLocalNodeGid() {

        InfoId srcId = Sources.LOCAL_INST_ID;
        String nodeGid = column(srcId, "node_gid", String.class);
        return nodeGid;
    }

    @Override
    public InfoId persist(boolean local, DataShot... traces) {

        if(local){
            return this.persistLocal(traces);
        }else{
            return this.persistRemote(traces);
        }

    }

    @JdbiTran(readOnly = true)
    @Override
    public List<Map<String, String>> result(InfoId chronicalId){

        List<Map<String, String>> rlist = Lists.newArrayList();

        SelectBuilder selectBuilder = syncTraceDAO.selectSql();
        selectBuilder.column("trace_code", "state", "result");
        selectBuilder.where("chronical_id = "+chronicalId.getId());

        rlist = rows(selectBuilder.build(), (r, ctx) -> {
            Map<String, String> info = Maps.newHashMap();
            info.put("trace_code", r.getString("trace_code"));
            info.put("state", r.getString("state"));
            info.put("result", r.getString("result"));
            return info;
        });

        return rlist;
    }

    @JdbiTran(readOnly = true)
    @Override
    public List<DataShot> peek(Map<HierKey, Set<String>> cond) {

        StringBuffer buffer = new StringBuffer(100);
        int cnt = 0;
        for(Map.Entry<HierKey, Set<String>> entry : cond.entrySet()) {

            if(cnt == 0){
                buffer.append('(');
            }else{
                buffer.append(')');
                buffer.append(System.lineSeparator()).append("UNION").append(System.lineSeparator());
                buffer.append('(');
            }
            Set<String> keys = entry.getValue();
            SelectBuilder select = syncTraceDAO.selectSql();
            select.all();
            select.where("state = '" + Syncs.MessageState.PROCESSED.name() + "'");
            select.and("hier_key = '" + entry.getKey().toString() + "'");

            String filter = keys.stream().map(k -> "FIND_IN_SET('" + k + "', attr_keys)").collect(Collectors.joining(" OR "));
            filter += " OR trace_op = 'DELETE'";

            select.and("(" + filter + ")");
            select.orderBy("trace_id", SortOrder.DESC);
            select.limit(0, 1);

            buffer.append(select.build());
            cnt++;
        }
        buffer.append(')');

        return rows(buffer.toString(), (rs, ctx) -> {
            DataShot shot = new DataShot();

            // parse the trace op
            Syncs.TraceOp op = Enums.getIfPresent(Syncs.TraceOp.class, rs.getString("trace_op")).or(Syncs.TraceOp.UNKNOWN);
            InfoId traceKey = IdKeys.getInfoId(MasterIdKey.SYNC_TRACE,rs.getLong("trace_id"));
            shot.setTraceId(traceKey);
            shot.setTraceOp(op);
            shot.setTraceCode(rs.getString("trace_code"));
            shot.setHierKey(HierKey.getHierKey(rs.getString("hier_key")));

            Date time = rs.getTimestamp("trace_time");
            shot.setTraceTime( null == time ? null : time.getTime());

            time = rs.getTimestamp("sync_time");
            shot.setSyncTime(null == time ? null : time.getTime());

            return shot;
        });
    }

    @JdbiTran(readOnly = true)
    @Override
    public List<DataShot> fetch(InfoId chronicalId){

        SelectBuilder select = syncTraceDAO.selectSql();
        select.all();
        select.where("chronical_id = " + chronicalId.getId());
        select.orderBy(MasterIdKey.SYNC_TRACE.idColumn(), SortOrder.ASC);

        return rows(select.toString(), (rs, ctx) -> {
            DataShot shot = new DataShot();

            // parse the trace op
            Syncs.TraceOp op = Enums.getIfPresent(Syncs.TraceOp.class, rs.getString("trace_op")).or(Syncs.TraceOp.UNKNOWN);
            InfoId traceKey = IdKeys.getInfoId(MasterIdKey.SYNC_TRACE,rs.getLong("trace_id"));
            shot.setTraceId(traceKey);
            shot.setTraceOp(op);
            shot.setTraceCode(rs.getString("trace_code"));
            shot.setHierKey(HierKey.getHierKey(rs.getString("hier_key")));

            String json = rs.getString("data");
            shot.setData(JsonUtils.toMap(json, Object.class));

            Date time = rs.getTimestamp("trace_time");
            shot.setTraceTime( null == time ? null : time.getTime());

            time = rs.getTimestamp("sync_time");
            shot.setSyncTime(null == time ? null : time.getTime());

            return shot;
        });
    }
}
