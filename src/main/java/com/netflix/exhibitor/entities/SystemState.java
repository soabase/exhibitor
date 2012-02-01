package com.netflix.exhibitor.entities;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SystemState
{
    private boolean         running;
    private boolean         restartsEnabled;
    private String          version;
    private ConfigPojo      config;

    public SystemState()
    {
        this(new ConfigPojo(), false, false, "");
    }

    public SystemState(ConfigPojo config, boolean running, boolean restartsEnabled, String version)
    {
        this.config = config;
        this.running = running;
        this.restartsEnabled = restartsEnabled;
        this.version = version;
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
}
