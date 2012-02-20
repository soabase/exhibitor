package com.netflix.exhibitor.core.backup.s3;

import com.google.common.collect.Maps;
import org.testng.annotations.Test;
import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

public class TestS3BackupProvider
{
    @Test
    public void   testUpload() throws Exception
    {
        URL                 resource = getClass().getClassLoader().getResource("com/netflix/exhibitor/core/backup/s3/DummyFile.txt");
        File                sourceFile = new File(resource.getPath());

        MockS3Client        s3Client = new MockS3Client();
        S3BackupProvider    provider = new S3BackupProvider(new MockS3ClientFactory(s3Client), new PropertyBasedS3Credential(new Properties()));

        Map<String,String>  configValues = Maps.newHashMap();
        provider.uploadBackup(null, "test", sourceFile, configValues);
    }
}
