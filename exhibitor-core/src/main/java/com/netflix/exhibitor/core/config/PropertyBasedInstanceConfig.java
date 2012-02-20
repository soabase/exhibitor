package com.netflix.exhibitor.core.config;

import com.netflix.exhibitor.core.state.InstanceConfig;
import java.util.Properties;

/**
 * Config imp that uses a Properties file
 */
class PropertyBasedInstanceConfig implements InstanceConfig
{
    private final Properties properties;
    private final Properties defaults;

    /**
     * Used to wrap an existing config
     *
     * @param source source config
     */
    PropertyBasedInstanceConfig(InstanceConfig source)
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

    /**
     * @param properties the properties
     * @param defaults default values
     */
    PropertyBasedInstanceConfig(Properties properties, Properties defaults)
    {
        this.properties = properties;
        this.defaults = defaults;
    }

    Properties getProperties()
    {
        Properties      copy = new Properties();
        copy.putAll(properties);
        return copy;
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
        return DefaultProperties.asInt(properties.getProperty(propertyName, defaults.getProperty(propertyName, "0")));
    }
    
    private String toName(Enum e)
    {
        String  s = e.name();
        s = s.replace('_', '-');
        s = s.toLowerCase();
        return "com.netflix.exhibitor." + s;
    }
}
