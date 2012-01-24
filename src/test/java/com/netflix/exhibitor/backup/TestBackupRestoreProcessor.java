package com.netflix.exhibitor.backup;

import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.CuratorFrameworkFactory;
import com.netflix.curator.retry.RetryOneTime;
import com.netflix.curator.test.DirectoryUtils;
import com.netflix.curator.test.TestingServer;
import com.netflix.exhibitor.InstanceConfig;
import com.netflix.exhibitor.activity.ActivityLog;
import com.netflix.exhibitor.mocks.MockBackupSource;
import com.netflix.exhibitor.mocks.MockGlobalSharedConfig;
import com.netflix.exhibitor.spi.BackupPath;
import com.netflix.exhibitor.spi.BackupSource;
import com.netflix.exhibitor.spi.BackupSpec;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;

public class TestBackupRestoreProcessor
{
    @Test
    public void testBasic() throws Exception
    {
        File                tempDirectory = null;
        CuratorFramework    client = null;
        TestingServer       server = new TestingServer();
        try
        {
            tempDirectory = Files.createTempDir();
            
            client = CuratorFrameworkFactory.newClient(server.getConnectString(), new RetryOneTime(1));
            client.start();

            client.create().forPath("/test", "hello".getBytes());

            MockGlobalSharedConfig  sharedConfig = new MockGlobalSharedConfig();
            InstanceConfig          config = InstanceConfig.builder().hostname("localhost").build();
            BackupSource            source = new MockBackupSource(tempDirectory);

            BackupProcessor         backupProcessor = new BackupProcessor(sharedConfig, client, new ActivityLog(), source, config);
            sharedConfig.setBackupPaths(Arrays.asList(new BackupPath("/test", false)));
            backupProcessor.execute();

            Collection<BackupSpec>  backups = source.getAvailableBackups();
            Assert.assertEquals(backups.iterator().next().getPath(), "/test");

            client.setData().forPath("/test", "something else".getBytes());

            RestoreProcessor         restoreProcessor = new RestoreProcessor(client, new ActivityLog(), source, config);
            restoreProcessor.restoreFromBackup(backups.iterator().next());
            
            Assert.assertEquals(client.getData().forPath("/test"), "hello".getBytes());
        }
        finally
        {
            if ( tempDirectory != null )
            {
                DirectoryUtils.deleteRecursively(tempDirectory.getCanonicalFile());
            }
            
            Closeables.closeQuietly(client);
            Closeables.closeQuietly(server);
        }
    }

    @Test
    public void testRecursive() throws Exception
    {
        File                tempDirectory = null;
        CuratorFramework    client = null;
        TestingServer       server = new TestingServer();
        try
        {
            tempDirectory = Files.createTempDir();

            client = CuratorFrameworkFactory.newClient(server.getConnectString(), new RetryOneTime(1));
            client.start();

            client.create().forPath("/a1", "a-one".getBytes());
            client.create().forPath("/a2", "a-two".getBytes());
            client.create().forPath("/a1/b1", "a-one/b-one".getBytes());
            client.create().forPath("/a1/b2", "a-one/b-two".getBytes());
            client.create().forPath("/a2/b1", "a-two/b-one".getBytes());
            client.create().forPath("/a2/b2", "a-two/b-two".getBytes());

            MockGlobalSharedConfig  sharedConfig = new MockGlobalSharedConfig();
            InstanceConfig          config = InstanceConfig.builder().hostname("localhost").build();
            BackupSource            source = new MockBackupSource(tempDirectory);

            BackupProcessor         backupProcessor = new BackupProcessor(sharedConfig, client, new ActivityLog(), source, config);
            sharedConfig.setBackupPaths(Arrays.asList(new BackupPath("/a1", true)));
            backupProcessor.execute();

            Collection<BackupSpec>  backups = source.getAvailableBackups();
            Assert.assertEquals(backups.iterator().next().getPath(), "/a1");

            client.delete().forPath("/a2/b2");
            client.delete().forPath("/a2/b1");
            client.delete().forPath("/a1/b2");
            client.delete().forPath("/a1/b1");
            client.delete().forPath("/a1");
            client.delete().forPath("/a2");

            RestoreProcessor         restoreProcessor = new RestoreProcessor(client, new ActivityLog(), source, config);
            restoreProcessor.restoreFromBackup(backups.iterator().next());

            Assert.assertEquals(client.getData().forPath("/a1"), "a-one".getBytes());
            Assert.assertEquals(client.getData().forPath("/a1/b1"), "a-one/b-one".getBytes());
            Assert.assertEquals(client.getData().forPath("/a1/b2"), "a-one/b-two".getBytes());

            Assert.assertNull(client.checkExists().forPath("/a2"));
            Assert.assertNull(client.checkExists().forPath("/a2/b1"));
            Assert.assertNull(client.checkExists().forPath("/a2/b2"));
        }
        finally
        {
            if ( tempDirectory != null )
            {
                DirectoryUtils.deleteRecursively(tempDirectory.getCanonicalFile());
            }

            Closeables.closeQuietly(client);
            Closeables.closeQuietly(server);
        }
    }
}
