package com.netflix.exhibitor.core.rest.jersey;

import com.netflix.exhibitor.core.entities.*;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

@Provider
class NaturalNotationContextResolver implements ContextResolver<JAXBContext>
{
    private JAXBContext context;

    NaturalNotationContextResolver()
    {
        try
        {
            this.context = new JSONJAXBContext
            (
                JSONConfiguration.natural().build(),
                Index.class,
                NewIndexRequest.class,
                Result.class,
                SearchId.class,
                SearchRequest.class,
                SearchResult.class,
                UITabSpec.class,
                InstanceStatus.class,
                VersionedInstanceStatus.class,
                NameAndModifiedDate.class
            );
        }
        catch ( JAXBException e )
        {
            throw new RuntimeException(e);
        }
    }

    public JAXBContext getContext(Class<?> objectType)
    {
        return context;
    }
}