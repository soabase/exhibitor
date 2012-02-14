package com.netflix.exhibitor.core.config;

import com.netflix.exhibitor.core.InstanceConfig;
import java.util.Properties;

public class PropertyBasedInstanceConfig implements InstanceConfig
{
    private final Properties properties;
    private final Properties defaults;

    public PropertyBasedInstanceConfig(InstanceConfig source)
    {
        defaults = new Properties();

        properties = new Properties();
        for ( StringConfigs config : StringConfigs.values() )
        {
            properties.setProperty(toName(config), source.getString(config));
        }
        for ( IntConfigs config : IntConfigs.values() )
        {
            properties.setProperty(toName(config), Integer.toString(source.getInt(config)));
        }
    }

    public Properties getProperties()
    {
        Properties      copy = new Properties();
        copy.putAll(properties);
        return copy;
    }

    public PropertyBasedInstanceConfig(Properties properties, Properties defaults)
    {
        this.properties = properties;
        this.defaults = defaults;
    }

    @Override
    public String getString(StringConfigs config)
    {
        String  propertyName = toName(config);
        return properties.getProperty(propertyName, defaults.getProperty(propertyName, ""));
    }

    @Override
    public int getInt(IntConfigs config)
    {
        String propertyName = toName(config);
        return asInt(properties.getProperty(propertyName, defaults.getProperty(propertyName, "0")));
    }
    
    private String toName(Enum e)
    {
        String  s = e.name();
        s = s.replace('_', '-');
        s = s.toLowerCase();
        return "com.netflix.exhibitor." + s;
    }

    private int asInt(String property)
    {
        try
        {
            return Integer.parseInt(property);
        }
        catch ( NumberFormatException e )
        {
            // ignore
        }
        return 0;
    }
}
