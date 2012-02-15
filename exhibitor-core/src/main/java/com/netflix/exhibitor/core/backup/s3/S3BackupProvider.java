package com.netflix.exhibitor.core.backup.s3;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import com.netflix.curator.RetryPolicy;
import com.netflix.exhibitor.core.BackupProvider;
import com.netflix.exhibitor.core.backup.BackupConfig;
import org.apache.commons.codec.binary.Base64;
import org.apache.zookeeper.server.ByteBufferInputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

// liberally copied and modified from Priam
public class S3BackupProvider implements BackupProvider
{
    // TODO - add logging

    private final AmazonS3Client    s3Client;
    private final Throttle          throttle;
    private final S3Config          config;
    private final Compressor        compressor;
    private final RetryPolicy       retryPolicy;

    public S3BackupProvider(S3Credential credential, final S3Config config, Compressor compressor, RetryPolicy retryPolicy)
    {
        this.config = config;
        this.compressor = compressor;
        this.retryPolicy = retryPolicy;
        BasicAWSCredentials credentials = new BasicAWSCredentials(credential.getAccessKeyId(), credential.getSecretAccessKey());
        s3Client = new AmazonS3Client(credentials);
        throttle = new Throttle(this.getClass().getCanonicalName(), new Throttle.ThroughputFunction()
        {
            public int targetThroughput()
            {
                return Math.max(config.getUploadThrottleBytesPerMs(), Integer.MAX_VALUE);
            }
        });
    }

    @Override
    public List<BackupConfig> getConfigs()
    {
        return null;
    }

    @Override
    public void backupFile(File f, Map<String, String> configValues) throws Exception
    {
        String                          key = "exhibitor-backup-" + System.currentTimeMillis();

        InitiateMultipartUploadRequest  initRequest = new InitiateMultipartUploadRequest(config.getBucketName(), key);
        InitiateMultipartUploadResult   initResponse = s3Client.initiateMultipartUpload(initRequest);
        List<PartETag>                  partETags = Lists.newArrayList();

        CompressorIterator      compressorIterator = compressor.compress(f);
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

                PartETag eTag = uploadChunkWithRetry(chunk, initResponse, index++);
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

    private PartETag uploadChunkWithRetry(ByteBuffer bytes, InitiateMultipartUploadResult initResponse, int index) throws Exception
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
        byte[]          md5 = md5(bytes);
        
        UploadPartRequest   request = new UploadPartRequest();
        request.setBucketName(config.getBucketName());
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
        CompleteMultipartUploadRequest completeRequest = new CompleteMultipartUploadRequest(config.getBucketName(), initResponse.getKey(), initResponse.getUploadId(), eTags);
        s3Client.completeMultipartUpload(completeRequest);
    }

    private void abortUpload(InitiateMultipartUploadResult initResponse)
    {
        AbortMultipartUploadRequest abortRequest = new AbortMultipartUploadRequest(config.getBucketName(), initResponse.getKey(), initResponse.getUploadId());
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
