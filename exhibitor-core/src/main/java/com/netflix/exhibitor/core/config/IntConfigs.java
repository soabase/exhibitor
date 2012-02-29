package com.netflix.exhibitor.core.config;

/**
 * Config selectors for integer values
 */
public enum IntConfigs
{
    /**
     * The port to connect to the ZK server - default: 2181
     */
    CLIENT_PORT,

    /**
     * The port ZK instances use to connect to each other - default: 2888
     */
    CONNECT_PORT,

    /**
     * The 2nd port ZK instances use to connect to each other - default: 3888
     */
    ELECTION_PORT,

    /**
     * Period in ms to check that ZK is running - default: 30000
     */
    CHECK_MS,

    /**
     * Period in ms to perform log cleanup - default: 12 hours
     */
    CLEANUP_PERIOD_MS,

    /**
     * Value to pass to PurgeTxnLog as max - default: 3
     */
    CLEANUP_MAX_FILES,

    /**
     * Max backup session to retain - default: 5
     */
    BACKUP_MAX_STORE_MS,

    /**
     * Period in ms to perform backups - default: 60000
     */
    BACKUP_PERIOD_MS
}
