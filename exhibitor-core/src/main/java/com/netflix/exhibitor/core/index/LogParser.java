package com.netflix.exhibitor.core.index;

import org.apache.jute.BinaryInputArchive;
import org.apache.jute.InputArchive;
import org.apache.jute.Record;
import org.apache.zookeeper.server.persistence.FileHeader;
import org.apache.zookeeper.server.persistence.FileTxnLog;
import org.apache.zookeeper.server.util.SerializeUtils;
import org.apache.zookeeper.txn.TxnHeader;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

// based on ~/.m2/repository/org/apache/zookeeper/zookeeper/3.3.3/zookeeper-3.3.3-sources.jar!/org/apache/zookeeper/server/LogFormatter.java
public class LogParser
{
    private final InputStream log;

    public LogParser(InputStream log)
    {
        this.log = log;
    }

    public void parse(LogEntryReceiver receiver) throws Exception
    {
        BinaryInputArchive logStream = BinaryInputArchive.getArchive(log);

        FileHeader fhdr = new FileHeader();
        fhdr.deserialize(logStream, "fileheader");
        if ( fhdr.getMagic() != FileTxnLog.TXNLOG_MAGIC )
        {
            throw new Exception("Invalid magic number for");
        }

        int count = 0;
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

            count++;
        }
    }
}
