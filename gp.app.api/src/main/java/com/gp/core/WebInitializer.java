package com.gp.core;

import com.google.common.collect.Maps;
import com.gp.bind.BindScanner;
import com.gp.cache.CacheManager;
import com.gp.cache.ICache;
import com.gp.common.Caches;
import com.gp.common.GeneralConfig;
import com.gp.common.Instructs;
import com.gp.common.Operations;
import com.gp.config.AppStartupHook;
import com.gp.db.JdbiContext;
import com.gp.exception.BaseException;
import com.gp.launcher.CoreInitializer;
import com.gp.launcher.Lifecycle.LifeState;
import com.gp.web.client.NodeAccess;
import com.gp.web.model.AuthenData;
import com.gp.web.util.ConfigUtils;
import com.networknt.config.Config;
import com.networknt.service.SingletonServiceFactory;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Map;

/**
 * The initializer try to set the core event listeners to Disruptor instance.
 * Event evolve routine:<br>
 * <pre>
 * </pre>
 * 
 * 1 and 2 are executed in parallel. 2 generate the SYNC event and fire the SyncListener.
 **/
public class WebInitializer extends CoreInitializer{

	static Logger LOGGER = LoggerFactory.getLogger(AppStartupHook.class);
	
	public static final String DATA_SOURCE = "datasource";
	
	public WebInitializer() throws BaseException {
		super("WebInitializer", 10);
	}

	@Override
	public void initial() {
		
		LOGGER.debug("Prepare operations definition");
		Instructs.putInstruct(Operations.values());

		this.bindLifeEvent(LifeState.STARTUP, (state) -> {
			
			LOGGER.debug("Start: setup caches");
			ICache cache = CacheManager.getGuavaCache(Caches.EXTRA_CACHE_TTL, 1000);
			CacheManager.instance().register(Caches.EXTRA_CACHE, cache);
			
			cache = CacheManager.getGuavaCache(Caches.FILE_CACHE_TTL, 1000);
			CacheManager.instance().register(Caches.FILE_CACHE, cache);
			
			cache = CacheManager.getGuavaCache(Caches.DICT_CACHE_TTL, 10000);
			CacheManager.instance().register(Caches.DICT_CACHE, cache);
						
			LOGGER.debug("Initialize the datasources");
			initialDataSource();
			
			LOGGER.debug("Initialize the DAOs and Services with BeanScanner");
			initialService();
			
			LOGGER.debug("Initialize the CoreDelegate");
			// initialize the engine
			CoreDelegate coreFacade = new CoreDelegate();
			CoreEngine.initial(coreFacade);
			
			try {
				LOGGER.debug("Start: setup node api agent");
				initialApiAgents();
			} catch (Exception e) {
				LOGGER.error("Start: Fail initial api agent");
			}
			
		});
		
	}

	private void initialService() {
		// Forward bean get invocation to SingletonServiceFactory
		BindScanner.instance().setBeanGetter((Class<?> clazz) -> {
			return SingletonServiceFactory.getBean(clazz);
		});
		// Save bean into SingletonServiceFactory
		BindScanner.instance().setBeanSetter((Class<?> clazz, Object bean) -> {
			SingletonServiceFactory.setBean(clazz.getCanonicalName(), bean);
		});
		
		// scan dao and service
		BindScanner.instance().scanPackages("com.gp.dao", "com.gp.svc");

	}
	
	@SuppressWarnings("unchecked")
	private void initialDataSource() {
		
        Map<String, Object> dataSourceMap = (Map<String, Object>) Config.getInstance().getJsonMapConfig(DATA_SOURCE);
        
        JdbiContext context = JdbiContext.instance();
        dataSourceMap.forEach((k, v) -> {

			HikariConfig config = new HikariConfig();

			config.setDriverClassName(((Map<String, String>)v).get("driverClassName"));
			config.setJdbcUrl(((Map<String, String>)v).get("jdbcUrl"));
			config.setUsername(((Map<String, String>)v).get("username"));
			config.setPassword(((Map<String, String>)v).get("password"));
			config.setMaximumPoolSize(((Map<String, Integer>)v).get("maximumPoolSize"));
			config.setMinimumIdle(((Map<String, Integer>)v).get("minimumIdle"));

			Map<String, String> configParams = (Map<String, String>)((Map<String, Object>)v).get("parameters");
			configParams.forEach((p, q) -> config.addDataSourceProperty(p, q));

			HikariDataSource ds = new HikariDataSource(config);

			context.register(k, ds);
        });
        context.setDataSource("primary");
        
        SingletonServiceFactory.setBean(context.getClass().getName(), context);
    }
	
	private void initialApiAgents() throws Exception {
		
		// Prepare global api agent
		NodeAccess gblAuth = NodeAccess.newNodeAccess(NodeApiAgent.GlobalName);
		
		AuthenData authen = new AuthenData();
		authen.setAudience("1101");
		authen.setGrantType(CoreConsts.GRANT_NODE_CRED);
		
		String appKey = ConfigUtils.getSystemOption("node.app.key");
		String appSecret = ConfigUtils.getSystemOption("node.app.secret");
		authen.setPrincipal(appKey);
		authen.setCredential(appSecret);
		
    	Map<String, Object> extra = Maps.newHashMap();
    	extra.put("client_secret", "sslssl");
    	extra.put("scope", "read");
    	extra.put("device", "101010111");
    	
    	authen.setExtra(extra);
    	
    	gblAuth.setAuthenData(authen);
    	
    	// prepare the SyncHttpClient
    	String accessUrl = ConfigUtils.getSystemOption("global.access");
    	URL globalUrl = new URL(accessUrl);    	
    	gblAuth.setHost(globalUrl.getHost());
    	gblAuth.setPort(globalUrl.getPort());
    	
    	LOGGER.info("Prepare Global Agent: {}:{}", globalUrl.getHost(), globalUrl.getPort());
    	
    	// Prepare sync api agent
    	NodeAccess syncAuth = NodeAccess.newClientAccess(NodeApiAgent.SyncName);
		
		AuthenData syncData = new AuthenData();
		syncData.setGrantType(CoreConsts.GRANT_CLIENT_CRED);
		
		String princ = GeneralConfig.getStringByKeys("authen", "sync.princ");
		String cred = GeneralConfig.getStringByKeys("authen", "sync.cred");
		syncData.setPrincipal(princ);
		syncData.setCredential(cred);
		
    	Map<String, Object> sextra = Maps.newHashMap();
    	extra.put("scope", "read");
    	extra.put("device", "101010111");
    	
    	syncData.setExtra(sextra);
    	
    	syncAuth.setAuthenData(syncData);
    	
    	accessUrl = ConfigUtils.getSystemOption("sync.node.access");
    	URL syncUrl = new URL(accessUrl);
    	syncAuth.setHost(syncUrl.getHost());
    	syncAuth.setPort(syncUrl.getPort());
    	
    	LOGGER.info("Prepare Sync Node Agent: {}:{}", syncUrl.getHost(), syncUrl.getPort());
    	
    	NodeApiAgent.instance().setNodeAccess(gblAuth, syncAuth);
    	
    	// Prepare converter api agent
    	NodeAccess convertAuth = NodeAccess.newClientAccess(NodeApiAgent.ConvertName);
		
		AuthenData convertData = new AuthenData();
		convertData.setGrantType(CoreConsts.GRANT_CLIENT_CRED);
		
		String cvtprinc = GeneralConfig.getStringByKeys("authen", "convert.princ");
		String cvtcred = GeneralConfig.getStringByKeys("authen", "convert.cred");
		convertData.setPrincipal(cvtprinc);
		convertData.setCredential(cvtcred);
		
    	Map<String, Object> cextra = Maps.newHashMap();
    	cextra.put("scope", "read");
    	cextra.put("device", "101010111");
    	
    	convertData.setExtra(cextra);
    	
    	convertAuth.setAuthenData(convertData);
    	
    	accessUrl = ConfigUtils.getSystemOption("convert.access");
    	URL cvertUrl = new URL(accessUrl);
    	convertAuth.setHost(cvertUrl.getHost());
    	convertAuth.setPort(cvertUrl.getPort());
    	
    	LOGGER.info("Prepare Converter Node Agent: {}:{}", cvertUrl.getHost(), cvertUrl.getPort());
    	
    	NodeApiAgent.instance().setNodeAccess(gblAuth, syncAuth, convertAuth);
	}
}
