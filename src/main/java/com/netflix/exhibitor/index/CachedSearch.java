package com.netflix.exhibitor.index;

import org.apache.lucene.search.TopDocs;

public class CachedSearch
{
    private final TopDocs docs;
    private final int totalDocs;

    CachedSearch(TopDocs docs, int totalDocs)
    {
        this.docs = docs;
        this.totalDocs = totalDocs;
    }

    public int  getTotalHits()
    {
        return (docs != null) ? docs.totalHits : totalDocs;
    }
    
    public int  getNthDocId(int n)
    {
        return (docs != null) ? docs.scoreDocs[n].doc : n;
    }
}
