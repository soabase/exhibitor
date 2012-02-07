package com.netflix.exhibitor.core.state;

import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.activity.Activity;
import com.netflix.exhibitor.core.activity.ActivityLog;

public abstract class KillRunningInstance implements Activity
{
    private final Exhibitor exhibitor;

    protected KillRunningInstance(Exhibitor exhibitor)
    {
        this.exhibitor = exhibitor;
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
