package com.netflix.exhibitor.state;

import com.netflix.exhibitor.Exhibitor;
import com.netflix.exhibitor.activity.Activity;
import com.netflix.exhibitor.activity.ActivityLog;
import com.netflix.exhibitor.activity.QueueGroups;
import com.netflix.exhibitor.activity.RepeatingActivity;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class MonitorRunningInstance implements Closeable
{
    private final Exhibitor                         exhibitor;
    private final AtomicLong                        notServingStartMs = new AtomicLong(0);
    private final AtomicReference<InstanceState>    currentInstanceState = new AtomicReference<InstanceState>();
    private final RepeatingActivity                 repeatingActivity;

    private static final int        ON_HOLD_FACTOR = 10;

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
            public void run()
            {
                doWork();
            }
        };
        repeatingActivity = new RepeatingActivity(exhibitor, QueueGroups.MAIN, activity, TimeUnit.MILLISECONDS.convert(exhibitor.getConfig().getCheckSeconds(), TimeUnit.SECONDS));
    }

    public void start()
    {
        repeatingActivity.start();
    }

    @Override
    public void close() throws IOException
    {
        repeatingActivity.close();
    }

    private void doWork()
    {
        InstanceState   instanceState = exhibitor.getInstanceStateManager().getInstanceState();
        if ( isOnHold(instanceState) )
        {
            exhibitor.getLog().add(ActivityLog.Type.INFO, "Start/restart is on hold...");
            return;
        }

        InstanceState localCurrentInstanceState = currentInstanceState.get();
        if ( !instanceState.equals(localCurrentInstanceState) )
        {
            boolean         serverListChange = (localCurrentInstanceState != null) && !localCurrentInstanceState.getServers().equals(instanceState.getServers());
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
                        notServingStartMs.set(System.currentTimeMillis());
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
                            exhibitor.getProcessOperations().startInstance(exhibitor, instanceState);
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

    private boolean       isOnHold(InstanceState instanceState)
    {
        long localMs = notServingStartMs.get();
        if ( localMs > 0 )
        {
            long        endOfHold = localMs + (ON_HOLD_FACTOR * exhibitor.getConfig().getCheckSeconds() * 1000);
            if ( (instanceState.getState() != InstanceStateTypes.SERVING) && (System.currentTimeMillis() < endOfHold) )
            {
                return true;
            }
            notServingStartMs.set(-1);
        }
        return false;
    }
}
