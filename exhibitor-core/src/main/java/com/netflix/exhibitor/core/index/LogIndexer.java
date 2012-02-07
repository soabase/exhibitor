package com.netflix.exhibitor.core.index;

import com.google.common.io.Closeables;
import com.google.common.io.CountingInputStream;
import org.apache.jute.Record;
import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.SingleInstanceLockFactory;
import org.apache.lucene.util.Version;
import org.apache.zookeeper.txn.CreateTxn;
import org.apache.zookeeper.txn.DeleteTxn;
import org.apache.zookeeper.txn.SetDataTxn;
import org.apache.zookeeper.txn.TxnHeader;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class LogIndexer
{
    private final File logFile;
    private final File indexDirectory;
    private final CountingInputStream inputStream;
    private final IndexWriter writer;
    private final NIOFSDirectory directory;
    private final LogParser logParser;

    public LogIndexer(File logFile, File indexDirectory) throws Exception
    {
        if ( !indexDirectory.exists() && !indexDirectory.mkdirs() )
        {
            throw new IOException("Could not make: " + indexDirectory);
        }

        this.logFile = logFile;
        this.indexDirectory = indexDirectory;
        inputStream = new CountingInputStream(new BufferedInputStream(new FileInputStream(logFile)));

        IndexWriterConfig conf = new IndexWriterConfig(Version.LUCENE_35, new KeywordAnalyzer())
            .setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        directory = new NIOFSDirectory(indexDirectory, new SingleInstanceLockFactory());
        writer = new IndexWriter(directory, conf);

        logParser = new LogParser(inputStream);
    }

    public void index() throws Exception
    {
        final AtomicInteger         count = new AtomicInteger(0);
        final AtomicLong            from = new AtomicLong(Long.MAX_VALUE);
        final AtomicLong            to = new AtomicLong(Long.MIN_VALUE);
        try
        {
            logParser.parse
            (
                new LogEntryReceiver()
                {
                    @Override
                    public void receiveEntry(TxnHeader header, Record record) throws Exception
                    {
                        indexRecord(header, record, count, from, to);
                    }
                }
            );

            IndexMetaData       metaData = new IndexMetaData(new Date(from.get()), new Date(to.get()), count.get());
            IndexMetaData.write(metaData, IndexMetaData.getMetaDataFile(indexDirectory));
        }
        finally
        {
            Closeables.closeQuietly(writer);
            Closeables.closeQuietly(directory);
            Closeables.closeQuietly(inputStream);
        }
    }

    public int getPercentDone()
    {
        synchronized(inputStream)   // inputStream.getCount() should be sync/volatile but it isn't
        {
            return (int)((100 * inputStream.getCount()) / logFile.length());
        }
    }

    public File getLogFile()
    {
        return logFile;
    }

    private void indexRecord(TxnHeader header, Record record, AtomicInteger count, AtomicLong from, AtomicLong to) throws IOException
    {
        if ( record instanceof CreateTxn )
        {
            CreateTxn   createTxn = (CreateTxn)record;

            EntryTypes type = createTxn.getEphemeral() ? EntryTypes.CREATE_EPHEMERAL: EntryTypes.CREATE_PERSISTENT;
            Document document = makeDocument(header, type, count, from, to);
            addPath(document, createTxn.getPath());
            addData(document, createTxn.getData());
            writer.addDocument(document);
        }
        else if ( record instanceof DeleteTxn )
        {
            DeleteTxn   deleteTxn = (DeleteTxn)record;

            Document document = makeDocument(header, EntryTypes.DELETE, count, from, to);
            addPath(document, deleteTxn.getPath());
            writer.addDocument(document);
        }
        else if ( record instanceof SetDataTxn )
        {
            SetDataTxn   setDataTxn = (SetDataTxn)record;

            NumericField versionField = new NumericField(FieldNames.VERSION, Field.Store.YES, true);
            versionField.setIntValue(setDataTxn.getVersion());

            Document document = makeDocument(header, EntryTypes.SET_DATA, count, from, to);
            addPath(document, setDataTxn.getPath());
            addData(document, setDataTxn.getData());
            document.add(versionField);
        }
    }

    private void addData(Document document, byte[] data)
    {
        document.add(new Field(FieldNames.DATA, data));
    }

    private void addPath(Document document, String path)
    {
        document.add(new Field(FieldNames.PATH, path, Field.Store.YES, Field.Index.NOT_ANALYZED));
    }

    private Document makeDocument(TxnHeader header, EntryTypes type, AtomicInteger count, AtomicLong from, AtomicLong to)
    {
        count.incrementAndGet();
        if ( header.getTime() < from.get() )
        {
            from.set(header.getTime());
        }
        if ( header.getTime() > to.get() )
        {
            to.set(header.getTime());
        }

        NumericField dateField = new NumericField(FieldNames.DATE, Field.Store.YES, true);
        dateField.setLongValue(header.getTime());

        Document    document = new Document();
        document.add(new Field(FieldNames.TYPE, Integer.toString(type.getId()), Field.Store.YES, Field.Index.NOT_ANALYZED));
        document.add(dateField);
        return document;
    }
}
