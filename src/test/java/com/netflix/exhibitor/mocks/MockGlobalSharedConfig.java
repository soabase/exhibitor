package com.netflix.exhibitor.mocks;

import com.netflix.exhibitor.spi.BackupPath;
import com.netflix.exhibitor.spi.GlobalSharedConfig;
import com.netflix.exhibitor.spi.ServerInfo;
import java.util.Arrays;
import java.util.Collection;

public class MockGlobalSharedConfig implements GlobalSharedConfig
{
    private volatile Collection<BackupPath> backupPaths;
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

    @Override
    public Collection<BackupPath> getBackupPaths()
    {
        return backupPaths;
    }

    @Override
    public void setBackupPaths(Collection<BackupPath> newBackupPaths) throws Exception
    {
        this.backupPaths = newBackupPaths;
    }
}
