package com.netflix.exhibitor.state;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.netflix.exhibitor.Exhibitor;
import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class InstanceStateManager implements Closeable
{
    private final Exhibitor         exhibitor;
    private final Checker           checker;

    public InstanceStateManager(Exhibitor exhibitor)
    {
        this.exhibitor = exhibitor;
        checker = new Checker(exhibitor, this);
    }

    public void start()
    {
        checker.start();
    }

    @Override
    public void close() throws IOException
    {
        checker.close();
    }

    public InstanceState getInstanceState()
    {
        String[]                servers = exhibitor.getConfig().getServers().split(",");
        Iterable<ServerInfo>    cleanedServers = Iterables.transform
        (
            Arrays.asList(servers),
            new Function<String, ServerInfo>()
            {
                @Override
                public ServerInfo apply(String str)
                {
                    String hostname = str.toLowerCase().trim();
                    return new ServerInfo(hostname, exhibitor.getConfig().getServerIdForHostname(hostname));
                }
            }
        );

        List<ServerInfo>    cleanedServersList = Lists.newArrayList(cleanedServers);
        boolean             weAreInList = Iterables.any
        (
            cleanedServers,
            new Predicate<ServerInfo>()
            {
                @Override
                public boolean apply(ServerInfo info)
                {
                    return info.getHostname().equals(exhibitor.getConfig().getThisHostname());
                }
            }
        );

        int                 ourId = exhibitor.getConfig().getServerIdForHostname(exhibitor.getConfig().getThisHostname());
        InstanceStateTypes state = (weAreInList && (ourId > 0)) ? checker.getState() : InstanceStateTypes.WAITING;
        return new InstanceState
        (
            cleanedServersList,
            exhibitor.getConfig().getConnectPort(),
            exhibitor.getConfig().getElectionPort(),
            ourId,
            state,
            checker.getState()
        );
    }
}
