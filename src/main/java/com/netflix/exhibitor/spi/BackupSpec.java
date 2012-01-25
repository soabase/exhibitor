package com.netflix.exhibitor.spi;

import java.util.Date;

/**
 * POJO that specifies a single backup
 */
public class BackupSpec
{
    private final String    path;
    private final Date      date;
    private final String    userValue;

    /**
     * @param path the backup path
     * @param date the date of the backup
     * @param userValue any value the client needs
     */
    public BackupSpec(String path, Date date, String userValue)
    {
        this.path = path;
        this.date = date;
        this.userValue = userValue;
    }

    public String getPath()
    {
        return path;
    }

    public Date getDate()
    {
        return date;
    }

    public String getUserValue()
    {
        return userValue;
    }

    @Override
    public String toString()
    {
        return "BackupSpec{" +
            "path='" + path + '\'' +
            ", date=" + date +
            ", userValue='" + userValue + '\'' +
            '}';
    }
}
