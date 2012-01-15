package com.netflix.exhibitor.state;

import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;

public class InstanceState
{
    private final List<ServerInfo>      servers;
    private final int                   connectPort;
    private final int                   electPort;
    private final int                   serverId;
    private final InstanceStateTypes    state;
    private final InstanceStateTypes    rawState;

    InstanceState(Collection<ServerInfo> servers, int connectPort, int electPort, int serverId, InstanceStateTypes state, InstanceStateTypes rawState)
    {
        this.rawState = rawState;
        this.servers = ImmutableList.copyOf(servers);
        this.connectPort = connectPort;
        this.electPort = electPort;
        this.serverId = serverId;
        this.state = state;
    }

    public List<ServerInfo> getServers()
    {
        return servers;
    }

    public int getConnectPort()
    {
        return connectPort;
    }

    public int getElectPort()
    {
        return electPort;
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
        if ( electPort != that.electPort )
        {
            return false;
        }
        if ( serverId != that.serverId )
        {
            return false;
        }
        if ( !servers.equals(that.servers) )
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
        int result = servers.hashCode();
        result = 31 * result + connectPort;
        result = 31 * result + electPort;
        result = 31 * result + serverId;
        result = 31 * result + state.hashCode();
        return result;
    }
}
