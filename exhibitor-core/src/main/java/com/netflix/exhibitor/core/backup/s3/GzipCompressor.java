package com.netflix.exhibitor.core.backup.s3;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;

public class GzipCompressor implements Compressor
{
    private static final int        CHUNK_SIZE = 1024 * 1024;   // 1 MB

    @Override
    public CompressorIterator compress(File f) throws Exception
    {
        final InputStream           in = new GZIPInputStream(new BufferedInputStream(new FileInputStream(f)));
        return new CompressorIterator()
        {
            @Override
            public ByteBuffer next() throws Exception
            {
                byte[]          bytes = new byte[CHUNK_SIZE];
                int             bytesRead = in.read(bytes);
                if ( bytesRead < 0  )
                {
                    return null;
                }
                return ByteBuffer.wrap(bytes, 0, bytesRead);
            }

            @Override
            public void close() throws IOException
            {
                in.close();
            }
        };
    }
}
