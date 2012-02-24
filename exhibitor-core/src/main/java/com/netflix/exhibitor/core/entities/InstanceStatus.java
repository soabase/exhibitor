package com.netflix.exhibitor.core.entities;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@SuppressWarnings("UnusedDeclaration")
public class InstanceStatus
{
    private String    hostname;
    private int       serverId;
    private int       statusCode;

    public InstanceStatus()
    {
        this("", -1, 0);
    }

    public InstanceStatus(String hostname, int serverId, int statusCode)
    {
        this.hostname = hostname;
        this.serverId = serverId;
        this.statusCode = statusCode;
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

    public int getStatusCode()
    {
        return statusCode;
    }

    public void setStatusCode(int statusCode)
    {
        this.statusCode = statusCode;
    }
}
