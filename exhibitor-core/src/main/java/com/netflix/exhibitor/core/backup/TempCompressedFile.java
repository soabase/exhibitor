package com.netflix.exhibitor.core.backup;

import com.google.common.io.Closeables;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

class TempCompressedFile
{
    private final File      tempFile;
    private final File      source;

    private static final int        BUFFER_SIZE = 1024 * 1024;  // 1 MB

    TempCompressedFile(File source) throws IOException
    {
        this.source = source;
        tempFile = File.createTempFile("exhibitor", ".tmp");
    }

    void        compress() throws IOException
    {
        byte[]          buffer = new byte[BUFFER_SIZE];

        InputStream     in = null;
        OutputStream    out = null;
        try
        {
            in = new FileInputStream(source);
            out = new GZIPOutputStream(new FileOutputStream(tempFile));

            for(;;)
            {
                int     bytesRead = in.read(buffer);
                if ( bytesRead < 0 )
                {
                    break;
                }
                out.write(buffer, 0, bytesRead);
            }
        }
        finally
        {
            Closeables.closeQuietly(in);
            Closeables.closeQuietly(out);
        }
    }

    File getTempFile()
    {
        return tempFile;
    }
}
