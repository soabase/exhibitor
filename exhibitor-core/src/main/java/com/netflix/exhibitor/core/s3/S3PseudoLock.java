package com.netflix.exhibitor.core.s3;

import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import java.io.ByteArrayInputStream;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * An approximation of a lock using S3
 */
public class S3PseudoLock
{
    private final S3Client                  client;
    private final String                    bucket;
    private final String                    lockPrefix;
    private final int                       timeoutMs;
    private final String                    id;
    private final int                       pollingMs;
    private final int                       settlingMs;

    // all guarded by sync
    private long      lastUpdateMs = 0;
    private boolean   ownsTheLock;
    private String    key;

    private static final Random             random = new SecureRandom();

    private static final String     SEPARATOR = "_";
    private static final int        DEFAULT_SETTLING_MS = 5000;

    /**
     * @param client the S3 client
     * @param bucket the S3 bucket
     * @param lockPrefix key prefix
     * @param timeoutMs max age for locks
     * @param pollingMs how often to poll S3
     */
    public S3PseudoLock(S3Client client, String bucket, String lockPrefix, int timeoutMs, int pollingMs)
    {
        this(client, bucket, lockPrefix, timeoutMs, pollingMs, DEFAULT_SETTLING_MS);
    }

    /**
     * @param client the S3 client
     * @param bucket the S3 bucket
     * @param lockPrefix key prefix
     * @param timeoutMs max age for locks
     * @param pollingMs how often to poll S3
     * @param settlingMs how long to wait for S3 to reach consistency
     */
    public S3PseudoLock(S3Client client, String bucket, String lockPrefix, int timeoutMs, int pollingMs, int settlingMs)
    {
        this.settlingMs = settlingMs;
        Preconditions.checkArgument(!lockPrefix.contains(SEPARATOR), "lockPrefix cannot contain " + SEPARATOR);

        this.pollingMs = pollingMs;
        this.client = client;
        this.bucket = bucket;
        this.lockPrefix = lockPrefix;
        this.timeoutMs = timeoutMs;

        id = UUID.randomUUID().toString() + SEPARATOR + System.currentTimeMillis();
    }

    /**
     * Acquire the lock, blocking until it is acquired
     *
     * @throws Exception errors
     */
    public synchronized void  lock() throws Exception
    {
        lock(0, null);
    }

    /**
     * Acquire the lock, blocking at most <code>maxWait</code> until it is acquired
     *
     * @param maxWait max time to wait
     * @param unit time unit
     * @return true if the lock was acquired
     * @throws Exception errors
     */
    public synchronized boolean lock(long maxWait, TimeUnit unit) throws Exception
    {
        if ( ownsTheLock )
        {
            throw new IllegalStateException("Already locked");
        }

        key = lockPrefix + SEPARATOR + newRandomSequence();

        long        startMs = System.currentTimeMillis();
        boolean     hasMaxWait = (unit != null);
        long        maxWaitMs = hasMaxWait ? TimeUnit.MILLISECONDS.convert(maxWait, unit) : Long.MAX_VALUE;
        Preconditions.checkState(maxWaitMs >= settlingMs, String.format("The maxWait ms (%d) is less than the settling ms (%d)", maxWaitMs, settlingMs));

        ObjectMetadata      metadata = new ObjectMetadata();
        metadata.setContentLength(id.length());
        PutObjectRequest    request = new PutObjectRequest(bucket, key, new ByteArrayInputStream(id.getBytes()), metadata);
        client.putObject(request);

        for(;;)
        {
            checkUpdate();
            if ( ownsTheLock )
            {
                break;
            }
            long        thisWaitMs;
            if ( hasMaxWait )
            {
                long        elapsedMs = System.currentTimeMillis() - startMs;
                thisWaitMs = maxWaitMs - elapsedMs;
                if ( thisWaitMs <= 0 )
                {
                    break;
                }
            }
            else
            {
                thisWaitMs = pollingMs;
            }
            wait(Math.min(pollingMs, thisWaitMs));
        }
        return ownsTheLock;
    }

    /**
     * Release the lock
     *
     * @throws Exception errors
     */
    public synchronized void unlock() throws Exception
    {
        if ( !ownsTheLock )
        {
            throw new IllegalStateException("Already unlocked");
        }

        client.deleteObject(bucket, key);
        notifyAll();
    }

    private void checkUpdate() throws Exception
    {
        if ( (System.currentTimeMillis() - lastUpdateMs) < pollingMs )
        {
            return;
        }

        ListObjectsRequest  request = new ListObjectsRequest();
        request.setBucketName(bucket);
        request.setPrefix(lockPrefix);
        ObjectListing       objectListing = client.listObjects(request);

        List<String>        keys = Lists.transform
        (
            objectListing.getObjectSummaries(),
            new Function<S3ObjectSummary, String>()
            {
                @Override
                public String apply(S3ObjectSummary summary)
                {
                    return summary.getKey();
                }
            }
        );
        keys = cleanOldObjects(keys);
        Collections.sort(keys);

        if ( keys.size() > 0 )
        {
            String      lockerKey = keys.get(0);
            S3Object    object = client.getObject(bucket, lockerKey);
            if ( object != null )
            {
                byte[]      bytes = new byte[(int)object.getObjectMetadata().getContentLength()];
                ByteStreams.read(object.getObjectContent(), bytes, 0, bytes.length);
                String      lockerId = new String(bytes);
                long        lockerAge = System.nanoTime() - getNanoStampForKey(key);
                ownsTheLock = (lockerKey.equals(key) && lockerId.equals(id)) && (lockerAge >= TimeUnit.NANOSECONDS.convert(settlingMs, TimeUnit.MILLISECONDS));
            }
            else    // was deleted probably
            {
                ownsTheLock = false;
            }
        }
        else
        {
            throw new Exception("Our key is missing: " + key);
        }

        lastUpdateMs = System.currentTimeMillis();

        notifyAll();
    }

    private List<String>    cleanOldObjects(List<String> keys) throws Exception
    {
        List<String>        newKeys = Lists.newArrayList();
        for ( String key : keys )
        {
            long    nanoStamp = getNanoStampForKey(key);
            if ( (System.nanoTime() - nanoStamp) > TimeUnit.NANOSECONDS.convert(timeoutMs, TimeUnit.MILLISECONDS) )
            {
                client.deleteObject(bucket, key);
            }
            else
            {
                newKeys.add(key);
            }
        }
        return newKeys;
    }

    private static long getNanoStampForKey(String key)
    {
        String[]        parts = key.split(SEPARATOR);
        long            millisecondStamp = 0;
        try
        {
            millisecondStamp = Long.parseLong(parts[1]);
        }
        catch ( NumberFormatException ignore )
        {
            // ignore
        }
        return millisecondStamp;
    }

    private String newRandomSequence()
    {
        return "" + System.nanoTime() + SEPARATOR + Math.abs(random.nextLong());
    }
}
