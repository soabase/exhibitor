package com.netflix.exhibitor.application;

import com.google.common.collect.Sets;
import com.netflix.exhibitor.core.BackupProvider;
import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.backup.s3.PropertyBasedS3Credential;
import com.netflix.exhibitor.core.backup.s3.S3BackupProvider;
import com.netflix.exhibitor.core.config.DefaultProperties;
import com.netflix.exhibitor.core.config.LocalFileConfigProvider;
import com.netflix.exhibitor.rest.ExplorerResource;
import com.netflix.exhibitor.rest.IndexResource;
import com.netflix.exhibitor.rest.UIContext;
import com.netflix.exhibitor.rest.UIResource;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Set;

public class ExhibitorMain implements Closeable
{
    private final Server server;

    public static void main(String[] args) throws Exception
    {
        File        propertiesFile = new File("exhibitor.properties");

        Options     options  = new Options();
        options.addOption(null, "properties", true, "Path to store Exhibitor properties. Default location is: " + propertiesFile.getCanonicalPath());
        options.addOption(null, "s3backup", true, "Enables AWS S3 backup of ZooKeeper log files. The argument is the path to an AWS credential properties file with two properties: " + PropertyBasedS3Credential.PROPERTY_S3_KEY_ID + " and " + PropertyBasedS3Credential.PROPERTY_S3_SECRET_KEY);
        options.addOption(null, "filesystembackup", false, "Enables file system backup of ZooKeeper log files.");
        options.addOption("?", "help", false, "Print this help");

        CommandLine         commandLine;
        try
        {
            CommandLineParser   parser = new PosixParser();
            commandLine = parser.parse(options, args);
            if ( commandLine.hasOption('?') || commandLine.hasOption("help") || (commandLine.getArgList().size() > 0) )
            {
                printHelp(options);
                return;
            }
        }
        catch ( ParseException e )
        {
            printHelp(options);
            return;
        }

        BackupProvider      backupProvider = null;
        if ( commandLine.hasOption("s3backup") )
        {
            PropertyBasedS3Credential credential = new PropertyBasedS3Credential(new File(commandLine.getOptionValue("s3backup")));
            backupProvider = new S3BackupProvider(credential);
        }
        else if ( commandLine.hasOption("filesystembackup") )
        {
            backupProvider = new FileSystemBackupProvider();
        }

        ExhibitorMain exhibitorMain = new ExhibitorMain(propertiesFile, backupProvider);
        exhibitorMain.start();
        exhibitorMain.join();
    }

    public ExhibitorMain(File propertiesFile, BackupProvider backupProvider) throws Exception
    {
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
        server = new Server(8080);
        Context root = new Context(server, "/", Context.SESSIONS);
        root.addServlet(new ServletHolder(container), "/*");
    }

    public void start() throws Exception
    {
        server.start();
    }

    public void join() throws Exception
    {
        server.join();
    }

    @Override
    public void close() throws IOException
    {
        server.destroy();
    }

    private static void printHelp(Options options)
    {
        HelpFormatter       formatter = new HelpFormatter();
        formatter.printHelp("ExhibitorMain", options);
        System.exit(0);
    }
}
