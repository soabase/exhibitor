package com.netflix.exhibitor.core.backup;

/**
 * POJO that holds a displayable name and the backup session ID
 */
public class SessionAndName
{
    public final String        name;
    public final long          session;

    SessionAndName(String name, long session)
    {
        this.name = name;
        this.session = session;
    }
}
