package com.netflix.exhibitor.spi;

/**
 * Used to add additional tabs to the Exhibitor UI
 */
public interface UITab
{
    /**
     * Return the tab name
     *
     * @return name
     */
    public String       getName();

    /**
     * Return the content (as text/plain) for the tab
     * @return content
     * @throws Exception errors
     */
    public String       getContent() throws Exception;
}
