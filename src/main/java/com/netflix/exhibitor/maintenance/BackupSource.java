package com.netflix.exhibitor.maintenance;

import com.netflix.exhibitor.spi.ExhibitorConfig;
import java.io.InputStream;

public interface BackupSource
{
    public void     backup(ExhibitorConfig backupConfig, String name, InputStream stream) throws Exception;

    public void     checkRotation(ExhibitorConfig backupConfig) throws Exception;

    public RestoreInstance  newRestoreInstance(ExhibitorConfig backupConfig) throws Exception;
}
