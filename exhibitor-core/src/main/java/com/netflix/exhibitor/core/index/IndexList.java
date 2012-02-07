package com.netflix.exhibitor.core.index;

import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.FileFilter;
import java.util.List;

public class IndexList
{
    private final List<File>        indexes;

    public IndexList(File directory)
    {
        File[] filtered = directory.listFiles
        (
            new FileFilter()
            {
                @Override
                public boolean accept(File f)
                {
                    if ( IndexMetaData.isValid(f) )
                    {
                        File    metaDataFile = IndexMetaData.getMetaDataFile(f);
                        if ( metaDataFile.exists() )
                        {
                            return true;
                        }
                    }
                    return false;
                }
            }
        );
        indexes = (filtered != null) ? ImmutableList.copyOf(filtered) : ImmutableList.<File>of();
    }

    public List<File> getIndexes()
    {
        return indexes;
    }
}
