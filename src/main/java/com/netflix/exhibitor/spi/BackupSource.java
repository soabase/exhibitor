package com.netflix.exhibitor.spi;

import com.netflix.exhibitor.InstanceConfig;
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
     * @param backupConfig config
     * @param path the path being backed-up
     * @param stream stream of the backup. The stream is buffered and compressed.
     * @throws Exception errors
     */
    public void     backup(InstanceConfig backupConfig, BackupPath path, InputStream stream) throws Exception;

    /**
     * Return the list of available backups
     *
     * @return list of backups
     */
    public Collection<BackupSpec> getAvailableBackups();

    /**
     * Open a stream for the specified backup. There's no need to buffer the stream.
     *
     * @param backupConfig config
     * @param spec the backup
     * @return the stream
     * @throws Exception errors
     */
    public InputStream openRestoreStream(InstanceConfig backupConfig, BackupSpec spec) throws Exception;
}
