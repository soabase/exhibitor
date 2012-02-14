package com.netflix.exhibitor.core.backup;

import com.google.common.base.Optional;
import com.netflix.exhibitor.core.BackupProvider;
import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.activity.Activity;
import com.netflix.exhibitor.core.activity.ActivityLog;
import com.netflix.exhibitor.core.activity.QueueGroups;
import com.netflix.exhibitor.core.activity.RepeatingActivity;
import com.netflix.exhibitor.core.config.IntConfigs;
import com.netflix.exhibitor.core.index.ZookeeperLogFiles;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public class BackupManager implements Closeable
{
    private final Exhibitor exhibitor;
    private final Optional<BackupProvider> backupProvider;
    private final RepeatingActivity repeatingActivity;

    public BackupManager(Exhibitor exhibitor, BackupProvider backupProvider)
    {
        this.exhibitor = exhibitor;
        this.backupProvider = Optional.fromNullable(backupProvider);

        Activity activity = new Activity()
        {
            @Override
            public void completed(boolean wasSuccessful)
            {
            }

            @Override
            public Boolean call() throws Exception
            {
                doBackup();
                return true;
            }
        };
        // TODO - notice change in check ms
        repeatingActivity = new RepeatingActivity(exhibitor.getActivityQueue(), QueueGroups.IO, activity, exhibitor.getConfig().getInt(IntConfigs.BACKUP_PERIOD_MS));
    }

    public void start()
    {
        if ( isActive() )
        {
            repeatingActivity.start();
        }
    }

    @Override
    public void close() throws IOException
    {
        if ( isActive() )
        {
            repeatingActivity.close();
        }
    }

    public boolean isActive()
    {
        return backupProvider.isPresent();
    }

    private void doBackup() throws Exception
    {
        BackupProvider provider = backupProvider.get();

        exhibitor.getLog().add(ActivityLog.Type.INFO, "Backup starting");
        try
        {
            for ( File f : new ZookeeperLogFiles(exhibitor).getPaths() )
            {
                exhibitor.getLog().add(ActivityLog.Type.INFO, "Backing up: " + f);
                provider.backupFile(f);
            }
        }
        finally
        {
            exhibitor.getLog().add(ActivityLog.Type.INFO, "Backup complete");
        }
    }
}
