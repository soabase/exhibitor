package com.netflix.exhibitor.activity;

public interface Activity extends Runnable
{
    public void     completed(boolean wasSuccessful);
}
