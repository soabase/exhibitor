package com.netflix.exhibitor.backup;

import org.apache.jute.BinaryInputArchive;
import org.apache.jute.InputArchive;
import org.apache.jute.Record;
import org.apache.jute.RecordReader;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.proto.ConnectRequest;
import org.apache.zookeeper.proto.CreateRequest;
import org.apache.zookeeper.proto.DeleteRequest;
import org.apache.zookeeper.proto.ExistsRequest;
import org.apache.zookeeper.proto.GetChildren2Request;
import org.apache.zookeeper.proto.GetChildrenRequest;
import org.apache.zookeeper.proto.GetDataRequest;
import org.apache.zookeeper.proto.SetDataRequest;
import org.apache.zookeeper.server.DataTree;
import org.apache.zookeeper.server.persistence.FileHeader;
import org.apache.zookeeper.server.persistence.FileTxnLog;
import org.apache.zookeeper.server.util.SerializeUtils;
import org.apache.zookeeper.txn.CreateTxn;
import org.apache.zookeeper.txn.DeleteTxn;
import org.apache.zookeeper.txn.SetDataTxn;
import org.apache.zookeeper.txn.SetMaxChildrenTxn;
import org.apache.zookeeper.txn.TxnHeader;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.zip.Adler32;
import java.util.zip.Checksum;

// copied and modified from ~/.m2/repository/org/apache/zookeeper/zookeeper/3.3.3/zookeeper-3.3.3-sources.jar!/org/apache/zookeeper/server/LogFormatter.java
public class LogFormatter {
    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(LogFormatter.class);

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("USAGE: LogFormatter log_file");
            System.exit(2);
        }
        FileInputStream fis = new FileInputStream(args[0]);
        BinaryInputArchive logStream = BinaryInputArchive.getArchive(fis);
        FileHeader fhdr = new FileHeader();
        fhdr.deserialize(logStream, "fileheader");

        if (fhdr.getMagic() != FileTxnLog.TXNLOG_MAGIC) {
            System.err.println("Invalid magic number for " + args[0]);
            System.exit(2);
        }
        System.out.println("ZooKeeper Transactional Log File with dbid "
            + fhdr.getDbid() + " txnlog format version "
            + fhdr.getVersion());

        int count = 0;
        while (true) {
            long crcValue;
            byte[] bytes;
            try {
                crcValue = logStream.readLong("crcvalue");

                bytes = logStream.readBuffer("txnEntry");
            } catch (EOFException e) {
                System.out.println("EOF reached after " + count + " txns.");
                return;
            }
            if (bytes.length == 0) {
                // Since we preallocate, we define EOF to be an
                // empty transaction
                System.out.println("EOF reached after " + count + " txns.");
                return;
            }
            Checksum crc = new Adler32();
            crc.update(bytes, 0, bytes.length);
            if (crcValue != crc.getValue()) {
                throw new IOException("CRC doesn't match " + crcValue +
                    " vs " + crc.getValue());
            }
            InputArchive iab = BinaryInputArchive
                .getArchive(new ByteArrayInputStream(bytes));

            TxnHeader hdr = new TxnHeader();
            Record record = SerializeUtils.deserializeTxn(iab, hdr);
            System.out.println(DateFormat.getDateTimeInstance(DateFormat.SHORT,
                DateFormat.LONG).format(new Date(hdr.getTime()))
                + " session 0x"
                + Long.toHexString(hdr.getClientId())
                + " cxid 0x"
                + Long.toHexString(hdr.getCxid())
                + " zxid 0x"
                + Long.toHexString(hdr.getZxid())
                + " " + op2String(hdr.getType()));

            listRecord(record);
            if (logStream.readByte("EOR") != 'B') {
                LOG.error("Last transaction was partial.");
                throw new EOFException("Last transaction was partial.");
            }
            count++;
        }
    }

    private static void listRecord(Record record)
    {
        if ( record instanceof CreateTxn )
        {
            CreateTxn createRequest = (CreateTxn)record;
            System.out.println
            (
                String.format
                (
                    "CREATE - path: \"%s\", ephemeral: %s, data: %s",
                    createRequest.getPath(),
                    Boolean.toString(createRequest.getEphemeral()),
                    Arrays.toString(createRequest.getData())
                )
            );
        }

        if ( record instanceof DeleteTxn )
        {
            DeleteTxn   deleteTxn = (DeleteTxn)record;
            System.out.println
            (
                String.format
                (
                    "DELETE - path: \"%s\"",
                    deleteTxn.getPath()
                )
            );
        }

        if ( record instanceof SetDataTxn )
        {
            SetDataTxn   setDataTxn = (SetDataTxn)record;
            System.out.println
            (
                String.format
                (
                    "SET_DATA - path: \"%s\"; version: %d; data: %s",
                    setDataTxn.getPath(),
                    setDataTxn.getVersion(),
                    Arrays.toString(setDataTxn.getData())
                )
            );
        }
    }
    
    private static Record makeRecord(int op)
    {
        switch (op) {
        case ZooDefs.OpCode.create:
            return new CreateRequest();
        case ZooDefs.OpCode.delete:
            return new DeleteRequest();
        case ZooDefs.OpCode.exists:
            return new ExistsRequest();
        case ZooDefs.OpCode.getData:
            return new GetDataRequest();
        case ZooDefs.OpCode.setData:
            return new SetDataRequest();
        case ZooDefs.OpCode.getChildren:
            return new GetChildrenRequest();
        case ZooDefs.OpCode.getChildren2:
            return new GetChildren2Request();
        default:
            break;
        }
        return null;
    }

    static String op2String(int op) {
        switch (op) {
        case ZooDefs.OpCode.notification:
            return "notification";
        case ZooDefs.OpCode.create:
            return "create";
        case ZooDefs.OpCode.delete:
            return "delete";
        case ZooDefs.OpCode.exists:
            return "exists";
        case ZooDefs.OpCode.getData:
            return "getDate";
        case ZooDefs.OpCode.setData:
            return "setData";
        case ZooDefs.OpCode.getACL:
            return "getACL";
        case ZooDefs.OpCode.setACL:
            return "setACL";
        case ZooDefs.OpCode.getChildren:
            return "getChildren";
        case ZooDefs.OpCode.getChildren2:
            return "getChildren2";
        case ZooDefs.OpCode.ping:
            return "ping";
        case ZooDefs.OpCode.createSession:
            return "createSession";
        case ZooDefs.OpCode.closeSession:
            return "closeSession";
        case ZooDefs.OpCode.error:
            return "error";
        default:
            return "unknown " + op;
        }
    }
}
