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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;

public class S3ClientFactoryImpl implements S3ClientFactory
{
    @Override
    public S3Client makeNewClient(AWSCredentials credentials) throws Exception
    {
        final AmazonS3Client        client = new AmazonS3Client(credentials);
        return new S3Client()
        {
            @Override
            public InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest request) throws Exception
            {
                return client.initiateMultipartUpload(request);
            }

            @Override
            public PutObjectResult putObject(PutObjectRequest request) throws Exception
            {
                return client.putObject(request);
            }

            @Override
            public S3Object getObject(String bucket, String key) throws Exception
            {
                return client.getObject(bucket, key);
            }

            @Override
            public ObjectListing listObjects(ListObjectsRequest request) throws Exception
            {
                return client.listObjects(request);
            }

            @Override
            public void deleteObject(String bucket, String key) throws Exception
            {
                client.deleteObject(bucket, key);
            }

            @Override
            public UploadPartResult uploadPart(UploadPartRequest request) throws Exception
            {
                return client.uploadPart(request);
            }

            @Override
            public void completeMultipartUpload(CompleteMultipartUploadRequest request) throws Exception
            {
                client.completeMultipartUpload(request);
            }

            @Override
            public void abortMultipartUpload(AbortMultipartUploadRequest request) throws Exception
            {
                client.abortMultipartUpload(request);
            }
        };
    }
}
