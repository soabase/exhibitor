package com.netflix.exhibitor.core.config;

import com.netflix.exhibitor.core.state.InstanceConfig;

/**
 * Abstraction loading/storing config
 */
public interface ConfigProvider
{
    /**
     * Load and return the config
     *
     * @return config
     * @throws Exception errors
     */
    public InstanceConfig loadConfig() throws Exception;

    /**
     * Store the config
     *
     * @param config config
     * @throws Exception errors
     */
    public void           storeConfig(InstanceConfig config) throws Exception;
}
