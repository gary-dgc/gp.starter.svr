package com.gp.sync;

import com.gp.bind.BindScanner;
import com.gp.sync.hier.HierSchema;
import com.gp.sync.hier.NodeBuilder;
import com.gp.sync.hier.SyncTraceAccess;

public class SyncMQ {

    public static final String SYNC_INTN_ORG = "org:intern";
    public static final String SYNC_INTN_DEPT = "dept:intern";
    public static final String SYNC_INTN_DUTY = "duty:intern";
    public static final String SYNC_INTN_SYS = "sys:intern";
    public static final String SYNC_INTN_USR = "usr:intern";
    public static final String SYNC_INTN_DMDL = "dmdl:intern";

    private static SyncMQ instance;

    private SyncTraceAccess traceAccess;

    public static SyncMQ instance(){

        if(null == instance){
            instance = new SyncMQ();
        }

        return instance;
    }

    private SyncMQ(){

    }

    /**
     * Prepare and initialize the Hierarchy Schema synchronization
     **/
    public void initial(){

        traceAccess = BindScanner.instance().getBean(SyncTraceAccess.class);
        defineChannelSchema();


    }

    public void initialMQ(){

    }

    private void defineChannelSchema(){
        // Define news schema
        NodeBuilder chnl = HierSchema.newSchemaBuilder(SYNC_INTN_ORG, true);
        chnl.setTraceKey("channel_code");
        chnl.setAttrs("channel_name", "channel_type", "avatar_url", "cover_url",
                "description", "tags");

        chnl.setNode("binds", builder -> {
            builder.setMultiple(true);
            builder.setTraceKey("scope");
            builder.setAttrs("binder_code", "state", "contribute_on");
        });

        chnl.build();
    }
}
