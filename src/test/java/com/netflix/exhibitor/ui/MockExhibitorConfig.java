package com.netflix.exhibitor.ui;

import com.google.common.collect.Lists;
import com.netflix.exhibitor.spi.ExhibitorConfig;
import com.netflix.exhibitor.spi.ServerInfo;
import com.netflix.exhibitor.spi.UITab;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class MockExhibitorConfig implements ExhibitorConfig
{
    @Override
    public Collection<ServerInfo> getServers()
    {
        return Arrays.asList(new ServerInfo("localhost", 1, true));
    }

    @Override
    public int getConnectPort()
    {
        return 3888;
    }

    @Override
    public int getElectionPort()
    {
        return 2888;
    }

    @Override
    public int getCheckSeconds()
    {
        return 1;
    }

    @Override
    public int getClientPort()
    {
        return 2181;
    }

    @Override
    public int getConnectionTimeoutMs()
    {
        return 10000;
    }

    @Override
    public int getBackupPeriodMs()
    {
        return (int)TimeUnit.MILLISECONDS.convert(2, TimeUnit.MINUTES);
    }

    @Override
    public int getCleanupPeriodMs()
    {
        return (int)TimeUnit.MILLISECONDS.convert(2, TimeUnit.MINUTES);
    }

    @Override
    public int getMaxBackups()
    {
        return 0;
    }

    @Override
    public Collection<UITab> getAdditionalUITabs()
    {
        return null;
    }

    @Override
    public Collection<String> getBackupPaths()
    {
        return Lists.newArrayList("/one/two", "/three/four/five");
    }
}
