package com.netflix.exhibitor.core.backup.s3;

import java.io.Closeable;
import java.nio.ByteBuffer;

public interface CompressorIterator extends Closeable
{
    public ByteBuffer next() throws Exception;
}
