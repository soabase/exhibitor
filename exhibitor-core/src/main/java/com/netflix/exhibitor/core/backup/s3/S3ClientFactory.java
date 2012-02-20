package com.netflix.exhibitor.core.backup.s3;

import com.amazonaws.auth.AWSCredentials;

public interface S3ClientFactory
{
    public S3Client makeNewClient(AWSCredentials credentials) throws Exception;
}
