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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

/**
 * Config imp that uses a Properties file
 */
public class PropertyBasedInstanceConfig implements ConfigCollection
{
    private final Properties properties;
    private final Properties defaults;
    private final Collection<String> rollingHostNames;
    private final WrappedInstanceConfig rootConfig;
    private final WrappedInstanceConfig rollingConfig;

    private static final String ROOT_PROPERTY_PREFIX = "com.netflix.exhibitor.";
    private static final String ROLLING_PROPERTY_PREFIX = "com.netflix.exhibitor-rolling.";

    private static final String PROPERTY_IS_ROLLING = "com.netflix.exhibitor-is-rolling.";
    private static final String PROPERTY_ROLLING_HOSTNAMES = "com.netflix.exhibitor-rolling-hostnames.";

    /**
     * Used to wrap an existing config
     *
     * @param source source config
     */
    public PropertyBasedInstanceConfig(InstanceConfig source)
    {
        this(buildPropertiesFromSource(source), new Properties());
    }

    /**
     * @param properties the properties
     * @param defaults default values
     */
    public PropertyBasedInstanceConfig(Properties properties, Properties defaults)
    {
        this.properties = properties;
        this.defaults = defaults;

        rollingHostNames = internalGetRollingHostNames();
        rootConfig = new WrappedInstanceConfig(ROOT_PROPERTY_PREFIX);
        rollingConfig = new WrappedInstanceConfig(ROLLING_PROPERTY_PREFIX);
    }

    public Properties getProperties()
    {
        Properties      copy = new Properties();
        copy.putAll(properties);
        return copy;
    }

    @Override
    public InstanceConfig getConfigForThisInstance(String hostname)
    {
        return (isRolling() && rollingHostNames.contains(hostname)) ? rollingConfig : rootConfig;
    }

    @Override
    public InstanceConfig getRootConfig()
    {
        return rootConfig;
    }

    @Override
    public InstanceConfig getRollingConfig()
    {
        return rollingConfig;
    }

    @Override
    public boolean isRolling()
    {
        return "true".equalsIgnoreCase(properties.getProperty(PROPERTY_IS_ROLLING, "false"));
    }

    @Override
    public Collection<String> getRollingHostNames()
    {
        return rollingHostNames;
    }

    private static String toName(Enum e, String prefix)
    {
        String  s = e.name();
        s = s.replace('_', '-');
        s = s.toLowerCase();
        return prefix + s;
    }

    private static Properties buildPropertiesFromSource(InstanceConfig source)
    {
        Properties      properties = new Properties();
        for ( StringConfigs config : StringConfigs.values() )
        {
            properties.setProperty(toName(config, ROOT_PROPERTY_PREFIX), source.getString(config));
        }
        for ( IntConfigs config : IntConfigs.values() )
        {
            properties.setProperty(toName(config, ROOT_PROPERTY_PREFIX), Integer.toString(source.getInt(config)));
        }
        return properties;
    }

    private Collection<String> internalGetRollingHostNames()
    {
        String  hostnames = properties.getProperty(PROPERTY_ROLLING_HOSTNAMES, null);
        if ( (hostnames != null) && (hostnames.trim().length() > 0) )
        {
            Iterables.transform
                (
                    Arrays.asList(hostnames.split(",")),
                    new Function<String, String>()
                    {
                        @Override
                        public String apply(String str)
                        {
                            return str.trim();
                        }
                    }
                );
            return ImmutableList.copyOf(Arrays.asList(hostnames.split(",")));
        }
        return ImmutableList.of();
    }

    private class WrappedInstanceConfig implements InstanceConfig
    {
        private final String prefix;

        public WrappedInstanceConfig(String prefix)
        {
            this.prefix = prefix;
        }

        @Override
        public String getString(StringConfigs config)
        {
            String  propertyName = toName(config, prefix);
            return properties.getProperty(propertyName, defaults.getProperty(propertyName, ""));
        }

        @Override
        public int getInt(IntConfigs config)
        {
            String propertyName = toName(config, prefix);
            return DefaultProperties.asInt(properties.getProperty(propertyName, defaults.getProperty(propertyName, "0")));
        }
    }
}
