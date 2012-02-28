package com.netflix.exhibitor.core.entities;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@SuppressWarnings("UnusedDeclaration")
public class NewIndexRequest
{
    private String type;
    private String value;
    private NameAndModifiedDate backup;

    public NewIndexRequest()
    {
        this("", "", null);
    }

    public NewIndexRequest(String type, String value, NameAndModifiedDate backup)
    {
        this.type = type;
        this.value = value;
        this.backup = backup;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

    public NameAndModifiedDate getBackup()
    {
        return backup;
    }

    public void setBackup(NameAndModifiedDate backup)
    {
        this.backup = backup;
    }
}
