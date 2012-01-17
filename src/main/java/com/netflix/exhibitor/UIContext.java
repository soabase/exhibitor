package com.netflix.exhibitor;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Provider
public class UIContext implements ContextResolver<UIContext>
{
    private final Exhibitor exhibitor;

    public UIContext(Exhibitor exhibitor)
    {
        this.exhibitor = exhibitor;
    }

    public Exhibitor getExhibitor()
    {
        return exhibitor;
    }

    @Override
    public UIContext getContext(Class<?> type)
    {
        return this;
    }
}
