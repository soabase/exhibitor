package com.netflix.exhibitor.core;

import com.netflix.exhibitor.core.state.InstanceConfig;

public interface ConfigProvider
{
    public InstanceConfig loadConfig() throws Exception;

    public void           storeConfig(InstanceConfig config) throws Exception;
}
