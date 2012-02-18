package com.netflix.exhibitor.core.activity;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class RepeatingActivity implements Closeable
{
    private final QueueGroups   group;
    private final AtomicBoolean isStarted = new AtomicBoolean(false);
    private final Activity      activity;
    private final AtomicLong    timePeriodMs;
    private final ActivityQueue queue;

    public RepeatingActivity(ActivityQueue queue, QueueGroups group, final Activity actualActivity, long timePeriodMs)
    {
        this.queue = queue;
        this.group = group;
        this.activity = new Activity()
        {
            @Override
            public void completed(boolean wasSuccessful)
            {
                actualActivity.completed(wasSuccessful);
            }

            @Override
            public Boolean call() throws Exception
            {
                boolean     result = false;
                if ( isStarted.get() )
                {
                    result = actualActivity.call();
                    reQueue();
                }
                return result;
            }
        };
        this.timePeriodMs = new AtomicLong(timePeriodMs);
    }

    public void start()
    {
        isStarted.set(true);
        reQueue();
    }

    @Override
    public void close() throws IOException
    {
        isStarted.set(false);
    }

    public void setTimePeriodMs(long newTimePeriodMs)
    {
        timePeriodMs.set(newTimePeriodMs);
        queue.replace(group, activity, timePeriodMs.get(), TimeUnit.MILLISECONDS);
    }

    private void reQueue()
    {
        if ( timePeriodMs.get() > 0 )
        {
            queue.add(group, activity, timePeriodMs.get(), TimeUnit.MILLISECONDS);
        }
    }
}
