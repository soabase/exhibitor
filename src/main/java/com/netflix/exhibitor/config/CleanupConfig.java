package com.netflix.exhibitor.config;

public interface CleanupConfig
{
    public String        getZookeeperJarPath();

    public String        getLog4jJarPath();

    public String        getConfDirPath();

    public String        getDataPath();

    public String        getTimesSpec();
}
