package com.netflix.exhibitor.core.index;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Closeables;
import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.config.StringConfigs;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

public class ZooKeeperLogFiles
{
    private final List<File>        paths;
    private final boolean           isValid;

    public static File      getDataDir(Exhibitor exhibitor)
    {
        String      path = exhibitor.getConfig().getString(StringConfigs.ZOOKEEPER_DATA_DIRECTORY);
        return new File(path, "version-2");
    }

    public ZooKeeperLogFiles(Exhibitor exhibitor) throws Exception
    {
        ImmutableList.Builder<File> builder = ImmutableList.builder();

        File        path = getDataDir(exhibitor);
        isValid = path.isDirectory();
        if ( isValid )
        {
            File[]      logs = path.listFiles();
            if ( logs != null )
            {
                for ( File f : logs )
                {
                    if ( isLogFile(f) )
                    {
                        builder.add(f);
                    }
                }
            }
        }

        paths = builder.build();
    }

    public boolean isValid()
    {
        return isValid;
    }

    public List<File> getPaths()
    {
        return paths;
    }

    private boolean isLogFile(File f) throws Exception
    {
        InputStream         log = new BufferedInputStream(new FileInputStream(f));
        try
        {
            ZooKeeperLogParser  logParser = new ZooKeeperLogParser(log);
            return logParser.isValid();
        }
        finally
        {
            Closeables.closeQuietly(log);
        }
    }
}
