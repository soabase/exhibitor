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
            public void run()
            {
                try
                {
                    exhibitor.getProcessOperations().cleanupInstance(exhibitor);
                }
                catch ( Exception e )
                {
                    exhibitor.getLog().add(ActivityLog.Type.ERROR, "Doing cleanup", e);
                }
            }
        };
        repeatingActivity = new RepeatingActivity(exhibitor, QueueGroups.IO, activity, exhibitor.getConfig().getCleanupPeriodMs());
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
