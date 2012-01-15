package com.netflix.exhibitor.s3;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.netflix.exhibitor.maintenance.Restorable;
import com.netflix.exhibitor.maintenance.RestoreInstance;
import org.xerial.snappy.SnappyInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

// much code copied from https://github.com/Netflix/priam
class S3RestoreInstance implements RestoreInstance
{
    private final AmazonS3Client s3Client;
    private final S3Config config;

    S3RestoreInstance(AmazonS3Client s3Client, S3Config config)
    {
        this.s3Client = s3Client;
        this.config = config;
    }

    @Override
    public List<Restorable> start() throws Exception
    {
        ListObjectsRequest request = new ListObjectsRequest();
        request.setBucketName(config.getS3BucketName());
        ObjectListing listing = s3Client.listObjects(request);
        return Lists.transform
        (
            listing.getObjectSummaries(),
            new Function<S3ObjectSummary, Restorable>()
            {
                @Override
                public Restorable apply(S3ObjectSummary summary)
                {
                    return new S3Restorable(summary);
                }
            }
        );
    }

    @Override
    public void close() throws IOException
    {
        // NOP
    }

    private File    download(String key) throws Exception
    {
        File        tempFile = null;
        InputStream objectContent = null;
        try
        {
            S3Object s3Object = s3Client.getObject(config.getS3BucketName(), key);
            tempFile = File.createTempFile("s3temp", ".tmp");
            objectContent = s3Object.getObjectContent();
            ByteStreams.copy(objectContent, Files.newOutputStreamSupplier(tempFile));
        }
        catch ( Exception e )
        {
            if ( tempFile != null )
            {
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
            }
            throw e;
        }
        finally
        {
            Closeables.closeQuietly(objectContent);
        }

        return tempFile;
    }

    private class S3Restorable implements Restorable
    {
        private final S3ObjectSummary summary;

        public S3Restorable(S3ObjectSummary summary)
        {
            this.summary = summary;
        }

        @Override
        public String getName()
        {
            return S3Common.keyToName(summary.getKey());
        }

        @Override
        public InputStream open() throws Exception
        {
            // Extra step: snappy seems to have boundary problems with stream
            final File        tempFile = download(summary.getKey());
            return new SnappyInputStream(new FileInputStream(tempFile))
            {
                @Override
                public void close() throws IOException
                {
                    super.close();
                    //noinspection ResultOfMethodCallIgnored
                    tempFile.delete();
                }
            };
        }
    }
}
