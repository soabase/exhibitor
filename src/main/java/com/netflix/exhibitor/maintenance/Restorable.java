package com.netflix.exhibitor.maintenance;

import java.io.InputStream;

public interface Restorable
{
    public String       getName();

    public InputStream  open() throws Exception;
}
