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

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o)
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        BackupPath that = (BackupPath)o;

        if ( recursive != that.recursive )
        {
            return false;
        }
        if ( !path.equals(that.path) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = path.hashCode();
        result = 31 * result + (recursive ? 1 : 0);
        return result;
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
