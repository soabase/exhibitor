package com.netflix.exhibitor.core.activity;

import java.util.concurrent.Callable;

public interface Activity extends Callable<Boolean>
{
    public void     completed(boolean wasSuccessful);
}
