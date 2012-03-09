/*
 *
 *  Copyright 2011 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

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
