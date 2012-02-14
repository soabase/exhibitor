package com.netflix.exhibitor.core.maintenance;

import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.activity.Activity;
import com.netflix.exhibitor.core.activity.ActivityLog;
import com.netflix.exhibitor.core.activity.QueueGroups;
import com.netflix.exhibitor.core.activity.RepeatingActivity;
import com.netflix.exhibitor.core.config.IntConfigs;
import com.netflix.exhibitor.core.state.ControlPanelTypes;
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
                if ( exhibitor.isControlPanelSettingEnabled(ControlPanelTypes.CLEANUP) )
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
        repeatingActivity = new RepeatingActivity(exhibitor.getActivityQueue(), QueueGroups.IO, activity, exhibitor.getConfig().getInt(IntConfigs.CLEANUP_PERIOD_MS));
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
