package com.netflix.exhibitor.s3;

import com.netflix.curator.RetryPolicy;

public interface S3Config
{
    public int          getUploadThrottleMb();

    public String       getS3BucketName();

    public String       getKeyPrefix();

    public int          getChunkSizeMb();

    public RetryPolicy  getRetryPolicy();
}
