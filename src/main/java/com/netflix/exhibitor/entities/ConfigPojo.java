package com.netflix.exhibitor.entities;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ConfigPojo
{
    private String    thisHostname;
    private int       thisServerId;
    private String    serversSpec;
    private int       clientPort;
    private int       connectPort;
    private int       electionPort;
    private int       checkMs;
    private int       connectionTimeoutMs;
    private int       cleanupPeriodMs;

    public ConfigPojo()
    {
        this
        (
            "",
            "",
            -1,
            0,
            0,
            0,
            0,
            0,
            0
        );
    }

    public ConfigPojo
        (
            String serversSpec,
            String thisHostname,
            int thisServerId,
            int clientPort,
            int connectPort,
            int electionPort,
            int checkMs,
            int connectionTimeoutMs,
            int cleanupPeriodMs
        )
    {
        this.serversSpec = serversSpec;
        this.thisHostname = thisHostname;
        this.thisServerId = thisServerId;
        this.clientPort = clientPort;
        this.connectPort = connectPort;
        this.electionPort = electionPort;
        this.checkMs = checkMs;
        this.connectionTimeoutMs = connectionTimeoutMs;
        this.cleanupPeriodMs = cleanupPeriodMs;
    }

    public String getServersSpec()
    {
        return serversSpec;
    }

    public void setServersSpec(String serversSpec)
    {
        this.serversSpec = serversSpec;
    }

    public int getThisServerId()
    {
        return thisServerId;
    }

    public void setThisServerId(int thisServerId)
    {
        this.thisServerId = thisServerId;
    }

    public String getThisHostname()
    {
        return thisHostname;
    }

    public void setThisHostname(String thisHostname)
    {
        this.thisHostname = thisHostname;
    }

    public int getConnectPort()
    {
        return connectPort;
    }

    public void setConnectPort(int connectPort)
    {
        this.connectPort = connectPort;
    }

    public int getElectionPort()
    {
        return electionPort;
    }

    public void setElectionPort(int electionPort)
    {
        this.electionPort = electionPort;
    }

    public int getCheckMs()
    {
        return checkMs;
    }

    public void setCheckMs(int checkMs)
    {
        this.checkMs = checkMs;
    }

    public int getClientPort()
    {
        return clientPort;
    }

    public void setClientPort(int clientPort)
    {
        this.clientPort = clientPort;
    }

    public int getConnectionTimeoutMs()
    {
        return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(int connectionTimeoutMs)
    {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public int getCleanupPeriodMs()
    {
        return cleanupPeriodMs;
    }

    public void setCleanupPeriodMs(int cleanupPeriodMs)
    {
        this.cleanupPeriodMs = cleanupPeriodMs;
    }
}
