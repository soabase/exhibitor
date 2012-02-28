package com.netflix.exhibitor.core.state;

import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.activity.Activity;
import com.netflix.exhibitor.core.activity.ActivityLog;

public class KillRunningInstance implements Activity
{
    private final Exhibitor exhibitor;
    private final boolean restart;

    public KillRunningInstance(Exhibitor exhibitor)
    {
        this(exhibitor, false);
    }

    public KillRunningInstance(Exhibitor exhibitor, boolean restart)
    {
        this.exhibitor = exhibitor;
        this.restart = restart;
    }

    @Override
    public void completed(boolean wasSuccessful)
    {
        if ( wasSuccessful && restart )
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

    @Override
    public Boolean call() throws Exception
    {
        exhibitor.getLog().add(ActivityLog.Type.INFO, "Attempting to stop instance");

        boolean     success = false;
        try
        {
            exhibitor.getProcessOperations().killInstance();
            success = true;
        }
        catch ( Exception e )
        {
            exhibitor.getLog().add(ActivityLog.Type.ERROR, "Trying to kill running instance", e);
        }
        return success;
    }
}
