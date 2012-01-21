package com.netflix.exhibitor.ui;

import com.google.common.collect.Sets;
import com.netflix.exhibitor.Exhibitor;
import com.netflix.exhibitor.InstanceConfig;
import com.netflix.exhibitor.UIContext;
import com.netflix.exhibitor.UIResource;
import com.netflix.exhibitor.imps.StandardProcessOperations;
import com.netflix.exhibitor.spi.BackupSource;
import com.netflix.exhibitor.spi.BackupSpec;
import com.netflix.exhibitor.spi.GlobalSharedConfig;
import com.netflix.exhibitor.spi.ProcessOperations;
import com.netflix.exhibitor.spi.ServerInfo;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import javax.ws.rs.core.Application;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

public class UIIntegrationTest
{
    public static void main(String[] args) throws Exception
    {
        if ( args.length != 2 )
        {
            System.err.println("UIIntegrationTest [path to ZooKeeper root] [path to data directory]");
            return;
        }

        ProcessOperations processOperations = new StandardProcessOperations(args[0], args[1]);
        BackupSource      backupSource = new BackupSource()
        {
            @Override
            public Collection<BackupSpec> getAvailableBackups()
            {
                return null;
            }

            @Override
            public void backup(InstanceConfig backupConfig, String name, InputStream stream) throws Exception
            {
            }

            @Override
            public InputStream openRestoreStream(InstanceConfig backupConfig, BackupSpec spec) throws Exception
            {
                return null;
            }
        };

        GlobalSharedConfig  globalSharedConfig = new GlobalSharedConfig()
        {
            private volatile Collection<String> backupPaths;
            private volatile Collection<ServerInfo> servers;

            @Override
            public Collection<ServerInfo> getServers()
            {
                return servers;
            }

            @Override
            public void setServers(Collection<ServerInfo> newServers) throws Exception
            {
                servers = newServers;
            }

            @Override
            public Collection<String> getBackupPaths()
            {
                return backupPaths;
            }

            @Override
            public void setBackupPaths(Collection<String> newBackupPaths) throws Exception
            {
                this.backupPaths = newBackupPaths;
            }
        };

        InstanceConfig      config = InstanceConfig.builder().hostname("localhost").build();

        Exhibitor           exhibitor = new Exhibitor(config, globalSharedConfig, processOperations, backupSource);
        exhibitor.start();

        final UIContext   context = new UIContext(exhibitor);
        Application application = new DefaultResourceConfig()
        {
            @Override
            public Set<Class<?>> getClasses()
            {
                Set<Class<?>>       classes = Sets.newHashSet();
                classes.add(UIResource.class);
                return classes;
            }

            @Override
            public Set<Object> getSingletons()
            {
                Set<Object>     singletons = Sets.newHashSet();
                singletons.add(context);
                return singletons;
            }
        };
        ServletContainer container = new ServletContainer(application);
        Server  server = new Server(8080);
        Context root = new Context(server, "/", Context.SESSIONS);
        root.addServlet(new ServletHolder(container), "/*");
        server.start();

        server.join();
    }

}
