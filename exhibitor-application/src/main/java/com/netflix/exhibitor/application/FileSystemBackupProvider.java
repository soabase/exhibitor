package com.netflix.exhibitor.application;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Files;
import com.netflix.exhibitor.core.BackupProvider;
import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.activity.ActivityLog;
import com.netflix.exhibitor.core.backup.BackupConfig;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class FileSystemBackupProvider implements BackupProvider
{
    private static final BackupConfig       CONFIG_DIRECTORY = new BackupConfig("directory", "Destination Path", "The path of the directory where backups are written to", "", BackupConfig.Type.STRING);

    private static final List<BackupConfig> BACKUP_CONFIGS = Arrays.asList(CONFIG_DIRECTORY);

    @Override
    public List<BackupConfig> getConfigs()
    {
        return BACKUP_CONFIGS;
    }

    @Override
    public void uploadBackup(Exhibitor exhibitor, String key, File source, Map<String, String> configValues) throws Exception
    {
        String      path = configValues.get(CONFIG_DIRECTORY.getKey());
        if ( path == null )
        {
            exhibitor.getLog().add(ActivityLog.Type.ERROR, "No backup directory set in config");
            return;
        }
        File        directory = new File(path);
        if ( !directory.isDirectory() && !directory.mkdirs() )
        {
            exhibitor.getLog().add(ActivityLog.Type.ERROR, "Could not create backup directory: " + directory);
            return;
        }

        File        destinationFile = new File(directory, key);
        Files.copy(source, destinationFile);
    }

    @Override
    public List<String> getAvailableBackupKeys(Exhibitor exhibitor, Map<String, String> configValues) throws Exception
    {
        ImmutableList.Builder<String>   builder = ImmutableList.builder();
        File                            directory = new File(configValues.get(CONFIG_DIRECTORY.getKey()));
        if ( directory.isDirectory() )
        {
            String[] files = directory.list();
            if ( files != null )
            {
                builder.addAll(Arrays.asList(files));
            }
        }
        return builder.build();
    }

    @Override
    public void deleteBackup(Exhibitor exhibitor, String key, Map<String, String> configValues) throws Exception
    {
        File        directory = new File(configValues.get(CONFIG_DIRECTORY.getKey()));
        File        destinationFile = new File(directory, key);
        if ( !destinationFile.delete() )
        {
            exhibitor.getLog().add(ActivityLog.Type.ERROR, "Could not delete old backup: " + destinationFile);
        }
    }

    @Override
    public void downloadBackup(Exhibitor exhibitor, String key, File destination, Map<String, String> configValues) throws Exception
    {
        File        directory = new File(configValues.get(CONFIG_DIRECTORY.getKey()));
        File        source = new File(directory, key);
        Files.copy(source, destination);
    }
}
