package com.netflix.exhibitor.spi;

import java.util.Date;

/**
 * POJO that specifies a single backup
 */
public class BackupSpec
{
    private final String    path;
    private final Date      date;

    /**
     * @param path the backup path
     * @param date the date of the backup
     */
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
