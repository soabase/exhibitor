package com.netflix.exhibitor.core.entities;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SystemState
{
    private boolean         running;
    private boolean         restartsEnabled;
    private boolean         cleanupEnabled;
    private boolean         unlistedRestartsEnabled;
    private String          version;
    private Config          config;

    public SystemState()
    {
        this(new Config(), false, false, false, "", true);
    }

    public SystemState
        (
            Config config,
            boolean running,
            boolean restartsEnabled,
            boolean unlistedRestartsEnabled,
            String version,
            boolean cleanupEnabled
        )
    {
        this.config = config;
        this.running = running;
        this.restartsEnabled = restartsEnabled;
        this.unlistedRestartsEnabled = unlistedRestartsEnabled;
        this.version = version;
        this.cleanupEnabled = cleanupEnabled;
    }

    public boolean isUnlistedRestartsEnabled()
    {
        return unlistedRestartsEnabled;
    }

    public void setUnlistedRestartsEnabled(boolean unlistedRestartsEnabled)
    {
        this.unlistedRestartsEnabled = unlistedRestartsEnabled;
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

    public Config getConfig()
    {
        return config;
    }

    public void setConfig(Config config)
    {
        this.config = config;
    }

    public boolean isCleanupEnabled()
    {
        return cleanupEnabled;
    }

    public void setCleanupEnabled(boolean cleanupEnabled)
    {
        this.cleanupEnabled = cleanupEnabled;
    }
}
