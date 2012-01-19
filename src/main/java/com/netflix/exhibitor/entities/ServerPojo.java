package com.netflix.exhibitor.entities;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ServerPojo
{
    private String      hostname;
    private int         serverId;

    public ServerPojo()
    {
        this("", -1);
    }

    public ServerPojo(String hostname, int serverId)
    {
        this.hostname = hostname;
        this.serverId = serverId;
    }

    public String getHostname()
    {
        return hostname;
    }

    public void setHostname(String hostname)
    {
        this.hostname = hostname;
    }

    public int getServerId()
    {
        return serverId;
    }

    public void setServerId(int serverId)
    {
        this.serverId = serverId;
    }
}
