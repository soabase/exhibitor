package com.netflix.exhibitor.index;

import java.util.Date;

public class SearchItem
{
    private final int           type;
    private final String        path;
    private final int           version;
    private final Date          date;
    private final boolean       ephemeral;

    public SearchItem(int type, String path, int version, Date date, boolean ephemeral)
    {
        this.type = type;
        this.path = path;
        this.version = version;
        this.date = date;
        this.ephemeral = ephemeral;
    }

    public int getType()
    {
        return type;
    }

    public String getPath()
    {
        return path;
    }

    public int getVersion()
    {
        return version;
    }

    public Date getDate()
    {
        return date;
    }

    public boolean isEphemeral()
    {
        return ephemeral;
    }

    @Override
    public String toString()
    {
        return "SearchItem{" +
            "type=" + type +
            ", path='" + path + '\'' +
            ", version=" + version +
            ", date=" + date +
            ", ephemeral=" + ephemeral +
            '}';
    }
}
