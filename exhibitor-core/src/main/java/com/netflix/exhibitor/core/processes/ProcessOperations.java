package com.netflix.exhibitor.core.processes;

/**
 * Various inject-able operations. In most cases, you can use {@link StandardProcessOperations}
 */
public interface ProcessOperations
{
    /**
     * Start the instance
     *
     *
     * @throws Exception errors
     */
    public void         startInstance() throws Exception;

    /**
     * Kill the instance
     *
     * @throws Exception errors
     */
    public void         killInstance() throws Exception;

    /**
     * Perform an instance log/etc. cleanup
     *
     * @throws Exception errors
     */
    public void         cleanupInstance() throws Exception;
}
