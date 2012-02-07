package com.netflix.exhibitor.core.entities;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class NewIndexRequest
{
    private String      path;

    public NewIndexRequest()
    {
        this("");
    }

    public NewIndexRequest(String path)
    {
        this.path = path;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }
}
