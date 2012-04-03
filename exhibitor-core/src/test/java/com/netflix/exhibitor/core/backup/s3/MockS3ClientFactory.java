package com.netflix.exhibitor.core.backup.s3;

import com.netflix.exhibitor.core.s3.S3Client;
import com.netflix.exhibitor.core.s3.S3ClientFactory;
import com.netflix.exhibitor.core.s3.S3Credential;

public class MockS3ClientFactory implements S3ClientFactory
{
    private final MockS3Client s3Client;

    public MockS3ClientFactory(MockS3Client s3Client)
    {
        this.s3Client = s3Client;
    }

    @Override
    public S3Client makeNewClient(S3Credential credentials) throws Exception
    {
        return s3Client;
    }
}
