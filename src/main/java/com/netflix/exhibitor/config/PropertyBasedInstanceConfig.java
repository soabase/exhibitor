package com.netflix.exhibitor.config;

import com.netflix.exhibitor.InstanceConfig;
import java.util.Properties;

public class PropertyBasedInstanceConfig implements InstanceConfig
{
    private final Properties properties;
    private final InstanceConfig defaults;

    public PropertyBasedInstanceConfig(Properties properties, InstanceConfig defaults)
    {
        this.properties = properties;
        this.defaults = defaults;
    }

    @Override
    public String getZooKeeperInstallDirectory()
    {
        return PropertyBasedConfigNames.ZOOKEEPER_INSTALL_DIRECTORY.getValue(properties, defaults);
    }

    @Override
    public String getZooKeeperDataDirectory()
    {
        return PropertyBasedConfigNames.ZOOKEEPER_DATA_DIRECTORY.getValue(properties, defaults);
    }

    @Override
    public String getHostname()
    {
        return PropertyBasedConfigNames.HOSTNAME.getValue(properties, defaults);
    }

    @Override
    public String getServersSpec()
    {
        return PropertyBasedConfigNames.SEVERS_SPEC.getValue(properties, defaults);
    }

    @Override
    public int getClientPort()
    {
        return asInt(PropertyBasedConfigNames.CLIENT_PORT.getValue(properties, defaults));
    }

    @Override
    public int getConnectPort()
    {
        return asInt(PropertyBasedConfigNames.CONNECT_PORT.getValue(properties, defaults));
    }

    @Override
    public int getElectionPort()
    {
        return asInt(PropertyBasedConfigNames.ELECTION_PORT.getValue(properties, defaults));
    }

    @Override
    public int getCheckMs()
    {
        return asInt(PropertyBasedConfigNames.CHECK_MS.getValue(properties, defaults));
    }

    @Override
    public int getConnectionTimeoutMs()
    {
        return asInt(PropertyBasedConfigNames.CONNECTION_TIMEOUT_MS.getValue(properties, defaults));
    }

    @Override
    public int getCleanupPeriodMs()
    {
        return asInt(PropertyBasedConfigNames.CLEANUP_PERIOD_MS.getValue(properties, defaults));
    }

    @Override
    public int getCleanupMaxFiles()
    {
        return asInt(PropertyBasedConfigNames.CLEANUP_MAX_FILES.getValue(properties, defaults));
    }

    private int asInt(String property)
    {
        try
        {
            return Integer.parseInt(property);
        }
        catch ( NumberFormatException e )
        {
            // ignore
        }
        return 0;
    }
}
