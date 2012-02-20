package com.netflix.exhibitor.core.backup.s3;

import org.testng.Assert;
import org.testng.annotations.Test;
import java.io.IOException;
import java.nio.ByteBuffer;

import static com.netflix.exhibitor.core.backup.s3.Filer.getFileBytes;

public class TestChunkedStreamer
{
    @Test
    public void     testByteAtATime() throws Exception
    {
        byte[] bytes = getFileBytes();

        ChunkedStreamer     streamer = new ChunkedStreamer(1024);
        for ( byte b : bytes )
        {
            streamer.write(b);
        }
        streamer.flush();

        compare(streamer, bytes);
    }

    @Test
    public void     testMixed() throws Exception
    {
        byte[] bytes = getFileBytes();

        ChunkedStreamer     streamer = new ChunkedStreamer(1024);
        for ( int i = 0; i < bytes.length; /* no inc */)
        {
            if ( (i + 10) >= bytes.length )
            {
                streamer.write(bytes, i, bytes.length - i);
                i = bytes.length;
            }
            else
            {
                streamer.write(bytes[i]);
                ++i;
                streamer.write(bytes, i, 9);
                i += 9;
            }
        }
        streamer.flush();

        compare(streamer, bytes);
    }

    private void compare(ChunkedStreamer streamer, byte[] bytes) throws IOException
    {
        ByteBuffer      buffer = null;
        for ( byte b : bytes )
        {
            if ( (buffer == null) || !buffer.hasRemaining() )
            {
                buffer = streamer.getNextBuffer();
                Assert.assertNotNull(buffer);
            }
            Assert.assertEquals(b, buffer.get());
        }
    }
}
