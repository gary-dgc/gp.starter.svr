package dao.sql.tool;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dao.sql.tool.BindHooker;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.CaseStrategy;
import org.jdbi.v3.core.mapper.MapMapper;
import org.jdbi.v3.core.mapper.MapMappers;
import org.jdbi.v3.core.statement.Query;

import javax.sql.DataSource;

/**
 * class to access database, help to query tables and columns
 * 
 *  @author gdiao
 **/
public class DataBaseAccessor {

	private String INF_SCHEMA = "information_schema";
	protected String host;
	protected String port;
	protected String schema;
	protected String user;
	protected String pwd;

	Jdbi jdbi;
	BindHooker hooker ;

	public DataBaseAccessor(String host, String port, String schema, String user, String pwd) {
		this.host = host;
		this.port = port;
		this.schema = schema;
		this.user = user;
		this.pwd = pwd;
	}
	
	public void setHooker(BindHooker hooker) {
		this.hooker = hooker;
	}
	
	/**
	 * Try to connect to database 
	 **/
	public void tryConn() {
		Map<String, Object> hookMap = Maps.newHashMap();
		String url = "jdbc:mysql://" + host;
        if (!Strings.isNullOrEmpty(port)) {
            url += ":" + port;
        }
        if (!Strings.isNullOrEmpty(INF_SCHEMA)) {
            url += "/" + INF_SCHEMA;
        }
        url += "?characterEncoding=UTF-8&useEncoding=true&autoReconnect=true&serverTimezone=PRC&useLegacyDatetimeCode=false&useSSL=false";

		HikariConfig config = new HikariConfig();

		config.setJdbcUrl(url);
		config.setUsername(user);
		config.setPassword(pwd);
		config.setDriverClassName("com.mysql.cj.jdbc.Driver");

		HikariDataSource dataSource = new HikariDataSource(config);

        hookMap.put("url", url);

        this.jdbi = Jdbi.create(dataSource);
        this.jdbi.getConfig().get(MapMappers.class).setCaseChange(CaseStrategy.NOP);
        
        try {
			jdbi.useHandle(handle -> {
				Query query = handle.createQuery("select 1 from dual");

				int one = query.mapTo(Integer.class).one();
				hookMap.put("conn", String.valueOf(one));
				hookMap.put("message", "Success connect to database");
			});
		}catch(Exception e){

			e.printStackTrace();
			hookMap.put("message", "Cannot connect to database");
			hookMap.put("exception", e.getLocalizedMessage());
		}
        hooker.callback(BindHooker.TRY_CONN, hookMap);
	}
	
	/**
	 * load tables 
	 **/
	public void loadTables() {
		 Map<String, Object> paramMap = new HashMap<>();

	     String sql = "SELECT TABLE_NAME FROM `TABLES` WHERE TABLE_SCHEMA = :TABLE_SCHEMA AND TABLE_TYPE = :TABLE_TYPE ";
	     Map<String, Object> hookMap = Maps.newHashMap();

		try {
			jdbi.useHandle(handle -> {
				Query query = handle.createQuery(sql);

				query.bind("TABLE_SCHEMA", schema);
				query.bind("TABLE_TYPE", "BASE TABLE");

				List<String> tables = query.mapTo(String.class).list();
				hookMap.put("message", "Success load tables from database");
				hookMap.put("tables", tables);
			});
		}catch(Exception e){

			e.printStackTrace();
			hookMap.put("message", "Cannot load tables from database");
			hookMap.put("exception", e.getLocalizedMessage());
		}

		hooker.callback(BindHooker.LOAD_TBLS, hookMap);
	}
	
	/**
	 * find column list 
	 **/
    public List<Map<String, Object>> findColumnList(String table) {

		Map<String, Object> hookMap = Maps.newHashMap();

        String sql = "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_KEY, EXTRA FROM `COLUMNS` WHERE TABLE_SCHEMA = :TABLE_SCHEMA AND TABLE_NAME = :TABLE_NAME ";
		List<Map<String, Object>> rtv  =Lists.newArrayList();

		try {
			rtv = jdbi.withHandle(handle -> {
				Query query = handle.createQuery(sql);

				query.bind("TABLE_SCHEMA", schema);
				query.bind("TABLE_NAME", table);


				hookMap.put("message", "Success load columns from database");
				return query.mapToMap().list();
			});

		}catch(Exception e){

			e.printStackTrace();
			hookMap.put("message", "Cannot load columns from database");
			hookMap.put("exception", e.getLocalizedMessage());

		}

		return rtv;
    }
       
    /**
     * find table primary key 
     **/
    public String findPrimaryKey(String table) {

		Map<String, Object> hookMap = Maps.newHashMap();
    	String sql = "SELECT COLUMN_NAME FROM columns where TABLE_SCHEMA =:TABLE_SCHEMA and TABLE_NAME=:TABLE_NAME and COLUMN_KEY='PRI'";
		String rtv = "";
		try {
			rtv = jdbi.withHandle(handle -> {
				Query query = handle.createQuery(sql);

				query.bind("TABLE_SCHEMA", schema);
				query.bind("TABLE_NAME", table);


				hookMap.put("message", "Success load columns from database");
				return query.mapTo(String.class).one();
			});

		}catch(Exception e){

			e.printStackTrace();
			hookMap.put("message", "Cannot load columns from database");
			hookMap.put("exception", e.getLocalizedMessage());

		}

		return rtv;
    }
}
