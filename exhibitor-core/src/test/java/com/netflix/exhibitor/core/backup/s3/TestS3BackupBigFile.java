package com.netflix.exhibitor.core.backup.s3;

import com.google.common.io.Closeables;
import com.google.common.io.Files;
import org.testng.annotations.AfterClass;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class TestS3BackupBigFile extends TestS3BackupProviderBase
{
    public TestS3BackupBigFile() throws IOException
    {
        super(getFile());
    }

    private static File getFile() throws IOException
    {
        File        f = Filer.getFile();
        byte[]      bytes = Files.toByteArray(f);
        
        File            tempFile = File.createTempFile("temp", ".tmp");
        OutputStream    out = new FileOutputStream(tempFile);
        try
        {
            for ( int i = 0; i < 100; ++i )
            {
                out.write(bytes);
            }
        }
        finally
        {
            Closeables.closeQuietly(out);
        }

        return tempFile;
    }

    @AfterClass
    public void     teardown()
    {
        //noinspection ResultOfMethodCallIgnored
        sourceFile.delete();
    }
}
