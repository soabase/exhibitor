package com.netflix.exhibitor.core.index;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Closeables;
import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.config.StringConfigs;
import org.apache.jute.BinaryInputArchive;
import org.apache.zookeeper.server.persistence.FileHeader;
import org.apache.zookeeper.server.persistence.FileTxnLog;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

public class ZookeeperLogFiles
{
    private final List<File>        paths;

    public ZookeeperLogFiles(Exhibitor exhibitor) throws Exception
    {
        ImmutableList.Builder<File> builder = ImmutableList.builder();

        File[]      logs = new File(exhibitor.getConfig().getString(StringConfigs.ZOOKEEPER_DATA_DIRECTORY), "version-2").listFiles();
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

        paths = builder.build();
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
            BinaryInputArchive  logStream = BinaryInputArchive.getArchive(log);

            FileHeader fhdr = new FileHeader();
            fhdr.deserialize(logStream, "fileheader");
            return (fhdr.getMagic() == FileTxnLog.TXNLOG_MAGIC);
        }
        finally
        {
            Closeables.closeQuietly(log);
        }
    }
}
