/*
 *
 *  Copyright 2011 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.netflix.exhibitor.core.config;

import java.util.Properties;

/**
 * Config imp that uses a Properties file
 */
public class PropertyBasedInstanceConfig implements InstanceConfig
{
    private final Properties properties;
    private final Properties defaults;

    /**
     * Used to wrap an existing config
     *
     * @param source source config
     */
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

    /**
     * @param properties the properties
     * @param defaults default values
     */
    public PropertyBasedInstanceConfig(Properties properties, Properties defaults)
    {
        this.properties = properties;
        this.defaults = defaults;
    }

    public Properties getProperties()
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
