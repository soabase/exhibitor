package com.netflix.exhibitor.core.state;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class ServerList
{
    private final List<ServerSpec>      specs;

    public static class ServerSpec
    {
        private final String    hostname;
        private final int       serverId;

        ServerSpec(String hostname, int serverId)
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
                    String      trimmedHost = hostAndId[0].trim();
                    String      trimmedId = hostAndId[1].trim();
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
}
