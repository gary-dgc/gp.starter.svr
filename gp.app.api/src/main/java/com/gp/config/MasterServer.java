package com.gp.config;

import com.gp.asm.AgentBinder;
import com.gp.db.asm.JdbiTransformer;
import com.networknt.server.Server;

public class MasterServer {

    public static void main(String[] args) throws Exception {

        // initial agent
        initialAgent();

        Server webServer = new Server();
        webServer.init();
    }

    // try to initial the java agent
    public static void initialAgent() throws Exception {
        JdbiTransformer transformer = new JdbiTransformer();
        AgentBinder.bindAgent(transformer);
    }
}
