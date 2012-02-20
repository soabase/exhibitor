package com.netflix.exhibitor.core.config;

/**
 * Listener for config changes
 */
public interface ConfigListener
{
    /**
     * Called when the config has been updated
     */
    public void     configUpdated();
}
