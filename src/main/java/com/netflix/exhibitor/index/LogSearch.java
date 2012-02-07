package com.netflix.exhibitor.index;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.io.Closeables;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.NativeFSLockFactory;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class LogSearch implements Closeable
{
    private final Directory directory;
    private final IndexReader reader;
    private final IndexSearcher searcher;
    private final Cache<String, TopDocs> cache = CacheBuilder.newBuilder()
        .expireAfterAccess(5, TimeUnit.MINUTES) // does this need to be configurable?
        .build();

    private static final int            MAX_RESULTS = 5000; // does this need to be configurable?

    public LogSearch(File indexDirectory) throws Exception
    {
        directory = new NIOFSDirectory(indexDirectory, new NativeFSLockFactory());
        reader = IndexReader.open(directory);
        searcher = new IndexSearcher(reader);
    }

    public int      getDocQty()
    {
        return reader.numDocs();
    }

    public String    cacheSearch(Query query, String reuseId, int maxResults) throws IOException
    {
        if ( (maxResults <= 0) || (maxResults > MAX_RESULTS) )
        {
            maxResults = MAX_RESULTS;
        }
        
        String      id = ((reuseId != null) && (reuseId.length() > 0)) ? reuseId : UUID.randomUUID().toString();
        if ( query != null )    // otherwise it's an All Docs search which is the default
        {
            TopDocs docs = search(query, maxResults);
            docs.totalHits = Math.min(maxResults, docs.totalHits);
            cache.put(id, docs);
        }
        return id;
    }
    
    public CachedSearch getCachedSearch(String id)
    {
        return new CachedSearch(cache.getIfPresent(id), getDocQty());
    }

    public TopDocs   search(Query query, int maxResults) throws IOException
    {
        return searcher.search(query, maxResults);
    }

    public SearchItem toResult(int documentId) throws IOException
    {
        Document        document = searcher.doc(documentId);

        String          type = document.getFieldable(FieldNames.TYPE).stringValue();
        NumericField    date = (NumericField)document.getFieldable(FieldNames.DATE);
        Fieldable       path = document.getFieldable(FieldNames.PATH);
        NumericField    version = (NumericField)document.getFieldable(FieldNames.VERSION);
        Fieldable       ephemeral = document.getFieldable(FieldNames.EPHEMERAL);
        return new SearchItem
        (
            Integer.parseInt(type),
            path.stringValue(),
            (version != null) ? version.getNumericValue().intValue() : -1,
            new Date(date.getNumericValue().longValue()),
            (ephemeral != null) && ephemeral.stringValue().equals("1")
        );
    }

    public byte[]           toData(int documentId) throws IOException
    {
        Document document = searcher.doc(documentId);
        return document.getBinaryValue(FieldNames.DATA);
    }

    @Override
    public void close()
    {
        Closeables.closeQuietly(searcher);
        Closeables.closeQuietly(reader);
        Closeables.closeQuietly(directory);
    }
}
