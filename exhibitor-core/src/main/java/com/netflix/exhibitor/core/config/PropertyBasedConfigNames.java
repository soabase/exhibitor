package com.netflix.exhibitor.core.config;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.netflix.exhibitor.core.InstanceConfig;
import java.util.Arrays;
import java.util.Properties;

public enum PropertyBasedConfigNames
{
    ZOOKEEPER_INSTALL_DIRECTORY("com.netflix.exhibitor.zookeeper.install-directory")
    {
        @Override
        protected String getValueFromConfig(InstanceConfig config)
        {
            return config.getZooKeeperInstallDirectory();
        }
    },

    ZOOKEEPER_DATA_DIRECTORY("com.netflix.exhibitor.zookeeper.data-directory")
    {
        @Override
        protected String getValueFromConfig(InstanceConfig config)
        {
            return config.getZooKeeperDataDirectory();
        }
    },

    LOG_INDEX_DIRECTORY("com.netflix.exhibitor.log-index.directory")
    {
        @Override
        protected String getValueFromConfig(InstanceConfig config)
        {
            return config.getLogIndexDirectory();
        }
    },

    HOSTNAME("com.netflix.exhibitor.hostname")
    {
        @Override
        protected String getValueFromConfig(InstanceConfig config)
        {
            return config.getHostname();
        }
    },

    SEVERS_SPEC("com.netflix.exhibitor.server-spec")
    {
        @Override
        protected String getValueFromConfig(InstanceConfig config)
        {
            return config.getServersSpec();
        }
    },

    CLIENT_PORT("com.netflix.exhibitor.client-port")
    {
        @Override
        protected String getValueFromConfig(InstanceConfig config)
        {
            return Integer.toString(config.getClientPort());
        }
    },

    CONNECT_PORT("com.netflix.exhibitor.connect-port")
    {
        @Override
        protected String getValueFromConfig(InstanceConfig config)
        {
            return Integer.toString(config.getConnectPort());
        }
    },

    ELECTION_PORT("com.netflix.exhibitor.election-port")
    {
        @Override
        protected String getValueFromConfig(InstanceConfig config)
        {
            return Integer.toString(config.getElectionPort());
        }
    },

    CHECK_MS("com.netflix.exhibitor.check-ms")
    {
        @Override
        protected String getValueFromConfig(InstanceConfig config)
        {
            return Integer.toString(config.getCheckMs());
        }
    },

    CONNECTION_TIMEOUT_MS("com.netflix.exhibitor.connection-timeout-ms")
    {
        @Override
        protected String getValueFromConfig(InstanceConfig config)
        {
            return Integer.toString(config.getConnectionTimeoutMs());
        }
    },

    CLEANUP_PERIOD_MS("com.netflix.exhibitor.cleanup.period-ms")
    {
        @Override
        protected String getValueFromConfig(InstanceConfig config)
        {
            return Integer.toString(config.getCleanupPeriodMs());
        }
    },

    CLEANUP_MAX_FILES("com.netflix.exhibitor.cleanup.max-files")
    {
        @Override
        protected String getValueFromConfig(InstanceConfig config)
        {
            return Integer.toString(config.getCleanupMaxFiles());
        }
    }

    ;
    private final String key;

    public static PropertyBasedConfigNames getByKey(final String key)
    {
        return Iterables.find
        (
            Arrays.asList(values()),
            new Predicate<PropertyBasedConfigNames>()
            {
                @Override
                public boolean apply(PropertyBasedConfigNames config)
                {
                    return config.key.equals(key);
                }
            }
        );
    }

    public String       getValue(Properties properties, InstanceConfig defaultValues)
    {
        return properties.getProperty(key, getValueFromConfig(defaultValues));
    }
    
    public void         setValue(Properties properties, InstanceConfig newValues)
    {
        properties.setProperty(key, getValueFromConfig(newValues));
    }
    
    protected abstract String getValueFromConfig(InstanceConfig config);

    private PropertyBasedConfigNames(String key)
    {
        this.key = key;
    }
}
