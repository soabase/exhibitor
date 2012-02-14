package com.netflix.exhibitor.core.state;

import com.google.common.collect.Iterables;
import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.config.IntConfigs;
import com.netflix.exhibitor.core.config.StringConfigs;

public class InstanceStateManager
{
    private final Exhibitor     exhibitor;
    private final Checker       checker;

    public InstanceStateManager(Exhibitor exhibitor)
    {
        this.exhibitor = exhibitor;
        checker = new Checker(exhibitor, this);
    }

    public InstanceState getInstanceState()
    {
        ServerList              serverList = new ServerList(exhibitor.getConfig().getString(StringConfigs.SERVERS_SPEC));
        ServerList.ServerSpec   us = Iterables.find(serverList.getSpecs(), ServerList.isUs(exhibitor.getConfig().getString(StringConfigs.HOSTNAME)), null);
        return new InstanceState
        (
            serverList,
            exhibitor.getConfig().getInt(IntConfigs.CONNECT_PORT),
            exhibitor.getConfig().getInt(IntConfigs.ELECTION_PORT),
            (us != null) ? us.getServerId() : -1,
            checker.getState()
        );
    }
}
