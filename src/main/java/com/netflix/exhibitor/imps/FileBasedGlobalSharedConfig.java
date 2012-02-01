package com.netflix.exhibitor.imps;

import com.google.common.base.Preconditions;
import com.google.common.io.Closeables;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.netflix.exhibitor.InstanceConfig;
import com.netflix.exhibitor.activity.ActivityLog;
import com.netflix.exhibitor.pojos.ServerInfo;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
    private final File                              sharedFile;
    private final File                              localFile;
    private final ActivityLog                       log;
    private final int                               checkPeriodMs;
    private final ExecutorService                   service = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(true).setNameFormat("FileBasedGlobalSharedConfig-%d").build());
    private final Random                            random;
    private final PropertyBasedGlobalSharedConfig   propertyConfig;

    public FileBasedGlobalSharedConfig(File sharedFile, File localFile, InstanceConfig config, ActivityLog log)
    {
        this(sharedFile, localFile, config, log, (int)TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES));
    }

    public FileBasedGlobalSharedConfig(File sharedFile, File localFile, InstanceConfig config, ActivityLog log, int checkPeriodMs)
    {
        this.sharedFile = sharedFile;
        this.localFile = localFile;
        this.log = log;
        random = new Random();
        this.checkPeriodMs = checkPeriodMs;
        propertyConfig = new PropertyBasedGlobalSharedConfig(log, config);
    }

    @Override
    public void start()
    {
        Preconditions.checkArgument(!service.isShutdown());

        if ( !readProperties() )
        {
            tryReadLocalProperties();
        }

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
        Properties properties = propertyConfig.getProperties();
        writeProperties(properties);
        updateLocalFile(properties);
    }

    private synchronized void updateLocalFile(Properties properties)
    {
        if ( localFile == null )
        {
            return;
        }

        RandomAccessFile    raf = null;
        try
        {
            raf = new RandomAccessFile(localFile, "rw");
            writeToRaf(raf, properties);
        }
        catch ( Exception e )
        {
            log.add(ActivityLog.Type.ERROR, "Could not write local property file: " + localFile, e);
        }
        finally
        {
            try
            {
                if ( raf != null )
                {
                    raf.close();
                }
            }
            catch ( IOException ignore )
            {
                // ignore
            }
        }
    }

    private synchronized void writeProperties(Properties properties)
    {
        RandomAccessFile    raf = null;
        try
        {
            raf = new RandomAccessFile(sharedFile, "rw");  // always read/write

            for(;;)
            {
                try
                {
                    FileLock    lock = raf.getChannel().lock();
                    try
                    {
                        writeToRaf(raf, properties);
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
            log.add(ActivityLog.Type.ERROR, "Could not write shared property file: " + sharedFile, e);
        }
        finally
        {
            Closeables.closeQuietly(raf);
        }
    }

    private void writeToRaf(RandomAccessFile raf, Properties properties) throws IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        properties.store(bytes, "Auto-generated properties");
        raf.setLength(0);
        raf.seek(0);
        raf.write(bytes.toByteArray());
    }

    private void tryReadLocalProperties()
    {
        if ( (localFile != null) && localFile.exists() )
        {
            InputStream     in = null;
            try
            {
                in = new BufferedInputStream(new FileInputStream(localFile));
                Properties      newProperties = new Properties();
                newProperties.load(in);
                propertyConfig.setProperties(newProperties);
            }
            catch ( IOException e )
            {
                log.add(ActivityLog.Type.ERROR, "Could not read local property file: " + localFile, e);
            }
            finally
            {
                Closeables.closeQuietly(in);
            }
        }
    }

    private synchronized boolean readProperties()
    {
        if ( !sharedFile.exists() )
        {
            return false;
        }

        boolean             success = true;
        RandomAccessFile    raf = null;
        try
        {
            raf = new RandomAccessFile(sharedFile, "rw");  // always read/write
            for(;;)
            {
                try
                {
                    FileLock    lock = raf.getChannel().lock();
                    try
                    {
                        byte[]      bytes = new byte[(int)sharedFile.length()];
                        raf.readFully(bytes);

                        Properties  newProperties = new Properties();
                        newProperties.load(new ByteArrayInputStream(bytes));
                        propertyConfig.setProperties(newProperties);
                        updateLocalFile(newProperties);

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
                        success = false;
                        break;
                    }
                }
            }
        }
        catch ( Exception e )
        {
            log.add(ActivityLog.Type.ERROR, "Could not read shared property file: " + sharedFile, e);
            success = false;
        }
        finally
        {
            Closeables.closeQuietly(raf);
        }

        return success;
    }
}
