package com.netflix.exhibitor.entities;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SystemState
{
    private boolean         running;
    private boolean         restartsEnabled;
    private boolean         backupCleanupEnabled;
    private String          version;
    private ConfigPojo      config;

    public SystemState()
    {
        this(new ConfigPojo(), false, false, "", true);
    }

    public SystemState(ConfigPojo config, boolean running, boolean restartsEnabled, String version, boolean backupCleanupEnabled)
    {
        this.config = config;
        this.running = running;
        this.restartsEnabled = restartsEnabled;
        this.version = version;
        this.backupCleanupEnabled = backupCleanupEnabled;
    }

    public boolean isRunning()
    {
        return running;
    }

    public void setRunning(boolean running)
    {
        this.running = running;
    }

    public boolean isRestartsEnabled()
    {
        return restartsEnabled;
    }

    public void setRestartsEnabled(boolean restartsEnabled)
    {
        this.restartsEnabled = restartsEnabled;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public ConfigPojo getConfig()
    {
        return config;
    }

    public void setConfig(ConfigPojo config)
    {
        this.config = config;
    }

    public boolean isBackupCleanupEnabled()
    {
        return backupCleanupEnabled;
    }

    public void setBackupCleanupEnabled(boolean backupCleanupEnabled)
    {
        this.backupCleanupEnabled = backupCleanupEnabled;
    }
}
