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

public class CleanupManager implements Closeable
{
    private final RepeatingActivity repeatingActivity;
    private final Exhibitor exhibitor;

    public CleanupManager(final Exhibitor exhibitor)
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

        repeatingActivity = new RepeatingActivity(exhibitor.getActivityQueue(), QueueGroups.IO, activity, exhibitor.getConfigManager().getConfig().getInt(IntConfigs.CLEANUP_PERIOD_MS));
    }

    public void start()
    {
        repeatingActivity.start();
        exhibitor.getConfigManager().addConfigListener
        (
            new ConfigListener()
            {
                @Override
                public void configUpdated()
                {
                    repeatingActivity.setTimePeriodMs(exhibitor.getConfigManager().getConfig().getInt(IntConfigs.CLEANUP_PERIOD_MS));
                }
            }
        );
    }

    public void close() throws IOException
    {
        repeatingActivity.close();
    }
}
