package com.netflix.exhibitor.mocks;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.io.ByteStreams;
import com.netflix.exhibitor.InstanceConfig;
import com.netflix.exhibitor.spi.BackupSource;
import com.netflix.exhibitor.spi.BackupSpec;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

public class MockBackupSource implements BackupSource
{
    private final File tempDirectory;

    public MockBackupSource(File tempDirectory)
    {
        this.tempDirectory = tempDirectory;
    }

    @Override
    public void backup(InstanceConfig backupConfig, String path, InputStream stream) throws Exception
    {
        File                    f = new File(tempDirectory, URLEncoder.encode(path, "UTF-8"));
        BufferedOutputStream    out = new BufferedOutputStream(new FileOutputStream(f));
        try
        {
            ByteStreams.copy(stream, out);
        }
        finally
        {
            out.close();
        }
    }

    @Override
    public InputStream openRestoreStream(InstanceConfig backupConfig, BackupSpec spec) throws Exception
    {
        File                    f = new File(tempDirectory, URLEncoder.encode(spec.getPath(), "UTF-8"));
        return new BufferedInputStream(new FileInputStream(f));
    }

    @Override
    public Collection<BackupSpec> getAvailableBackups()
    {
        File[]        files = tempDirectory.listFiles();        
        return Collections2.transform
        (
            Arrays.asList(files),
            new Function<File, BackupSpec>()
            {
                @Override
                public BackupSpec apply(File f)
                {
                    try
                    {
                        return new BackupSpec(URLDecoder.decode(f.getName(), "UTF-8"), new Date(f.lastModified()));
                    }
                    catch ( UnsupportedEncodingException e )
                    {
                        throw new RuntimeException(e);
                    }
                }
            }
        );
    }
}
