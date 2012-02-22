package com.netflix.exhibitor.core.s3;

import com.amazonaws.services.s3.model.*;

/**
 * Adapts the S3 client interface so that it can be tested - all methods are direct proxies
 * to the S3 client
 */
public interface S3Client
{
    public InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request) throws Exception;

    public S3Object getObject(String bucket, String key) throws Exception;

    public ObjectListing listObjects(ListObjectsRequest request) throws Exception;

    public PutObjectResult putObject(PutObjectRequest request) throws Exception;

    public void deleteObject(String bucket, String key) throws Exception;

    public UploadPartResult uploadPart(UploadPartRequest request) throws Exception;

    public void completeMultipartUpload(CompleteMultipartUploadRequest request) throws Exception;

    public void abortMultipartUpload(AbortMultipartUploadRequest request) throws Exception;
}
