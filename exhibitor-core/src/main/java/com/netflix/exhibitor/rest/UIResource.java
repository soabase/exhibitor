package com.netflix.exhibitor.rest;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import com.netflix.exhibitor.core.activity.QueueGroups;
import com.netflix.exhibitor.core.backup.BackupConfigParser;
import com.netflix.exhibitor.core.backup.BackupConfigSpec;
import com.netflix.exhibitor.core.cluster.ServerList;
import com.netflix.exhibitor.core.cluster.ServerSpec;
import com.netflix.exhibitor.core.config.InstanceConfig;
import com.netflix.exhibitor.core.config.IntConfigs;
import com.netflix.exhibitor.core.config.StringConfigs;
import com.netflix.exhibitor.core.controlpanel.ControlPanelTypes;
import com.netflix.exhibitor.core.entities.Result;
import com.netflix.exhibitor.core.entities.UITabSpec;
import com.netflix.exhibitor.core.state.FourLetterWord;
import com.netflix.exhibitor.core.state.KillRunningInstance;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import javax.activation.MimetypesFileTypeMap;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ContextResolver;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * REST calls for the general Exhibitor UI
 */
@Path("exhibitor/v1/ui")
public class UIResource
{
    private final UIContext context;
    private final List<UITab> tabs;

    public UIResource(@Context ContextResolver<UIContext> resolver)
    {
        context = resolver.getContext(UIContext.class);
        tabs = buildTabs();
    }

    @Path("{file:.*}")
    @GET
    public Response getResource(@PathParam("file") String fileName) throws IOException
    {
        URL resource;
        try
        {
            resource = Resources.getResource("com/netflix/exhibitor/core/ui/" + fileName);
        }
        catch ( IllegalArgumentException dummy )
        {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        String resourceFile = resource.getFile();
        String contentType;
        if ( resourceFile.endsWith(".png") )
        {
            contentType = "image/png";  // not in default mime types
        }
        else if ( resourceFile.endsWith(".js") )
        {
            contentType = "text/javascript";  // not in default mime types
        }
        else if ( resourceFile.endsWith(".css") )
        {
            contentType = "text/css";  // not in default mime types
        }
        else
        {
            contentType = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(resourceFile);
        }
        Object entity;
        if ( contentType.startsWith("text/") )
        {
            entity = Resources.toString(resource, Charset.forName("UTF-8"));
        }
        else
        {
            entity = Resources.toByteArray(resource);
        }
        return Response.ok(entity).type(contentType).build();
    }

    @Path("tabs")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAdditionalTabSpecs()
    {
        final AtomicInteger     index = new AtomicInteger(0);
        List<UITabSpec>         names = Lists.transform
        (
            tabs,
            new Function<UITab, UITabSpec>()
            {
                @Override
                public UITabSpec apply(UITab tab)
                {
                    return new UITabSpec(tab.getName(), "tab/" + index.getAndIncrement());
                }
            }
        );

        GenericEntity<List<UITabSpec>> entity = new GenericEntity<List<UITabSpec>>(names){};
        return Response.ok(entity).build();
    }

    @Path("4ltr/{word}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getFourLetterWord(@PathParam("word") String word) throws Exception
    {
        InstanceConfig config = context.getExhibitor().getConfigManager().getConfig();

        String      value;
        try
        {
            FourLetterWord.Word wordEnum = FourLetterWord.Word.valueOf(word.toUpperCase());
            value = new FourLetterWord(wordEnum, config, context.getExhibitor().getConnectionTimeOutMs()).getResponse();
        }
        catch ( IllegalArgumentException e )
        {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(value).build();
    }

    @Path("tab/{index}")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getAdditionalTabContent(@PathParam("index") int index) throws Exception
    {
        if ( (index < 0) || (index >= tabs.size()) )
        {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(tabs.get(index).getContent()).build();
    }

    @Path("able-backups/{value}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response tempDisableBackups(@PathParam("value") boolean value) throws Exception
    {
        context.getExhibitor().getBackupManager().setTempDisabled(!value);
        return Response.ok(new Result("OK", true)).build();
    }

    @Path("backup-config")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getBackupConfig() throws Exception
    {
        ObjectMapper        mapper = new ObjectMapper();
        ArrayNode           node = mapper.getNodeFactory().arrayNode();

        if ( context.getExhibitor().getBackupManager().isActive() )
        {
            BackupConfigParser  parser = context.getExhibitor().getBackupManager().getBackupConfigParser();
            List<BackupConfigSpec>  configs = context.getExhibitor().getBackupManager().getConfigSpecs();
            for ( BackupConfigSpec c : configs )
            {
                ObjectNode      n = mapper.getNodeFactory().objectNode();
                n.put("key", c.getKey());
                n.put("name", c.getDisplayName());
                n.put("help", c.getHelpText());
                n.put("value", parser.getValues().get(c.getKey()));
                n.put("type", c.getType().name().toLowerCase().substring(0, 1));

                node.add(n);
            }
        }

        return mapper.writer().writeValueAsString(node);
    }

    @Path("state")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getSystemState() throws Exception
    {
        InstanceConfig              config = context.getExhibitor().getConfigManager().getConfig();

        String                      response = new FourLetterWord(FourLetterWord.Word.RUOK, config, context.getExhibitor().getConnectionTimeOutMs()).getResponse();
        ServerList                  serverList = new ServerList(config.getString(StringConfigs.SERVERS_SPEC));
        ServerSpec us = Iterables.find(serverList.getSpecs(), ServerList.isUs(context.getExhibitor().getThisJVMHostname()), null);

        ObjectMapper                mapper = new ObjectMapper();
        ObjectNode                  mainNode = mapper.getNodeFactory().objectNode();
        ObjectNode                  configNode = mapper.getNodeFactory().objectNode();

        mainNode.put("version", "v0.0.1");       // TODO - correct version
        mainNode.put("running", "imok".equals(response));

        configNode.put("hostname", context.getExhibitor().getThisJVMHostname());
        configNode.put("serverId", (us != null) ? us.getServerId() : -1);
        for ( StringConfigs c : StringConfigs.values() )
        {
            configNode.put(fixName(c), config.getString(c));
        }
        for ( IntConfigs c : IntConfigs.values() )
        {
            configNode.put(fixName(c), config.getInt(c));
        }

        if ( context.getExhibitor().getBackupManager().isActive() )
        {
            ObjectNode          backupExtraNode = mapper.getNodeFactory().objectNode();
            BackupConfigParser  parser = context.getExhibitor().getBackupManager().getBackupConfigParser();
            List<BackupConfigSpec>  configs = context.getExhibitor().getBackupManager().getConfigSpecs();
            for ( BackupConfigSpec c : configs )
            {
                backupExtraNode.put(c.getKey(), parser.getValues().get(c.getKey()));
            }
            configNode.put("backupExtra", backupExtraNode);
        }

        mainNode.put("config", configNode);

        return mapper.writer().writeValueAsString(mainNode);
    }

    @Path("set/config")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response setConfig(String newConfigJson) throws Exception
    {
        // TODO - should flush caches as needed

        ObjectMapper          mapper = new ObjectMapper();
        final JsonNode        tree = mapper.reader().readTree(newConfigJson);

        String                backupExtraValue = "";
        if ( tree.has("backupExtra") )
        {
            Map<String, String>     values = Maps.newHashMap();
            JsonNode                backupExtra = tree.get("backupExtra");
            Iterator<String>        fieldNames = backupExtra.getFieldNames();
            while ( fieldNames.hasNext() )
            {
                String      name = fieldNames.next();
                String      value = backupExtra.get(name).getTextValue();
                values.put(name, value);
            }
            backupExtraValue = new BackupConfigParser(values).toEncoded();
        }

        final String          finalBackupExtraValue = backupExtraValue;
        InstanceConfig wrapped = new InstanceConfig()
        {
            @Override
            public String getString(StringConfigs config)
            {
                if ( config == StringConfigs.BACKUP_EXTRA )
                {
                    return finalBackupExtraValue;
                }

                JsonNode node = tree.get(fixName(config));
                if ( node == null )
                {
                    return "";
                }
                return node.getTextValue();
            }

            @Override
            public int getInt(IntConfigs config)
            {
                JsonNode node = tree.get(fixName(config));
                if ( node == null )
                {
                    return 0;
                }
                try
                {
                    return Integer.parseInt(node.getTextValue());
                }
                catch ( NumberFormatException e )
                {
                    // ignore
                }
                return 0;
            }
        };
        
        Result  result;
        if ( context.getExhibitor().getConfigManager().updateConfig(wrapped) )
        {
            result = new Result("OK", true);
        }
        else
        {
            result = new Result("Another process has updated the config.", false);
        }
        context.getExhibitor().resetLocalConnection();

        return Response.ok(result).build();
    }

    @Path("set/{type}/{value}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response setControlPanelSetting(@PathParam("type") String typeStr, @PathParam("value") boolean newValue) throws Exception
    {
        ControlPanelTypes type;
        try
        {
            typeStr = typeStr.replace("-", "_");
            type = ControlPanelTypes.valueOf(typeStr.toUpperCase());
        }
        catch ( IllegalArgumentException e )
        {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        context.getExhibitor().getControlPanelValues().set(type, newValue);
        return Response.ok(new Result("OK", true)).build();
    }

    @Path("stop")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response stopZooKeeper() throws Exception
    {
        context.getExhibitor().getActivityQueue().add
        (
            QueueGroups.MAIN,
            new KillRunningInstance(context.getExhibitor())
            {
                @Override
                public void completed(boolean wasSuccessful)
                {
                }
            }
        );

        return Response.ok(new Result("OK", true)).build();
    }

    private String getLog()
    {
        List<String> log = context.getExhibitor().getLog().toDisplayList("\t");
        StringBuilder str = new StringBuilder();
        for ( String s : log )
        {
            str.append(s).append("\n");
        }

        return str.toString();
    }

    private ImmutableList<UITab> buildTabs()
    {
        ImmutableList.Builder<UITab> builder = ImmutableList.builder();

        builder.add
        (
            new UITab("Log")
            {
                @Override
                public String getContent() throws Exception
                {
                    return getLog();
                }
            }
        );
        Collection<UITab> additionalUITabs = context.getExhibitor().getAdditionalUITabs();
        if ( additionalUITabs != null )
        {
            builder.addAll(additionalUITabs);
        }

        return builder.build();
    }

    static String fixName(Enum c)
    {
        StringBuilder   str = new StringBuilder();
        String[]        parts = c.name().toLowerCase().split("_");
        for ( String p : parts )
        {
            if ( p.length() > 0 )
            {
                if ( str.length() > 0 )
                {
                    str.append(p.substring(0, 1).toUpperCase());
                    if ( p.length() > 1 )
                    {
                        str.append(p.substring(1));
                    }
                }
                else
                {
                    str.append(p);
                }
            }
        }
        return str.toString();
    }
}
