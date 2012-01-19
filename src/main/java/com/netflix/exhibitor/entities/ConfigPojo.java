package com.netflix.exhibitor.entities;

import com.google.common.collect.Lists;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;

@XmlRootElement
public class ConfigPojo
{
    private Collection<ServerPojo>  servers;
    private int                     thisServerId;
    private int                     checkSeconds;
    private int                     backupPeriodMs;
    private int                     cleanupPeriodMs;
    private int                     maxBackups;

    public ConfigPojo()
    {
        this(Lists.<ServerPojo>newArrayList(), -1, 0, 0, 0, 0);
    }

    public ConfigPojo(Collection<ServerPojo> servers, int thisServerId, int checkSeconds, int backupPeriodMs, int cleanupPeriodMs, int maxBackups)
    {
        this.servers = servers;
        this.thisServerId = thisServerId;
        this.checkSeconds = checkSeconds;
        this.backupPeriodMs = backupPeriodMs;
        this.cleanupPeriodMs = cleanupPeriodMs;
        this.maxBackups = maxBackups;
    }

    public Collection<ServerPojo> getServers()
    {
        return servers;
    }

    public void setServers(Collection<ServerPojo> servers)
    {
        this.servers = servers;
    }

    public int getThisServerId()
    {
        return thisServerId;
    }

    public void setThisServerId(int thisServerId)
    {
        this.thisServerId = thisServerId;
    }

    public int getCheckSeconds()
    {
        return checkSeconds;
    }

    public void setCheckSeconds(int checkSeconds)
    {
        this.checkSeconds = checkSeconds;
    }

    public int getBackupPeriodMs()
    {
        return backupPeriodMs;
    }

    public void setBackupPeriodMs(int backupPeriodMs)
    {
        this.backupPeriodMs = backupPeriodMs;
    }

    public int getCleanupPeriodMs()
    {
        return cleanupPeriodMs;
    }

    public void setCleanupPeriodMs(int cleanupPeriodMs)
    {
        this.cleanupPeriodMs = cleanupPeriodMs;
    }

    public int getMaxBackups()
    {
        return maxBackups;
    }

    public void setMaxBackups(int maxBackups)
    {
        this.maxBackups = maxBackups;
    }
}
