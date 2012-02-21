package com.netflix.exhibitor.core.config.s3;

public class S3ConfigArguments
{
    private final String        bucket;
    private final String        key;

    public S3ConfigArguments(String bucket, String key)
    {
        this.bucket = bucket;
        this.key = key;
    }

    public String getBucket()
    {
        return bucket;
    }

    public String getKey()
    {
        return key;
    }
}
