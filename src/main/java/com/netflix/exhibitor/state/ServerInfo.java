package com.netflix.exhibitor.state;

public class ServerInfo
{
    private final String            hostname;
    private final int               id;

    public ServerInfo(String hostname, int id)
    {
        this.hostname = hostname;
        this.id = id;
    }

    public String getHostname()
    {
        return hostname;
    }

    public int getId()
    {
        return id;
    }

    @Override
    public boolean equals(Object o)
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        ServerInfo that = (ServerInfo)o;

        if ( id != that.id )
        {
            return false;
        }
        //noinspection RedundantIfStatement
        if ( !hostname.equals(that.hostname) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = hostname.hashCode();
        result = 31 * result + id;
        return result;
    }
}
