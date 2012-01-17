package com.netflix.exhibitor.s3;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.netflix.curator.RetryPolicy;
import com.netflix.exhibitor.spi.ExhibitorConfig;
import com.netflix.exhibitor.activity.ActivityLog;
import com.netflix.exhibitor.maintenance.BackupSource;
import com.netflix.exhibitor.maintenance.RestoreInstance;
import com.netflix.exhibitor.maintenance.Throttle;
import org.apache.commons.codec.binary.Base64;
import org.xerial.snappy.SnappyInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

// much code copied from https://github.com/Netflix/priam
public class S3BackupSource implements BackupSource
{
    private final AmazonS3Client s3Client;
    private final Throttle throttle;
    private final S3Config          config;
    private final ActivityLog log;
    private final byte[]            chunk;

    public S3BackupSource(AWSCredential awsCredential, S3Config s3Config, ActivityLog log)
    {
        this.config = s3Config;
        this.log = log;
        AWSCredentials cred = new BasicAWSCredentials(awsCredential.getAccessKeyId(), awsCredential.getSecretAccessKey());
        s3Client = new AmazonS3Client(cred);
        chunk = new byte[config.getChunkSizeMb() * 1024 * 1024];

        throttle = new Throttle
        (
            "S3BackupSource",
            new Throttle.ThroughputFunction()
            {
                public int targetThroughput()
                {
                    if ( config.getUploadThrottleMb() < 1 )
                    {
                        return 0;
                    }
                    return (config.getUploadThrottleMb() * 1024 * 1024) / 1000;
                }
            },
            log
        );
    }

    @Override
    public RestoreInstance newRestoreInstance(ExhibitorConfig backupConfig) throws Exception
    {
        return new S3RestoreInstance(s3Client, config);
    }

    @Override
    public void backup(ExhibitorConfig backupConfig, String name, InputStream stream) throws Exception
    {
        RetryPolicy retryPolicy = config.getRetryPolicy();
        
        long                start = System.currentTimeMillis();
        int                 retryCount = 0;
        boolean             done = false;
        while ( !done )
        {
            try
            {
                tryBackup(name, stream);
                done = true;
            }
            catch ( Exception e )
            {
                boolean     willRetry = retryPolicy.allowRetry(retryCount, System.currentTimeMillis() - start);
                log.add(ActivityLog.Type.ERROR, String.format("Uploading error for %s - retry count: %d - will retry: %s", name, retryCount, Boolean.toString(willRetry)), e);
                ++retryCount;

                if ( !willRetry )
                {
                    throw e;
                }
            }
        }
    }

    @Override
    public void     checkRotation(ExhibitorConfig backupConfig) throws Exception
    {
        ListObjectsRequest request = new ListObjectsRequest();
        request.setBucketName(config.getS3BucketName());
        ObjectListing listing = s3Client.listObjects(request);
        Set<String>             keys = Sets.newTreeSet
        (
            Lists.transform
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
                )
        );

        Iterator<String>    iterator = keys.iterator();
        int                 overSpec = keys.size() - backupConfig.getMaxBackups();
        while ( overSpec-- > 0 )
        {
            String      thisKey = iterator.next();
            s3Client.deleteObject(config.getS3BucketName(), thisKey);
        }
    }

    private void tryBackup(String name, InputStream stream) throws Exception
    {
        String                          key = S3Common.nameToKey(name);
        InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(config.getS3BucketName(), key);
        InitiateMultipartUploadResult initResponse = s3Client.initiateMultipartUpload(initRequest);

        SnappyInputStream snappy = new SnappyInputStream(stream);
        List<PartETag> partETags = Lists.newArrayList();
        for(;;)
        {
            int bytesRead = snappy.read(chunk);
            if ( bytesRead <= 0 )
            {
                break;
            }
            throttle.throttle(bytesRead);
            uploadChunk(initRequest, initResponse, bytesRead, partETags);
        }
    }

    private void            uploadChunk(InitiateMultipartUploadRequest initRequest, InitiateMultipartUploadResult initResponse, int length, List<PartETag> partETags) throws Exception
    {
        byte[]            md5Bytes = md5(chunk, length);

        UploadPartRequest request = new UploadPartRequest();
        request.setBucketName(initRequest.getBucketName());
        request.setKey(initRequest.getKey());
        request.setUploadId(initResponse.getUploadId());
        request.setPartNumber(partETags.size());
        request.setPartSize(length);
        request.setMd5Digest(toBase64(md5Bytes));
        request.setInputStream(new ByteArrayInputStream(chunk, 0, length));

        try
        {
            UploadPartResult result = s3Client.uploadPart(request);
            PartETag partETag = result.getPartETag();
            if ( !partETag.getETag().equals(toHex(md5Bytes)) )
            {
                throw new Exception(String.format("Unable to match MD5 for part #%d of %s" + request.getPartNumber(), initRequest.getKey()));
            }
            partETags.add(partETag);
        }
        catch ( Exception e )
        {
            abortUpload(request);
        }
    }

    private void abortUpload(UploadPartRequest request)
    {
        AbortMultipartUploadRequest abortRequest = new AbortMultipartUploadRequest(request.getBucketName(), request.getKey(), request.getUploadId());
        s3Client.abortMultipartUpload(abortRequest);
    }

    private static String toBase64(byte[] md5)
    {
        byte encoded[] = Base64.encodeBase64(md5, false);
        return new String(encoded);
    }

    private static byte[] md5(byte[] buf, int length)
    {
        try
        {
            MessageDigest mdigest = MessageDigest.getInstance("MD5");
            mdigest.update(buf, 0, length);
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
