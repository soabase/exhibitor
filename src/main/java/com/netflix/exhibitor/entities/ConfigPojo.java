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

    public ConfigPojo()
    {
        this(null, -1, "");
    }

    public ConfigPojo
        (
            Collection<ServerPojo> servers,
            int thisServerId,
            String thisHostname
        )
    {
        this.thisHostname = thisHostname;
        this.servers = (servers != null) ? servers : Lists.<ServerPojo>newArrayList();
        this.thisServerId = thisServerId;
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

    public String getThisHostname()
    {
        return thisHostname;
    }

    public void setThisHostname(String thisHostname)
    {
        this.thisHostname = thisHostname;
    }
}
