package com.netflix.exhibitor.rest.jersey;

import com.google.common.collect.Sets;
import com.netflix.exhibitor.rest.ExplorerResource;
import com.netflix.exhibitor.rest.IndexResource;
import com.netflix.exhibitor.rest.UIContext;
import com.netflix.exhibitor.rest.UIResource;
import com.sun.jersey.api.core.DefaultResourceConfig;
import java.util.Set;

public class JerseySupport
{
    public static DefaultResourceConfig newApplication(UIContext context)
    {
        final Set<Object> singletons = Sets.newHashSet();
        singletons.add(context);
        singletons.add(new NaturalNotationContextResolver());

        final Set<Class<?>>     classes = Sets.newHashSet();
        classes.add(UIResource.class);
        classes.add(IndexResource.class);
        classes.add(ExplorerResource.class);

        return new DefaultResourceConfig()
        {
            @Override
            public Set<Class<?>> getClasses()
            {
                return classes;
            }

            @Override
            public Set<Object> getSingletons()
            {
                return singletons;
            }
        };
    }
}
