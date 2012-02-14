package com.netflix.exhibitor.core;

import com.netflix.exhibitor.core.config.IntConfigs;
import com.netflix.exhibitor.core.config.StringConfigs;

public interface InstanceConfig
{
    public String       getString(StringConfigs config);

    public int          getInt(IntConfigs config);
}
