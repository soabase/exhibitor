package com.netflix.exhibitor.activity;

import com.netflix.exhibitor.Exhibitor;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class RepeatingActivity implements Closeable
{
    private final Exhibitor     exhibitor;
    private final QueueGroups   group;
    private final AtomicBoolean isStarted = new AtomicBoolean(false);
    private final Activity      activity;
    private final long          timePeriodMs;

    public RepeatingActivity(Exhibitor exhibitor, QueueGroups group, final Activity actualActivity, long timePeriodMs)
    {
        this.exhibitor = exhibitor;
        this.group = group;
        this.activity = new Activity()
        {
            @Override
            public void completed(boolean wasSuccessful)
            {
                actualActivity.completed(wasSuccessful);
            }

            @Override
            public void run()
            {
                if ( isStarted.get() )
                {
                    actualActivity.run();
                    reQueue();
                }
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

    private void reQueue()
    {
        exhibitor.getActivityQueue().add(group, activity, timePeriodMs, TimeUnit.MILLISECONDS);
    }
}
