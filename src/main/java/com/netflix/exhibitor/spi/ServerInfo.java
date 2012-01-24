package com.netflix.exhibitor.spi;

/**
 * POJO representing a server in the ensemble
 */
public class ServerInfo
{
    private final String            hostname;
    private final int               id;
    private final boolean           isThisServer;

    /**
     * @param hostname hostname of the server
     * @param id Server ID
     * @param thisServer true if this represents the instance this Exhibitor is monitoring
     */
    public ServerInfo(String hostname, int id, boolean thisServer)
    {
        this.hostname = hostname;
        this.id = id;
        isThisServer = thisServer;
    }

    public boolean isThisServer()
    {
        return isThisServer;
    }

    public String getHostname()
    {
        return hostname;
    }

    public int getId()
    {
        return id;
    }

    @SuppressWarnings("RedundantIfStatement")
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
        if ( isThisServer != that.isThisServer )
        {
            return false;
        }
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
        result = 31 * result + (isThisServer ? 1 : 0);
        return result;
    }
}
