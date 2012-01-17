package com.netflix.exhibitor.ui;

import com.google.common.collect.Sets;
import com.netflix.exhibitor.Exhibitor;
import com.netflix.exhibitor.ProcessOperations;
import com.netflix.exhibitor.UIContext;
import com.netflix.exhibitor.UIResource;
import com.netflix.exhibitor.maintenance.BackupSource;
import com.netflix.exhibitor.state.InstanceState;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import javax.ws.rs.core.Application;
import java.util.Set;

public class UIIntegrationTest
{
    public static void main(String[] args) throws Exception
    {
        ProcessOperations processOperations = new ProcessOperations()
        {
            @Override
            public void startInstance(Exhibitor exhibitor, InstanceState instanceState) throws Exception
            {
            }

            @Override
            public void killInstance(Exhibitor exhibitor) throws Exception
            {
            }

            @Override
            public void backupInstance(Exhibitor exhibitor, BackupSource source) throws Exception
            {
            }

            @Override
            public void cleanupInstance(Exhibitor exhibitor) throws Exception
            {
            }
        };
        Exhibitor         exhibitor = new Exhibitor(new MockExhibitorConfig(), processOperations);
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
