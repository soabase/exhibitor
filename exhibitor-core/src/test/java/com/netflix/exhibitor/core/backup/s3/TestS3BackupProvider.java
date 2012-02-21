package com.netflix.exhibitor.core.backup.s3;

import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.google.common.io.OutputSupplier;
import com.netflix.exhibitor.core.s3.PropertyBasedS3Credential;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

public class TestS3BackupProvider
{
    @Test
    public void   testUpload() throws Exception
    {
        File                      sourceFile = Filer.getFile();
        byte[]                    uploadedBytes = getUploadedBytes(sourceFile);

        ByteArrayOutputStream     zipBytes = new ByteArrayOutputStream();
        final GZIPOutputStream    zip = new GZIPOutputStream(zipBytes);
        byte[] fileBytes = Files.toByteArray(sourceFile);
        ByteStreams.write
        (
            fileBytes,
            new OutputSupplier<OutputStream>()
            {
                @Override
                public OutputStream getOutput() throws IOException
                {
                    return zip;
                }
            }
        );
        zip.close();
        Assert.assertEquals(uploadedBytes, zipBytes.toByteArray());
    }

    @Test
    public void     testDownload() throws Exception
    {
        File                sourceFile = Filer.getFile();
        byte[]              uploadedBytes = getUploadedBytes(sourceFile);

        S3Object            object = new S3Object();
        object.setBucketName("foo");
        object.setKey("test");
        object.setObjectContent(new ByteArrayInputStream(uploadedBytes));

        File                tempFile = File.createTempFile("test", ".test");
        try
        {
            MockS3Client        s3Client = new MockS3Client(object, null);
            S3BackupProvider    provider = new S3BackupProvider(new MockS3ClientFactory(s3Client), new PropertyBasedS3Credential(new Properties()));
            provider.downloadBackup(null, "test", tempFile, Maps.<String, String>newHashMap());
            
            Assert.assertEquals(Filer.getFileBytes(), Files.toByteArray(tempFile));
        }
        finally
        {
            //noinspection ResultOfMethodCallIgnored
            tempFile.delete();
        }
    }

    @Test
    public void testGetAvailableBackupKeys() throws Exception
    {
        ObjectListing       listing = new ObjectListing()
        {
            @Override
            public List<S3ObjectSummary> getObjectSummaries()
            {
                List<S3ObjectSummary>       list = Lists.newArrayList();
                S3ObjectSummary             summary = new S3ObjectSummary();
                summary.setKey("one");
                list.add(summary);
                summary = new S3ObjectSummary();
                summary.setKey("two");
                list.add(summary);
                summary = new S3ObjectSummary();
                summary.setKey("three");
                list.add(summary);
                return list;
            }
        };

        MockS3Client        s3Client = new MockS3Client(null, listing);
        S3BackupProvider    provider = new S3BackupProvider(new MockS3ClientFactory(s3Client), new PropertyBasedS3Credential(new Properties()));
        List<String>        backupKeys = provider.getAvailableBackupKeys(null, Maps.<String, String>newHashMap());
        Assert.assertEquals(backupKeys, Arrays.asList("one", "two", "three"));
    }

    private byte[] getUploadedBytes(File sourceFile) throws Exception
    {
        MockS3Client s3Client = new MockS3Client();
        S3BackupProvider provider = new S3BackupProvider(new MockS3ClientFactory(s3Client), new PropertyBasedS3Credential(new Properties()));

        provider.uploadBackup(null, "test", sourceFile, Maps.<String, String>newHashMap());

        ByteArrayOutputStream   out = new ByteArrayOutputStream();
        for ( byte[] bytes : s3Client.getUploadedBytes() )
        {
            out.write(bytes);
        }
        return out.toByteArray();
    }
}
