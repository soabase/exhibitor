package com.netflix.exhibitor.spi;

import com.netflix.exhibitor.Exhibitor;
import com.netflix.exhibitor.state.InstanceState;

public interface ProcessOperations
{
    public void         startInstance(Exhibitor exhibitor, InstanceState instanceState) throws Exception;

    public void         killInstance(Exhibitor exhibitor) throws Exception;

    public void         cleanupInstance(Exhibitor exhibitor) throws Exception;
}
