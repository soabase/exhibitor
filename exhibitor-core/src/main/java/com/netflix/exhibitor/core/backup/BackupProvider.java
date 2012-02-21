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

    /**
     * Upload an object into the backup.
     *
     * @param exhibitor instance
     * @param key unique key for this instance
     * @param source the source file
     * @param configValues values for provider-specific config
     * @throws Exception any errors
     */
    public void     uploadBackup(Exhibitor exhibitor, String key, File source, Map<String, String> configValues) throws Exception;

    /**
     * Return the set of available backups as a list of keys.
     *
     * @param exhibitor instance
     * @param configValues values for provider-specific config
     * @return keys
     * @throws Exception any errors
     */
    public List<String> getAvailableBackupKeys(Exhibitor exhibitor, Map<String, String> configValues) throws Exception;

    /**
     * Delete the given backup
     *
     * @param exhibitor instance
     * @param key backup to delete
     * @param configValues values for provider-specific config
     * @throws Exception any errors
     */
    public void     deleteBackup(Exhibitor exhibitor, String key, Map<String, String> configValues) throws Exception;

    /**
     * Download a backed-up object
     *
     * @param exhibitor instance
     * @param key backup to download
     * @param destination destination file
     * @param configValues values for provider-specific config
     * @throws Exception any errors
     */
    public void     downloadBackup(Exhibitor exhibitor, String key, File destination, Map<String, String> configValues) throws Exception;

    /**
     * Determine if the provider-specific config is in a good state. If not, backups/restores will be disallowed
     *
     * @param exhibitor instance
     * @param configValues values for provider-specific config
     * @return true/false
     */
    public boolean  isValidConfig(Exhibitor exhibitor, Map<String, String> configValues);
}
