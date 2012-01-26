package com.netflix.exhibitor.imps;

import com.netflix.exhibitor.spi.GlobalSharedConfig;
import java.io.Closeable;

public interface GlobalSharedConfigBase extends GlobalSharedConfig, Closeable
{
    public void     start() throws Exception;
}
