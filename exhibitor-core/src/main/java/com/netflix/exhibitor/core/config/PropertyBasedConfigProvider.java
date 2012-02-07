package com.netflix.exhibitor.core.config;

import com.netflix.exhibitor.core.ConfigProvider;
import com.netflix.exhibitor.core.InstanceConfig;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

public class PropertyBasedConfigProvider implements ConfigProvider
{
    private final AtomicReference<Properties> properties = new AtomicReference<Properties>(new Properties());
    private final InstanceConfig defaults;

    public PropertyBasedConfigProvider()
    {
        this(new DefaultInstanceConfig());
    }

    public PropertyBasedConfigProvider(InstanceConfig defaults)
    {
        this.defaults = defaults;
    }

    @Override
    public InstanceConfig loadConfig() throws Exception
    {
        return new PropertyBasedInstanceConfig(properties.get(), defaults);
    }

    @Override
    public void storeConfig(InstanceConfig config) throws Exception
    {
        Properties      newProperties = new Properties();
        for ( PropertyBasedConfigNames name : PropertyBasedConfigNames.values() )
        {
            name.setValue(newProperties, config);
        }
        setProperties(newProperties);
    }

    public void setProperties(Properties newProperties)
    {
        properties.set(newProperties);
    }

    public Properties getProperties()
    {
        return properties.get();
    }
}
