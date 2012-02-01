package com.netflix.exhibitor;

import com.google.common.collect.ImmutableList;
import com.netflix.exhibitor.pojos.UITab;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class InstanceConfigBuilder
{
    final String    serversSpec;
    final String    hostname;
    final int       connectPort;
    final int       electionPort;
    final int       checkSeconds;
    final int       clientPort;
    final int       connectionTimeoutMs;
    final int       backupPeriodMs;
    final int       cleanupPeriodMs;
    final int       maxBackups;
    final Collection<UITab> additionalUITabs;

    InstanceConfigBuilder()
    {
        this.serversSpec = "";
        this.hostname = "";
        this.connectPort = 2888;
        this.electionPort = 3888;
        this.checkSeconds = 1;
        this.clientPort = 2181;
        this.connectionTimeoutMs = (int)TimeUnit.MILLISECONDS.convert(10, TimeUnit.SECONDS);
        this.maxBackups = 5;
        this.cleanupPeriodMs = (int)TimeUnit.MILLISECONDS.convert(6, TimeUnit.HOURS);
        this.backupPeriodMs = (int)TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);
        this.additionalUITabs = ImmutableList.of();
    }

    public InstanceConfig   build()
    {
        return new InstanceConfig(this);
    }

    private InstanceConfigBuilder
        (
            String serversSpec,
            String hostname,
            int connectPort,
            int electionPort,
            int checkSeconds,
            int clientPort,
            int connectionTimeoutMs,
            int backupPeriodMs,
            int cleanupPeriodMs,
            int maxBackups,
            Collection<UITab> additionalUITabs
        )
    {
        this.serversSpec = serversSpec;
        this.hostname = hostname;
        this.connectPort = connectPort;
        this.electionPort = electionPort;
        this.checkSeconds = checkSeconds;
        this.clientPort = clientPort;
        this.connectionTimeoutMs = connectionTimeoutMs;
        this.backupPeriodMs = backupPeriodMs;
        this.cleanupPeriodMs = cleanupPeriodMs;
        this.maxBackups = maxBackups;
        this.additionalUITabs = additionalUITabs;
    }

    /**
     * @param serversSpec the server ensemble list in the form: <code>[Hostname A]:[Server ID],[Hostname B]:[Server ID]...</code>
     * @return modified builder
     */
    public InstanceConfigBuilder serversSpec(String serversSpec)
    {
        return new InstanceConfigBuilder(serversSpec, hostname, connectPort, electionPort, checkSeconds, clientPort, connectionTimeoutMs, backupPeriodMs, cleanupPeriodMs, maxBackups, additionalUITabs);
    }

    /**
     * @param hostname The hostname of the instance this JVM is running on. Must match a host in the list
     *                 returned by {@link InstanceConfig#getServerConnectionSpec()} otherwise this instance
     *                 will be considered to be out of service
     * @return modified builder
     */
    public InstanceConfigBuilder hostname(String hostname)
    {
        return new InstanceConfigBuilder(serversSpec, hostname, connectPort, electionPort, checkSeconds, clientPort, connectionTimeoutMs, backupPeriodMs, cleanupPeriodMs, maxBackups, additionalUITabs);
    }

    /**
     * @param connectPort The connect port to connect to ZooKeeper - default is 2888. There are three ZooKeeper ports: client, connect, elect
     * @return modified builder
     */
    public InstanceConfigBuilder connectPort(int connectPort)
    {
        return new InstanceConfigBuilder(serversSpec, hostname, connectPort, electionPort, checkSeconds, clientPort, connectionTimeoutMs, backupPeriodMs, cleanupPeriodMs, maxBackups, additionalUITabs);
    }

    /**
     * @param electionPort The election port to connect to ZooKeeper - default is 3888. There are three ZooKeeper ports: client, connect, elect
     * @return modified builder
     */
    public InstanceConfigBuilder electionPort(int electionPort)
    {
        return new InstanceConfigBuilder(serversSpec, hostname, connectPort, electionPort, checkSeconds, clientPort, connectionTimeoutMs, backupPeriodMs, cleanupPeriodMs, maxBackups, additionalUITabs);
    }

    /**
     * @param clientPort The public/client port to connect to ZooKeeper - default is 2181. There are three ZooKeeper ports: client, connect, elect
     * @return modified builder
     */
    public InstanceConfigBuilder clientPort(int clientPort)
    {
        return new InstanceConfigBuilder(serversSpec, hostname, connectPort, electionPort, checkSeconds, clientPort, connectionTimeoutMs, backupPeriodMs, cleanupPeriodMs, maxBackups, additionalUITabs);
    }

    /**
     * @param checkSeconds Number of seconds to wait between alive checks on ZooKeeper - default is 1 second
     * @return modified builder
     */
    public InstanceConfigBuilder checkSeconds(int checkSeconds)
    {
        return new InstanceConfigBuilder(serversSpec, hostname, connectPort, electionPort, checkSeconds, clientPort, connectionTimeoutMs, backupPeriodMs, cleanupPeriodMs, maxBackups, additionalUITabs);
    }

    /**
     * @param connectionTimeoutMs Connection timeout to use when connecting to the local Zookeeper - default is 10 seconds
     * @return modified builder
     */
    public InstanceConfigBuilder connectionTimeoutMs(int connectionTimeoutMs)
    {
        return new InstanceConfigBuilder(serversSpec, hostname, connectPort, electionPort, checkSeconds, clientPort, connectionTimeoutMs, backupPeriodMs, cleanupPeriodMs, maxBackups, additionalUITabs);
    }

    /**
     * @param backupPeriodSeconds Number of seconds between backups - default is 5 minutes
     * @return modified builder
     */
    public InstanceConfigBuilder backupPeriodSeconds(int backupPeriodSeconds)
    {
        int     backupPeriodMs = (int)TimeUnit.MILLISECONDS.convert(backupPeriodSeconds, TimeUnit.SECONDS);
        return new InstanceConfigBuilder(serversSpec, hostname, connectPort, electionPort, checkSeconds, clientPort, connectionTimeoutMs, backupPeriodMs, cleanupPeriodMs, maxBackups, additionalUITabs);
    }

    /**
     * @param cleanupPeriodSeconds Number of seconds between ZooKeeper log cleanup - default is 6 hours
     * @return modified builder
     */
    public InstanceConfigBuilder cleanupPeriodSeconds(int cleanupPeriodSeconds)
    {
        int     cleanupPeriodMs = (int)TimeUnit.MILLISECONDS.convert(cleanupPeriodSeconds, TimeUnit.SECONDS);
        return new InstanceConfigBuilder(serversSpec, hostname, connectPort, electionPort, checkSeconds, clientPort, connectionTimeoutMs, backupPeriodMs, cleanupPeriodSeconds, maxBackups, additionalUITabs);
    }

    /**
     * @param maxBackups the maximum backups to save - default is 5
     * @return modified builder
     */
    public InstanceConfigBuilder maxBackups(int maxBackups)
    {
        return new InstanceConfigBuilder(serversSpec, hostname, connectPort, electionPort, checkSeconds, clientPort, connectionTimeoutMs, backupPeriodMs, cleanupPeriodMs, maxBackups, additionalUITabs);
    }

    /**
     * @param additionalUITabs any additional UI tabs to show - default is none
     * @return modified builder
     */
    public InstanceConfigBuilder additionalUITabs(Collection<UITab> additionalUITabs)
    {
        return new InstanceConfigBuilder(serversSpec, hostname, connectPort, electionPort, checkSeconds, clientPort, connectionTimeoutMs, backupPeriodMs, cleanupPeriodMs, maxBackups, additionalUITabs);
    }
}
