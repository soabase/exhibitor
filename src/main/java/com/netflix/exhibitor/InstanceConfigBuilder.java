package com.netflix.exhibitor;

import com.google.common.collect.ImmutableList;
import com.netflix.exhibitor.spi.UITab;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class InstanceConfigBuilder
{
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
        this.hostname = "";
        this.connectPort = 2888;
        this.electionPort = 3888;
        this.checkSeconds = 1;
        this.clientPort = 2181;
        this.connectionTimeoutMs = 10000;
        this.maxBackups = 3;
        this.cleanupPeriodMs = (int)TimeUnit.MILLISECONDS.convert(6, TimeUnit.HOURS);
        this.backupPeriodMs = (int)TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES);
        this.additionalUITabs = ImmutableList.of();
    }

    public InstanceConfig   build()
    {
        return new InstanceConfig(this);
    }

    private InstanceConfigBuilder(String hostname, int connectPort, int electionPort, int checkSeconds, int clientPort, int connectionTimeoutMs, int backupPeriodMs, int cleanupPeriodMs, int maxBackups, Collection<UITab> additionalUITabs)
    {
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

    public InstanceConfigBuilder hostname(String hostname)
    {
        return new InstanceConfigBuilder(hostname, connectPort, electionPort, checkSeconds, clientPort, connectionTimeoutMs, backupPeriodMs, cleanupPeriodMs, maxBackups, additionalUITabs);
    }

    public InstanceConfigBuilder connectPort(int connectPort)
    {
        return new InstanceConfigBuilder(hostname, connectPort, electionPort, checkSeconds, clientPort, connectionTimeoutMs, backupPeriodMs, cleanupPeriodMs, maxBackups, additionalUITabs);
    }

    public InstanceConfigBuilder electionPort(int electionPort)
    {
        return new InstanceConfigBuilder(hostname, connectPort, electionPort, checkSeconds, clientPort, connectionTimeoutMs, backupPeriodMs, cleanupPeriodMs, maxBackups, additionalUITabs);
    }

    public InstanceConfigBuilder checkSeconds(int checkSeconds)
    {
        return new InstanceConfigBuilder(hostname, connectPort, electionPort, checkSeconds, clientPort, connectionTimeoutMs, backupPeriodMs, cleanupPeriodMs, maxBackups, additionalUITabs);
    }

    public InstanceConfigBuilder clientPort(int clientPort)
    {
        return new InstanceConfigBuilder(hostname, connectPort, electionPort, checkSeconds, clientPort, connectionTimeoutMs, backupPeriodMs, cleanupPeriodMs, maxBackups, additionalUITabs);
    }

    public InstanceConfigBuilder connectionTimeoutMs(int connectionTimeoutMs)
    {
        return new InstanceConfigBuilder(hostname, connectPort, electionPort, checkSeconds, clientPort, connectionTimeoutMs, backupPeriodMs, cleanupPeriodMs, maxBackups, additionalUITabs);
    }

    public InstanceConfigBuilder backupPeriodMs(int backupPeriodMs)
    {
        return new InstanceConfigBuilder(hostname, connectPort, electionPort, checkSeconds, clientPort, connectionTimeoutMs, backupPeriodMs, cleanupPeriodMs, maxBackups, additionalUITabs);
    }

    public InstanceConfigBuilder cleanupPeriodMs(int cleanupPeriodMs)
    {
        return new InstanceConfigBuilder(hostname, connectPort, electionPort, checkSeconds, clientPort, connectionTimeoutMs, backupPeriodMs, cleanupPeriodMs, maxBackups, additionalUITabs);
    }

    public InstanceConfigBuilder maxBackups(int maxBackups)
    {
        return new InstanceConfigBuilder(hostname, connectPort, electionPort, checkSeconds, clientPort, connectionTimeoutMs, backupPeriodMs, cleanupPeriodMs, maxBackups, additionalUITabs);
    }

    public InstanceConfigBuilder additionalUITabs(Collection<UITab> additionalUITabs)
    {
        return new InstanceConfigBuilder(hostname, connectPort, electionPort, checkSeconds, clientPort, connectionTimeoutMs, backupPeriodMs, cleanupPeriodMs, maxBackups, additionalUITabs);
    }
}
