package com.netflix.exhibitor.standard_config;

import com.google.common.io.Closeables;
import com.netflix.exhibitor.ConfigProvider;
import com.netflix.exhibitor.InstanceConfig;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

public class LocalFileConfigProvider implements ConfigProvider
{
    private final File              propertiesFile;
    private final InstanceConfig    defaultConfig;

    private static final String     PROPERTY_HOSTNAME = "exhibitor.hostname";
    private static final String     PROPERTY_SEVER_SPEC = "exhibitor.server-spec";
    private static final String     PROPERTY_CLIENT_PORT = "exhibitor.client-port";
    private static final String     PROPERTY_CONNECT_PORT = "exhibitor.connect-port";
    private static final String     PROPERTY_ELECTION_PORT = "exhibitor.election-port";
    private static final String     PROPERTY_CHECK_MS = "exhibitor.check-ms";
    private static final String     PROPERTY_CONNECTION_TIMEOUT_MS = "exhibitor.connection-timeout-ms";
    private static final String     PROPERTY_CLEANUP_MS = "exhibitor.cleanup-ms";

    public LocalFileConfigProvider(File propertiesFile)
    {
        this(propertiesFile, new DefaultInstanceConfig());
    }

    public LocalFileConfigProvider(File propertiesFile, InstanceConfig defaultConfig)
    {
        this.propertiesFile = propertiesFile;
        this.defaultConfig = defaultConfig;
    }

    @Override
    public InstanceConfig loadConfig() throws Exception
    {
        Properties      properties = new Properties();
        if ( propertiesFile.exists() )
        {
            InputStream         in = new BufferedInputStream(new FileInputStream(propertiesFile));
            try
            {
                properties.load(in);
            }
            finally
            {
                Closeables.closeQuietly(in);
            }
        }
        return new InstanceConfigPojo(properties, defaultConfig);
    }

    @Override
    public void storeConfig(InstanceConfig config) throws Exception
    {
        Properties      properties = new Properties();
        properties.setProperty(PROPERTY_HOSTNAME, config.getHostname());
        properties.setProperty(PROPERTY_SEVER_SPEC, config.getServersSpec());
        properties.setProperty(PROPERTY_CONNECT_PORT, Integer.toString(config.getConnectPort()));
        properties.setProperty(PROPERTY_ELECTION_PORT, Integer.toString(config.getElectionPort()));
        properties.setProperty(PROPERTY_CHECK_MS, Integer.toString(config.getCheckMs()));
        properties.setProperty(PROPERTY_CONNECTION_TIMEOUT_MS, Integer.toString(config.getConnectionTimeoutMs()));
        properties.setProperty(PROPERTY_CLEANUP_MS, Integer.toString(config.getCleanupPeriodMs()));

        OutputStream    out = new BufferedOutputStream(new FileOutputStream(propertiesFile));
        try
        {
            properties.store(out, "Auto-generated Exhibitor Config");
        }
        finally
        {
            Closeables.closeQuietly(out);
        }
    }
}
