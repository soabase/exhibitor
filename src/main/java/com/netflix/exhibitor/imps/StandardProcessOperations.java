package com.netflix.exhibitor.imps;

import com.google.common.collect.Iterables;
import com.google.common.io.Closeables;
import com.google.common.io.Files;
import com.netflix.exhibitor.Exhibitor;
import com.netflix.exhibitor.activity.ActivityLog;
import com.netflix.exhibitor.spi.ProcessOperations;
import com.netflix.exhibitor.spi.ServerInfo;
import com.netflix.exhibitor.state.InstanceStateManager;
import java.io.*;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StandardProcessOperations implements ProcessOperations
{
    private final File zooKeeperDirectory;
    private final File dataDirectory;
    private final File configDirectory;
    private final File log4jJarPath;
    private final File zooKeeperJarPath;
    private final Properties properties;
    private final Exhibitor exhibitor;

    private static final String     MODIFIED_CONFIG_NAME = "exhibitor.cfg";
    private static final String     SNAPSHOT_PREFIX = "snapshot.";

    private static final int        LOG_BACKUP_COUNT = 3;   // TODO - make configurable

    public StandardProcessOperations(Exhibitor exhibitor, String zooKeeperDirectory, String dataDirectory) throws IOException
    {
        this.exhibitor = exhibitor;
        this.zooKeeperDirectory = new File(zooKeeperDirectory);
        this.dataDirectory = new File(dataDirectory);

        configDirectory = new File(zooKeeperDirectory, "conf");
        log4jJarPath = findJar(new File(zooKeeperDirectory, "lib"), "log4j");
        zooKeeperJarPath = findJar(this.zooKeeperDirectory, "zookeeper");

        properties = new Properties();
        InputStream     in = new BufferedInputStream(new FileInputStream(new File(configDirectory, "zoo.cfg")));
        try
        {
            properties.load(in);
        }
        finally
        {
            Closeables.closeQuietly(in);
        }
        properties.setProperty("dataDir", dataDirectory);
    }

    @Override
    public void cleanupInstance() throws Exception
    {
        // see http://zookeeper.apache.org/doc/r3.3.3/zookeeperAdmin.html#Ongoing+Data+Directory+Cleanup
        ProcessBuilder      builder = new ProcessBuilder
        (
            "java",
            "-cp",
            String.format("%s:%s:%s", zooKeeperJarPath.getPath(), log4jJarPath.getPath(), configDirectory.getPath()),
            "org.apache.zookeeper.server.PurgeTxnLog",
            dataDirectory.getPath(),
            dataDirectory.getPath(),
            "-n",
            Integer.toString(LOG_BACKUP_COUNT)
        );

        ExecutorService errorService = Executors.newSingleThreadExecutor();
        StringWriter        errors = new StringWriter();
        final PrintWriter   errorOut = new PrintWriter(errors);
        try
        {
            Process             process = builder.start();
            final InputStream   errorStream = process.getErrorStream();
            errorService.submit
            (
                new Callable<Object>()
                {
                    public Object call() throws Exception
                    {
                        BufferedReader      in = new BufferedReader(new InputStreamReader(errorStream));
                        for(;;)
                        {
                            String  line = in.readLine();
                            if ( line == null )
                            {
                                break;
                            }
                            errorOut.println(line);
                        }
                        return null;
                    }
                }
            );
            process.waitFor();

            errorOut.close();
            String      errorStr = errors.toString();
            if ( errorStr.length() > 0 )
            {
                exhibitor.getLog().add(ActivityLog.Type.ERROR, "Cleanup task reported errors: " + errorStr);
            }
            else
            {
                exhibitor.getLog().add(ActivityLog.Type.INFO, "Cleanup task ran successfully");
            }
        }
        finally
        {
            errorService.shutdownNow();
        }
    }

    @Override
    public void killInstance() throws Exception
    {
        exhibitor.getLog().add(ActivityLog.Type.INFO, "Attempting to start/restart ZooKeeper");

        ProcessBuilder          builder = new ProcessBuilder("jps");
        Process                 jpsProcess = builder.start();
        String                  pid = null;
        try
        {
            BufferedReader in = new BufferedReader(new InputStreamReader(jpsProcess.getInputStream()));
            for(;;)
            {
                String  line = in.readLine();
                if ( line == null )
                {
                    break;
                }
                String[]  components = line.split("[ \t]");
                if ( (components.length == 2) && components[1].equals("QuorumPeerMain") )
                {
                    pid = components[0];
                    break;
                }
            }
        }
        finally
        {
            jpsProcess.destroy();
        }

        if ( pid == null )
        {
            exhibitor.getLog().add(ActivityLog.Type.ERROR, "jps didn't find instance - assuming ZK is not running");
        }
        else
        {
            builder = new ProcessBuilder("kill", "-9", pid);
            try
            {
                int     result = builder.start().waitFor();
                exhibitor.getLog().add(ActivityLog.Type.INFO, "Kill attempted result: " + result);
            }
            catch ( InterruptedException e )
            {
                // don't reset thread interrupted status

                exhibitor.getLog().add(ActivityLog.Type.ERROR, "Process interrupted while running: kill -9 " + pid);
                throw e;
            }
        }
    }

    @Override
    public void startInstance() throws Exception
    {
        File            configFile = prepConfigFile(exhibitor);
        File            binDirectory = new File(zooKeeperDirectory, "bin");
        File            startScript = new File(binDirectory, "zkServer.sh");
        ProcessBuilder  builder = new ProcessBuilder(startScript.getPath(), "start").directory(binDirectory.getParentFile());
        builder.environment().put("ZOOCFG", configFile.getName());
        builder.start();

        exhibitor.getLog().add(ActivityLog.Type.INFO, "Process started via: " + startScript.getPath());
    }

    private File findJar(File dir, final String name) throws IOException
    {
        File[]          snapshots = dir.listFiles
        (
            new FileFilter()
            {
                @Override
                public boolean accept(File f)
                {
                    return f.getName().startsWith(name) && f.getName().endsWith(".jar");
                }
            }
        );
        if ( snapshots.length == 0 )
        {
            throw new IOException("Could not find " + name + " jar");
        }
        return snapshots[0];
    }

    private File prepConfigFile(Exhibitor exhibitor) throws IOException
    {
        ServerInfo      us = Iterables.find(exhibitor.getGlobalSharedConfig().getServers(), InstanceStateManager.isUs);
        
        File            idFile = new File(dataDirectory, "myid");
        Files.createParentDirs(idFile);
        String          id = String.format("%d\n", us.getId());
        Files.write(id.getBytes(), idFile);

        Properties      localProperties = new Properties();
        localProperties.putAll(properties);

        localProperties.setProperty("clientPort", Integer.toString(exhibitor.getConfig().getClientPort()));

        String          portSpec = String.format(":%d:%d", exhibitor.getConfig().getConnectPort(), exhibitor.getConfig().getElectionPort());
        for ( ServerInfo server : exhibitor.getGlobalSharedConfig().getServers() )
        {
            localProperties.setProperty("server." + server.getId(), server.getHostname() + portSpec);
        }

        File            configFile = new File(configDirectory, MODIFIED_CONFIG_NAME);
        OutputStream out = new BufferedOutputStream(new FileOutputStream(configFile));
        try
        {
            localProperties.store(out, "Auto-generated by Exhibitor - " + new Date());
        }
        finally
        {
            Closeables.closeQuietly(out);
        }

        return configFile;
    }
}
