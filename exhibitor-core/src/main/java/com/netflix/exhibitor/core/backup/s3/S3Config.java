package com.netflix.exhibitor.core.backup.s3;

public interface S3Config
{
    public int getUploadThrottleBytesPerMs();

    public String getBucketName();
}
