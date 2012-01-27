package com.netflix.exhibitor.imps;

import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.netflix.exhibitor.InstanceConfig;
import com.netflix.exhibitor.activity.ActivityLog;
import com.netflix.exhibitor.pojos.BackupPath;
import com.netflix.exhibitor.pojos.ServerInfo;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class TestLocalFileBasedGlobalSharedConfig
{
    private static final List<ServerInfo>   serversList = Arrays.asList(new ServerInfo("localhost", 1, true), new ServerInfo("anotherhost", 2, false));
    private static final List<BackupPath>   backupPathList = Arrays.asList(new BackupPath("/a", false), new BackupPath("/b", true), new BackupPath("/c", true));

    @Test
    public void     testMissingShared() throws Exception
    {
        InstanceConfig instanceConfig = InstanceConfig.builder().hostname("localhost").build();
        ActivityLog log = new ActivityLog();
        PropertyBasedGlobalSharedConfig     worker = new PropertyBasedGlobalSharedConfig(log, instanceConfig);
        worker.setServers(serversList);
        worker.setBackupPaths(backupPathList);

        File        localFile = File.createTempFile("temp", ".txt");
        try
        {
            File        badSharedFile = new File("/foo/bar");
            OutputStream out = null;
            try
            {
                out = new BufferedOutputStream(new FileOutputStream(localFile));
                worker.getProperties().store(out, "");
            }
            finally
            {
                Closeables.closeQuietly(out);
            }

            FileBasedGlobalSharedConfig     config = new FileBasedGlobalSharedConfig(badSharedFile, localFile, instanceConfig, log);
            try
            {
                config.start();
                Assert.assertEquals(config.getServers(), serversList);
                Assert.assertEquals(config.getBackupPaths(), backupPathList);
            }
            finally
            {
                Closeables.closeQuietly(config);
                config = null;
            }

            File        emptySharedFile = File.createTempFile("temp", ".txt");
            Files.append("\\uXYZPDQ", emptySharedFile, Charset.defaultCharset());  // write value that will cause Properties to throw
            try
            {
                config = new FileBasedGlobalSharedConfig(emptySharedFile, localFile, instanceConfig, log);
                config.start();
                Assert.assertEquals(config.getServers(), serversList);
                Assert.assertEquals(config.getBackupPaths(), backupPathList);
            }
            finally
            {
                Closeables.closeQuietly(config);
                emptySharedFile.delete();
            }
        }
        finally
        {
            localFile.delete();
        }
    }
}
