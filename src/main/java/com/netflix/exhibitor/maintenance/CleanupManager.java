package com.netflix.exhibitor.maintenance;

import com.netflix.exhibitor.Exhibitor;
import com.netflix.exhibitor.activity.Activity;
import com.netflix.exhibitor.activity.ActivityLog;
import com.netflix.exhibitor.activity.QueueGroups;
import com.netflix.exhibitor.activity.RepeatingActivity;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class CleanupManager implements Closeable
{
    private final RepeatingActivity repeatingActivity;
    private final AtomicBoolean     enabled = new AtomicBoolean(true);

    public CleanupManager(final Exhibitor exhibitor)
    {
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
                if ( enabled.get() )
                {
                    try
                    {
                        exhibitor.getProcessOperations().cleanupInstance();
                    }
                    catch ( Exception e )
                    {
                        exhibitor.getLog().add(ActivityLog.Type.ERROR, "Doing cleanup", e);
                    }
                }
                return true;
            }
        };

        // TODO - notice change in cleanup period
        repeatingActivity = new RepeatingActivity(exhibitor.getActivityQueue(), QueueGroups.IO, activity, exhibitor.getConfig().getCleanupPeriodMs());
    }

    public void start()
    {
        repeatingActivity.start();
    }

    public void close() throws IOException
    {
        repeatingActivity.close();
    }
    
    public void setEnable(boolean newValue)
    {
        enabled.set(newValue);
    }
    
    public boolean isEnabled()
    {
        return enabled.get();
    }
}
