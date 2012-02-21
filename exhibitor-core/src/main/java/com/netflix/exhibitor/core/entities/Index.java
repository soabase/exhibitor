package com.netflix.exhibitor.core.entities;

import javax.xml.bind.annotation.XmlRootElement;

@SuppressWarnings("UnusedDeclaration")
@XmlRootElement
public class Index
{
    private String  name;
    private String  from;
    private String  to;
    private int     entryCount;

    public Index()
    {
        this("", "", "", 0);
    }

    public Index(String name, String from, String to, int entryCount)
    {
        this.name = name;
        this.from = from;
        this.to = to;
        this.entryCount = entryCount;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getFrom()
    {
        return from;
    }

    public void setFrom(String from)
    {
        this.from = from;
    }

    public String getTo()
    {
        return to;
    }

    public void setTo(String to)
    {
        this.to = to;
    }

    public int getEntryCount()
    {
        return entryCount;
    }

    public void setEntryCount(int entryCount)
    {
        this.entryCount = entryCount;
    }
}
