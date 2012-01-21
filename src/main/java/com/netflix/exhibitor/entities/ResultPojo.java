package com.netflix.exhibitor.entities;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class ResultPojo
{
    private String      message;
    private boolean     succeeded;

    public ResultPojo()
    {
        this("", false);
    }

    public ResultPojo(String message, boolean succeeded)
    {
        this.message = message;
        this.succeeded = succeeded;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public boolean isSucceeded()
    {
        return succeeded;
    }

    public void setSucceeded(boolean succeeded)
    {
        this.succeeded = succeeded;
    }
}
