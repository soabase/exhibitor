/*
 *
 *  Copyright 2011 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.netflix.exhibitor.core.s3;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class S3ClientFactoryImpl implements S3ClientFactory
{
    @Override
    public S3Client makeNewClient(final S3Credential credentials) throws Exception
    {
        return new S3Client()
        {
            private final AtomicReference<AmazonS3Client>   client = new AtomicReference<AmazonS3Client>();

            {
                changeCredentials(credentials);
            }

            @Override
            public void changeCredentials(S3Credential credential) throws Exception
            {
                BasicAWSCredentials basicAWSCredentials = (credential != null) ? new BasicAWSCredentials(credentials.getAccessKeyId(), credentials.getSecretAccessKey()) : null;
                AmazonS3Client      newClient = (basicAWSCredentials != null) ? new AmazonS3Client(basicAWSCredentials) : null;
                AmazonS3Client      oldClient = client.getAndSet(newClient);
                if ( oldClient != null )
                {
                    oldClient.shutdown();
                }
            }

            @Override
            public void close() throws IOException
            {
                try
                {
                    changeCredentials(null);
                }
                catch ( Exception e )
                {
                    throw new IOException(e);
                }
            }

            @Override
            public InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request) throws Exception
            {
                return client.get().initiateMultipartUpload(request);
            }

            @Override
            public PutObjectResult putObject(PutObjectRequest request) throws Exception
            {
                return client.get().putObject(request);
            }

            @Override
            public S3Object getObject(String bucket, String key) throws Exception
            {
                return client.get().getObject(bucket, key);
            }

            @Override
            public ObjectListing listObjects(ListObjectsRequest request) throws Exception
            {
                return client.get().listObjects(request);
            }

            @Override
            public ObjectListing listNextBatchOfObjects(ObjectListing previousObjectListing) throws Exception
            {
                return client.get().listNextBatchOfObjects(previousObjectListing);
            }

            @Override
            public void deleteObject(String bucket, String key) throws Exception
            {
                client.get().deleteObject(bucket, key);
            }

            @Override
            public UploadPartResult uploadPart(UploadPartRequest request) throws Exception
            {
                return client.get().uploadPart(request);
            }

            @Override
            public void completeMultipartUpload(CompleteMultipartUploadRequest request) throws Exception
            {
                client.get().completeMultipartUpload(request);
            }

            @Override
            public void abortMultipartUpload(AbortMultipartUploadRequest request) throws Exception
            {
                client.get().abortMultipartUpload(request);
            }
        };
    }
}
