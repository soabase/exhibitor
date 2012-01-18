package com.netflix.exhibitor.ui;

import com.google.common.collect.Sets;
import com.netflix.exhibitor.Exhibitor;
import com.netflix.exhibitor.UIContext;
import com.netflix.exhibitor.UIResource;
import com.netflix.exhibitor.imps.StandardProcessOperations;
import com.netflix.exhibitor.maintenance.BackupSource;
import com.netflix.exhibitor.maintenance.RestoreInstance;
import com.netflix.exhibitor.spi.ExhibitorConfig;
import com.netflix.exhibitor.spi.ProcessOperations;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import javax.ws.rs.core.Application;
import java.io.InputStream;
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
            public void backup(ExhibitorConfig backupConfig, String name, InputStream stream) throws Exception
            {
            }

            @Override
            public void checkRotation(ExhibitorConfig backupConfig) throws Exception
            {
            }

            @Override
            public RestoreInstance newRestoreInstance(ExhibitorConfig backupConfig) throws Exception
            {
                return null;
            }
        };
        Exhibitor         exhibitor = new Exhibitor(new MockExhibitorConfig(), processOperations, backupSource);
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
