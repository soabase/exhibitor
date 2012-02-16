package com.netflix.exhibitor.core.state;

import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.activity.Activity;
import com.netflix.exhibitor.core.activity.ActivityLog;
import com.netflix.exhibitor.core.activity.QueueGroups;
import com.netflix.exhibitor.core.activity.RepeatingActivity;
import com.netflix.exhibitor.core.config.ConfigListener;
import com.netflix.exhibitor.core.config.IntConfigs;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class MonitorRunningInstance implements Closeable
{
    private final Exhibitor                         exhibitor;
    private final AtomicLong                        onHoldStart = new AtomicLong(0);
    private final AtomicReference<InstanceState>    currentInstanceState = new AtomicReference<InstanceState>();
    private final RepeatingActivity                 repeatingActivity;

    public MonitorRunningInstance(Exhibitor exhibitor)
    {
        this.exhibitor = exhibitor;
        Activity activity = new Activity()
        {
            @Override
            public void completed(boolean wasSuccessful)
            {
                // NOP
            }

            @Override
            public Boolean call() throws Exception
            {
                doWork();
                return true;
            }
        };

        repeatingActivity = new RepeatingActivity(exhibitor.getActivityQueue(), QueueGroups.MAIN, activity, exhibitor.getConfig().getInt(IntConfigs.CHECK_MS));
    }

    public void start()
    {
        repeatingActivity.start();
        exhibitor.addConfigListener
        (
            new ConfigListener()
            {
                @Override
                public void configUpdated()
                {
                    repeatingActivity.setTimePeriodMs(exhibitor.getConfig().getInt(IntConfigs.CHECK_MS));
                }
            }
        );
    }

    @Override
    public void close() throws IOException
    {
        repeatingActivity.close();
    }

    private void doWork()
    {
        InstanceState   instanceState = exhibitor.getInstanceStateManager().getInstanceState();
        InstanceState   localCurrentInstanceState = currentInstanceState.get();
        if ( !instanceState.equals(localCurrentInstanceState) )
        {
            boolean         serverListChange = (localCurrentInstanceState != null) && !localCurrentInstanceState.getServerList().equals(instanceState.getServerList());
            currentInstanceState.set(instanceState);

            exhibitor.getLog().add(ActivityLog.Type.INFO, "State: " + instanceState.getStateDescription());

            if ( serverListChange )
            {
                exhibitor.getLog().add(ActivityLog.Type.INFO, "Server list has changed");
                restartZooKeeper(instanceState);
            }
            else
            {
                switch ( instanceState.getState() )
                {
                    case NOT_SERVING:
                    case UNKNOWN:
                    {
                        restartZooKeeper(instanceState);
                        break;
                    }

                    default:
                    {
                        // nop
                        break;
                    }
                }
            }
        }
    }

    private void restartZooKeeper(final InstanceState instanceState)
    {
        if ( !exhibitor.isControlPanelSettingEnabled(ControlPanelTypes.RESTARTS) )
        {
            return;
        }

        exhibitor.getActivityQueue().add
        (
            QueueGroups.MAIN,
            new KillRunningInstance(exhibitor)
            {
                @Override
                public void completed(boolean wasSuccessful)
                {
                    if ( wasSuccessful )
                    {
                        try
                        {
                            exhibitor.getProcessOperations().startInstance();
                        }
                        catch ( Exception e )
                        {
                            exhibitor.getLog().add(ActivityLog.Type.ERROR, "Monitoring instance", e);
                        }
                    }
                }
            }
        );
    }
}
