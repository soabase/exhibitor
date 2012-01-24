package com.netflix.exhibitor.spi;

import java.util.Date;

public class BackupSpec
{
    private final String    path;
    private final Date      date;

    public BackupSpec(String path, Date date)
    {
        this.path = path;
        this.date = date;
    }

    public String getPath()
    {
        return path;
    }

    public Date getDate()
    {
        return date;
    }
}
