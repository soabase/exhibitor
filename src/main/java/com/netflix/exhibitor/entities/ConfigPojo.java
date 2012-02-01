package com.netflix.exhibitor.entities;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ConfigPojo
{
    private String                  thisHostname;
    private int                     thisServerId;
    private String                  serversSpec;

    public ConfigPojo()
    {
        this("", "", -1);
    }

    public ConfigPojo(String serversSpec, String thisHostname, int thisServerId)
    {
        this.serversSpec = serversSpec;
        this.thisHostname = thisHostname;
        this.thisServerId = thisServerId;
    }

    public String getServersSpec()
    {
        return serversSpec;
    }

    public void setServersSpec(String serversSpec)
    {
        this.serversSpec = serversSpec;
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
