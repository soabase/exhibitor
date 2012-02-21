package com.netflix.exhibitor.core.config;

/**
 * Config selectors for string values
 */
public enum StringConfigs
{
    /**
     * Path to stored indexed log files
     */
    LOG_INDEX_DIRECTORY,

    /**
     * The base director of the zookeeper installation
     */
    ZOOKEEPER_INSTALL_DIRECTORY,

    /**
     * Where to store ZooKeeper data
     */
    ZOOKEEPER_DATA_DIRECTORY,

    /**
     * List of servers in the cluster of the form [id]:[hostname],[id]:[hostname],...
     */
    SERVERS_SPEC,

    /**
     * Used to store the provider-specific backup config
     */
    BACKUP_EXTRA
}
