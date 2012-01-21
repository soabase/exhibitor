package com.netflix.exhibitor.state;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.netflix.exhibitor.Exhibitor;
import com.netflix.exhibitor.spi.ServerInfo;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;

public class InstanceStateManager implements Closeable
{
    private final Exhibitor         exhibitor;
    private final Checker           checker;

    public static final Predicate<ServerInfo>   isUs = new Predicate<ServerInfo>()
    {
        @Override
        public boolean apply(ServerInfo info)
        {
            return info.isThisServer();
        }
    };

    public InstanceStateManager(Exhibitor exhibitor)
    {
        this.exhibitor = exhibitor;
        checker = new Checker(exhibitor, this);
    }

    public void start()
    {
        checker.start();
    }

    @Override
    public void close() throws IOException
    {
        checker.close();
    }

    public InstanceState getInstanceState()
    {
        Collection<ServerInfo>  servers = exhibitor.getGlobalSharedConfig().getServers();
        if ( servers == null )
        {
            servers = Lists.newArrayList();
        }
        ServerInfo              us = Iterables.find(servers, isUs, null);

        InstanceStateTypes state = (us != null) ? checker.getState() : InstanceStateTypes.WAITING;
        return new InstanceState
        (
            servers,
            exhibitor.getConfig().getConnectPort(),
            exhibitor.getConfig().getElectionPort(),
            (us != null) ? us.getId() : -1,
            state,
            checker.getState()
        );
    }
}
