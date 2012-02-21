package com.netflix.exhibitor.core.backup.s3;

import java.io.File;
import java.io.InputStream;

/**
 * Pluggable method for compressing/decompressing in chunks
 */
public interface Compressor
{
    /**
     * Compress the given file
     *
     * @param f file
     * @return compression chunks
     * @throws Exception errors
     */
    public CompressorIterator     compress(File f) throws Exception;

    /**
     * Decompress the given stream
     *
     * @param in strema
     * @return compression chunks
     * @throws Exception errors
     */
    public CompressorIterator     decompress(InputStream in) throws Exception;
}
