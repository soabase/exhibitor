package com.netflix.exhibitor.core.backup.s3;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.zip.GZIPOutputStream;

public class GzipCompressor implements Compressor
{
    static final int        CHUNK_SIZE = 1024 * 1024;   // 1 MB

    @Override
    public CompressorIterator compress(File f) throws Exception
    {
        final ByteBuffer            buffer = ByteBuffer.allocate(CHUNK_SIZE);
        final InputStream           in = new BufferedInputStream(new FileInputStream(f));
        final ChunkedGZipper        gZipper = new ChunkedGZipper();
        return new CompressorIterator()
        {
            @Override
            public ByteBuffer next() throws Exception
            {
                while ( gZipper.isOpen() )
                {
                    ByteBuffer      pending = gZipper.getNextBuffer();
                    if ( pending != null )
                    {
                        return pending;
                    }

                    byte[]          bytes = new byte[CHUNK_SIZE];
                    int             bytesRead = in.read(bytes);
                    if ( bytesRead >= 0  )
                    {
                        gZipper.write(bytes, 0, bytesRead);
                    }
                    else
                    {
                        gZipper.close();
                    }
                }

                return null;
            }

            @Override
            public void close() throws IOException
            {
                in.close();
                gZipper.close();
            }
        };
    }

    @Override
    public CompressorIterator decompress(InputStream in) throws Exception
    {
        return null;
    }
}
