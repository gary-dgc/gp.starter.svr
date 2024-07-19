package com.gp.core;

import com.google.common.collect.Maps;
import com.gp.bind.BindScanner;
import com.gp.cache.CacheManager;
import com.gp.cache.ICache;
import com.gp.common.*;
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

		});
		
	}

	private void initialService() {
		// Set bean priority calculator
		BindScanner.instance().setPrioritySetter(Services::calcPriority);

		// Save bean into SingletonServiceFactory
		BindScanner.instance().setBeanMonitor((Class<?> clazz, Object bean) -> {
			SingletonServiceFactory.setBean(clazz.getCanonicalName(), bean);
		});
		
		// scan dao and service
		BindScanner.instance().scanPackages("com.gp.dao");

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

    }

}
