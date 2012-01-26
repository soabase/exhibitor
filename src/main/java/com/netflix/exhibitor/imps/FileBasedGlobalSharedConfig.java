package com.netflix.exhibitor.imps;

import com.google.common.base.Preconditions;
import com.google.common.io.Closeables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.netflix.exhibitor.InstanceConfig;
import com.netflix.exhibitor.activity.ActivityLog;
import com.netflix.exhibitor.spi.BackupPath;
import com.netflix.exhibitor.spi.ServerInfo;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.Collection;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class FileBasedGlobalSharedConfig implements GlobalSharedConfigBase
{
    private final File                              theFile;
    private final ActivityLog                       log;
    private final int                               checkPeriodMs;
    private final ExecutorService                   service = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(true).setNameFormat("FileBasedGlobalSharedConfig-%d").build());
    private final Random                            random;
    private final PropertyBasedGlobalSharedConfig   propertyConfig;

    public FileBasedGlobalSharedConfig(File theFile, InstanceConfig config, ActivityLog log)
    {
        this(theFile, config, log, (int)TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES));
    }

    public FileBasedGlobalSharedConfig(File theFile, InstanceConfig config, ActivityLog log, int checkPeriodMs)
    {
        this.theFile = theFile;
        this.log = log;
        random = new Random();
        this.checkPeriodMs = checkPeriodMs;
        propertyConfig = new PropertyBasedGlobalSharedConfig(log, config);
    }

    @Override
    public void start()
    {
        Preconditions.checkArgument(!service.isShutdown());

        readProperties();
        service.submit
        (
            new Runnable()
            {
                @Override
                public void run()
                {
                    while ( !Thread.currentThread().isInterrupted() )
                    {
                        try
                        {
                            sleep();
                            readProperties();
                        }
                        catch ( InterruptedException e )
                        {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        );
    }

    private void sleep() throws InterruptedException
    {
        Thread.sleep(checkPeriodMs + random.nextInt(checkPeriodMs));
    }

    @Override
    public void close() throws IOException
    {
        Preconditions.checkArgument(!service.isShutdown());

        service.shutdownNow();
    }

    @Override
    public Collection<ServerInfo> getServers()
    {
        return propertyConfig.getServers();
    }

    @Override
    public void setServers(Collection<ServerInfo> newServers) throws Exception
    {
        readProperties();

        propertyConfig.setServers(newServers);
        writeProperties(propertyConfig.getProperties());
    }

    @Override
    public Collection<BackupPath> getBackupPaths()
    {
        return propertyConfig.getBackupPaths();
    }

    @Override
    public void setBackupPaths(Collection<BackupPath> newBackupPaths) throws Exception
    {
        readProperties();

        propertyConfig.setBackupPaths(newBackupPaths);
        writeProperties(propertyConfig.getProperties());
    }

    private synchronized void writeProperties(Properties properties)
    {
        RandomAccessFile    raf = null;
        try
        {
            raf = new RandomAccessFile(theFile, "rw");  // always read/write

            for(;;)
            {
                try
                {
                    FileLock    lock = raf.getChannel().lock();
                    try
                    {
                        ByteArrayOutputStream   bytes = new ByteArrayOutputStream();
                        properties.store(bytes, "Auto-generated properties");
                        raf.setLength(0);
                        raf.seek(0);
                        raf.write(bytes.toByteArray());

                        break;
                    }
                    finally
                    {
                        lock.release();
                    }
                }
                catch ( OverlappingFileLockException ignore )
                {
                    log.add(ActivityLog.Type.INFO, "Shared property file in use - retrying write");
                    try
                    {
                        sleep();
                    }
                    catch ( InterruptedException e )
                    {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        catch ( Exception e )
        {
            log.add(ActivityLog.Type.ERROR, "Could not read shared property file: " + theFile, e);
        }
        finally
        {
            Closeables.closeQuietly(raf);
        }
    }

    private synchronized void readProperties()
    {
        if ( !theFile.exists() )
        {
            return;
        }

        RandomAccessFile    raf = null;
        try
        {
            raf = new RandomAccessFile(theFile, "rw");  // always read/write
            for(;;)
            {
                try
                {
                    FileLock    lock = raf.getChannel().lock();
                    try
                    {
                        byte[]      bytes = new byte[(int)theFile.length()];
                        raf.readFully(bytes);

                        Properties  newProperties = new Properties();
                        newProperties.load(new ByteArrayInputStream(bytes));
                        propertyConfig.setProperties(newProperties);

                        break;
                    }
                    finally
                    {
                        lock.release();
                    }
                }
                catch ( OverlappingFileLockException ignore )
                {
                    log.add(ActivityLog.Type.INFO, "Shared property file in use - retrying read");
                    try
                    {
                        sleep();
                    }
                    catch ( InterruptedException e )
                    {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        catch ( Exception e )
        {
            log.add(ActivityLog.Type.ERROR, "Could not read shared property file: " + theFile, e);
        }
        finally
        {
            Closeables.closeQuietly(raf);
        }
    }
}
