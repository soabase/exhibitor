package com.netflix.exhibitor.mocks;

import com.netflix.exhibitor.pojos.ServerInfo;
import com.netflix.exhibitor.spi.GlobalSharedConfig;
import java.util.Arrays;
import java.util.Collection;

public class MockGlobalSharedConfig implements GlobalSharedConfig
{
    private volatile Collection<ServerInfo> servers = Arrays.asList(new ServerInfo("localhost", 1, true));

    @Override
    public Collection<ServerInfo> getServers()
    {
        return servers;
    }

    @Override
    public void setServers(Collection<ServerInfo> newServers) throws Exception
    {
        servers = newServers;
    }
}
