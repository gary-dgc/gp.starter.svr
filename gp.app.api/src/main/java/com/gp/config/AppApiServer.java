package com.gp.config;

import com.gp.asm.AgentBinder;
import com.gp.core.AppRunner;
import com.gp.db.asm.JdbiTransformer;
import com.networknt.server.Server;

public class AppApiServer extends AppRunner {

    public static void main(String[] args) throws Exception {

        // show framework and app banner info
        showBanner();

        // initial agent
        initialAgent();

        Server webServer = new Server();

        webServer.init();
    }

}
