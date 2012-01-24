package com.netflix.exhibitor.activity;

import java.util.concurrent.Callable;

public interface Activity extends Callable<Boolean>
{
    public void     completed(boolean wasSuccessful);
}
