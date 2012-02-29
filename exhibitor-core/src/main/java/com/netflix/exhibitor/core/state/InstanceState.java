package com.netflix.exhibitor.core.state;

class InstanceState
{
    private final InstanceStateTypes    state;
    private final ServerList            serverList;

    InstanceState(ServerList serverList, InstanceStateTypes state)
    {
        this.serverList = serverList;
        this.state = state;
    }

    ServerList getServerList()
    {
        return serverList;
    }

    InstanceStateTypes getState()
    {
        return state;
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

        InstanceState that = (InstanceState)o;

        if ( !serverList.equals(that.serverList) )
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
        int result = state.hashCode();
        result = 31 * result + serverList.hashCode();
        return result;
    }
}
