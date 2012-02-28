package com.netflix.exhibitor.core.backup;

import com.netflix.exhibitor.core.Exhibitor;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Abstraction for backups/restores
 */
public interface BackupProvider
{
    /**
     * Return set of provider-specific config needed
     *
     * @return configs (or empty list)
     */
    public List<BackupConfigSpec> getConfigs();

    public enum UploadResult
    {
        FAILED,
        SUCCEEDED,
        DUPLICATE,
        REPLACED_OLD_VERSION
    }

    /**
     * Upload an object into the backup.
     *
     * @param exhibitor instance
     * @param metaData identity of the backup
     * @param source the source file
     * @param configValues values for provider-specific config
     * @return the upload result
     * @throws Exception any errors
     */
    public UploadResult uploadBackup(Exhibitor exhibitor, BackupMetaData metaData, File source, Map<String, String> configValues) throws Exception;

    /**
     * Return the set of available backups
     *
     * @param exhibitor instance
     * @param configValues values for provider-specific config
     * @return backups
     * @throws Exception any errors
     */
    public List<BackupMetaData> getAvailableBackups(Exhibitor exhibitor, Map<String, String> configValues) throws Exception;

    /**
     * Delete the given backup
     *
     * @param exhibitor instance
     * @param backup backup to delete
     * @param configValues values for provider-specific config
     * @throws Exception any errors
     */
    public void     deleteBackup(Exhibitor exhibitor, BackupMetaData backup, Map<String, String> configValues) throws Exception;

    /**
     * Download a backed-up object
     *
     *
     * @param exhibitor instance
     * @param backup the backup to pull down
     * @param destination destination file
     * @param configValues values for provider-specific config
     * @throws Exception any errors
     */
    public void     downloadBackup(Exhibitor exhibitor, BackupMetaData backup, File destination, Map<String, String> configValues) throws Exception;

    /**
     * Determine if the provider-specific config is in a good state. If not, backups/restores will be disallowed
     *
     * @param exhibitor instance
     * @param configValues values for provider-specific config
     * @return true/false
     */
    public boolean  isValidConfig(Exhibitor exhibitor, Map<String, String> configValues);
}
