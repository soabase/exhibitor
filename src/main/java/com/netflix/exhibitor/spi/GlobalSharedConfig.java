package com.netflix.exhibitor.spi;

import java.util.Collection;

public interface GlobalSharedConfig
{
    public Collection<ServerInfo>   getServers();

    public void                     setServers(Collection<ServerInfo> newServers) throws Exception;

    public Collection<String>       getBackupPaths();

    public void                     setBackupPaths(Collection<String> newBackupPaths) throws Exception;
}
