package com.netflix.exhibitor.core.activity;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class RepeatingActivity implements Closeable
{
    private final QueueGroups   group;
    private final AtomicBoolean isStarted = new AtomicBoolean(false);
    private final Activity      activity;
    private final long          timePeriodMs;
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
        this.timePeriodMs = timePeriodMs;
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

    public void forceReQueue()
    {
        queue.replace(group, activity);
    }

    private void reQueue()
    {
        queue.add(group, activity, timePeriodMs, TimeUnit.MILLISECONDS);
    }
}
