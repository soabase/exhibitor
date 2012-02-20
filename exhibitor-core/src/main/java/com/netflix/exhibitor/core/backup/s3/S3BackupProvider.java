package com.netflix.exhibitor.core.backup.s3;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import com.netflix.curator.RetryPolicy;
import com.netflix.curator.retry.ExponentialBackoffRetry;
import com.netflix.exhibitor.core.BackupProvider;
import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.backup.BackupConfigSpec;
import org.apache.commons.codec.binary.Base64;
import org.apache.zookeeper.server.ByteBufferInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.netflix.exhibitor.core.config.DefaultProperties.asInt;

// liberally copied and modified from Priam
public class S3BackupProvider implements BackupProvider
{
    // TODO - add logging

    private final AmazonS3Client    s3Client;
    private final Compressor        compressor;

    private static final BackupConfigSpec CONFIG_THROTTLE = new BackupConfigSpec("throttle", "Throttle (bytes/ms)", "Data throttling. Maximum bytes per millisecond.", Integer.toString(1024 * 1024), BackupConfigSpec.Type.INTEGER);
    private static final BackupConfigSpec CONFIG_BUCKET = new BackupConfigSpec("bucket-name", "S3 Bucket Name", "The S3 bucket to use", "", BackupConfigSpec.Type.STRING);
    private static final BackupConfigSpec CONFIG_MAX_RETRIES = new BackupConfigSpec("max-retries", "Max Retries", "Maximum retries when uploading/downloading S3 data", "3", BackupConfigSpec.Type.INTEGER);
    private static final BackupConfigSpec CONFIG_RETRY_SLEEP_MS = new BackupConfigSpec("retry-sleep-ms", "Retry Sleep (ms)", "Sleep time in milliseconds when retrying", "1000", BackupConfigSpec.Type.INTEGER);

    private static final List<BackupConfigSpec>     CONFIGS = Arrays.asList(CONFIG_THROTTLE, CONFIG_BUCKET, CONFIG_MAX_RETRIES, CONFIG_RETRY_SLEEP_MS);

    public S3BackupProvider(S3Credential credential)
    {
        this.compressor = new GzipCompressor();
        BasicAWSCredentials credentials = new BasicAWSCredentials(credential.getAccessKeyId(), credential.getSecretAccessKey());
        s3Client = new AmazonS3Client(credentials);
    }

    @Override
    public List<BackupConfigSpec> getConfigs()
    {
        return CONFIGS;
    }

    @Override
    public boolean isValidConfig(Exhibitor exhibitor, Map<String, String> configValues)
    {
        return (configValues.get(CONFIG_BUCKET.getKey()).trim().length() > 0);
    }

    @Override
    public void uploadBackup(Exhibitor exhibitor, String key, File source, final Map<String, String> configValues) throws Exception
    {
        RetryPolicy retryPolicy = makeRetryPolicy(configValues);
        Throttle    throttle = makeThrottle(configValues);

        InitiateMultipartUploadRequest  initRequest = new InitiateMultipartUploadRequest(configValues.get(CONFIG_BUCKET.getKey()), key);
        InitiateMultipartUploadResult   initResponse = s3Client.initiateMultipartUpload(initRequest);
        List<PartETag>                  partETags = Lists.newArrayList();

        CompressorIterator      compressorIterator = compressor.compress(source);
        try
        {
            List<PartETag>      eTags = Lists.newArrayList();
            int                 index = 0;
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
    }

    @Override
    public void downloadBackup(Exhibitor exhibitor, String key, File destination, Map<String, String> configValues) throws Exception
    {
        S3Object        object = s3Client.getObject(configValues.get(CONFIG_BUCKET.getKey()), key);

        RetryPolicy     retryPolicy = makeRetryPolicy(configValues);
        Throttle        throttle = makeThrottle(configValues);

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
                channel.write(bytes);
            }
        }
        finally
        {
            Closeables.closeQuietly(in);
            Closeables.closeQuietly(out);
        }
    }

    @Override
    public List<String> getAvailableBackupKeys(Exhibitor exhibitor, Map<String, String> configValues) throws Exception
    {
        ListObjectsRequest  request = new ListObjectsRequest();
        request.setBucketName(configValues.get(CONFIG_BUCKET.getKey()));
        ObjectListing       listing = s3Client.listObjects(request);
        return Lists.transform
        (
            listing.getObjectSummaries(),
            new Function<S3ObjectSummary, String>()
            {
                @Override
                public String apply(S3ObjectSummary summary)
                {
                    return summary.getKey();
                }
            }
        );
    }

    @Override
    public void deleteBackup(Exhibitor exhibitor, String key, Map<String, String> configValues) throws Exception
    {
        s3Client.deleteObject(configValues.get(CONFIG_BUCKET.getKey()), key);
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
                return uploadChunk(bytes, initResponse, index, retryPolicy);
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

    private PartETag uploadChunk(ByteBuffer bytes, InitiateMultipartUploadResult initResponse, int index, RetryPolicy retryPolicy) throws Exception
    {
        byte[]          md5 = md5(bytes);
        
        UploadPartRequest   request = new UploadPartRequest();
        request.setBucketName(initResponse.getBucketName());
        request.setKey(initResponse.getKey());
        request.setUploadId(initResponse.getUploadId());
        request.setPartNumber(index);
        request.setPartSize(bytes.limit());
        request.setMd5Digest(toBase64(md5));
        request.setInputStream(new ByteBufferInputStream(bytes));

        UploadPartResult    response = s3Client.uploadPart(request);
        PartETag            partETag = response.getPartETag();
        if ( !response.getPartETag().getETag().equals(toHex(md5)) )
        {
            throw new Exception("Unable to match MD5 for part " + index);
        }
        
        return partETag;
    }

    private void completeUpload(InitiateMultipartUploadResult initResponse, List<PartETag> eTags) throws Exception
    {
        CompleteMultipartUploadRequest completeRequest = new CompleteMultipartUploadRequest(initResponse.getBucketName(), initResponse.getKey(), initResponse.getUploadId(), eTags);
        s3Client.completeMultipartUpload(completeRequest);
    }

    private void abortUpload(InitiateMultipartUploadResult initResponse)
    {
        AbortMultipartUploadRequest abortRequest = new AbortMultipartUploadRequest(initResponse.getBucketName(), initResponse.getKey(), initResponse.getUploadId());
        s3Client.abortMultipartUpload(abortRequest);
    }

    private static String toBase64(byte[] md5)
    {
        byte encoded[] = Base64.encodeBase64(md5, false);
        return new String(encoded);
    }

    private static byte[] md5(ByteBuffer buffer)
    {
        try
        {
            MessageDigest   mdigest = MessageDigest.getInstance("MD5");
            mdigest.update(buffer);
            return mdigest.digest();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static String toHex(byte[] digest)
    {
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for ( byte b : digest )
        {
            String hex = Integer.toHexString(b);
            if ( hex.length() == 1 )
            {
                sb.append("0");
            }
            else if ( hex.length() == 8 )
            {
                hex = hex.substring(6);
            }
            sb.append(hex);
        }
        return sb.toString().toLowerCase();
    }
}
