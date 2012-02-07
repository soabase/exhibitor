package com.netflix.exhibitor.application;

import com.google.common.collect.Sets;
import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.IndexResource;
import com.netflix.exhibitor.core.UIContext;
import com.netflix.exhibitor.core.UIResource;
import com.netflix.exhibitor.core.config.LocalFileConfigProvider;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import java.io.File;
import java.util.Set;

public class ExhibitorMain
{
    public static void main(String[] args) throws Exception
    {
        Exhibitor exhibitor = new Exhibitor(new LocalFileConfigProvider(new File("exhibitor.properties")), null);
        exhibitor.start();

        final UIContext context = new UIContext(exhibitor);
        DefaultResourceConfig application = new DefaultResourceConfig()
        {
            @Override
            public Set<Class<?>> getClasses()
            {
                Set<Class<?>>       classes = Sets.newHashSet();
                classes.add(UIResource.class);
                classes.add(IndexResource.class);
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
        Server server = new Server(8080);
        Context root = new Context(server, "/", Context.SESSIONS);
        root.addServlet(new ServletHolder(container), "/*");
        server.start();

        server.join();
    }
}
