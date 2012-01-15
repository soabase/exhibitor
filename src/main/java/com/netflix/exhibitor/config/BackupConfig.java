package com.netflix.exhibitor.config;

public interface BackupConfig
{
    public int      getBackupPeriodMs();

    public int      getMaxBackups();
}
