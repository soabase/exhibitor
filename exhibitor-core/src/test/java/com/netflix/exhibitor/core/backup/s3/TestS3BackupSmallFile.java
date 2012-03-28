package com.netflix.exhibitor.core.backup.s3;

import java.io.IOException;

public class TestS3BackupSmallFile extends TestS3BackupProviderBase
{
    public TestS3BackupSmallFile() throws IOException
    {
        super(Filer.getFile());
    }
}
