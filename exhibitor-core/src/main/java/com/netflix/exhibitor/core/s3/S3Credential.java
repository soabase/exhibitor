package com.netflix.exhibitor.core.s3;

/**
 * Authentication details for AWS
 */
public interface S3Credential
{
    public String getAccessKeyId();

    public String getSecretAccessKey();
}
