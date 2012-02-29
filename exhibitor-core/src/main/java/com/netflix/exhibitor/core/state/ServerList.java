package com.netflix.exhibitor.core.state;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
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

    public List<ServerSpec> getSpecs()
    {
        return specs;
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
