package com.netflix.exhibitor.maintenance;

import com.netflix.exhibitor.config.BackupConfig;
import java.io.InputStream;

public interface BackupSource
{
    public void     backup(BackupConfig backupConfig, String name, InputStream stream) throws Exception;

    public void     checkRotation(BackupConfig backupConfig) throws Exception;

    public RestoreInstance  newRestoreInstance(BackupConfig backupConfig) throws Exception;
}
