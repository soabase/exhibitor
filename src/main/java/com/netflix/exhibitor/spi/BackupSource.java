package com.netflix.exhibitor.spi;

import com.netflix.exhibitor.InstanceConfig;
import java.io.InputStream;
import java.util.Collection;

public interface BackupSource
{
    public void     backup(InstanceConfig backupConfig, String name, InputStream stream) throws Exception;

    public InputStream openRestoreStream(InstanceConfig backupConfig, BackupSpec spec) throws Exception;

    public Collection<BackupSpec> getAvailableBackups();
}
