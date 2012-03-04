package com.netflix.exhibitor.core.index;

import com.google.common.io.Closeables;
import org.apache.jute.BinaryInputArchive;
import org.apache.jute.InputArchive;
import org.apache.jute.Record;
import org.apache.zookeeper.server.persistence.FileHeader;
import org.apache.zookeeper.server.persistence.FileTxnLog;
import org.apache.zookeeper.server.util.SerializeUtils;
import org.apache.zookeeper.txn.TxnHeader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

// based on ~/.m2/repository/org/apache/zookeeper/zookeeper/3.3.3/zookeeper-3.3.3-sources.jar!/org/apache/zookeeper/server/LogFormatter.java
// idea suggested by Kishore Gopalakrishna <kgopalakrishna@linkedin.com>
public class ZooKeeperLogParser
{
    private final BinaryInputArchive logStream;
    private final boolean            validHeader;

    public ZooKeeperLogParser(InputStream log) throws IOException
    {
        logStream = BinaryInputArchive.getArchive(log);

        FileHeader fhdr = new FileHeader();
        fhdr.deserialize(logStream, "fileheader");
        validHeader = (fhdr.getMagic() == FileTxnLog.TXNLOG_MAGIC);
    }

    public boolean isValid()
    {
        return validHeader;
    }

    public void parse(LogEntryReceiver receiver) throws Exception
    {
        if ( !validHeader )
        {
            throw new Exception("Invalid magic number for");
        }

        while ( true )
        {
            long crcValue;
            byte[] bytes;
            try
            {
                crcValue = logStream.readLong("crcvalue");

                bytes = logStream.readBuffer("txnEntry");
            }
            catch ( EOFException e )
            {
                break;
            }
            if ( bytes.length == 0 )
            {
                // Since we preallocate, we define EOF to be an
                // empty transaction
                break;
            }

            Checksum crc = new Adler32();
            crc.update(bytes, 0, bytes.length);
            if ( crcValue != crc.getValue() )
            {
                throw new IOException("CRC doesn't match " + crcValue + " vs " + crc.getValue());
            }
            InputArchive iab = BinaryInputArchive.getArchive(new ByteArrayInputStream(bytes));

            TxnHeader   hdr = new TxnHeader();
            Record      record = SerializeUtils.deserializeTxn(iab, hdr);

            if ( logStream.readByte("EOR") != 'B' )
            {
                break;  // partial transaction
            }

            receiver.receiveEntry(hdr, record);
        }
    }
}
