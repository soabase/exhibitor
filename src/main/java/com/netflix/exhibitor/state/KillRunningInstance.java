package com.netflix.exhibitor.state;

import com.netflix.exhibitor.Exhibitor;
import com.netflix.exhibitor.activity.Activity;
import com.netflix.exhibitor.activity.ActivityLog;

public abstract class KillRunningInstance implements Activity
{
    private final Exhibitor exhibitor;

    protected KillRunningInstance(Exhibitor exhibitor)
    {
        this.exhibitor = exhibitor;
    }

    @Override
    public void run()
    {
        boolean     success = false;
        try
        {
            exhibitor.getProcessOperations().killInstance(exhibitor);
            success = true;
        }
        catch ( Exception e )
        {
            exhibitor.getLog().add(ActivityLog.Type.ERROR, "Trying to kill running instance", e);
        }
        completed(success);
    }
}
