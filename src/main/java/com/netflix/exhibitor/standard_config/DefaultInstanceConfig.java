package com.netflix.exhibitor.standard_config;

import com.netflix.exhibitor.InstanceConfig;
import java.util.concurrent.TimeUnit;

class DefaultInstanceConfig implements InstanceConfig
{
    @Override
    public String getHostname()
    {
        return "localhost";
    }

    @Override
    public String getServersSpec()
    {
        return "";
    }

    @Override
    public int getClientPort()
    {
        return 2181;
    }

    @Override
    public int getConnectPort()
    {
        return 2888;
    }

    @Override
    public int getElectionPort()
    {
        return 3888;
    }

    @Override
    public int getCheckMs()
    {
        return 10000;
    }

    @Override
    public int getConnectionTimeoutMs()
    {
        return 10000;
    }

    @Override
    public int getCleanupPeriodMs()
    {
        return (int)TimeUnit.MILLISECONDS.convert(6, TimeUnit.HOURS);
    }
}
