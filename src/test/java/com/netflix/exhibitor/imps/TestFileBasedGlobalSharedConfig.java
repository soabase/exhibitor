package com.netflix.exhibitor.imps;

import com.netflix.exhibitor.InstanceConfig;
import com.netflix.exhibitor.activity.ActivityLog;
import com.netflix.exhibitor.pojos.BackupPath;
import com.netflix.exhibitor.pojos.ServerInfo;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class TestFileBasedGlobalSharedConfig extends TestGlobalSharedConfigImps<File>
{
    private static final List<ServerInfo>   serversList = Arrays.asList(new ServerInfo("localhost", 1, true), new ServerInfo("anotherhost", 2, false));
    private static final List<BackupPath>   backupPathList = Arrays.asList(new BackupPath("/a", false), new BackupPath("/b", true), new BackupPath("/c", true));

    @Override
    protected GlobalSharedConfigBase makeConfig(File context, InstanceConfig config, ActivityLog log, int sleepMs)
    {
        return new FileBasedGlobalSharedConfig(context, config, log, sleepMs);
    }

    @Override
    protected File makeContext() throws IOException
    {
        return File.createTempFile("temp", ".txt");
    }

    @Override
    protected void deleteContext(File context)
    {
        //noinspection ResultOfMethodCallIgnored
        context.delete();
    }
}
