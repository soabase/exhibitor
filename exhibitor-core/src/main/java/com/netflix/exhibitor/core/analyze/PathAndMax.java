package com.netflix.exhibitor.core.analyze;

import com.google.common.base.Preconditions;

/**
 * Input for the analyzer. Defines a lock path and the maximum locks
 * allowed for the path.
 */
public class PathAndMax
{
    private final String path;
    private final int max;

    /**
     * @param path path for the lock. Either the parent path or one of the lock ZNodes
     * @param maxLocks max locks for this path. i.e. a mutex has a max of 1, a semaphore has a max
     *                 of 1 - n.
     */
    public PathAndMax(String path, int maxLocks)
    {
        this.path = Preconditions.checkNotNull(path, "path cannot be null");
        max = maxLocks;
    }

    public String getPath()
    {
        return path;
    }

    public int getMax()
    {
        return max;
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

        PathAndMax that = (PathAndMax)o;

        if ( !path.equals(that.path) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return path.hashCode();
    }

    @Override
    public String toString()
    {
        return path + "(" + max + ")";
    }
}
