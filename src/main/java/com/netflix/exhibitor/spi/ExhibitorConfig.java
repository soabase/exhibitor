package com.netflix.exhibitor.spi;

import java.util.Collection;

public interface ExhibitorConfig
{
    public String   getServers();

    public String   getThisHostname();
    
    public int      getServerIdForHostname(String hostname);

    public int      getConnectPort();

    public int      getElectionPort();

    public int      getCheckSeconds();

    public int      getClientPort();

    public int      getConnectionTimeoutMs();

    public int      getBackupPeriodMs();

    public int      getCleanupPeriodMs();

    public int      getMaxBackups();

    public Collection<UITab>    getAdditionalUITabs();
}
