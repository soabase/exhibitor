package com.netflix.exhibitor.core.index;

import com.google.common.collect.Lists;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class DirectoryInputStream extends InputStream
{
    private final List<File>    files;
    private final long          totalLength;
    private final File          directory;

    // guarded by sync
    private int            index = -1;
    private InputStream    currentStream = null;

    public DirectoryInputStream(File directory)
    {
        this.directory = directory;
        files = Lists.newArrayList(Arrays.asList(directory.listFiles()));
        
        int     length = 0;
        for ( File f : files )
        {
            length += f.length();
        }
        totalLength = length;
    }

    public File getDirectory()
    {
        return directory;
    }

    public long length()
    {
        return totalLength;
    }

    @Override
    public synchronized int read() throws IOException
    {
        if ( checkStream() )
        {
            int b = currentStream.read();
            if ( b < 0 )
            {
                currentStream.close();
                currentStream = null;
                return read();
            }
        }

        return -1;
    }

    @Override
    public synchronized void close() throws IOException
    {
        if ( currentStream != null )
        {
            currentStream.close();
            currentStream = null;
        }
        files.clear();
        index = -1;
    }

    @Override
    public synchronized int read(byte[] b) throws IOException
    {
        return super.read(b, 0, b.length);
    }

    @Override
    public synchronized int read(byte[] b, int off, int len) throws IOException
    {
        int     bytesRead = 0;
        while ( (len > 0) && checkStream() )
        {
            int         thisBytesRead = currentStream.read(b, off, len);
            if ( thisBytesRead > 0 )
            {
                bytesRead += thisBytesRead;
            }
            len -= thisBytesRead;
            off += thisBytesRead;
        }
        return (bytesRead == 0) ? -1 : bytesRead;
    }

    private boolean checkStream() throws IOException
    {
        if ( currentStream == null )
        {
            if ( (index + 1) < files.size() )
            {
                ++index;
                currentStream = new BufferedInputStream(new FileInputStream(files.get(index)));
            }
        }

        return currentStream != null;
    }
}
