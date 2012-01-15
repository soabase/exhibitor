package com.netflix.exhibitor.maintenance;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.Closeables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.netflix.exhibitor.ActivityLog;
import com.netflix.exhibitor.config.BackupConfig;
import com.netflix.exhibitor.config.InstanceConfig;
import com.netflix.exhibitor.state.FourLetterWord;
import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;

public class BackupManager implements Closeable
{
    private final String dataPath;
    private final BackupConfig backupConfig;
    private final BackupSource source;
    private final ActivityLog log;
    private final Lock lock;
    private final InstanceConfig instanceConfig;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("BackupManager-%d").build());

    private static final String snapshotPrefix = "snapshot.";

    public BackupManager(String dataPath, BackupConfig backupConfig, BackupSource source, ActivityLog log, Lock lock, InstanceConfig instanceConfig)
    {
        this.dataPath = dataPath;
        this.backupConfig = backupConfig;
        this.source = source;
        this.log = log;
        this.lock = lock;
        this.instanceConfig = instanceConfig;
    }

    public void     start()
    {
        executorService.submit
        (
            new Runnable()
            {
                @Override
                public void run()
                {
                    doWork();
                }
            }
        );
    }

    @Override
    public void close() throws IOException
    {
        executorService.shutdownNow();
    }

    private void backupRunner()
    {
        File            dir = new File(dataPath);
        if ( !dir.exists() )
        {
            log.add(ActivityLog.Type.ERROR, "Data directory not found: " + dataPath);
            return;
        }
        File[]          snapshots = dir.listFiles
        (
            new FileFilter()
            {
                @Override
                public boolean accept(File f)
                {
                    return f.getName().startsWith(snapshotPrefix);
                }
            }
        );
        
        for ( File f : snapshots )
        {
            try
            {
                InputStream     in = new BufferedInputStream(new FileInputStream(f));
                try
                {
                    source.backup(backupConfig, f.getName(), in);
                }
                finally
                {
                    Closeables.closeQuietly(in);
                }
            }
            catch ( Exception e )
            {
                log.add(ActivityLog.Type.ERROR, "Error backing up " + f.getPath(), e);
            }
        }
    }

    @VisibleForTesting
    protected boolean isLeader()
    {
        FourLetterWord stat = new FourLetterWord(FourLetterWord.Word.STAT, instanceConfig);
        return "leader".equalsIgnoreCase(stat.getResponseMap().get("mode"));
    }

    private void doWork()
    {
        try
        {
            while ( !Thread.currentThread().isInterrupted() )
            {
                Thread.sleep(backupConfig.getBackupPeriodMs());

                if ( isLeader() )
                {
                    doRotate();
                    doBackup();
                }
            }
        }
        catch ( InterruptedException e )
        {
            Thread.currentThread().interrupt();
        }
    }

    private void doRotate()
    {
        try
        {
            source.checkRotation(backupConfig);
        }
        catch ( Exception e )
        {
            log.add(ActivityLog.Type.ERROR, "Rotation error", e);
        }
    }

    private void doBackup()
    {
        lock.lock();
        try
        {
            log.add(ActivityLog.Type.INFO, "Starting backup");
            backupRunner();
            log.add(ActivityLog.Type.INFO, "Backup ended");
        }
        finally
        {
            lock.unlock();
        }
    }
}
