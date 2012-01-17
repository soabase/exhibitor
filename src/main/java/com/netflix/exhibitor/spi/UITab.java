package com.netflix.exhibitor.spi;

public interface UITab
{
    public String       getName();

    public String       getContent() throws Exception;
}
