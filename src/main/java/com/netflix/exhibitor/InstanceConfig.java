package com.netflix.exhibitor;

import java.util.Collection;

/**
 * Static configuration needed by Exhibitor
 */
public class InstanceConfig
{
    private final InstanceConfigBuilder builder;

    /**
     * Start a builder for instance config
     *
     * @return builder
     */
    public static InstanceConfigBuilder builder()
    {
        return new InstanceConfigBuilder();
    }

    InstanceConfig(InstanceConfigBuilder builder)
    {
        //To change body of created methods use File | Settings | File Templates.
        this.builder = builder;
    }

    public String getHostname()
    {
        return builder.hostname;
    }

    public int getConnectPort()
    {
        return builder.connectPort;
    }

    public int getElectionPort()
    {
        return builder.electionPort;
    }

    public int getCheckSeconds()
    {
        return builder.checkSeconds;
    }

    public int getClientPort()
    {
        return builder.clientPort;
    }

    public Collection<UITab> getAdditionalUITabs()
    {
        return builder.additionalUITabs;
    }

    public int getConnectionTimeoutMs()
    {
        return builder.connectionTimeoutMs;
    }

    public int getCleanupPeriodMs()
    {
        return builder.cleanupPeriodMs;
    }

    public String getServerSpec()
    {
        return builder().serversSpec;
    }
}
