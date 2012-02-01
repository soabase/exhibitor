package com.netflix.exhibitor.entities;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ConfigPojo
{
    private String                  thisHostname;
    private int                     thisServerId;

    public ConfigPojo()
    {
        this(-1, "");
    }

    public ConfigPojo
        (
            int thisServerId,
            String thisHostname
        )
    {
        this.thisHostname = thisHostname;
        this.thisServerId = thisServerId;
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
