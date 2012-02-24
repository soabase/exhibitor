package com.netflix.exhibitor.rest;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.netflix.exhibitor.core.controlpanel.ControlPanelTypes;
import com.netflix.exhibitor.core.state.InstanceState;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ContextResolver;
import java.net.URI;
import java.util.List;

/**
 * REST calls for dealing with indexed (via Lucene) log files
 */
@SuppressWarnings("UnusedDeclaration")
@Path("exhibitor/v1/ui/cluster")
public class ClusterResource
{
    private final UIContext context;
    private final Client client;
    private final LoadingCache<URI, WebResource> webResources = CacheBuilder.newBuilder()
        .softValues()
        .build
        (
            new CacheLoader<URI, WebResource>()
            {
                @Override
                public WebResource load(URI remoteUri) throws Exception
                {
                    return client.resource(remoteUri);
                }
            }
        );

    public ClusterResource(@Context ContextResolver<UIContext> resolver)
    {
        context = resolver.getContext(UIContext.class);
        client = Client.create();
    }

    @Path("state/{hostname}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String   getRemoteStatus(@Context UriInfo uriInfo, @PathParam("hostname") String hostname) throws Exception
    {
        String      remoteResponse;
        String      errorMessage;
        if ( hostname.equals("localhost") )
        {
            remoteResponse = getStatus();
            errorMessage = "";
        }
        else
        {
            String      thisPath = uriInfo.getPath();
            if ( !thisPath.endsWith(hostname) )
            {
                throw new IllegalStateException("Unknown path format: " + thisPath);
            }
            String      remotePath = thisPath.substring(0, thisPath.length() - hostname.length());
            UriBuilder  builder = uriInfo.getRequestUriBuilder();
            List<PathSegment> segments = uriInfo.getPathSegments();
            URI         remoteUri = builder.replacePath(remotePath).host(hostname).build();

            try
            {
                WebResource         resource = webResources.get(remoteUri);
                remoteResponse = resource.accept(MediaType.APPLICATION_JSON_TYPE).get(String.class);
                errorMessage = "";
            }
            catch ( Exception e )
            {
                remoteResponse = "{}";
                errorMessage = e.getMessage();
                if ( errorMessage == null )
                {
                    errorMessage = "Unknown";
                }
            }
        }

        ObjectMapper        mapper = new ObjectMapper();
        ObjectNode          node = mapper.getNodeFactory().objectNode();
        node.put("response", mapper.readTree(remoteResponse));
        node.put("errorMessage", errorMessage);
        node.put("success", errorMessage.length() == 0);

        return mapper.writer().writeValueAsString(node);
    }

    @Path("state")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String   getStatus() throws Exception
    {
        ObjectMapper        mapper = new ObjectMapper();
        ObjectNode          mainNode = mapper.getNodeFactory().objectNode();

        ObjectNode          switchesNode = mapper.getNodeFactory().objectNode();
        for ( ControlPanelTypes type : ControlPanelTypes.values() )
        {
            switchesNode.put(UIResource.fixName(type), context.getExhibitor().getControlPanelValues().isSet(type));
        }
        mainNode.put("switches", switchesNode);

        InstanceState instanceState = context.getExhibitor().getInstanceStateManager().getInstanceState();
        mainNode.put("state", instanceState.getState().getCode());

        return mapper.writer().writeValueAsString(mainNode);
    }
}
