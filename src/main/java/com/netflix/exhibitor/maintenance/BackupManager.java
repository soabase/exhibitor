package com.netflix.exhibitor.maintenance;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.netflix.exhibitor.Exhibitor;
import com.netflix.exhibitor.activity.Activity;
import com.netflix.exhibitor.activity.ActivityLog;
import com.netflix.exhibitor.activity.QueueGroups;
import com.netflix.exhibitor.activity.RepeatingActivity;
import com.netflix.exhibitor.backup.BackupProcessor;
import com.netflix.exhibitor.spi.BackupSource;
import com.netflix.exhibitor.spi.BackupSpec;
import com.netflix.exhibitor.state.FourLetterWord;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BackupManager implements Closeable
{
    private final BackupSource          source;
    private final Exhibitor             exhibitor;
    private final RepeatingActivity     repeatingActivity;

    private static final String snapshotPrefix = "snapshot.";

    public BackupManager(Exhibitor exhibitor, BackupSource source)
    {
        this.exhibitor = exhibitor;
        this.source = source;
        Activity activity = new Activity()
        {
            @Override
            public void completed(boolean wasSuccessful)
            {
                // nop
            }

            @Override
            public Boolean call() throws Exception
            {
                doBackup();
                deleteOldBackups();
                return true;
            }
        };
        repeatingActivity = new RepeatingActivity(exhibitor.getActivityQueue(), QueueGroups.IO, activity, exhibitor.getConfig().getBackupPeriodMs());
    }

    public void     start()
    {
        repeatingActivity.start();
    }

    @Override
    public void close() throws IOException
    {
        repeatingActivity.close();
    }

    public BackupSource getSource()
    {
        return source;
    }

    public void forceBackup()
    {
        repeatingActivity.forceReQueue();
    }

    @VisibleForTesting
    protected boolean isLeader()
    {
        FourLetterWord stat = new FourLetterWord(FourLetterWord.Word.STAT, exhibitor.getConfig());
        return "leader".equalsIgnoreCase(stat.getResponseMap().get("mode"));
    }

    private void deleteOldBackups()
    {
        if ( (exhibitor.getConfig().getMaxBackups() == 0) || !exhibitor.backupCleanupEnabled() )
        {
            return;
        }

        Collection<BackupSpec>  specs = exhibitor.getBackupManager().getSource().getAvailableBackups();
        if ( specs == null )
        {
            return;
        }
        List<BackupSpec>        sortedSpecs = Lists.newArrayList(specs);
        Collections.sort
        (
            sortedSpecs,
            new Comparator<BackupSpec>()
            {
                @Override
                public int compare(BackupSpec o1, BackupSpec o2)
                {
                    return o2.getDate().compareTo(o1.getDate());    // oldest first
                }
            }
        );

        while ( sortedSpecs.size() > exhibitor.getConfig().getMaxBackups() )
        {
            BackupSpec      spec = sortedSpecs.remove(0);

            exhibitor.getLog().add(ActivityLog.Type.INFO, "Cleaning old backup: " + spec);
            try
            {
                exhibitor.getBackupManager().getSource().deleteBackup(spec);
            }
            catch ( Exception e )
            {
                exhibitor.getLog().add(ActivityLog.Type.ERROR, "Deleting old backup: " + spec, e);
            }
        }
    }

    private void doBackup()
    {
        exhibitor.getLog().add(ActivityLog.Type.INFO, "Starting backup");
        try
        {
            new BackupProcessor
            (
                exhibitor.getGlobalSharedConfig(),
                exhibitor.getLocalConnection(),
                exhibitor.getLog(),
                exhibitor.getBackupManager().getSource(),
                exhibitor.getConfig()
            ).execute();
            exhibitor.getLog().add(ActivityLog.Type.INFO, "Backup ended");
        }
        catch ( Exception e )
        {
            exhibitor.getLog().add(ActivityLog.Type.ERROR, "During backup", e);
        }
    }
}
