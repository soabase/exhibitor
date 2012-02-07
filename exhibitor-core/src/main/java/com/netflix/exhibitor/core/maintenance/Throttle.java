package com.netflix.exhibitor.core.maintenance;

import com.netflix.exhibitor.core.activity.ActivityLog;

// copied from https://github.com/Netflix/priam
public class Throttle
{
    private final String name;
    private final ThroughputFunction fun;
    private final ActivityLog log;

    // the bytes that had been handled the last time we delayed to throttle,
    // and the time in milliseconds when we last throttled
    private long bytesAtLastDelay;
    private long timeAtLastDelay;

    // current target bytes of throughput per millisecond
    private int targetBytesPerMS = -1;

    public Throttle(String name, ThroughputFunction fun, ActivityLog log)
    {
        this.name = name;
        this.fun = fun;
        this.log = log;
    }

    /** @param currentBytes Bytes of throughput since the beginning of the task. */
    public void throttle(long currentBytes)
    {
        throttleDelta(currentBytes - bytesAtLastDelay);
    }

    /** @param bytesDelta Bytes of throughput since the last call to throttle*(). */
    public void throttleDelta(long bytesDelta)
    {
        int newTargetBytesPerMS = fun.targetThroughput();
        if ( newTargetBytesPerMS < 1 )
        // throttling disabled
        {
            return;
        }

        // if the target changed, log
        if ( newTargetBytesPerMS != targetBytesPerMS )
        {
            log.add(ActivityLog.Type.INFO, String.format("%s target throughput now %d bytes/ms.", toString(), newTargetBytesPerMS));
        }
        targetBytesPerMS = newTargetBytesPerMS;

        // time passed since last delay
        long msSinceLast = System.currentTimeMillis() - timeAtLastDelay;
        // the excess bytes in this period
        long excessBytes = bytesDelta - msSinceLast * targetBytesPerMS;

        // the time to delay to recap the deficit
        long timeToDelay = excessBytes / Math.max(1, targetBytesPerMS);
        if (timeToDelay > 0)
        {
            log.add(ActivityLog.Type.INFO, String.format("%s actual throughput was %d bytes in %d ms: throttling for %d ms",
                this, bytesDelta, msSinceLast, timeToDelay));
            try
            {
                Thread.sleep(timeToDelay);
            }
            catch (InterruptedException e)
            {
                throw new AssertionError(e);
            }
        }
        bytesAtLastDelay += bytesDelta;
        timeAtLastDelay = System.currentTimeMillis();
    }

    @Override
    public String toString()
    {
        return "Throttle(for=" + name + ")";
    }
    
    public interface ThroughputFunction
    {
        /**
         * @return The instantaneous target throughput in bytes per millisecond. Targets less
         * than or equal to zero will disable throttling.
         */
        public int targetThroughput();
    }
}
