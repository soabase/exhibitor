package com.netflix.exhibitor.ui;

import com.google.common.collect.Sets;
import com.netflix.exhibitor.Exhibitor;
import com.netflix.exhibitor.InstanceConfig;
import com.netflix.exhibitor.UIContext;
import com.netflix.exhibitor.UIResource;
import com.netflix.exhibitor.imps.StandardProcessOperations;
import com.netflix.exhibitor.mocks.MockGlobalSharedConfig;
import com.netflix.exhibitor.spi.GlobalSharedConfig;
import com.netflix.exhibitor.spi.ProcessOperations;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
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

        ProcessOperations   processOperations = new StandardProcessOperations(null, args[0], args[1]);
        GlobalSharedConfig  globalSharedConfig = new MockGlobalSharedConfig();

        InstanceConfig      config = InstanceConfig.builder().hostname("localhost").build();

        Exhibitor           exhibitor = new Exhibitor(config, globalSharedConfig, processOperations);
        exhibitor.start();

        final UIContext   context = new UIContext(exhibitor);
        DefaultResourceConfig application = new DefaultResourceConfig()
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
