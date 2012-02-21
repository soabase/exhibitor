package com.netflix.exhibitor.core.state;

import com.google.common.collect.Iterables;
import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.config.InstanceConfig;
import com.netflix.exhibitor.core.config.IntConfigs;
import com.netflix.exhibitor.core.config.StringConfigs;

public class InstanceStateManager
{
    private final Exhibitor     exhibitor;
    private final Checker       checker;

    public InstanceStateManager(Exhibitor exhibitor)
    {
        this.exhibitor = exhibitor;
        checker = new Checker(exhibitor);
    }

    public InstanceState getInstanceState()
    {
        InstanceConfig          config = exhibitor.getConfigManager().getConfig();

        ServerList              serverList = new ServerList(config.getString(StringConfigs.SERVERS_SPEC));
        ServerList.ServerSpec   us = Iterables.find(serverList.getSpecs(), ServerList.isUs(exhibitor.getThisJVMHostname()), null);
        return new InstanceState
        (
            serverList,
            config.getInt(IntConfigs.CONNECT_PORT),
            config.getInt(IntConfigs.ELECTION_PORT),
            (us != null) ? us.getServerId() : -1,
            checker.getState()
        );
    }
}
