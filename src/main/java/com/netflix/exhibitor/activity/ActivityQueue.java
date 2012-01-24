package com.netflix.exhibitor.activity;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ActivityQueue implements Closeable
{
    private static final Logger log = LoggerFactory.getLogger(ActivityQueue.class);

    private final ExecutorService               service = Executors.newCachedThreadPool();
    private final Map<QueueGroups, DelayQueue<ActivityHolder>> queues;

    private static class ActivityHolder implements Delayed
    {
        private final Activity      activity;
        private final long          endMs;

        private ActivityHolder(Activity activity, long delayMs)
        {
            this.activity = activity;
            endMs = System.currentTimeMillis() + delayMs;
        }

        @Override
        public long getDelay(TimeUnit unit)
        {
            return unit.convert(endMs - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed rhs)
        {
            if ( rhs == this )
            {
                return 0;
            }

            long    diff = getDelay(TimeUnit.MILLISECONDS) - rhs.getDelay(TimeUnit.MILLISECONDS);
            return (diff == 0) ? 0 : ((diff < 0) ? -1 : 1);
        }

        @Override
        public boolean equals(Object o)
        {
            if ( this == o )
            {
                return true;
            }
            if ( o == null || getClass() != o.getClass() )
            {
                return false;
            }

            ActivityHolder holder = (ActivityHolder)o;

            if ( endMs != holder.endMs )
            {
                return false;
            }
            //noinspection RedundantIfStatement
            if ( !activity.equals(holder.activity) )
            {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = activity.hashCode();
            result = 31 * result + (int)(endMs ^ (endMs >>> 32));
            return result;
        }
    }

    public ActivityQueue()
    {
        ImmutableMap.Builder<QueueGroups, DelayQueue<ActivityHolder>>   builder = ImmutableMap.builder();
        for ( QueueGroups group : QueueGroups.values() )
        {
            builder.put(group, new DelayQueue<ActivityHolder>());
        }
        queues = builder.build();
    }

    public void start()
    {
        for ( QueueGroups group : QueueGroups.values() )
        {
            final DelayQueue<ActivityHolder>      thisQueue = queues.get(group);
            service.submit
            (
                new Runnable()
                {
                    @Override
                    public void run()
                    {
                        try
                        {
                            while ( !Thread.currentThread().isInterrupted() )
                            {
                                ActivityHolder holder = thisQueue.take();
                                try
                                {
                                    Boolean result = holder.activity.call();
                                    holder.activity.completed((result != null) && result);
                                }
                                catch ( Throwable e )
                                {
                                    log.error("Unhandled exception in background task", e);
                                }
                            }
                        }
                        catch ( InterruptedException dummy )
                        {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            );
        }
    }

    public synchronized void     replace(QueueGroups group, Activity activity)
    {
        replace(group, activity, 0, TimeUnit.MILLISECONDS);
    }

    public synchronized void     replace(QueueGroups group, Activity activity, long delay, TimeUnit unit)
    {
        ActivityHolder  holder = new ActivityHolder(activity, TimeUnit.MILLISECONDS.convert(delay, unit));
        DelayQueue<ActivityHolder> queue = queues.get(group);
        queue.remove(holder);
        queue.offer(holder);
    }

    public synchronized void     add(QueueGroups group, Activity activity)
    {
        add(group, activity, 0, TimeUnit.MILLISECONDS);
    }

    public synchronized void     add(QueueGroups group, Activity activity, long delay, TimeUnit unit)
    {
        ActivityHolder  holder = new ActivityHolder(activity, TimeUnit.MILLISECONDS.convert(delay, unit));
        queues.get(group).offer(holder);
    }

    @Override
    public void close() throws IOException
    {
        service.shutdownNow();
    }
}
