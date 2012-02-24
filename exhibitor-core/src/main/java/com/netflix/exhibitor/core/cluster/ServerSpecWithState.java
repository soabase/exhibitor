package com.netflix.exhibitor.core.cluster;

import com.netflix.exhibitor.core.state.InstanceStateTypes;

public class ServerSpecWithState implements Comparable<ServerSpecWithState>
{
    private final ServerSpec            spec;
    private final InstanceStateTypes    state;

    public ServerSpecWithState(ServerSpec spec, InstanceStateTypes state)
    {
        this.spec = spec;
        this.state = state;
    }

    public ServerSpec getSpec()
    {
        return spec;
    }

    public InstanceStateTypes getState()
    {
        return state;
    }

    @Override
    // Note: this class has a natural ordering that is inconsistent with equals.
    public int compareTo(ServerSpecWithState o)
    {
        int     diff = spec.getServerId() - o.spec.getServerId();
        return (diff < 0) ? -1 : ((diff > 0) ? 1 : 0);
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

        ServerSpecWithState that = (ServerSpecWithState)o;

        if ( !spec.equals(that.spec) )
        {
            return false;
        }
        if ( state != that.state )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = spec.hashCode();
        result = 31 * result + state.hashCode();
        return result;
    }
}
