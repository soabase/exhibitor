package com.netflix.exhibitor.imps;

import com.google.common.collect.ImmutableList;
import com.netflix.exhibitor.InstanceConfig;
import com.netflix.exhibitor.activity.ActivityLog;
import com.netflix.exhibitor.pojos.BackupPath;
import com.netflix.exhibitor.pojos.ServerInfo;
import com.netflix.exhibitor.spi.GlobalSharedConfig;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

public class PropertyBasedGlobalSharedConfig implements GlobalSharedConfig
{
    private final AtomicReference<Properties>   cachedProperties = new AtomicReference<Properties>(new Properties());
    private final ActivityLog                   log;
    private final InstanceConfig                config;

    private static final String         PROPERTY_SERVER_QTY = "exhibitor.server.qty";
    private static final String         PROPERTY_SERVER_N_HOSTNAME = "exhibitor.server.%d.hostname";
    private static final String         PROPERTY_SERVER_N_ID = "exhibitor.server.%d.id";

    private static final String         PROPERTY_BACKUP_QTY = "exhibitor.backup.qty";
    private static final String         PROPERTY_BACKUP_N_PATH = "exhibitor.backup.%d.path";
    private static final String         PROPERTY_BACKUP_N_RECURSIVE = "exhibitor.backup.%d.is-recursive";

    public PropertyBasedGlobalSharedConfig(ActivityLog log, InstanceConfig config)
    {
        this.log = log;
        this.config = config;
    }

    public void setProperties(Properties newProperties)
    {
        cachedProperties.set(newProperties);
    }

    public Properties getProperties()
    {
        Properties  properties = cachedProperties.get();
        Properties  copy = new Properties();
        copy.putAll(properties);
        return copy;
    }

    @Override
    public Collection<ServerInfo> getServers()
    {
        Properties  properties = cachedProperties.get();

        ImmutableList.Builder<ServerInfo> builder = ImmutableList.builder();

        int     serverQty = getInt(properties, PROPERTY_SERVER_QTY);
        for ( int i = 0; i < serverQty; ++i )
        {
            String      hostname = properties.getProperty(String.format(PROPERTY_SERVER_N_HOSTNAME, i), null);
            int         id = getInt(properties, String.format(PROPERTY_SERVER_N_ID, i));
            if ( hostname == null )
            {
                log.add(ActivityLog.Type.ERROR, "Property file is incorrect. Missing server index " + i);
                break;
            }

            builder.add(new ServerInfo(hostname, id, config.getHostname().equals(hostname)));
        }

        return builder.build();
    }

    @Override
    public void setServers(Collection<ServerInfo> newServers) throws Exception
    {
        Properties  properties = new Properties();
        internalSetServers(properties, newServers);
        internalSetBackupPaths(properties, getBackupPaths());
        cachedProperties.set(properties);
    }

    @Override
    public Collection<BackupPath> getBackupPaths()
    {
        Properties  properties = cachedProperties.get();

        ImmutableList.Builder<BackupPath> builder = ImmutableList.builder();

        int     backupQty = getInt(properties, PROPERTY_BACKUP_QTY);
        for ( int i = 0; i < backupQty; ++i )
        {
            String      path = properties.getProperty(String.format(PROPERTY_BACKUP_N_PATH, i), null);
            boolean     recursive = properties.getProperty(String.format(PROPERTY_BACKUP_N_RECURSIVE, i), "false").equalsIgnoreCase("true");
            if ( path == null )
            {
                log.add(ActivityLog.Type.ERROR, "Property file is incorrect. Missing backup index " + i);
                break;
            }

            builder.add(new BackupPath(path, recursive));
        }

        return builder.build();
    }

    @Override
    public void setBackupPaths(Collection<BackupPath> newBackupPaths) throws Exception
    {
        Properties  properties = new Properties();
        internalSetServers(properties, getServers());
        internalSetBackupPaths(properties, newBackupPaths);
        cachedProperties.set(properties);
    }

    private int getInt(Properties properties, String name)
    {
        try
        {
            return Integer.parseInt(properties.getProperty(name, "0"));
        }
        catch ( NumberFormatException ignore )
        {
            // ignore
        }
        return 0;
    }

    private void internalSetServers(Properties properties, Collection<ServerInfo> newServers)
    {
        properties.setProperty(PROPERTY_SERVER_QTY, Integer.toString(newServers.size()));

        int     index = 0;
        for ( ServerInfo info : newServers )
        {
            properties.setProperty(String.format(PROPERTY_SERVER_N_HOSTNAME, index), info.getHostname());
            properties.setProperty(String.format(PROPERTY_SERVER_N_ID, index), Integer.toString(info.getId()));
            ++index;
        }
    }

    private void internalSetBackupPaths(Properties properties, Collection<BackupPath> newBackupPaths)
    {
        properties.setProperty(PROPERTY_BACKUP_QTY, Integer.toString(newBackupPaths.size()));

        int     index = 0;
        for ( BackupPath backupPath : newBackupPaths )
        {
            properties.setProperty(String.format(PROPERTY_BACKUP_N_PATH, index), backupPath.getPath());
            properties.setProperty(String.format(PROPERTY_BACKUP_N_RECURSIVE, index), Boolean.toString(backupPath.isRecursive()));
            ++index;
        }
    }
}
