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

public class MockS3Client implements S3Client
{
    @Override
    public InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request) throws Exception
    {
        return new InitiateMultipartUploadResult();
    }

    @Override
    public S3Object getObject(String bucket, String key) throws Exception
    {
        S3Object object = new S3Object();
        object.setBucketName(bucket);
        object.setKey(key);
        return object;
    }

    @Override
    public ObjectListing listObjects(ListObjectsRequest request) throws Exception
    {
        return new ObjectListing();
    }

    @Override
    public void deleteObject(String bucket, String key) throws Exception
    {
    }

    @Override
    public UploadPartResult uploadPart(UploadPartRequest request) throws Exception
    {
        return new UploadPartResult();
    }

    @Override
    public void completeMultipartUpload(CompleteMultipartUploadRequest request) throws Exception
    {
    }

    @Override
    public void abortMultipartUpload(AbortMultipartUploadRequest request) throws Exception
    {
    }
}
