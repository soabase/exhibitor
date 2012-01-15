package com.netflix.exhibitor.state;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.netflix.exhibitor.config.InstanceConfig;
import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class InstanceStateManager implements Closeable
{
    private final AtomicLong                        currentVersion = new AtomicLong(0);
    private final Checker                           checker;
    private final InstanceConfig                    config;

    public InstanceStateManager(InstanceConfig config)
    {
        this.config = config;
        checker = new Checker(this);
    }

    void incrementVersion()
    {
        currentVersion.incrementAndGet();
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

    public InstanceState        getInstanceState()
    {
        String[]                servers = config.getServers().split(",");
        Iterable<ServerInfo>    cleanedServers = Iterables.transform
        (
            Arrays.asList(servers),
            new Function<String, ServerInfo>()
            {
                @Override
                public ServerInfo apply(String str)
                {
                    String hostname = str.toLowerCase().trim();
                    return new ServerInfo(hostname, config.getServerIdForHostname(hostname));
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
                    return info.getHostname().equals(config.getThisHostname());
                }
            }
        );

        int                 ourId = config.getServerIdForHostname(config.getThisHostname());
        InstanceStateTypes  state = (weAreInList && (ourId > 0)) ? checker.getState() : InstanceStateTypes.WAITING;
        return new InstanceState
        (
            cleanedServersList,
            config.getConnectPort(),
            config.getElectionPort(),
            ourId,
            state,
            checker.getState()
        );
    }

    public InstanceConfig getConfig()
    {
        return config;
    }
}
