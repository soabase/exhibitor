package com.netflix.exhibitor.maintenance;

import com.netflix.exhibitor.Exhibitor;
import com.netflix.exhibitor.activity.Activity;
import com.netflix.exhibitor.activity.ActivityLog;
import com.netflix.exhibitor.activity.QueueGroups;
import com.netflix.exhibitor.activity.RepeatingActivity;
import java.io.Closeable;
import java.io.IOException;

public class CleanupManager implements Closeable
{
    private final RepeatingActivity repeatingActivity;

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
                try
                {
                    exhibitor.getProcessOperations().cleanupInstance();
                }
                catch ( Exception e )
                {
                    exhibitor.getLog().add(ActivityLog.Type.ERROR, "Doing cleanup", e);
                }
                return true;
            }
        };
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
}
