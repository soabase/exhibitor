package com.netflix.exhibitor.core.config;

import com.google.common.io.Closeables;
import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.activity.ActivityLog;
import com.netflix.exhibitor.core.activity.ActivityQueue;
import org.mockito.Mockito;
import org.testng.annotations.Test;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

public class TestRollingConfigChange
{
    @Test
    public void testChange() throws Exception
    {
        ActivityLog         log = new ActivityLog(100);
        ActivityQueue       activityQueue = new ActivityQueue();
        Exhibitor           mockExhibitor = Mockito.mock(Exhibitor.class);
        Mockito.when(mockExhibitor.getLog()).thenReturn(log);
        Mockito.when(mockExhibitor.getActivityQueue()).thenReturn(activityQueue);
        Mockito.when(mockExhibitor.getThisJVMHostname()).thenReturn("temp");

        ConfigProvider      provider = new ConfigProvider()
        {
            private volatile ConfigCollection      config = new PropertyBasedInstanceConfig(new Properties(), new Properties());
            private final AtomicLong               modified = new AtomicLong(1);

            @Override
            public LoadedInstanceConfig loadConfig() throws Exception
            {
                return new LoadedInstanceConfig(config, modified.get());
            }

            @Override
            public LoadedInstanceConfig storeConfig(ConfigCollection config, long compareLastModified) throws Exception
            {
                this.config = config;
                modified.incrementAndGet();
                return loadConfig();
            }
        };
        ConfigManager       manager = new ConfigManager(mockExhibitor, provider, 10);
        manager.start();
        try
        {
            Properties                      properties = new Properties();
            properties.setProperty(PropertyBasedInstanceConfig.toName(StringConfigs.SERVERS_SPEC, PropertyBasedInstanceConfig.ROOT_PROPERTY_PREFIX), "1:foo,2:bar");
            PropertyBasedInstanceConfig     config = new PropertyBasedInstanceConfig(properties, DefaultProperties.get(null));
            manager.updateConfig(config.getRootConfig());
        }
        finally
        {
            Closeables.closeQuietly(manager);
        }
    }
}
