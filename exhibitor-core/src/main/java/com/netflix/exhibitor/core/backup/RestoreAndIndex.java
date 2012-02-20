package com.netflix.exhibitor.core.backup;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.netflix.curator.test.DirectoryUtils;
import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.activity.Activity;
import com.netflix.exhibitor.core.activity.ActivityLog;
import com.netflix.exhibitor.core.index.IndexActivity;
import com.netflix.exhibitor.core.index.IndexerUtil;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * activity for pulling down a backup and indexing it
 */
public class RestoreAndIndex implements Activity
{
    private final Exhibitor exhibitor;
    private final String backupKeyName;

    /**
     * @param exhibitor instance
     * @param backupKeyName the backup key
     */
    public RestoreAndIndex(Exhibitor exhibitor, String backupKeyName)
    {
        this.exhibitor = exhibitor;
        this.backupKeyName = backupKeyName;
    }

    @Override
    public void completed(boolean wasSuccessful)
    {
    }

    @Override
    public Boolean call() throws Exception
    {
        Collection<SessionAndName>    sessionAndNames = exhibitor.getBackupManager().getAvailableSession();
        SessionAndName foundSessionAndName = Iterables.find
        (
            sessionAndNames,
            new Predicate<SessionAndName>()
            {
                @Override
                public boolean apply(SessionAndName sessionAndName)
                {
                    return sessionAndName.name.equals(backupKeyName);
                }
            },
            null
        );

        Collection<String>        backupKeys = (foundSessionAndName != null) ? exhibitor.getBackupManager().findKeyForSession(foundSessionAndName.session) : Lists.<String>newArrayList();
        if ( backupKeys.size() == 0 )
        {
            exhibitor.getLog().add(ActivityLog.Type.ERROR, "Backup not found - it was probably cleaned up: " + backupKeyName);
            return false;
        }

        final File                      tempDirectory = Files.createTempDir();
        try
        {
            for ( String key : backupKeys )
            {
                File        destinationFile = File.createTempFile("exhibitor", ".tmp", tempDirectory);
                exhibitor.getBackupManager().restoreKey(key, destinationFile);
            }

            IndexActivity.CompletionListener listener = new IndexActivity.CompletionListener()
            {
                @Override
                public void completed()
                {
                    deleteTempDirectory(tempDirectory);
                }
            };
            IndexerUtil.startIndexing(exhibitor, tempDirectory.getAbsoluteFile(), listener);
        }
        catch ( Exception e )
        {
            deleteTempDirectory(tempDirectory);
            exhibitor.getLog().add(ActivityLog.Type.ERROR, "Could not complete restore/index: " + backupKeyName, e);
        }

        return true;
    }

    private void deleteTempDirectory(File tempDirectory)
    {
        try
        {
            DirectoryUtils.deleteRecursively(tempDirectory.getCanonicalFile());
        }
        catch ( IOException e )
        {
            exhibitor.getLog().add(ActivityLog.Type.ERROR, "Could not delete temp directory: " + tempDirectory);
        }
    }
}
