package com.netflix.exhibitor.core.rest;

import javax.ws.rs.core.UriInfo;

/**
 * Used to add additional tabs to the Exhibitor UI
 */
@SuppressWarnings("UnusedDeclaration")
public class UITabImpl implements UITab
{
    private final String        name;
    private final String        content;

    public UITabImpl()
    {
        name = "";
        content = "";
    }

    public UITabImpl(String name)
    {
        this.name = name;
        content = "";
    }

    public UITabImpl(String name, String content)
    {
        this.name = name;
        this.content = content;
    }

    /**
     * Return the tab name
     *
     * @return name
     */
    @Override
    public String       getName()
    {
        return name;
    }

    /**
     * Return the content (as text/plain) for the tab
     * @return content
     * @throws Exception errors
     * @param info
     */
    @Override
    public String       getContent(UriInfo info) throws Exception
    {
        return content;
    }
}
