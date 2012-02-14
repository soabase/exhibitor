package com.netflix.exhibitor.core.state;

import com.google.common.collect.Iterables;
import com.netflix.exhibitor.core.Exhibitor;

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
        ServerList              serverList = new ServerList(exhibitor.getConfig().getServersSpec());
        ServerList.ServerSpec   us = Iterables.find(serverList.getSpecs(), ServerList.isUs(exhibitor.getConfig().getHostname()), null);
        return new InstanceState
        (
            serverList,
            exhibitor.getConfig().getConnectPort(),
            exhibitor.getConfig().getElectionPort(),
            (us != null) ? us.getServerId() : -1,
            checker.getState()
        );
    }
}
