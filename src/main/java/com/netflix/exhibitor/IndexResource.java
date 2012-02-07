package com.netflix.exhibitor;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.netflix.exhibitor.activity.ActivityLog;
import com.netflix.exhibitor.activity.QueueGroups;
import com.netflix.exhibitor.entities.Index;
import com.netflix.exhibitor.entities.NewIndexRequest;
import com.netflix.exhibitor.entities.Result;
import com.netflix.exhibitor.entities.SearchId;
import com.netflix.exhibitor.entities.SearchRequest;
import com.netflix.exhibitor.index.*;
import org.apache.lucene.search.Query;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ContextResolver;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Path("exhibitor/v1/ui/index")
public class IndexResource
{
    private final UIContext context;

    private static final int        MAX_PATH = 50;

    public IndexResource(@Context ContextResolver<UIContext> resolver)
    {
        context = resolver.getContext(UIContext.class);
    }

    @Path("new-index")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response newIndex(NewIndexRequest request) throws Exception
    {
        // TODO - handle default dir
        File        logFile = new File(request.getPath());
        if ( !logFile.exists() || logFile.isDirectory() )
        {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        File            indexDirectory = new File(context.getExhibitor().getConfig().getLogIndexDirectory(), "exhibitor-" + System.currentTimeMillis());

        LogIndexer      logIndexer = new LogIndexer(logFile, indexDirectory);
        IndexActivity   activity = new IndexActivity(logIndexer, context.getExhibitor().getLog());
        context.getExhibitor().getActivityQueue().add(QueueGroups.MAIN, activity);

        return Response.ok(new Result("OK", true)).build();
    }

    @Path("dataTable/{index-name}/{search-handle}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getDataTableData
        (
            @PathParam("index-name") String indexName,
            @PathParam("search-handle") String searchHandle,
            @QueryParam("iDisplayStart") int iDisplayStart,
            @QueryParam("iDisplayLength") int iDisplayLength,
            @QueryParam("sEcho") String sEcho
        ) throws Exception
    {
        LogSearch logSearch = getLogSearch(indexName);
        if ( logSearch == null )
        {
            return "{}";
        }
        CachedSearch        cachedSearch = logSearch.getCachedSearch(searchHandle);

        DateFormat          dateFormatter = new SimpleDateFormat("MM/dd/yyyy-HH:ss");
        ArrayNode           dataTab = JsonNodeFactory.instance.arrayNode();
        for ( int i = iDisplayStart; i < (iDisplayStart + iDisplayLength); ++i )
        {
            if ( i < cachedSearch.getTotalHits() )
            {
                ArrayNode       data = JsonNodeFactory.instance.arrayNode();
                int             docId = cachedSearch.getNthDocId(i);
                SearchItem      item = logSearch.toResult(docId);
                data.add(getTypeName(item.getType(), item.isEphemeral()));
                data.add(dateFormatter.format(item.getDate()));
                data.add(trimPath(item.getPath()));

                dataTab.add(data);
            }
        }

        ObjectNode          node = JsonNodeFactory.instance.objectNode();
        node.put("sEcho", sEcho);
        node.put("iTotalRecords", logSearch.getDocQty());
        node.put("iTotalDisplayRecords", cachedSearch.getTotalHits());
        node.put("aaData", dataTab);

        return node.toString();
    }

    @Path("indexed-logs")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getIndexedLogs()
    {
        final DateFormat    format = new SimpleDateFormat("MM/dd/yyyy-HH:mm");
        final IndexCache    indexCache = context.getExhibitor().getIndexCache();
        IndexList           indexList = new IndexList(new File(context.getExhibitor().getConfig().getLogIndexDirectory()));
        GenericEntity<List<Index>> entity = new GenericEntity<List<Index>>
        (
            Lists.transform
                (
                    indexList.getIndexes(),
                    new Function<File, Index>()
                    {
                        @Override
                        public Index apply(File f)
                        {
                            IndexMetaData   metaData;
                            try
                            {
                                metaData = indexCache.getMetaData(f);
                            }
                            catch ( Exception e )
                            {
                                context.getExhibitor().getLog().add(ActivityLog.Type.ERROR, "Loading index metadata: " + f, e);
                                metaData = new IndexMetaData(new Date(), new Date(), 0);
                            }
                            return new Index
                            (
                                f.getName(),
                                format.format(metaData.getFrom()),
                                format.format(metaData.getTo()),
                                metaData.getEntryCount()
                            );
                        }
                    }
                )
        ){};
        return Response.ok(entity).build();
    }

    @Path("cache-search")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response cacheSearch(SearchRequest request) throws Exception
    {
        LogSearch   logSearch = getLogSearch(request.getIndexName());
        if ( logSearch == null )
        {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        boolean         hasTerms = false;
        QueryBuilder    builder = QueryBuilder.builder();
        if ( request.getPathPrefix().length() > 0 )
        {
            hasTerms = true;
            builder.pathPrefix(request.getPathPrefix());
        }
        if ( request.getEphemeral() != null )
        {
            hasTerms = true;
            builder.ephemeralState(request.getEphemeral());
        }
        if ( request.getOperationType() >= 0 )
        {
            hasTerms = true;
            builder.operationType(request.getOperationType());
        }
        if ( (request.getFirstDate() != null) && (request.getSecondDate() != null) )
        {
            hasTerms = true;
            Date        startDate;
            Date        endDate;
            if ( request.getFirstDate().before(request.getSecondDate()) )
            {
                startDate = request.getFirstDate();
                endDate = request.getSecondDate();
            }
            else
            {
                startDate = request.getSecondDate();
                endDate = request.getFirstDate();
            }
            Calendar      endOfDayEndDate = Calendar.getInstance();
            endOfDayEndDate.setTime(endDate);
            endOfDayEndDate.set(Calendar.HOUR_OF_DAY, 23);
            endOfDayEndDate.set(Calendar.MINUTE, 59);
            endOfDayEndDate.set(Calendar.SECOND, 59);
            endDate = endOfDayEndDate.getTime();
            builder.dateRange(startDate, endDate);
        }
        Query       query = hasTerms ? builder.build(QueryBuilder.Type.AND) : null;
        String      id = logSearch.cacheSearch(query, request.getReuseHandle(), request.getMaxResults());
        SearchId    searchId = new SearchId(id);
        return Response.ok(searchId).build();
    }

    private String trimPath(String path)
    {
        if ( path.length() > MAX_PATH )
        {
            int half = MAX_PATH / 2;
            path = path.substring(0, half) + "&hellip;" + path.substring(path.length() - half);
        }
        return path;
    }

    private LogSearch   getLogSearch(String indexName) throws Exception
    {
        File indexFile = getLogFile(indexName);
        if ( indexFile == null )
        {
            return null;
        }
        return context.getExhibitor().getIndexCache().getLogSearch(indexFile);
    }

    private File getLogFile(String indexName)
    {
        String      indexDirectory = context.getExhibitor().getConfig().getLogIndexDirectory();
        File        indexFile = new File(indexDirectory, indexName);
        if ( !IndexMetaData.isValid(indexFile) )
        {
            return null;
        }
        return indexFile;
    }

    private String getTypeName(int type, boolean ephemeral)
    {
        switch ( type )
        {
            case EntryTypes.CREATE:
            {
                return ephemeral ? "Ephemeral" : "Persistent";
            }

            case EntryTypes.DELETE:
            {
                return "Delete";
            }

            case EntryTypes.SET_DATA:
            {
                return "SetData";
            }
        }
        return "n/a";
    }
}
