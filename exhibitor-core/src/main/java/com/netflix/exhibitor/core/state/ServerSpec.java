package com.netflix.exhibitor.core.state;

public class ServerSpec
{
    private final String    hostname;
    private final int       serverId;

    public ServerSpec(String hostname, int serverId)
    {
        this.hostname = hostname;
        this.serverId = serverId;
    }

    public String getHostname()
    {
        return hostname;
    }

    public int getServerId()
    {
        return serverId;
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

        ServerSpec that = (ServerSpec)o;

        if ( serverId != that.serverId )
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
        result = 31 * result + serverId;
        return result;
    }
}
