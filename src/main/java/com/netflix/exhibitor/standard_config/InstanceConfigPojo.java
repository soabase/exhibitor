package com.netflix.exhibitor.standard_config;

import com.netflix.exhibitor.InstanceConfig;
import java.util.Properties;

class InstanceConfigPojo implements InstanceConfig
{
    private final Properties properties;
    private final InstanceConfig defaults;

    InstanceConfigPojo(Properties properties, InstanceConfig defaults)
    {
        this.properties = properties;
        this.defaults = defaults;
    }

    @Override
    public String getHostname()
    {
        return properties.getProperty(PropertyNames.PROPERTY_HOSTNAME, defaults.getHostname());
    }

    @Override
    public String getServersSpec()
    {
        return properties.getProperty(PropertyNames.PROPERTY_SEVER_SPEC, defaults.getServersSpec());
    }

    @Override
    public int getClientPort()
    {
        return asInt(properties.getProperty(PropertyNames.PROPERTY_CLIENT_PORT), defaults.getClientPort());
    }

    @Override
    public int getConnectPort()
    {
        return asInt(properties.getProperty(PropertyNames.PROPERTY_CONNECT_PORT), defaults.getConnectPort());
    }

    @Override
    public int getElectionPort()
    {
        return asInt(properties.getProperty(PropertyNames.PROPERTY_ELECTION_PORT), defaults.getElectionPort());
    }

    @Override
    public int getCheckMs()
    {
        return asInt(properties.getProperty(PropertyNames.PROPERTY_CHECK_MS), defaults.getCheckMs());
    }

    @Override
    public int getConnectionTimeoutMs()
    {
        return asInt(properties.getProperty(PropertyNames.PROPERTY_CONNECTION_TIMEOUT_MS), defaults.getConnectionTimeoutMs());
    }

    @Override
    public int getCleanupPeriodMs()
    {
        return asInt(properties.getProperty(PropertyNames.PROPERTY_CLEANUP_MS), defaults.getCheckMs());
    }

    private int asInt(String property, int defaultValue)
    {
        try
        {
            return Integer.parseInt(property);
        }
        catch ( NumberFormatException e )
        {
            // ignore
        }
        return defaultValue;
    }
}
