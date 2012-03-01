package com.netflix.exhibitor.core.controlpanel;

import com.google.common.cache.CacheLoader;
import com.netflix.exhibitor.core.temp.CacheBuilder;
import com.netflix.exhibitor.core.temp.LoadingCache;
import java.util.prefs.Preferences;

public class ControlPanelValues
{
    private final LoadingCache<ControlPanelTypes, Boolean> cache;
    private final Preferences preferences;

    private static final String         BASE_KEY = "com.netflix.exhibitor.control-panel.";

    public ControlPanelValues()
    {
        preferences = Preferences.userRoot();
        cache = CacheBuilder.newBuilder().build
        (
            new CacheLoader<ControlPanelTypes, Boolean>()
            {
                @Override
                public Boolean load(ControlPanelTypes type) throws Exception
                {
                    return preferences.getBoolean(makeKey(type), true);
                }
            }
        );
    }

    public boolean      isSet(ControlPanelTypes type) throws Exception
    {
        return cache.get(type);
    }

    public void         set(ControlPanelTypes type, boolean newValue) throws Exception
    {
        cache.put(type, newValue);
        preferences.putBoolean(makeKey(type), newValue);
        preferences.flush();
    }

    private String makeKey(ControlPanelTypes type)
    {
        return BASE_KEY + type.name().toLowerCase();
    }
}
