package com.netflix.exhibitor.core.activity;

import java.util.concurrent.Callable;

/**
 * Represents a queue-able activity
 */
public interface Activity extends Callable<Boolean>
{
    /**
     * Called when the activity has completed
     * 
     * @param wasSuccessful the result of {@link #call()}
     */
    public void     completed(boolean wasSuccessful);
}
