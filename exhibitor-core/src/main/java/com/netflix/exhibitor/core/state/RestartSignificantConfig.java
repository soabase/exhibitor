package com.netflix.exhibitor.core.state;

import com.netflix.exhibitor.core.config.InstanceConfig;
import com.netflix.exhibitor.core.config.IntConfigs;
import com.netflix.exhibitor.core.config.StringConfigs;

public class RestartSignificantConfig
{
    private final Object        blob;

    public RestartSignificantConfig(InstanceConfig config)
    {
        StringBuilder       str = new StringBuilder();
        if ( config != null )
        {
            for ( StringConfigs v : StringConfigs.values() )
            {
                if ( v.isRestartSignificant() )
                {
                    str.append(config.getString(v));
                }
            }
            for ( IntConfigs v : IntConfigs.values() )
            {
                if ( v.isRestartSignificant() )
                {
                    str.append(config.getInt(v));
                }
            }
        }
        blob = str.toString();
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean equals(Object o)
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        RestartSignificantConfig that = (RestartSignificantConfig)o;

        if ( !blob.equals(that.blob) )
        {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        return blob.hashCode();
    }
}
