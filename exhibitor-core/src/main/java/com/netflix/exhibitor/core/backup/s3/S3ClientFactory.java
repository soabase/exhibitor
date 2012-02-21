package com.netflix.exhibitor.core.backup.s3;

import com.amazonaws.auth.AWSCredentials;

/**
 * Factory for allocating new S3 clients
 */
public interface S3ClientFactory
{
    /**
     * Create a client with the given credentials
     *
     * @param credentials credentials
     * @return client
     * @throws Exception errors
     */
    public S3Client makeNewClient(AWSCredentials credentials) throws Exception;
}
