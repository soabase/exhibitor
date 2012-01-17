package com.netflix.exhibitor;

import com.netflix.exhibitor.maintenance.BackupSource;
import com.netflix.exhibitor.state.InstanceState;

public interface ProcessOperations
{
    public void         startInstance(Exhibitor exhibitor, InstanceState instanceState) throws Exception;

    public void         killInstance(Exhibitor exhibitor) throws Exception;

    public void         backupInstance(Exhibitor exhibitor, BackupSource source) throws Exception;

    public void         cleanupInstance(Exhibitor exhibitor) throws Exception;
}
