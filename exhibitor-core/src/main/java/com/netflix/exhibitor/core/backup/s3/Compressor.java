package com.netflix.exhibitor.core.backup.s3;

import java.io.File;
import java.io.InputStream;

public interface Compressor
{
    public CompressorIterator     compress(File f) throws Exception;
    
    public CompressorIterator     decompress(InputStream in) throws Exception;
}
