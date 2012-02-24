package com.netflix.exhibitor.core.cluster;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.activity.Activity;
import com.netflix.exhibitor.core.activity.QueueGroups;
import com.netflix.exhibitor.core.activity.RepeatingActivity;
import com.netflix.exhibitor.core.config.ConfigListener;
import com.netflix.exhibitor.core.config.InstanceConfig;
import com.netflix.exhibitor.core.config.IntConfigs;
import com.netflix.exhibitor.core.config.StringConfigs;
import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class ClusterStatus implements Closeable
{
    private final RepeatingActivity     repeatingActivity;
    private final Exhibitor             exhibitor;
    private final ExecutorService       service;
    private final AtomicReference<VersionedServerSpecWithState>  currentStatus = new AtomicReference<VersionedServerSpecWithState>(new VersionedServerSpecWithState());

    public ClusterStatus(final Exhibitor exhibitor)
    {
        this.exhibitor = exhibitor;
        ConfigListener listener = new ConfigListener()
        {
            @Override
            public void configUpdated()
            {
                repeatingActivity.setTimePeriodMs(exhibitor.getConfigManager().getConfig().getInt(IntConfigs.CHECK_MS));
            }
        };
        exhibitor.getConfigManager().addConfigListener(listener);

        Activity        activity = new Activity()
        {
            @Override
            public void completed(boolean wasSuccessful)
            {
            }

            @Override
            public Boolean call() throws Exception
            {
                updateStatus();
                return true;
            }
        };
        repeatingActivity = new RepeatingActivity(exhibitor.getActivityQueue(), QueueGroups.IO, activity, exhibitor.getConfigManager().getConfig().getInt(IntConfigs.CHECK_MS));

        service = Executors.newCachedThreadPool();
    }

    public void start() throws Exception
    {
        updateStatus();
        repeatingActivity.start();
    }

    @Override
    public void close() throws IOException
    {
        service.shutdownNow();
        repeatingActivity.close();
    }

    public VersionedServerSpecWithState getCurrentStatus()
    {
        return currentStatus.get();
    }

    private void updateStatus() throws Exception
    {
        InstanceConfig      config = exhibitor.getConfigManager().getConfig();
        ServerList          serverList = new ServerList(config.getString(StringConfigs.SERVERS_SPEC));
        List<ServerSpec>    specs = Lists.newArrayList(serverList.getSpecs());
        ServerSpec          us = Iterables.find(specs, ServerList.isUs(exhibitor.getThisJVMHostname()), null);
        if ( us == null )
        {
            specs.add(new ServerSpec(exhibitor.getThisJVMHostname(), -1));
        }

        ExecutorCompletionService<ServerSpecWithState>      completionService = new ExecutorCompletionService<ServerSpecWithState>(service);
        for ( final ServerSpec spec : specs )
        {
            completionService.submit
            (
                new Callable<ServerSpecWithState>()
                {
                    @Override
                    public ServerSpecWithState call() throws Exception
                    {
                        String      hostname = spec.getHostname();
                        if ( hostname.equals(exhibitor.getThisJVMHostname()) )
                        {
                            hostname = "localhost";
                        }
                        Checker     checker = new Checker(exhibitor, hostname);
                        return new ServerSpecWithState(spec, checker.getState());
                    }
                }
            );
        }

        ImmutableSortedSet.Builder<ServerSpecWithState> builder = ImmutableSortedSet.naturalOrder();

        //noinspection ForLoopReplaceableByForEach
        for ( int i = 0; i < specs.size(); ++i )
        {
            ServerSpecWithState serverSpecWithState = completionService.take().get();
            builder.add(serverSpecWithState);
        }

        VersionedServerSpecWithState        localCurrentStatus = currentStatus.get();
        VersionedServerSpecWithState        potentialNewStatus = new VersionedServerSpecWithState(builder.build(), localCurrentStatus.getVersion());
        if ( !potentialNewStatus.equals(localCurrentStatus) )
        {
            currentStatus.set(new VersionedServerSpecWithState(builder.build(), localCurrentStatus.getVersion() + 1));
        }
    }
}
