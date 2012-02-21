package com.netflix.exhibitor.core.backup.s3;

import java.io.Closeable;
import java.nio.ByteBuffer;

/**
 * Returns compression chunks
 */
public interface CompressorIterator extends Closeable
{
    /**
     * Return the next chunk or null when complete
     *
     * @return chunk or null
     * @throws Exception errors
     */
    public ByteBuffer next() throws Exception;
}
