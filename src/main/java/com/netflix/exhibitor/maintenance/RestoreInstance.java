package com.netflix.exhibitor.maintenance;

import java.io.Closeable;
import java.util.List;

public interface RestoreInstance extends Closeable
{
    public List<Restorable> start() throws Exception;
}
