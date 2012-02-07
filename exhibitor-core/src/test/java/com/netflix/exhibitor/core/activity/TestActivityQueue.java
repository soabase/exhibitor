package com.netflix.exhibitor.core.activity;

import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class TestActivityQueue
{
    @Test
    public void testReplace() throws Exception
    {
        ActivityQueue queue = new ActivityQueue();
        queue.start();
        try
        {
            final AtomicInteger     count = new AtomicInteger(0);
            final CountDownLatch    latch = new CountDownLatch(1);
            Activity                activity = new Activity()
            {
                @Override
                public void completed(boolean wasSuccessful)
                {
                }

                @Override
                public Boolean call() throws Exception
                {
                    count.incrementAndGet();
                    latch.countDown();
                    return true;
                }
            };

            queue.add(QueueGroups.MAIN, activity, 1, TimeUnit.MINUTES);
            queue.replace(QueueGroups.MAIN, activity);
            Assert.assertTrue(latch.await(5, TimeUnit.SECONDS));
            Assert.assertEquals(count.get(), 1);
        }
        finally
        {
            Closeables.closeQuietly(queue);
        }
    }

    @Test
    public void testRepeating() throws Exception
    {
        final int DELAY = 500;

        RepeatingActivity       repeating = null;
        ActivityQueue           queue = new ActivityQueue();
        queue.start();
        try
        {
            final List<Long>        times = Lists.newArrayList();
            final CountDownLatch    latch = new CountDownLatch(3);
            Activity                activity = new Activity()
            {
                @Override
                public void completed(boolean wasSuccessful)
                {
                }

                @Override
                public Boolean call() throws Exception
                {
                    times.add(System.currentTimeMillis());
                    latch.countDown();
                    return true;
                }
            };
            repeating = new RepeatingActivity(queue, QueueGroups.MAIN, activity, DELAY);
            repeating.start();

            long                    start = System.currentTimeMillis();
            Assert.assertTrue(latch.await(5, TimeUnit.SECONDS));
            repeating.close();

            Assert.assertTrue(times.size() >= 3);

            long        check = start;
            for ( int i = 0; i < 3; ++i )
            {
                long thisTime = times.get(i);
                long elapsed = thisTime - check;
                Assert.assertTrue(elapsed >= (DELAY - (DELAY / 10)), "elapsed: " + elapsed);
                check = thisTime;
            }
        }
        finally
        {
            Closeables.closeQuietly(repeating);
            Closeables.closeQuietly(queue);
        }
    }
    
    @Test
    public void testDelay() throws Exception
    {
        ActivityQueue queue = new ActivityQueue();
        queue.start();
        try
        {
            final AtomicLong        callTime = new AtomicLong();
            final CountDownLatch    latch = new CountDownLatch(1);
            Activity                activity = new Activity()
            {
                @Override
                public void completed(boolean wasSuccessful)
                {
                }

                @Override
                public Boolean call() throws Exception
                {
                    callTime.set(System.currentTimeMillis());
                    latch.countDown();
                    return true;
                }
            };

            long                    start = System.currentTimeMillis();
            queue.add(QueueGroups.MAIN, activity, 2, TimeUnit.SECONDS);
            Assert.assertTrue(latch.await(5, TimeUnit.SECONDS));
            Assert.assertTrue((callTime.get() - start) >= 2000);
        }
        finally
        {
            Closeables.closeQuietly(queue);
        }
    }

    @Test
    public void testBasic() throws Exception
    {
        ActivityQueue queue = new ActivityQueue();
        queue.start();
        try
        {
            final CountDownLatch    latch = new CountDownLatch(2);
            Activity                activity = new Activity()
            {
                @Override
                public void completed(boolean wasSuccessful)
                {
                    latch.countDown();
                }

                @Override
                public Boolean call() throws Exception
                {
                    latch.countDown();
                    return true;
                }
            };

            queue.add(QueueGroups.MAIN, activity);
            Assert.assertTrue(latch.await(5, TimeUnit.SECONDS));
        }
        finally
        {
            Closeables.closeQuietly(queue);
        }
    }
}
