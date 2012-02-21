package com.netflix.exhibitor.core.config;

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
    public LoadedInstanceConfig loadConfig() throws Exception;

    /**
     * Store the config
     *
     * @param config config
     * @param compareLastModified modified value to compare with. If the config storage doesn't
     *                            match, the new config is not stored and null is returned
     * @throws Exception errors
     * @return return updated loaded values or null
     */
    public LoadedInstanceConfig storeConfig(InstanceConfig config, long compareLastModified) throws Exception;
}
