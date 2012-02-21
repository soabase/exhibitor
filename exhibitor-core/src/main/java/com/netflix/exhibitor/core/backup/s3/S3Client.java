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

/**
 * Adapts the S3 client interface so that it can be tested - all methods are direct correlaries
 * to the S3 client
 */
public interface S3Client
{
    public InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request) throws Exception;

    public S3Object getObject(String bucket, String key) throws Exception;

    public ObjectListing listObjects(ListObjectsRequest request) throws Exception;

    public void deleteObject(String bucket, String key) throws Exception;

    public UploadPartResult uploadPart(UploadPartRequest request) throws Exception;

    public void completeMultipartUpload(CompleteMultipartUploadRequest request) throws Exception;

    public void abortMultipartUpload(AbortMultipartUploadRequest request) throws Exception;
}
