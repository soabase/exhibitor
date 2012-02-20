package com.netflix.exhibitor.core.backup.s3;

import com.google.common.io.Files;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.GZIPOutputStream;

import static com.netflix.exhibitor.core.backup.s3.Filer.getFile;
import static com.netflix.exhibitor.core.backup.s3.Filer.getFileBytes;

public class TestGzipCompressor
{
    @Test
    public void     testDecompress() throws Exception
    {
        byte[]                  fileBytes = getFileBytes();
        ByteArrayOutputStream   zipOut = new ByteArrayOutputStream();
        GZIPOutputStream        zip = new GZIPOutputStream(zipOut);
        zip.write(fileBytes);
        zip.close();
        GzipCompressor          compressor = new GzipCompressor(1024);
        CompressorIterator      iterator = compressor.decompress(new ByteArrayInputStream(zipOut.toByteArray()));

        int         offset = 0;
        for(;;)
        {
            ByteBuffer buffer = iterator.next();
            if ( buffer == null )
            {
                break;
            }
            ByteBuffer  compare = ByteBuffer.wrap(fileBytes, offset, buffer.remaining());
            offset += buffer.remaining();
            Assert.assertEquals(buffer, compare);
        }
    }

    @Test
    public void     testCompress() throws Exception
    {
        ByteArrayOutputStream   out = new ByteArrayOutputStream();

        File                    file = getFile();
        GzipCompressor          compressor = new GzipCompressor(1024);
        CompressorIterator      iterator = compressor.compress(file);
        for(;;)
        {
            ByteBuffer  buffer = iterator.next();
            if ( buffer == null )
            {
                break;
            }
            out.write(buffer.array(), 0, buffer.remaining());
        }
        iterator.close();

        compare(out, file);
    }

    private void compare(ByteArrayOutputStream out, File file) throws IOException
    {
        ByteArrayOutputStream   zipOut = new ByteArrayOutputStream();
        GZIPOutputStream zip = new GZIPOutputStream(zipOut);
        zip.write(Files.toByteArray(file));
        zip.close();
        Assert.assertEquals(zipOut.toByteArray(), out.toByteArray());
    }
}
