package com.netflix.exhibitor.core.state;

public class InstanceState
{
    private final int                   connectPort;
    private final int                   electionPort;
    private final int                   serverId;
    private final InstanceStateTypes    state;
    private final ServerList            serverList;
    private final InstanceStateTypes    rawState;

    public InstanceState(ServerList serverList, int connectPort, int electionPort, int serverId, InstanceStateTypes state, InstanceStateTypes rawState)
    {
        this.serverList = serverList;
        this.rawState = rawState;
        this.connectPort = connectPort;
        this.electionPort = electionPort;
        this.serverId = serverId;
        this.state = state;
    }

    public ServerList getServerList()
    {
        return serverList;
    }

    public int getConnectPort()
    {
        return connectPort;
    }

    public int getElectionPort()
    {
        return electionPort;
    }

    public int getServerId()
    {
        return serverId;
    }

    public InstanceStateTypes getState()
    {
        return state;
    }

    public String   getStateDescription()
    {
        String  description = rawState.toString();
        if ( state == InstanceStateTypes.WAITING )
        {
            description += " (waiting)";
        }
        return description;
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

        InstanceState that = (InstanceState)o;

        if ( connectPort != that.connectPort )
        {
            return false;
        }
        if ( electionPort != that.electionPort )
        {
            return false;
        }
        if ( serverId != that.serverId )
        {
            return false;
        }
        if ( rawState != that.rawState )
        {
            return false;
        }
        if ( !serverList.equals(that.serverList) )
        {
            return false;
        }
        //noinspection RedundantIfStatement
        if ( state != that.state )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = connectPort;
        result = 31 * result + electionPort;
        result = 31 * result + serverId;
        result = 31 * result + state.hashCode();
        result = 31 * result + serverList.hashCode();
        result = 31 * result + rawState.hashCode();
        return result;
    }
}
