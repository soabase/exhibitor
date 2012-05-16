package com.netflix.exhibitor.core.entities;

import com.google.common.collect.Lists;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement
public class IdList
{
    private List<String> ids;

    public IdList()
    {
        this(Lists.<String>newArrayList());
    }

    public IdList(List<String> ids)
    {
        this.ids = ids;
    }

    public List<String> getIds()
    {
        return ids;
    }

    public void setIds(List<String> ids)
    {
        this.ids = ids;
    }
}
