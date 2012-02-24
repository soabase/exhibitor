package com.netflix.exhibitor.core.cluster;

import com.google.common.collect.ImmutableList;
import java.util.Collection;

public class VersionedServerSpecWithState
{
    private final Collection<ServerSpecWithState>       serverSpecWithState;
    private final long                                  version;

    public VersionedServerSpecWithState()
    {
        this(ImmutableList.<ServerSpecWithState>of(), 0);
    }

    public VersionedServerSpecWithState(Collection<ServerSpecWithState> serverSpecWithState, long version)
    {
        this.serverSpecWithState = serverSpecWithState;
        this.version = version;
    }

    public Collection<ServerSpecWithState> getServerSpecWithState()
    {
        return serverSpecWithState;
    }

    public long getVersion()
    {
        return version;
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

        VersionedServerSpecWithState that = (VersionedServerSpecWithState)o;

        if ( version != that.version )
        {
            return false;
        }
        //noinspection RedundantIfStatement
        if ( !serverSpecWithState.equals(that.serverSpecWithState) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = serverSpecWithState.hashCode();
        result = 31 * result + (int)(version ^ (version >>> 32));
        return result;
    }
}
