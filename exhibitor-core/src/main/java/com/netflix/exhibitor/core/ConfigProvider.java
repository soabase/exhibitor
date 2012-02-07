package com.netflix.exhibitor.core;

public interface ConfigProvider
{
    public InstanceConfig loadConfig() throws Exception;

    public void                 storeConfig(InstanceConfig config) throws Exception;
}
