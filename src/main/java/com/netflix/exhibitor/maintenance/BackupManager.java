package com.netflix.exhibitor.maintenance;

import com.google.common.annotations.VisibleForTesting;
import com.netflix.exhibitor.Exhibitor;
import com.netflix.exhibitor.activity.Activity;
import com.netflix.exhibitor.activity.ActivityLog;
import com.netflix.exhibitor.activity.QueueGroups;
import com.netflix.exhibitor.activity.RepeatingActivity;
import com.netflix.exhibitor.backup.BackupProcessor;
import com.netflix.exhibitor.spi.BackupSource;
import com.netflix.exhibitor.state.FourLetterWord;
import java.io.Closeable;
import java.io.IOException;

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
            public void run()
            {
                doBackup();
            }
        };
        repeatingActivity = new RepeatingActivity(exhibitor, QueueGroups.IO, activity, exhibitor.getConfig().getBackupPeriodMs());
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

    @VisibleForTesting
    protected boolean isLeader()
    {
        FourLetterWord stat = new FourLetterWord(FourLetterWord.Word.STAT, exhibitor.getConfig());
        return "leader".equalsIgnoreCase(stat.getResponseMap().get("mode"));
    }

    private void doBackup()
    {
        exhibitor.getLog().add(ActivityLog.Type.INFO, "Starting backup");
        try
        {
            new BackupProcessor(exhibitor).execute();
            exhibitor.getLog().add(ActivityLog.Type.INFO, "Backup ended");
        }
        catch ( Exception e )
        {
            exhibitor.getLog().add(ActivityLog.Type.ERROR, "During backup", e);
        }
    }
}
