package com.netflix.exhibitor.core.config;

import java.util.List;

public interface RollingConfigState
{
    public String           getRollingStatus();

    public int              getRollingPercentDone();

    public List<String>     getRollingHostNames();

    public int              getRollingHostNamesIndex();
}
