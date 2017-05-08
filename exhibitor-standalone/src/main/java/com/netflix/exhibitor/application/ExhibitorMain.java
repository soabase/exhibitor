/*
 *
 *  Copyright 2011 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.netflix.exhibitor.application;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.ExhibitorArguments;
import com.netflix.exhibitor.core.RemoteConnectionConfiguration;
import com.netflix.exhibitor.core.backup.BackupProvider;
import com.netflix.exhibitor.core.config.ConfigProvider;
import com.netflix.exhibitor.core.rest.UIContext;
import com.netflix.exhibitor.core.rest.jersey.JerseySupport;
import com.netflix.exhibitor.standalone.ExhibitorCreator;
import com.netflix.exhibitor.standalone.ExhibitorCreatorExit;
import com.netflix.exhibitor.standalone.SecurityArguments;
import com.sun.jersey.api.client.filter.ClientFilter;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.api.client.filter.HTTPDigestAuthFilter;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.ServletRequest;
import org.apache.curator.utils.CloseableUtils;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebXmlConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExhibitorMain implements Closeable
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Server server;
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final Exhibitor exhibitor;
    private final AtomicBoolean shutdownSignaled = new AtomicBoolean(false);
    private final Map<String, String> users = Maps.newHashMap();

    public static void main(String[] args) throws Exception
    {
        ExhibitorCreator creator;
        try {
            creator = new ExhibitorCreator(args);
        }
        catch (ExhibitorCreatorExit exit) {
            if (exit.getError() != null) {
                System.err.println(exit.getError());
            }

            exit.getCli().printHelp();
            return;
        }

        SecurityArguments securityArguments = new SecurityArguments(creator.getSecurityFile(), creator.getRealmSpec(), creator.getRemoteAuthSpec());
        ExhibitorMain exhibitorMain = new ExhibitorMain
            (
                creator.getBackupProvider(),
                creator.getConfigProvider(),
                creator.getBuilder(),
                creator.getHttpPort(),
                securityArguments
            );
        setShutdown(exhibitorMain);

        exhibitorMain.start();
        try {
            exhibitorMain.join();
        }
        finally {
            exhibitorMain.close();

            for (Closeable closeable : creator.getCloseables()) {
                CloseableUtils.closeQuietly(closeable);
            }
        }
    }

    public ExhibitorMain(BackupProvider backupProvider, ConfigProvider configProvider, ExhibitorArguments.Builder builder, int httpPort, SecurityArguments securityArguments) throws Exception
    {
        HashLoginService loginService = makeLoginService(securityArguments);

        if (securityArguments.getRemoteAuthSpec() != null) {
            addRemoteAuth(builder, securityArguments.getRemoteAuthSpec());
        }

        builder.shutdownProc(makeShutdownProc(this));
        exhibitor = new Exhibitor(configProvider, null, backupProvider, builder.build());
        exhibitor.start();

        server = new Server(httpPort);

        // This is some magic to get path of root directory of the JAR
        // see https://github.com/jetty-project/embedded-jetty-uber-jar/blob/master/src/main/java/jetty/uber/ServerMain.java
        URL webRootLocation = ExhibitorMain.class.getClassLoader().getResource("index.html");
        if (webRootLocation == null) {
            throw new IllegalStateException("Unable to find resource directory");
        }

        URI webRootUri = URI.create(webRootLocation.toURI().toASCIIString().replaceFirst("/index.html$", "/"));
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");

        DefaultResourceConfig application = JerseySupport.newApplicationConfig(new UIContext(exhibitor));
        ServletHolder jerseyServlet = new ServletHolder(new ServletContainer(application));
        context.addServlet(jerseyServlet, "/exhibitor/*");

        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(false);
        resourceHandler.setWelcomeFiles(new String[] {"index.html"});
        resourceHandler.setResourceBase(webRootUri.toString());

        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] {resourceHandler, context, new DefaultHandler()});
        server.setHandler(handlers);

        if (securityArguments.getSecurityFile() != null) {
            addSecurityFile(loginService, securityArguments.getSecurityFile(), context);
        }
    }

    private void addRemoteAuth(ExhibitorArguments.Builder builder, String remoteAuthSpec)
    {
        String[] parts = remoteAuthSpec.split(":");
        Preconditions.checkArgument(parts.length == 2, "Badly formed remote client authorization: " + remoteAuthSpec);

        String type = parts[0].trim();
        String userName = parts[1].trim();

        String password = Preconditions.checkNotNull(users.get(userName), "Realm user not found: " + userName);

        ClientFilter filter;
        if (type.equals("basic")) {
            filter = new HTTPBasicAuthFilter(userName, password);
        }
        else if (type.equals("digest")) {
            filter = new HTTPDigestAuthFilter(userName, password);
        }
        else {
            throw new IllegalStateException("Unknown remote client authorization type: " + type);
        }

        builder.remoteConnectionConfiguration(new RemoteConnectionConfiguration(Arrays.asList(filter)));
    }

    public void start() throws Exception
    {
        server.start();
    }

    public void join()
    {
        try {
            while (!shutdownSignaled.get() && !Thread.currentThread().isInterrupted()) {
                Thread.sleep(5000);
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() throws IOException
    {
        if (isClosed.compareAndSet(false, true)) {
            log.info("Shutting down");

            CloseableUtils.closeQuietly(exhibitor);
            try {
                server.stop();
            }
            catch (Exception e) {
                log.error("Error shutting down Jetty", e);
            }
            server.destroy();
        }
    }

    private static void setShutdown(final ExhibitorMain exhibitorMain)
    {
        Runtime.getRuntime().addShutdownHook
            (
                new Thread
                    (
                        makeShutdownProc(exhibitorMain)
                    )
            );
    }

    private static Runnable makeShutdownProc(final ExhibitorMain exhibitorMain)
    {
        return new Runnable()
        {
            @Override
            public void run()
            {
                exhibitorMain.shutdownSignaled.set(true);
            }
        };
    }

    private void addSecurityFile(HashLoginService realm, String securityFile, ServletContextHandler root) throws Exception
    {
        // create a temp Jetty context to parse the security portion of the web.xml file

        /*
            TODO

            This code assumes far too much internal knowledge of Jetty. I don't know
            of simple way to parse the web.xml though and don't want to write it myself.
         */

        final URL url = new URL("file", null, securityFile);
        final WebXmlConfiguration webXmlConfiguration = new WebXmlConfiguration();
        WebAppContext context = new WebAppContext();
        context.setDescriptor(url.getPath());
        context.setServer(server);

        ContextHandler contextHandler = new ContextHandler("/")
        {
            @Override
            protected void startContext() throws Exception
            {
                super.startContext();
                setServer(server);
                webXmlConfiguration.configure(context);
            }
        };
        contextHandler.start();
        try {
            SecurityHandler securityHandler = context.getSecurityHandler();

            if (realm != null) {
                securityHandler.setLoginService(realm);
            }

            root.setSecurityHandler(securityHandler);
        }
        finally {
            contextHandler.stop();
        }
    }

    private HashLoginService makeLoginService(SecurityArguments securityArguments) throws Exception
    {
        if (securityArguments.getRealmSpec() == null) {
            return null;
        }

        String[] parts = securityArguments.getRealmSpec().split(":");
        if (parts.length != 2) {
            throw new Exception("Bad realm spec: " + securityArguments.getRealmSpec());
        }

        return new HashLoginService(parts[0].trim(), parts[1].trim())
        {
            @Override
            public UserIdentity login(final String username, final Object credentials, final ServletRequest request)
            {
                users.put(String.valueOf(username), String.valueOf(credentials));
                return super.login(username, credentials, request);
            }
        };
    }
}
