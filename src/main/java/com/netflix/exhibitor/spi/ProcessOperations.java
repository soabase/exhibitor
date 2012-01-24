package com.netflix.exhibitor.spi;

import com.netflix.exhibitor.Exhibitor;
import com.netflix.exhibitor.imps.StandardProcessOperations;
import com.netflix.exhibitor.state.InstanceState;

/**
 * Various inject-able operations. In most cases, you can use {@link StandardProcessOperations}
 */
public interface ProcessOperations
{
    /**
     * Start the instance
     *
     *
     * @param exhibitor main
     * @throws Exception errors
     */
    public void         startInstance(Exhibitor exhibitor) throws Exception;

    /**
     * Kill the instance
     *
     * @param exhibitor main
     * @throws Exception errors
     */
    public void         killInstance(Exhibitor exhibitor) throws Exception;

    /**
     * Perform an instance log/etc. cleanup
     *
     * @param exhibitor main
     * @throws Exception errors
     */
    public void         cleanupInstance(Exhibitor exhibitor) throws Exception;
}
