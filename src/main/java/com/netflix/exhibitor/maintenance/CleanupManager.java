package com.netflix.exhibitor.maintenance;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.netflix.exhibitor.ActivityLog;
import com.netflix.exhibitor.config.CleanupConfig;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

public class CleanupManager implements Closeable
{
    private final List<Timer>   timers = Lists.newArrayList();
    private final ActivityLog log;
    private final Lock          lock;
    private final CleanupConfig config;

    private static final int        LOG_BACKUP_COUNT = 3;   // TODO - make configurable

    public CleanupManager(ActivityLog log, Lock lock, CleanupConfig config)
    {
        this.log = log;
        this.lock = lock;
        this.config = config;
    }

    public void start()
    {
        Preconditions.checkArgument(timers.size() == 0);

        for ( Date time : parseTimes() )
        {
            Timer   timer = new Timer("Cleanup: " + time.toString(), true);
            timer.schedule
            (
                new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        lock.lock();
                        try
                        {
                            process();
                        }
                        finally
                        {
                            lock.unlock();
                        }
                    }
                },
                time,
                TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS)
            );
            timers.add(timer);
        }
    }

    public void close()
    {
        for ( Timer timer : timers )
        {
            timer.cancel();
        }
    }

    private void process()
    {
        // see http://zookeeper.apache.org/doc/r3.3.3/zookeeperAdmin.html#Ongoing+Data+Directory+Cleanup
        ProcessBuilder      builder = new ProcessBuilder
        (
            "java",
            "-cp",
            String.format("%s:%s:%s", config.getZookeeperJarPath(), config.getLog4jJarPath(), config.getConfDirPath()),
            "org.apache.zookeeper.server.PurgeTxnLog",
            config.getDataPath(),
            config.getDataPath(),
            "-n",
            Integer.toString(LOG_BACKUP_COUNT)
        );

        ExecutorService     errorService = Executors.newSingleThreadExecutor();
        StringWriter        errors = new StringWriter();
        final PrintWriter   errorOut = new PrintWriter(errors);
        try
        {
            Process             process = builder.start();
            final InputStream   errorStream = process.getErrorStream();
            errorService.submit
            (
                new Callable<Object>()
                {
                    public Object call() throws Exception
                    {
                        BufferedReader      in = new BufferedReader(new InputStreamReader(errorStream));
                        for(;;)
                        {
                            String  line = in.readLine();
                            if ( line == null )
                            {
                                break;
                            }
                            errorOut.println(line);
                        }
                        return null;
                    }
                }
            );
            process.waitFor();

            errorOut.close();
            String      errorStr = errors.toString();
            if ( errorStr.length() > 0 )
            {
                log.add(ActivityLog.Type.ERROR, "Cleanup task reported errors: " + errorStr);
            }
        }
        catch ( InterruptedException e )
        {
            Thread.currentThread().interrupt();
        }
        catch ( IOException e )
        {
            log.add(ActivityLog.Type.ERROR, "Error trying to do cleanup", e);
        }
        finally
        {
            errorService.shutdownNow();
        }
    }

    private Set<Date> parseTimes()
    {
        Set<Date>           cleanupTimes = Sets.newHashSet();
        SimpleDateFormat    formatter = new SimpleDateFormat("HH:mm");
        String[]            times = config.getTimesSpec().split(",");
        for ( String time : times )
        {
            try
            {
                Calendar    now = Calendar.getInstance();
                Calendar    date = Calendar.getInstance();
                date.setTime(formatter.parse(time));
                date.set(Calendar.MONTH, now.get(Calendar.MONTH));
                date.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));
                date.set(Calendar.YEAR, now.get(Calendar.YEAR));

                if ( now.after(date) )
                {
                    date.add(Calendar.DATE, 1);
                }

                cleanupTimes.add(date.getTime());
            }
            catch ( ParseException e )
            {
                log.add(ActivityLog.Type.ERROR, "Bad zookeeperserver.cleanup-time: " + time, e);
            }
        }
        return cleanupTimes;
    }
}
