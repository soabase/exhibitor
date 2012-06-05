package com.netflix.exhibitor.core.entities;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class UsageListingRequest
{
    private int         maxChildrenForTraversal;
    private String      startPath;

    public UsageListingRequest()
    {
        this("/", Integer.MAX_VALUE);
    }

    public UsageListingRequest(String startPath, int maxChildrenForTraversal)
    {
        this.maxChildrenForTraversal = maxChildrenForTraversal;
        this.startPath = startPath;
    }

    public int getMaxChildrenForTraversal()
    {
        return maxChildrenForTraversal;
    }

    public void setMaxChildrenForTraversal(int maxChildrenForTraversal)
    {
        this.maxChildrenForTraversal = maxChildrenForTraversal;
    }

    public String getStartPath()
    {
        return startPath;
    }

    public void setStartPath(String startPath)
    {
        this.startPath = startPath;
    }
}
