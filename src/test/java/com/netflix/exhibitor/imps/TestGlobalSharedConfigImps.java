package com.netflix.exhibitor.imps;

import com.google.common.collect.Lists;
import com.google.common.io.Closeables;
import com.netflix.exhibitor.InstanceConfig;
import com.netflix.exhibitor.activity.ActivityLog;
import com.netflix.exhibitor.pojos.BackupPath;
import com.netflix.exhibitor.pojos.ServerInfo;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public abstract class TestGlobalSharedConfigImps<T>
{
    private static final List<ServerInfo>   serversList = Arrays.asList(new ServerInfo("localhost", 1, true), new ServerInfo("anotherhost", 2, false));
    private static final List<BackupPath>   backupPathList = Arrays.asList(new BackupPath("/a", false), new BackupPath("/b", true), new BackupPath("/c", true));
    private static final ActivityLog log = new ActivityLog()
    {
        @Override
        public void add(Type type, String message)
        {
            System.err.println(type + ": " + message);
        }

        @Override
        public void add(Type type, String message, Throwable exception)
        {
            System.err.println(type + ": " + message);
            exception.printStackTrace();
        }
    };

    @Test
    public void testShared() throws Exception
    {
        T                             context = makeContext();
        List<GlobalSharedConfigBase>  configs = Lists.newArrayList();
        try
        {
            final int SLEEP = 10;
            configs.add(makeConfig(context, InstanceConfig.builder().hostname("localhost").build(), log, SLEEP));
            configs.add(makeConfig(context, InstanceConfig.builder().hostname("anotherhost").build(), log, SLEEP));
            
            for ( GlobalSharedConfigBase config : configs )
            {
                config.start();
                Assert.assertEquals(config.getBackupPaths().size(), 0);
                Assert.assertEquals(config.getServers().size(), 0);
            }
            
            configs.get(0).setServers(serversList);
            configs.get(1).setBackupPaths(backupPathList);
            Thread.sleep(SLEEP * 3);

            for ( GlobalSharedConfigBase config : configs )
            {
                Assert.assertEquals(serverListToString(config.getServers()), serverListToString(serversList));
                Assert.assertEquals(config.getBackupPaths(), backupPathList);
            }
        }
        finally
        {
            deleteContext(context);
            for ( GlobalSharedConfigBase config : configs )
            {
                Closeables.closeQuietly(config);
            }
        }
    }

    protected abstract GlobalSharedConfigBase makeConfig(T context, InstanceConfig config, ActivityLog log, int sleepMs) throws Exception;

    protected abstract T makeContext() throws Exception;
    
    protected abstract void deleteContext(T context) throws Exception;

    private String serverListToString(Collection<ServerInfo> l)
    {
        StringBuilder       str = new StringBuilder();
        for ( ServerInfo info : l )
        {
            str.append(info.getHostname()).append("-").append(info.getId()).append("-");
        }
        return str.toString();
    }

    @Test
    public void testBasic() throws Exception
    {
        T                          context = makeContext();
        GlobalSharedConfigBase     config = null;
        try
        {
            config = makeConfig(context, InstanceConfig.builder().hostname("localhost").build(), log, 10000);
            config.start();

            Assert.assertEquals(config.getBackupPaths().size(), 0);
            Assert.assertEquals(config.getServers().size(), 0);

            config.setServers(serversList);
            Assert.assertEquals(config.getServers().size(), 2);
            Assert.assertEquals(config.getBackupPaths().size(), 0);

            config.setBackupPaths(backupPathList);

            Assert.assertEquals(serverListToString(config.getServers()), serverListToString(serversList));
            Assert.assertEquals(config.getBackupPaths(), backupPathList);

            config.close();
            config = makeConfig(context, InstanceConfig.builder().hostname("localhost").build(), log, 10000);
            
            config.start();
            Assert.assertEquals(serverListToString(config.getServers()), serverListToString(serversList));
            Assert.assertEquals(config.getBackupPaths(), backupPathList);
        }
        finally
        {
            deleteContext(context);
            Closeables.closeQuietly(config);
        }
    }
}
