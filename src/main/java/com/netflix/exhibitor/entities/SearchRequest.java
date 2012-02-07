package com.netflix.exhibitor.entities;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

@XmlRootElement
public class SearchRequest
{
    private String      indexName;
    private String      pathPrefix;
    private Date        firstDate;
    private Date        secondDate;
    private int         operationType;
    private Boolean     ephemeral;
    private String      reuseHandle;
    private int         maxResults;

    public SearchRequest()
    {
        this("", "", null, null, -1, null, null, 0);
    }

    public SearchRequest(String indexName, String pathPrefix, Date firstDate, Date secondDate, int operationType, Boolean ephemeral, String reuseHandle, int maxResults)
    {
        this.indexName = indexName;
        this.pathPrefix = pathPrefix;
        this.firstDate = firstDate;
        this.secondDate = secondDate;
        this.operationType = operationType;
        this.ephemeral = ephemeral;
        this.reuseHandle = reuseHandle;
        this.maxResults = maxResults;
    }

    public int getMaxResults()
    {
        return maxResults;
    }

    public void setMaxResults(int maxResults)
    {
        this.maxResults = maxResults;
    }

    public String getReuseHandle()
    {
        return reuseHandle;
    }

    public void setReuseHandle(String reuseHandle)
    {
        this.reuseHandle = reuseHandle;
    }

    public String getIndexName()
    {
        return indexName;
    }

    public void setIndexName(String indexName)
    {
        this.indexName = indexName;
    }

    public String getPathPrefix()
    {
        return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix)
    {
        this.pathPrefix = pathPrefix;
    }

    public Date getFirstDate()
    {
        return firstDate;
    }

    public void setFirstDate(Date firstDate)
    {
        this.firstDate = firstDate;
    }

    public Date getSecondDate()
    {
        return secondDate;
    }

    public void setSecondDate(Date secondDate)
    {
        this.secondDate = secondDate;
    }

    public int getOperationType()
    {
        return operationType;
    }

    public void setOperationType(int operationType)
    {
        this.operationType = operationType;
    }

    public Boolean getEphemeral()
    {
        return ephemeral;
    }

    public void setEphemeral(Boolean ephemeral)
    {
        this.ephemeral = ephemeral;
    }
}
