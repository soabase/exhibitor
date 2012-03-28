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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public class ServerList
{
    private final List<ServerSpec>      specs;

    public ServerList(String serverSpec)
    {
        ImmutableList.Builder<ServerSpec> builder = ImmutableList.builder();

        String[]        items = serverSpec.split(",");
        for ( String item : items )
        {
            String  trimmed = item.trim();
            if ( trimmed.length() > 0 )
            {
                String[]    hostAndId = trimmed.split(":");
                if ( hostAndId.length == 2 )
                {
                    String      trimmedId = hostAndId[0].trim();
                    String      trimmedHost = hostAndId[1].trim();
                    if ( (trimmedHost.length() > 0) && (trimmedId.length() > 0) )
                    {
                        try
                        {
                            int serverId = Integer.parseInt(trimmedId);
                            builder.add(new ServerSpec(trimmedHost, serverId));
                        }
                        catch ( NumberFormatException ignore )
                        {
                            // ignore
                        }
                    }
                }
            }
        }

        specs = builder.build();
    }
    
    public String toSpecString()
    {
        StringBuilder       str = new StringBuilder();
        for ( ServerSpec spec : specs )
        {
            if ( str.length() > 0 )
            {
                str.append(",");
            }
            str.append(spec.getServerId()).append(":").append(spec.getHostname());
        }

        return str.toString();
    }

    public List<ServerSpec> getSpecs()
    {
        return specs;
    }
    
    public Collection<String>   getHostnames()
    {
        return Lists.transform
        (
            specs,
            new Function<ServerSpec, String>()
            {
                @Override
                public String apply(ServerSpec spec)
                {
                    return spec.getHostname();
                }
            }
        );
    }

    public static Predicate<ServerSpec>  isUs(final String hostname)
    {
        return new Predicate<ServerSpec>()
        {
            @Override
            public boolean apply(ServerSpec spec)
            {
                return spec.getHostname().equals(hostname);
            }
        };
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

        ServerList that = (ServerList)o;

        //noinspection RedundantIfStatement
        if ( !specs.equals(that.specs) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return specs.hashCode();
    }
}
