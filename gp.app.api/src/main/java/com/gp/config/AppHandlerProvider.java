
package com.gp.config;

import com.gp.web.BaseApiProvider;
import com.gp.web.sse.EventSourceManager;
import com.networknt.handler.HandlerProvider;
import com.networknt.health.HealthGetHandler;
import com.networknt.info.ServerInfoGetHandler;

import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.sse.ServerSentEventHandler;
import io.undertow.util.Methods;

public class AppHandlerProvider implements HandlerProvider, BaseApiProvider {

    @Override
    public HttpHandler getHandler() {
        
    	RoutingHandler routing = Handlers.routing()
            .add(Methods.GET, "/v1/health", new HealthGetHandler())
            .add(Methods.GET, "/v1/server/info", new ServerInfoGetHandler());
      
    	// Initial the authentication handler, enable swap token and disable verify token
    	this.initialAuthHandlers(routing, true, false);
    	
    	// Detect api handler in specified package
    	this.detectHandlers(routing, "com.gp.web.api");

		this.initialDebugHandler(routing);

    	// Detect api handler in specified package
    	this.detectHandlers(routing, "com.gp.web.transfer");
        
    	ServerSentEventHandler sseHandler = EventSourceManager.instance().getSSEHandler();
    	// Add Server Send Event handler
    	routing.get("/sse", sseHandler);
    	
    	return routing;
    }

}
