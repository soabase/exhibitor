package com.netflix.exhibitor.application;

import com.netflix.exhibitor.core.BackupProvider;
import com.netflix.exhibitor.core.backup.BackupConfig;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class FileSystemBackupProvider implements BackupProvider
{
    private static final String             CONFIG_KEY = "directory";
    private static final List<BackupConfig> BACKUP_CONFIGS = Arrays.<BackupConfig>asList
    (
        new BackupConfig()
        {
            @Override
            public String getKey()
            {
                return CONFIG_KEY;
            }

            @Override
            public String getDisplayName()
            {
                return "Destination Path";
            }

            @Override
            public String getHelpText()
            {
                return "The path of the directory where backups are written to";
            }
        }
    );

    @Override
    public List<BackupConfig> getConfigs()
    {
        return BACKUP_CONFIGS;
    }

    @Override
    public void backupFile(File f, Map<String, String> configValues) throws Exception
    {
    }
}
