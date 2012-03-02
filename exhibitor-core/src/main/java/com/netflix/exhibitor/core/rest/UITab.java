package com.netflix.exhibitor.core.rest;

import javax.ws.rs.core.UriInfo;

public interface UITab
{
    /**
     * Return the tab name
     *
     * @return name
     */
    String       getName();

    /**
     * Return the content (as text/plain) for the tab
     * 
     * @param info uri context
     * @return content
     * @throws Exception errors
     */
    String       getContent(UriInfo info) throws Exception;
}
