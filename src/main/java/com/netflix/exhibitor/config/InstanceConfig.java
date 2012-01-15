package com.netflix.exhibitor.config;

public interface InstanceConfig
{
    public String   getServers();

    public String   getThisHostname();
    
    public int      getServerIdForHostname(String hostname);

    public int      getConnectPort();

    public int      getElectionPort();

    public int      getCheckSeconds();

    public int      getClientPort();

    public int      getConnectionTimeoutMs();
}
