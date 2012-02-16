package com.netflix.exhibitor.core.backup.s3;

import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.List;

class ChunkedGZipper extends OutputStream
{
    private final List<ByteBuffer> buffers = Lists.newArrayList();

    private ByteBuffer   currentBuffer = null;
    private State        state = State.OPEN;

    private enum State
    {
        OPEN,
        PENDING_CLOSE,
        CLOSED
    }

    @Override
    public void write(int b) throws IOException
    {
        getBuffer().put((byte)(b & 0xff));
    }

    @Override
    public void write(byte[] b) throws IOException
    {
        super.write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException
    {
        while ( len > 0 )
        {
            ByteBuffer buffer = getBuffer();

            int     thisLen = Math.min(buffer.remaining(), len);
            buffer.put(b, off, thisLen);
            off += thisLen;
            len -= thisLen;
        }
    }

    @Override
    public void close() throws IOException
    {
        if ( state == State.OPEN )
        {
            flush();
            state = State.PENDING_CLOSE;
        }
    }

    public boolean      isOpen()
    {
        if ( state == State.PENDING_CLOSE )
        {
            if ( buffers.size() == 0 )
            {
                state = State.CLOSED;
            }
        }
        return state != State.CLOSED;
    }

    public ByteBuffer getNextBuffer()
    {
        return (buffers.size() > 0) ? buffers.remove(0) : null;
    }

    @Override
    public void flush() throws IOException
    {
        if ( currentBuffer != null )
        {
            if ( currentBuffer.position() > 0 )
            {
                buffers.add(currentBuffer);
            }
        }
    }

    private ByteBuffer       getBuffer()
    {
        if ( (currentBuffer != null) && !currentBuffer.hasRemaining() )
        {
            buffers.add(currentBuffer);
            currentBuffer = null;
        }
        if ( currentBuffer == null )
        {
            currentBuffer = ByteBuffer.allocate(GzipCompressor.CHUNK_SIZE);
        }
        return currentBuffer;
    }
}
