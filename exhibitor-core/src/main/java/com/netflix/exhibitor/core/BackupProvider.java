package com.netflix.exhibitor.core;

import com.netflix.exhibitor.core.backup.BackupConfigSpec;
import java.io.File;
import java.util.List;
import java.util.Map;

public interface BackupProvider
{
    public List<BackupConfigSpec> getConfigs();
    
    public void     uploadBackup(Exhibitor exhibitor, String key, File source, Map<String, String> configValues) throws Exception;
    
    public List<String> getAvailableBackupKeys(Exhibitor exhibitor, Map<String, String> configValues) throws Exception;

    public void     deleteBackup(Exhibitor exhibitor, String key, Map<String, String> configValues) throws Exception;

    public void     downloadBackup(Exhibitor exhibitor, String key, File destination, Map<String, String> configValues) throws Exception;

    public boolean  isValidConfig(Exhibitor exhibitor, Map<String, String> configValues);
}
