package com.netflix.exhibitor.core.backup.s3;

import java.io.File;
import java.io.IOException;
import java.net.URL;

class Filer
{
    static File getFile() throws IOException
    {
        URL resource = ClassLoader.getSystemResource("com/netflix/exhibitor/core/backup/s3/DummyFile.txt");
        return new File(resource.getPath());
    }
}
