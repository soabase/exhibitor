package com.netflix.exhibitor.core.entities;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class PathAnalysisRequest
{
    private int         max;
    private String      path;

    public PathAnalysisRequest()
    {
        this(0, "");
    }

    public PathAnalysisRequest(int max, String path)
    {
        this.max = max;
        this.path = path;
    }

    public int getMax()
    {
        return max;
    }

    public void setMax(int max)
    {
        this.max = max;
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
