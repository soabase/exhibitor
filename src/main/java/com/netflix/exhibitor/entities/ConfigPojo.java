package com.netflix.exhibitor.entities;

import com.google.common.collect.Lists;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;

@XmlRootElement
public class ConfigPojo
{
    private Collection<ServerPojo>  servers;
    private String                  thisHostname;
    private int                     thisServerId;
    private Collection<String>      backupPaths;
    private Collection<BackupPojo>  availableRestores;

    public ConfigPojo()
    {
        this(null, -1, "", null, null);
    }

    public ConfigPojo
        (
            Collection<ServerPojo> servers,
            int thisServerId,
            String thisHostname,
            Collection<String> backupPaths,
            Collection<BackupPojo> availableRestores
        )
    {
        this.thisHostname = thisHostname;
        this.availableRestores = availableRestores;
        this.servers = (servers != null) ? servers : Lists.<ServerPojo>newArrayList();
        this.thisServerId = thisServerId;
        this.backupPaths = (backupPaths != null) ? backupPaths : Lists.<String>newArrayList();
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

    public Collection<String> getBackupPaths()
    {
        return backupPaths;
    }

    public void setBackupPaths(Collection<String> backupPaths)
    {
        this.backupPaths = backupPaths;
    }

    public Collection<BackupPojo> getAvailableRestores()
    {
        return availableRestores;
    }

    public void setAvailableRestores(Collection<BackupPojo> availableRestores)
    {
        this.availableRestores = availableRestores;
    }

    public String getThisHostname()
    {
        return thisHostname;
    }

    public void setThisHostname(String thisHostname)
    {
        this.thisHostname = thisHostname;
    }
}
