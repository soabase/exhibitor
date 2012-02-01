package com.netflix.exhibitor.imps;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.netflix.exhibitor.InstanceConfig;
import com.netflix.exhibitor.activity.ActivityLog;
import com.netflix.exhibitor.pojos.ServerInfo;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class JDBCBasedGlobalSharedConfig implements GlobalSharedConfigBase, Closeable
{
    private final Connection                        connection;
    private final JDBCQueries                       queries;
    private final ActivityLog                       log;
    private final int                               checkPeriodMs;
    private final ExecutorService                   service = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(true).setNameFormat("FileBasedGlobalSharedConfig-%d").build());
    private final Random                            random = new Random();
    private final PropertyBasedGlobalSharedConfig   propertyConfig;

    public static final String                      FIELD_NAME = "ExhibitorProperties";

    public JDBCBasedGlobalSharedConfig(Connection connection, JDBCQueries queries, InstanceConfig config, ActivityLog log)
    {
        this(connection, queries, config, log, (int)TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES));
    }

    public JDBCBasedGlobalSharedConfig(Connection connection, JDBCQueries queries, InstanceConfig config, ActivityLog log, int checkPeriodMs)
    {
        this.connection = connection;
        this.queries = queries;
        this.log = log;
        this.checkPeriodMs = checkPeriodMs;
        propertyConfig = new PropertyBasedGlobalSharedConfig(log, config);
    }

    public void start() throws Exception
    {
        Preconditions.checkArgument(!service.isShutdown());

        checkSchema();

        readProperties();
        service.submit
            (
                new Runnable()
                {
                    @Override
                    public void run()
                    {
                        while ( !Thread.currentThread().isInterrupted() )
                        {
                            try
                            {
                                Thread.sleep(checkPeriodMs + random.nextInt(checkPeriodMs));
                                readProperties();
                            }
                            catch ( InterruptedException e )
                            {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                }
            );
    }

    @Override
    public void close() throws IOException
    {
        Preconditions.checkArgument(!service.isShutdown());

        service.shutdownNow();
    }

    @Override
    public Collection<ServerInfo> getServers()
    {
        return propertyConfig.getServers();
    }

    @Override
    public void setServers(Collection<ServerInfo> newServers) throws Exception
    {
        readProperties();

        propertyConfig.setServers(newServers);
        writeProperties(propertyConfig.getProperties());
    }

    private synchronized void writeProperties(Properties properties)
    {
        PreparedStatement statement = null;
        try
        {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            properties.store(bytes, "Auto-generated properties");
            String      propertiesStr = new String(bytes.toByteArray());

            statement = connection.prepareStatement(queries.queryToWriteSchema());
            statement.setString(1, propertiesStr);
            statement.executeUpdate();
        }
        catch ( Exception e )
        {
            log.add(ActivityLog.Type.ERROR, "Reading properties", e);
        }
        finally
        {
            if ( statement != null )
            {
                try
                {
                    statement.close();
                }
                catch ( SQLException ignore )
                {
                    // ignore
                }
            }
        }
    }

    private synchronized void readProperties()
    {
        Statement   statement = null;
        ResultSet   resultSet = null;
        try
        {
            statement = connection.createStatement();
            resultSet = statement.executeQuery(queries.queryToReadSchema());
            if ( resultSet.next() )
            {
                String      propertiesStr = resultSet.getString(FIELD_NAME);
                Properties  newProperties = new Properties();
                newProperties.load(new ByteArrayInputStream(propertiesStr.getBytes()));
                propertyConfig.setProperties(newProperties);
            }
        }
        catch ( Exception e )
        {
            log.add(ActivityLog.Type.ERROR, "Reading properties", e);
        }
        finally
        {
            if ( resultSet != null )
            {
                try
                {
                    resultSet.close();
                }
                catch ( SQLException ignore )
                {
                    // ignore
                }
            }

            if ( statement != null )
            {
                try
                {
                    statement.close();
                }
                catch ( SQLException ignore )
                {
                    // ignore
                }
            }
        }
    }

    private void checkSchema() throws SQLException
    {
        Statement statement = connection.createStatement();
        try
        {
            statement.executeQuery(queries.queryToValidateSchema());
        }
        catch ( SQLException dummy )
        {
            // assume schema doesn't exist
            try
            {
                statement.executeUpdate(queries.queryToCreateSchema());
                statement.executeUpdate(queries.queryToInitializeSchema());
            }
            catch ( SQLException ignore )
            {
                // ignore
            }
        }
        finally
        {
            statement.close();
        }
    }
}
