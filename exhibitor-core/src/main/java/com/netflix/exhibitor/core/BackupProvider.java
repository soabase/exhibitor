package com.netflix.exhibitor.core;

import java.io.File;
import java.util.List;

public interface BackupProvider
{
    public List<String> getConfigNames();
    
    public void     backupFile(File f) throws Exception;
}
