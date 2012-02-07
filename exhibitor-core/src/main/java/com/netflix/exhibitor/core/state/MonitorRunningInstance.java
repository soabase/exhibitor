package com.netflix.exhibitor.core.state;

import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.activity.Activity;
import com.netflix.exhibitor.core.activity.ActivityLog;
import com.netflix.exhibitor.core.activity.QueueGroups;
import com.netflix.exhibitor.core.activity.RepeatingActivity;
import java.io.Closeable;
import java.io.IOException;
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
            public Boolean call() throws Exception
            {
                doWork();
                return true;
            }
        };

        // TODO - notice change in check ms
        repeatingActivity = new RepeatingActivity(exhibitor.getActivityQueue(), QueueGroups.MAIN, activity, exhibitor.getConfig().getCheckMs());
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
        if ( !instanceState.equals(localCurrentInstanceState) || (instanceState.getState() == InstanceStateTypes.UNKNOWN) )
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
                        setNotServing();
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

    private void setNotServing()
    {
        notServingStartMs.set(System.currentTimeMillis());
    }

    private void restartZooKeeper(final InstanceState instanceState)
    {
        if ( !exhibitor.restartsAreEnabled() )
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
                        if ( instanceState.getState() == InstanceStateTypes.WAITING )
                        {
                            setNotServing();
                        }
                        else
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
            }
        );
    }

    private boolean       isOnHold(InstanceState instanceState)
    {
        long localMs = notServingStartMs.get();
        if ( localMs > 0 )
        {
            long        endOfHold = localMs + (ON_HOLD_FACTOR * exhibitor.getConfig().getCheckMs());
            if ( (instanceState.getState() != InstanceStateTypes.SERVING) && (System.currentTimeMillis() < endOfHold) )
            {
                return true;
            }
            notServingStartMs.set(-1);
        }
        return false;
    }
}
