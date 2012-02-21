package com.netflix.exhibitor.core.backup.s3;

import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;

public class MockS3Client implements S3Client
{
    private final List<byte[]>      uploadedBytes = Lists.newArrayList();
    private final S3Object          object;
    private final ObjectListing     listing;

    public MockS3Client()
    {
        this(null, null);
    }

    public MockS3Client(S3Object object, ObjectListing listing)
    {
        this.object = object;
        this.listing = listing;
    }

    @Override
    public InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request) throws Exception
    {
        return new InitiateMultipartUploadResult();
    }

    @Override
    public S3Object getObject(String bucket, String key) throws Exception
    {
        return object;
    }

    @Override
    public ObjectListing listObjects(ListObjectsRequest request) throws Exception
    {
        return listing;
    }

    @Override
    public void deleteObject(String bucket, String key) throws Exception
    {
    }

    @Override
    public UploadPartResult uploadPart(UploadPartRequest request) throws Exception
    {
        ByteArrayOutputStream       out = new ByteArrayOutputStream();
        ByteStreams.copy(request.getInputStream(), out);

        uploadedBytes.add(out.toByteArray());

        byte[]              md5bytes = S3BackupProvider.md5(ByteBuffer.wrap(out.toByteArray()));

        UploadPartResult    result = new UploadPartResult();
        result.setPartNumber(request.getPartNumber());
        result.setETag(S3BackupProvider.toHex(md5bytes));
        return result;
    }

    @Override
    public void completeMultipartUpload(CompleteMultipartUploadRequest request) throws Exception
    {
    }

    @Override
    public void abortMultipartUpload(AbortMultipartUploadRequest request) throws Exception
    {
    }

    public List<byte[]> getUploadedBytes()
    {
        return uploadedBytes;
    }
}
