package com.netflix.exhibitor.core.entities;

import com.google.common.collect.Lists;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class PathAnalysisNode
{
    private String          path;
    private int             max;
    private List<String>    childIds;

    public PathAnalysisNode()
    {
        this("", 1, Lists.<String>newArrayList());
    }

    public PathAnalysisNode(String path, int max, List<String> childIds)
    {
        this.path = path;
        this.max = max;
        this.childIds = childIds;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
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

    public List<String> getChildIds()
    {
        return childIds;
    }

    public void setChildIds(List<String> childIds)
    {
        this.childIds = childIds;
    }
}
