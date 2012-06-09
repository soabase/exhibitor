package com.netflix.exhibitor.core.state;

import com.google.common.base.Joiner;
import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.activity.Activity;
import com.netflix.exhibitor.core.activity.ActivityLog;
import com.netflix.exhibitor.core.config.InstanceConfig;
import com.netflix.exhibitor.core.config.IntConfigs;
import com.netflix.exhibitor.core.config.StringConfigs;

// TODO - locking, checking for rolling config, etc.

public class AutoInstanceManagement implements Activity
{
    private final Exhibitor exhibitor;

    public AutoInstanceManagement(Exhibitor exhibitor)
    {
        this.exhibitor = exhibitor;
    }

    @Override
    public void completed(boolean wasSuccessful)
    {
    }

    @Override
    public Boolean call() throws Exception
    {
        if ( exhibitor.getConfigManager().getConfig().getInt(IntConfigs.AUTO_MANAGE_INSTANCES) != 0 )
        {
            if ( exhibitor.getMonitorRunningInstance().getCurrentInstanceState() != InstanceStateTypes.LATENT )
            {
                UsState     usState = new UsState(exhibitor);
                if ( usState.getUs() == null )
                {
                    addUsIn(usState);
                }
            }
        }
        return true;
    }

    private void addUsIn(UsState usState) throws Exception
    {
        int         maxServerId = 0;
        for ( ServerSpec spec : usState.getServerList().getSpecs() )
        {
            if ( spec.getServerId() > maxServerId )
            {
                maxServerId = spec.getServerId();
            }
        }

        final InstanceConfig    currentConfig = exhibitor.getConfigManager().getConfig();
        String                  spec = currentConfig.getString(StringConfigs.SERVERS_SPEC);
        final String            newSpec = Joiner.on(',').skipNulls().join((spec.length() > 0) ? spec : null, "" + (maxServerId + 1) + ":" + exhibitor.getThisJVMHostname());
        exhibitor.getLog().add(ActivityLog.Type.INFO, "Adding this instance to server list due to automatic instance management");
        InstanceConfig          newConfig = new InstanceConfig()
        {
            @Override
            public String getString(StringConfigs config)
            {
                if ( config == StringConfigs.SERVERS_SPEC )
                {
                    return newSpec;
                }
                return currentConfig.getString(config);
            }

            @Override
            public int getInt(IntConfigs config)
            {
                return currentConfig.getInt(config);
            }
        };
        exhibitor.getConfigManager().startRollingConfig(newConfig);
    }
}
