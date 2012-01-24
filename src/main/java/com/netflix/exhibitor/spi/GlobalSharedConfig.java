package com.netflix.exhibitor.spi;

import java.util.Collection;

/**
 * <p>
 *     Configuration values that are <strong>global</strong> and <strong>mutable</strong>. All
 *     instances in the ensemble should represent the same values. A call to the mutator methods should
 *     update the value in all instances in the ensemble.
 * </p>
 *
 * <p>
 *     How this is accomplished is up to the client application. Some suggestions: a shared file,
 *     a database, puppet, chef, etc.
 * </p>
 */
public interface GlobalSharedConfig
{
    /**
     * Return the set of all servers in the ensemble
     *
     * @return servers
     */
    public Collection<ServerInfo>   getServers();

    /**
     * Change the set of servers in the ensemble. IMPORTANT: THIS WILL CAUSES INSTANCES TO
     * RESTART/SHUTDOWN/etc.
     *
     * @param newServers new list of servers
     * @throws Exception errors
     */
    public void                     setServers(Collection<ServerInfo> newServers) throws Exception;

    /**
     * Return the set of all paths being backed-up
     *
     * @return list of paths
     */
    public Collection<BackupPath>   getBackupPaths();

    /**
     * Change the set of paths to be backed up
     *
     * @param newBackupPaths new paths (can be empty but not null)
     * @throws Exception errors
     */
    public void                     setBackupPaths(Collection<BackupPath> newBackupPaths) throws Exception;
}
