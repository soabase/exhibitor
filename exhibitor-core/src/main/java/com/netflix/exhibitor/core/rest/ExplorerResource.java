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

package com.netflix.exhibitor.core.rest;

import com.netflix.curator.utils.ZKPaths;
import com.netflix.exhibitor.core.activity.ActivityLog;
import com.netflix.exhibitor.core.entities.Result;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ContextResolver;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

/**
 * REST calls for the Explorer Tab (which uses the Dynatree JQuery plugin
 */
@Path("exhibitor/v1/explorer")
public class ExplorerResource
{
    private final UIContext context;

    private static final String         ERROR_KEY = "*";

    public ExplorerResource(@Context ContextResolver<UIContext> resolver)
    {
        context = resolver.getContext(UIContext.class);
    }

    public static String bytesToString(byte[] bytes)
    {
        StringBuilder       bytesStr = new StringBuilder();
        for ( byte b : bytes )
        {
            bytesStr.append(Integer.toHexString(b & 0xff)).append(" ");
        }
        return bytesStr.toString();
    }

    @DELETE
    @Path("znode/{path:.*}")
    @Produces("application/json")
    public Response deleteNode(@PathParam("path") String path)
    {
        Response    response;
        do
        {
            if ( !context.getExhibitor().nodeMutationsAllowed() )
            {
                response = Response.status(Response.Status.FORBIDDEN).build();
                break;
            }

            try
            {
                recursivelyDelete("/" + path);
            }
            catch ( Exception e )
            {
                response = Response.ok(new Result(e)).build();
                break;
            }

            response = Response.ok(new Result("OK", true)).build();
        } while ( false );

        return response;
    }

    private void recursivelyDelete(String path) throws Exception
    {
        List<String>        children = context.getExhibitor().getLocalConnection().getChildren().forPath(path);
        for ( String name : children )
        {
            recursivelyDelete(ZKPaths.makePath(path, name));
        }

        context.getExhibitor().getLocalConnection().delete().forPath(path);
    }

    @PUT
    @Path("znode/{path:.*}")
    @Produces("application/json")
    @Consumes("application/json")
    public Response createNode(@PathParam("path") String path, String data)
    {
        Response    response;
        do
        {
            if ( !context.getExhibitor().nodeMutationsAllowed() )
            {
                response = Response.status(Response.Status.FORBIDDEN).build();
                break;
            }

            path = "/" + path;
            try
            {
                try
                {
                    context.getExhibitor().getLocalConnection().setData().forPath(path, data.getBytes("UTF-8"));
                    context.getExhibitor().getLog().add(ActivityLog.Type.INFO, String.format("createNode() updated node [%s] to data [%s]", path, data));
                }
                catch ( KeeperException.NoNodeException dummy )
                {
                    context.getExhibitor().getLocalConnection().create().creatingParentsIfNeeded().forPath(path, data.getBytes("UTF-8"));
                    context.getExhibitor().getLog().add(ActivityLog.Type.INFO, String.format("createNode() created node [%s] with data [%s]", path, data));
                }
            }
            catch ( Exception e )
            {
                response = Response.ok(new Result(e)).build();
                break;
            }

            response = Response.ok(new Result("OK", true)).build();
        } while ( false );

        return response;
    }

    @GET
    @Path("node-data")
    @Produces("application/json")
    public String   getNodeData(@QueryParam("key") String key) throws Exception
    {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        try
        {
            Stat stat = context.getExhibitor().getLocalConnection().checkExists().forPath(key);
            byte[]          bytes = context.getExhibitor().getLocalConnection().getData().storingStatIn(stat).forPath(key);

            String          bytesStr = bytesToString(bytes);

            node.put("bytes", bytesStr);
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
            context.getExhibitor().resetLocalConnection();
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
