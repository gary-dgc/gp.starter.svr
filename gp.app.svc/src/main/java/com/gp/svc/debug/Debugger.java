package com.gp.svc.debug;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.gp.bind.BindComponent;
import com.gp.common.IPair;
import com.gp.common.Identifier;
import com.gp.info.BaseInfo;
import com.gp.sql.SqlBuilder;
import com.gp.sql.common.ConditionBuilder;
import com.gp.sql.select.SelectBuilder;
import com.gp.svc.BaseService;
import com.gp.svc.ServiceSupport;
import com.gp.util.JsonUtils;
import org.jdbi.v3.core.mapper.RowMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@BindComponent(priority = BaseService.BASE_PRIORITY - 20)
public class Debugger extends ServiceSupport implements BaseService {

    static Logger LOGGER = LoggerFactory.getLogger(Debugger.class);

    public <T> T peekRow(Identifier idf, RowMapper<T> rowMapper, IPair<String, Object> pair, IPair <String, Object> ... pairs) {

        try {
            T t = row(idf, rowMapper, pair, pairs);
            if (null == t) {
                LOGGER.debug("peek row data: NULL");
                return null;
            }
            Map<String, Object> data = Maps.newHashMap();
            if (t instanceof BaseInfo) {
                BaseInfo info = (BaseInfo) t;
                data = info.toBaseMap();
            } else if (t instanceof Map) {

                data = (Map<String, Object>) t;
            }

            String json = null;
            try {
                json = JsonUtils.JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(data);
                json = json.replace("\"", "");
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            StringBuffer buffer = new StringBuffer();
            if(null != pair){
                buffer.append(pair.getKey()).append("=").append(pair.getValue());
            }
            for(IPair<String, Object> p: pairs){
                if(buffer.length() != 0){
                    buffer.append(", ");
                }
                buffer.append(p.getKey()).append("=").append(p.getValue());
            }
            LOGGER.debug("peek row\n {} -> ({}) \n data: {}", idf.schema(), buffer, json);

            return t;
        }catch (Throwable t){
            // ignore
            return null;
        }
    }

    public <T> T peekRow(Identifier idf, RowMapper<T> rowMapper, Consumer<ConditionBuilder> cond, List<Object> params) {

        try{
            SelectBuilder select = SqlBuilder.select(idf);
            select.where(cond);
            T t = row(select.build(), rowMapper, params);

            if(null == t){
                LOGGER.debug("peek row data: NULL");
                return null;
            }

            Map<String, Object> data = Maps.newHashMap();
            if(t instanceof BaseInfo) {
                BaseInfo info = (BaseInfo) t;
                data = info.toBaseMap();
            }else if(t instanceof Map){

                data = (Map<String, Object>) t;
            }

            String json = null;
            try {
                json = JsonUtils.JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(data);
                json = json.replace("\"", "");
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            String rowkey = Joiner.on(',').join(params);
            LOGGER.debug("peek row \n {} -> ({}) \n data: {}", idf.schema(), rowkey, json);

            return t;
        }catch (Throwable t){
            // ignore
            return null;
        }
    }
}
