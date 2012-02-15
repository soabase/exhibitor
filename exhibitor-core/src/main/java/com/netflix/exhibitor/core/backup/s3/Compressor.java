package com.netflix.exhibitor.core.backup.s3;

import java.io.File;

public interface Compressor
{
    public CompressorIterator     compress(File f) throws Exception;
}
