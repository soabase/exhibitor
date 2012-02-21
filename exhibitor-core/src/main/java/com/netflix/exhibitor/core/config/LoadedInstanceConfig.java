package com.netflix.exhibitor.core.config;

public class LoadedInstanceConfig
{
    private final InstanceConfig        config;
    private final long                  lastModified;

    public LoadedInstanceConfig(InstanceConfig config, long lastModified)
    {
        this.config = config;
        this.lastModified = lastModified;
    }

    public InstanceConfig getConfig()
    {
        return config;
    }

    public long getLastModified()
    {
        return lastModified;
    }
}
