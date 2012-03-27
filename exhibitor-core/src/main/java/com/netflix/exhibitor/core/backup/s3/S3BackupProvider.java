/*
 *
 *  Copyright 2011 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.netflix.exhibitor.core.backup.s3;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.model.*;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import com.netflix.curator.RetryPolicy;
import com.netflix.curator.retry.ExponentialBackoffRetry;
import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.backup.BackupConfigSpec;
import com.netflix.exhibitor.core.backup.BackupMetaData;
import com.netflix.exhibitor.core.backup.BackupProvider;
import com.netflix.exhibitor.core.s3.S3Client;
import com.netflix.exhibitor.core.s3.S3ClientFactory;
import com.netflix.exhibitor.core.s3.S3Credential;
import com.netflix.exhibitor.core.s3.S3Utils;
import org.apache.zookeeper.server.ByteBufferInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.netflix.exhibitor.core.config.DefaultProperties.asInt;

// liberally copied and modified from Priam
public class S3BackupProvider implements BackupProvider
{
    // TODO - add logging

    private final AtomicReference<S3Client> s3Client = new AtomicReference<S3Client>();
    private final Compressor  compressor;
    private final S3ClientFactory factory;

    private static final BackupConfigSpec CONFIG_THROTTLE = new BackupConfigSpec("throttle", "Throttle (bytes/ms)", "Data throttling. Maximum bytes per millisecond.", Integer.toString(1024 * 1024), BackupConfigSpec.Type.INTEGER);
    private static final BackupConfigSpec CONFIG_BUCKET = new BackupConfigSpec("bucket-name", "S3 Bucket Name", "The S3 bucket to use", "", BackupConfigSpec.Type.STRING);
    private static final BackupConfigSpec CONFIG_KEY_PREFIX = new BackupConfigSpec("key-prefix", "S3 Key Prefix", "The prefix for S3 backup keys", "exhibitor-backup-", BackupConfigSpec.Type.STRING);
    private static final BackupConfigSpec CONFIG_MAX_RETRIES = new BackupConfigSpec("max-retries", "Max Retries", "Maximum retries when uploading/downloading S3 data", "3", BackupConfigSpec.Type.INTEGER);
    private static final BackupConfigSpec CONFIG_RETRY_SLEEP_MS = new BackupConfigSpec("retry-sleep-ms", "Retry Sleep (ms)", "Sleep time in milliseconds when retrying", "1000", BackupConfigSpec.Type.INTEGER);

    private static final List<BackupConfigSpec>     CONFIGS = Arrays.asList(CONFIG_THROTTLE, CONFIG_BUCKET, CONFIG_KEY_PREFIX, CONFIG_MAX_RETRIES, CONFIG_RETRY_SLEEP_MS);

    @VisibleForTesting
    static final String       SEPARATOR = "|";
    private static final String       SEPARATOR_REPLACEMENT = "_";

    public S3BackupProvider(S3ClientFactory factory, S3Credential credential) throws Exception
    {
        this.factory = factory;
        this.compressor = new GzipCompressor();
        changeCredentials(credential);
    }

    /**
     * Change the AWS credentials
     *
     * @param credential new credentials
     * @throws Exception errors
     */
    public void     changeCredentials(S3Credential credential) throws Exception
    {
        BasicAWSCredentials credentials = new BasicAWSCredentials(credential.getAccessKeyId(), credential.getSecretAccessKey());
        s3Client.set(factory.makeNewClient(credentials));
    }

    @Override
    public List<BackupConfigSpec> getConfigs()
    {
        return CONFIGS;
    }

    @Override
    public boolean isValidConfig(Exhibitor exhibitor, Map<String, String> configValues)
    {
        String bucket = configValues.get(CONFIG_BUCKET.getKey());
        return (bucket != null) && (bucket.trim().length() > 0);
    }

    @Override
    public UploadResult uploadBackup(Exhibitor exhibitor, BackupMetaData backup, File source, final Map<String, String> configValues) throws Exception
    {
        List<BackupMetaData>    availableBackups = getAvailableBackups(exhibitor, configValues);
        if ( availableBackups.contains(backup) )
        {
            return UploadResult.DUPLICATE;
        }

        RetryPolicy retryPolicy = makeRetryPolicy(configValues);
        Throttle    throttle = makeThrottle(configValues);

        String                          key = toKey(backup, configValues);
        InitiateMultipartUploadRequest  initRequest = new InitiateMultipartUploadRequest(configValues.get(CONFIG_BUCKET.getKey()), key);
        InitiateMultipartUploadResult   initResponse = s3Client.get().initiateMultipartUpload(initRequest);

        CompressorIterator      compressorIterator = compressor.compress(source);
        try
        {
            List<PartETag>      eTags = Lists.newArrayList();
            int                 index = 1;
            for(;;)
            {
                ByteBuffer  chunk = compressorIterator.next();
                if ( chunk == null )
                {
                    break;
                }
                throttle.throttle(chunk.limit());

                PartETag eTag = uploadChunkWithRetry(chunk, initResponse, index++, retryPolicy);
                eTags.add(eTag);
            }

            completeUpload(initResponse, eTags);
        }
        catch ( Exception e )
        {
            abortUpload(initResponse);
            throw e;
        }
        finally
        {
            Closeables.closeQuietly(compressorIterator);
        }

        UploadResult        result = UploadResult.SUCCEEDED;
        for ( BackupMetaData existing : availableBackups )
        {
            if ( existing.getName().equals(backup.getName()) )
            {
                deleteBackup(exhibitor, existing, configValues);
                result = UploadResult.REPLACED_OLD_VERSION;
            }
        }
        return result;
    }

    @Override
    public void downloadBackup(Exhibitor exhibitor, BackupMetaData backup, File destination, Map<String, String> configValues) throws Exception
    {
        S3Object        object = s3Client.get().getObject(configValues.get(CONFIG_BUCKET.getKey()), toKey(backup, configValues));

        long            startMs = System.currentTimeMillis();
        RetryPolicy     retryPolicy = makeRetryPolicy(configValues);
        int             retryCount = 0;
        boolean         done = false;
        while ( !done )
        {
            Throttle            throttle = makeThrottle(configValues);
            InputStream         in = null;
            FileOutputStream    out = null;
            try
            {
                out = new FileOutputStream(destination);
                in = object.getObjectContent();

                FileChannel             channel = out.getChannel();
                CompressorIterator      compressorIterator = compressor.decompress(in);
                for(;;)
                {
                    ByteBuffer bytes = compressorIterator.next();
                    if ( bytes == null )
                    {
                        break;
                    }

                    throttle.throttle(bytes.limit());
                    channel.write(bytes);
                }

                done = true;
            }
            catch ( Exception e )
            {
                if ( !retryPolicy.allowRetry(retryCount++, System.currentTimeMillis() - startMs) )
                {
                    done = true;
                }
            }
            finally
            {
                Closeables.closeQuietly(in);
                Closeables.closeQuietly(out);
            }
        }
    }

    @Override
    public List<BackupMetaData> getAvailableBackups(Exhibitor exhibitor, Map<String, String> configValues) throws Exception
    {
        ListObjectsRequest      request = new ListObjectsRequest();
        request.setBucketName(configValues.get(CONFIG_BUCKET.getKey()));
        ObjectListing           listing = s3Client.get().listObjects(request);

        final String                keyPrefix = getKeyPrefix(configValues);
        Iterable<S3ObjectSummary>   filtered = Iterables.filter
        (
            listing.getObjectSummaries(),
            new Predicate<S3ObjectSummary>()
            {
                @Override
                public boolean apply(S3ObjectSummary summary)
                {
                    return summary.getKey().startsWith(keyPrefix);
                }
            }
        );

        return Lists.transform
        (
            Lists.newArrayList(filtered),
            new Function<S3ObjectSummary, BackupMetaData>()
            {
                @Override
                public BackupMetaData apply(S3ObjectSummary summary)
                {
                    return fromKey(summary.getKey());
                }
            }
        );
    }

    @Override
    public void deleteBackup(Exhibitor exhibitor, BackupMetaData backup, Map<String, String> configValues) throws Exception
    {
        s3Client.get().deleteObject(configValues.get(CONFIG_BUCKET.getKey()), toKey(backup, configValues));
    }

    private Throttle makeThrottle(final Map<String, String> configValues)
    {
        return new Throttle(this.getClass().getCanonicalName(), new Throttle.ThroughputFunction()
        {
            public int targetThroughput()
            {
                return Math.max(asInt(configValues.get(CONFIG_THROTTLE.getKey())), Integer.MAX_VALUE);
            }
        });
    }

    private ExponentialBackoffRetry makeRetryPolicy(Map<String, String> configValues)
    {
        return new ExponentialBackoffRetry(asInt(configValues.get(CONFIG_RETRY_SLEEP_MS.getKey())), asInt(configValues.get(CONFIG_MAX_RETRIES.getKey())));
    }

    private PartETag uploadChunkWithRetry(ByteBuffer bytes, InitiateMultipartUploadResult initResponse, int index, RetryPolicy retryPolicy) throws Exception
    {
        long            startMs = System.currentTimeMillis();
        int             retries = 0;
        for(;;)
        {
            try
            {
                return uploadChunk(bytes, initResponse, index);
            }
            catch ( Exception e )
            {
                if ( !retryPolicy.allowRetry(retries++, System.currentTimeMillis() - startMs) )
                {
                    throw e;
                }
            }
        }
    }

    private PartETag uploadChunk(ByteBuffer bytes, InitiateMultipartUploadResult initResponse, int index) throws Exception
    {
        byte[]          md5 = S3Utils.md5(bytes);
        
        UploadPartRequest   request = new UploadPartRequest();
        request.setBucketName(initResponse.getBucketName());
        request.setKey(initResponse.getKey());
        request.setUploadId(initResponse.getUploadId());
        request.setPartNumber(index);
        request.setPartSize(bytes.limit());
        request.setMd5Digest(S3Utils.toBase64(md5));
        request.setInputStream(new ByteBufferInputStream(bytes));

        UploadPartResult    response = s3Client.get().uploadPart(request);
        PartETag            partETag = response.getPartETag();
        if ( !response.getPartETag().getETag().equals(S3Utils.toHex(md5)) )
        {
            throw new Exception("Unable to match MD5 for part " + index);
        }
        
        return partETag;
    }

    private void completeUpload(InitiateMultipartUploadResult initResponse, List<PartETag> eTags) throws Exception
    {
        CompleteMultipartUploadRequest completeRequest = new CompleteMultipartUploadRequest(initResponse.getBucketName(), initResponse.getKey(), initResponse.getUploadId(), eTags);
        s3Client.get().completeMultipartUpload(completeRequest);
    }

    private void abortUpload(InitiateMultipartUploadResult initResponse) throws Exception
    {
        AbortMultipartUploadRequest abortRequest = new AbortMultipartUploadRequest(initResponse.getBucketName(), initResponse.getKey(), initResponse.getUploadId());
        s3Client.get().abortMultipartUpload(abortRequest);
    }

    private String toKey(BackupMetaData backup, Map<String, String> configValues)
    {
        String  name = backup.getName().replace(SEPARATOR, SEPARATOR_REPLACEMENT);
        String  prefix = getKeyPrefix(configValues);

        return prefix + SEPARATOR + name + SEPARATOR + backup.getModifiedDate();
    }

    private String getKeyPrefix(Map<String, String> configValues)
    {
        String  prefix = configValues.get(CONFIG_KEY_PREFIX.getKey());
        if ( prefix != null )
        {
            prefix = prefix.replace(SEPARATOR, SEPARATOR_REPLACEMENT);
        }

        if ( (prefix == null) || (prefix.length() == 0))
        {
            prefix = "exhibitor-backup";
        }
        return prefix;
    }

    private BackupMetaData fromKey(String key)
    {
        String[]        parts = key.split("\\" + SEPARATOR);
        return new BackupMetaData(parts[1], Long.parseLong(parts[2]));
    }
}
