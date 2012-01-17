package com.netflix.exhibitor.activity;

import com.google.common.collect.ImmutableMap;
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

            long    diff = getDelay(TimeUnit.NANOSECONDS) - rhs.getDelay(TimeUnit.NANOSECONDS);
            return (diff == 0) ? 0 : ((diff < 0) ? -1 : 1);
        }

        @Override
        public int hashCode()
        {
            return (int)endMs;
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

            ActivityHolder that = (ActivityHolder)o;
            return compareTo(that) == 0;
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
                                holder.activity.run();
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
    
    public void     add(QueueGroups group, Activity activity)
    {
        add(group, activity, 0, TimeUnit.MILLISECONDS);
    }

    public void     add(QueueGroups group, Activity activity, long delay, TimeUnit unit)
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
