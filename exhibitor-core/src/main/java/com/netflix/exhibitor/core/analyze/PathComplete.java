package com.netflix.exhibitor.core.analyze;

import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 * A completely loaded path
 */
public class PathComplete
{
    private final String        path;
    private final int           max;
    private final List<String>  childIds;

    PathComplete(String path, int max, List<String> childIds)
    {
        this.path = path;
        this.max = max;
        this.childIds = ImmutableList.copyOf(childIds);
    }

    /**
     * @return the path
     */
    public String getPath()
    {
        return path;
    }

    /**
     * @return the max locks for the path
     */
    public int getMax()
    {
        return max;
    }

    /**
     * The process IDs of the path. The first {@link #getMax()} IDs
     * are deemed to own the lock
     *
     * @return process IDs
     */
    public List<String> getChildIds()
    {
        return childIds;
    }

    /**
     * @return number of children in the path
     */
    public int getChildQty()
    {
        return childIds.size();
    }

    @Override
    public String toString()
    {
        return "PathComplete{" +
            "path='" + path + '\'' +
            ", max=" + max +
            ", childIds=" + childIds +
            '}';
    }
}
