package com.netflix.exhibitor.core;

import com.netflix.exhibitor.core.backup.BackupConfig;
import java.io.File;
import java.util.List;
import java.util.Map;

public interface BackupProvider
{
    public List<BackupConfig> getConfigs();
    
    public void     backupFile(File f, Map<String, String> configValues) throws Exception;
}
