package com.netflix.exhibitor.core.entities;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SearchResult
{
    private int         docId;
    private int         type;
    private String      path;
    private String      date;
    private String      dataAsString;
    private String      dataBytes;

    public SearchResult()
    {
        this(0, 0, "", "", "", "");
    }

    public SearchResult(int docId, int type, String path, String date, String dataAsString, String dataBytes)
    {
        this.docId = docId;
        this.type = type;
        this.path = path;
        this.date = date;
        this.dataAsString = dataAsString;
        this.dataBytes = dataBytes;
    }

    public String getDate()
    {
        return date;
    }

    public void setDate(String date)
    {
        this.date = date;
    }

    public int getDocId()
    {
        return docId;
    }

    public void setDocId(int docId)
    {
        this.docId = docId;
    }

    public int getType()
    {
        return type;
    }

    public void setType(int type)
    {
        this.type = type;
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }

    public String getDataAsString()
    {
        return dataAsString;
    }

    public void setDataAsString(String dataAsString)
    {
        this.dataAsString = dataAsString;
    }

    public String getDataBytes()
    {
        return dataBytes;
    }

    public void setDataBytes(String dataBytes)
    {
        this.dataBytes = dataBytes;
    }
}
