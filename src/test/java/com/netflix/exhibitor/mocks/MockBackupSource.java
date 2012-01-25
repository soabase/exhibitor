package com.netflix.exhibitor.mocks;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.io.ByteStreams;
import com.netflix.exhibitor.spi.BackupPath;
import com.netflix.exhibitor.spi.BackupSource;
import com.netflix.exhibitor.spi.BackupSpec;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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

    private static final String SEPARATOR = "-";

    public MockBackupSource(File tempDirectory)
    {
        this.tempDirectory = tempDirectory;
    }

    @Override
    public void backup(BackupPath path, InputStream stream) throws Exception
    {
        File                    f = new File(tempDirectory, URLEncoder.encode(path.getPath() + SEPARATOR + System.currentTimeMillis(), "UTF-8"));
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
    public void deleteBackup(BackupSpec spec) throws Exception
    {
        File   f = new File(spec.getUserValue());
        if ( !f.delete() )
        {
            throw new IOException("Could not delete: " + f.getPath());
        }
    }

    @Override
    public InputStream openRestoreStream(BackupSpec spec) throws Exception
    {
        File                    f = new File(spec.getUserValue());
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
                        String[] parts = f.getName().split("\\" + SEPARATOR);
                        return new BackupSpec(URLDecoder.decode(parts[0], "UTF-8"), new Date(f.lastModified()), f.getPath());
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
