package com.netflix.exhibitor.entities;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Result
{
    private String      message;
    private boolean     succeeded;

    public Result()
    {
        this("", false);
    }

    public Result(String message, boolean succeeded)
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
