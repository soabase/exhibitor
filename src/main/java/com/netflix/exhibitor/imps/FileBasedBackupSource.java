package com.netflix.exhibitor.imps;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.io.ByteStreams;
import com.netflix.exhibitor.pojos.BackupPath;
import com.netflix.exhibitor.pojos.BackupSpec;
import com.netflix.exhibitor.spi.BackupSource;
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

/**
 * A possible implementation for backups. Stores the backups in files in the given directory
 */
public class FileBasedBackupSource implements BackupSource
{
    private final File backupDirectory;

    private static final String SEPARATOR = "-";

    /**
     * @param backupDirectory the directory to store backup files in
     */
    public FileBasedBackupSource(File backupDirectory)
    {
        this.backupDirectory = backupDirectory;
    }

    @Override
    public void backup(BackupPath path, InputStream stream) throws Exception
    {
        File                    f = new File(backupDirectory, URLEncoder.encode(path.getPath() + SEPARATOR + System.currentTimeMillis(), "UTF-8"));
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
        File[]        files = backupDirectory.listFiles();
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
