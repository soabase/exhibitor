package com.netflix.exhibitor.spi;

import java.io.InputStream;
import java.util.Collection;

/**
 * Abstraction for managing backups
 */
public interface BackupSource
{
    /**
     * Backup the given path
     *
     * @param path the path being backed-up
     * @param stream stream of the backup. The stream is buffered and compressed.
     * @throws Exception errors
     */
    public void     backup(BackupPath path, InputStream stream) throws Exception;

    /**
     * Return the list of available backups
     *
     * @return list of backups
     */
    public Collection<BackupSpec> getAvailableBackups();

    /**
     * Open a stream for the specified backup. There's no need to buffer the stream.
     *
     * @param spec the backup
     * @return the stream
     * @throws Exception errors
     */
    public InputStream openRestoreStream(BackupSpec spec) throws Exception;

    /**
     * Delete the given backup (it is being cleaned up)
     *
     * @param spec the backup
     * @throws Exception errors
     */
    public void deleteBackup(BackupSpec spec) throws Exception;
}
