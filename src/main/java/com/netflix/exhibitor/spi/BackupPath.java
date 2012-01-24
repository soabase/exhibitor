package com.netflix.exhibitor.spi;

import org.apache.zookeeper.common.PathUtils;

/**
 * POJO representing a single path to be backed up
 */
public class BackupPath
{
    private final String        path;
    private final boolean       recursive;

    /**
     * @param path The ZK path to backup
     * @param recursive if true, the path and all sub-paths/children are backed up. Otherwise
     *                  just the path
     * @throws IllegalArgumentException pad path argument
     */
    public BackupPath(String path, boolean recursive) throws IllegalArgumentException
    {
        PathUtils.validatePath(path);

        this.path = path;
        this.recursive = recursive;
    }

    public String getPath()
    {
        return path;
    }

    public boolean isRecursive()
    {
        return recursive;
    }

    @Override
    public String toString()
    {
        return "BackupPath{" +
            "path='" + path + '\'' +
            ", recursive=" + recursive +
            '}';
    }
}
