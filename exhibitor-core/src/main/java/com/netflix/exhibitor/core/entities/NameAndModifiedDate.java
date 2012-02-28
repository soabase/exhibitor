package com.netflix.exhibitor.core.entities;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@SuppressWarnings("UnusedDeclaration")
public class NameAndModifiedDate
{
    private String  name;
    private long    modifiedDate;

    public NameAndModifiedDate()
    {
        this("", 0);
    }

    public NameAndModifiedDate(String name, long modifiedDate)
    {
        this.name = name;
        this.modifiedDate = modifiedDate;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public long getModifiedDate()
    {
        return modifiedDate;
    }

    public void setModifiedDate(long modifiedDate)
    {
        this.modifiedDate = modifiedDate;
    }
}
