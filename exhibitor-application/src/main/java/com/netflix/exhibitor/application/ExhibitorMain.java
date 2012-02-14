package com.netflix.exhibitor.application;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.netflix.exhibitor.core.BackupProvider;
import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.config.DefaultProperties;
import com.netflix.exhibitor.rest.ExplorerResource;
import com.netflix.exhibitor.rest.IndexResource;
import com.netflix.exhibitor.rest.UIContext;
import com.netflix.exhibitor.rest.UIResource;
import com.netflix.exhibitor.core.config.LocalFileConfigProvider;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import java.io.File;
import java.util.List;
import java.util.Set;

public class ExhibitorMain
{
    public static void main(String[] args) throws Exception
    {
        File        propertiesFile = new File("exhibitor.properties");

        Options     options  = new Options();
        options.addOption("properties", true, "Path to store Exhibitor properties. Default location is: " + propertiesFile.getCanonicalPath());
        options.addOption("?", "help", false, "Print this help");

        CommandLineParser   parser = new PosixParser();
        CommandLine         commandLine = parser.parse(options, args);
        if ( commandLine.hasOption('?') || commandLine.hasOption("help") || (commandLine.getArgList().size() > 0) )
        {
            printHelp(options);
        }

        BackupProvider backupProvider = new BackupProvider()
        {
            @Override
            public List<String> getConfigNames()
            {
                return Lists.newArrayList();
            }

            @Override
            public void backupFile(File f) throws Exception
            {
            }
        };
        Exhibitor exhibitor = new Exhibitor(new LocalFileConfigProvider(propertiesFile, DefaultProperties.get()), null, backupProvider);
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
                classes.add(ExplorerResource.class);
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
    
    private static void printHelp(Options options)
    {
        HelpFormatter       formatter = new HelpFormatter();
        formatter.printHelp("ExhibitorMain", options);
        System.exit(0);
    }
}
