package com.netflix.exhibitor.spi;

import java.io.InputStream;

public interface BackupSource
{
    public void     backup(ExhibitorConfig backupConfig, String name, InputStream stream) throws Exception;

    public InputStream openRestoreStream(ExhibitorConfig backupConfig, BackupSpec spec) throws Exception;
}
