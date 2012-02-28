package com.netflix.exhibitor.core.backup;

import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.activity.Activity;
import com.netflix.exhibitor.core.activity.ActivityLog;
import com.netflix.exhibitor.core.index.IndexActivity;
import com.netflix.exhibitor.core.index.IndexerUtil;
import java.io.File;

/**
 * activity for pulling down a backup and indexing it
 */
public class RestoreAndIndex implements Activity
{
    private final Exhibitor exhibitor;
    private final BackupMetaData backup;

    /**
     * @param exhibitor instance
     * @param backup the backup to restore
     */
    public RestoreAndIndex(Exhibitor exhibitor, BackupMetaData backup)
    {
        this.exhibitor = exhibitor;
        this.backup = backup;
    }

    @Override
    public void completed(boolean wasSuccessful)
    {
    }

    @Override
    public Boolean call() throws Exception
    {
        File        destinationFile = null;
        try
        {
            destinationFile = File.createTempFile("exhibitor", ".tmp");
            exhibitor.getBackupManager().restore(backup, destinationFile);

            final File        finalDestinationFile = destinationFile;
            IndexActivity.CompletionListener listener = new IndexActivity.CompletionListener()
            {
                @Override
                public void completed()
                {
                    deleteTempFile(finalDestinationFile);
                }
            };
            IndexerUtil.startIndexing(exhibitor, destinationFile, listener);
        }
        catch ( Exception e )
        {
            deleteTempFile(destinationFile);
            exhibitor.getLog().add(ActivityLog.Type.ERROR, "Could not complete restore/index: " + backup, e);
        }

        return true;
    }

    private void deleteTempFile(File f)
    {
        if ( !f.delete() )
        {
            exhibitor.getLog().add(ActivityLog.Type.ERROR, "Could not delete temp file: " + f);
        }
    }
}
