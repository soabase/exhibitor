package com.netflix.exhibitor.spi;

import java.util.Date;

public class BackupSpec
{
    private final String        name;
    private final Date          date;

    public BackupSpec(String name, Date date)
    {
        this.name = name;
        this.date = date;
    }

    public String getName()
    {
        return name;
    }

    public Date getDate()
    {
        return date;
    }
}
