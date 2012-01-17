package com.netflix.exhibitor;

import com.google.common.io.Resources;
import com.netflix.curator.utils.ZKPaths;
import com.netflix.exhibitor.activity.ActivityLog;
import com.netflix.exhibitor.state.FourLetterWord;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import javax.activation.MimetypesFileTypeMap;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ContextResolver;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Collections;
import java.util.List;

@Path("exhibitor/v1/ui")
public class UIResource
{
    private final UIContext context;

    public UIResource(@Context ContextResolver<UIContext> resolver)
    {
        context = resolver.getContext(UIContext.class);
    }

    @Path("{file:.*}")
    @GET
    public Response getResource(@PathParam("file") String fileName) throws IOException
    {
        URL resource;
        try
        {
            resource = Resources.getResource("com/netflix/exhibitor/ui/" + fileName);
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
        return Response.ok(Resources.toByteArray(resource)).type(contentType).build();
    }

    @Path("log")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getLog()
    {
        List<String> log = context.getExhibitor().getLog().toDisplayList("\t");
        StringBuilder str = new StringBuilder();
        for ( String s : log )
        {
            str.append(s).append("\n");
        }
        return Response.ok(str.toString()).build();
    }

    @Path("zkstats")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getZKStats()
    {
        StringBuilder       str = new StringBuilder();
        for ( FourLetterWord.Word word : FourLetterWord.Word.values() )
        {
            String  value = new FourLetterWord(word, context.getExhibitor().getConfig()).getResponse();
            str.append(word.name()).append("\n");
            str.append("====").append("\n");
            str.append(value).append("\n");
            str.append("________________________________________________________________________________").append("\n\n");
        }

        return Response.ok(str.toString()).build();
    }
    private static final String         ERROR_KEY = "*";

    @GET
    @Path("node-data")
    @Produces("application/json")
    public String   getNodeData(@QueryParam("key") String key) throws Exception
    {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        try
        {
            Stat            stat = context.getExhibitor().getLocalConnection().checkExists().forPath(key);
            byte[]          bytes = context.getExhibitor().getLocalConnection().getData().storingStatIn(stat).forPath(key);

            StringBuilder       bytesStr = new StringBuilder();
            for ( byte b : bytes )
            {
                bytesStr.append(Integer.toHexString(b & 0xff)).append(" ");
            }

            node.put("bytes", bytesStr.toString());
            node.put("str", new String(bytes, "UTF-8"));
            node.put("stat", reflectToString(stat));
        }
        catch ( KeeperException.NoNodeException dummy )
        {
            node.put("bytes", "");
            node.put("str", "");
            node.put("stat", "* not found * ");
        }
        catch ( Throwable e )
        {
            node.put("bytes", "");
            node.put("str", "Exception");
            node.put("stat", e.getMessage());
        }
        return node.toString();
    }

    @GET
    @Path("node")
    @Produces("application/json")
    public String   getNode(@QueryParam("key") String key) throws Exception
    {
        ArrayNode children = JsonNodeFactory.instance.arrayNode();
        try
        {
            List<String> childrenNames = context.getExhibitor().getLocalConnection().getChildren().forPath(key);
            Collections.sort(childrenNames);
            for ( String name : childrenNames )
            {
                ObjectNode  node = children.addObject();
                node.put("title", name);
                node.put("key", ZKPaths.makePath(key, name));
                node.put("isLazy", true);
                node.put("expand", false);
            }
        }
        catch ( Throwable e )
        {
            context.getExhibitor().getLog().add(ActivityLog.Type.ERROR, "getNode: " + key, e);

            ObjectNode  node = children.addObject();
            node.put("title", "* Exception *");
            node.put("key", ERROR_KEY);
            node.put("isLazy", false);
            node.put("expand", false);
        }

        return children.toString();
    }

    private String  reflectToString(Object obj) throws Exception
    {
        StringBuilder       str = new StringBuilder();
        for ( Field f : obj.getClass().getDeclaredFields() )
        {
            f.setAccessible(true);

            if ( str.length() > 0 )
            {
                str.append(", ");
            }
            str.append(f.getName()).append(": ");
            str.append(f.get(obj));
        }
        return str.toString();
    }
}
