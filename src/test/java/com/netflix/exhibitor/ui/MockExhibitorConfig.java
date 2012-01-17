package com.netflix.exhibitor.ui;

import com.netflix.exhibitor.ExhibitorConfig;

public class MockExhibitorConfig implements ExhibitorConfig
{
    @Override
    public String getServers()
    {
        return "localhost";
    }

    @Override
    public String getThisHostname()
    {
        return "localhost";
    }

    @Override
    public int getServerIdForHostname(String hostname)
    {
        return 1;
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
        return 0;
    }

    @Override
    public int getCleanupPeriodMs()
    {
        return 0;
    }

    @Override
    public int getMaxBackups()
    {
        return 0;
    }
}
