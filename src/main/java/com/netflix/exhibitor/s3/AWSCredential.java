package com.netflix.exhibitor.s3;

public interface AWSCredential
{
    public String   getAccessKeyId();

    public String   getSecretAccessKey();
}
