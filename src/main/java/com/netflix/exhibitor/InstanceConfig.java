package com.netflix.exhibitor;

/**
 * Static configuration needed by Exhibitor
 */
public interface InstanceConfig
{
    /**
     * Return the hostname of this server (the JVM Exhibitor is running in)
     *
     * @return hostname
     */
    public String getHostname();

    /**
     * Return the set of servers in the ensemble (should include this instance if appropriate). The
     * set is in the form: <code>host:id,host:id...</code>
     *
     * @return spec
     */
    public String getServersSpec();

    /**
     * Return the port used by clients to connect to the ZooKeeper instance. This is usually <code>2181</code>.
     *
     * @return client port
     */
    public int getClientPort();

    /**
     * Return the port used by other instances in the ensemble to connect to the ZooKeeper instance.
     * This is usually <code>2888</code>.
     *
     * @return client port
     */
    public int getConnectPort();

    /**
     * Return the port used by other instances in the ensemble to connect to the ZooKeeper instance
     * for elections. This is usually <code>3888</code>.
     *
     * @return client port
     */
    public int getElectionPort();

    /**
     * Return the period in milliseconds between live-ness checks on the ZooKeeper instance
     *
     * @return check period milliseconds
     */
    public int getCheckMs();

    /**
     * Return the max connection timeout in milliseconds when connecting to the ZooKeeper instance
     *
     * @return connection timeout milliseconds
     */
    public int getConnectionTimeoutMs();

    /**
     * Return the period in milliseconds between ZooKeeper log file cleaning
     *
     * @return cleanup period milliseconds
     */
    public int getCleanupPeriodMs();
}
