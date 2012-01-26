package com.netflix.exhibitor.imps;

import com.netflix.exhibitor.InstanceConfig;
import com.netflix.exhibitor.activity.ActivityLog;
import com.netflix.exhibitor.spi.BackupPath;
import com.netflix.exhibitor.spi.ServerInfo;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

public class TestJDBCBasedGlobalSharedConfig extends TestGlobalSharedConfigImps<Connection>
{
    private static final List<ServerInfo>   serversList = Arrays.asList(new ServerInfo("localhost", 1, true), new ServerInfo("anotherhost", 2, false));
    private static final List<BackupPath>   backupPathList = Arrays.asList(new BackupPath("/a", false), new BackupPath("/b", true), new BackupPath("/c", true));

    @Override
    protected GlobalSharedConfigBase makeConfig(Connection context, InstanceConfig config, ActivityLog log, int sleepMs)
    {
        return new JDBCBasedGlobalSharedConfig(context, new StandardJDBCQueries(), config, log, sleepMs);
    }

    @Override
    protected Connection makeContext() throws Exception
    {
        Connection connection = DriverManager.getConnection("jdbc:derby:memory:temp" + System.nanoTime() + ";create=true");
        connection.setAutoCommit(true);
        return connection;
    }

    @Override
    protected void deleteContext(Connection context) throws SQLException
    {
        context.close();
    }
}
