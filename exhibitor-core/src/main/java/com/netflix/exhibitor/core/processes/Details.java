package com.netflix.exhibitor.core.processes;

import com.netflix.exhibitor.core.Exhibitor;
import com.netflix.exhibitor.core.config.EncodedConfigParser;
import com.netflix.exhibitor.core.config.InstanceConfig;
import com.netflix.exhibitor.core.config.StringConfigs;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Properties;

class Details
{
    final File zooKeeperDirectory;
    final File dataDirectory;
    final File configDirectory;
    final File log4jJarPath;
    final File zooKeeperJarPath;
    final Properties properties;

    Details(Exhibitor exhibitor) throws IOException
    {
        InstanceConfig config = exhibitor.getConfigManager().getConfig();

        this.zooKeeperDirectory = new File(config.getString(StringConfigs.ZOOKEEPER_INSTALL_DIRECTORY));
        this.dataDirectory = new File(config.getString(StringConfigs.ZOOKEEPER_DATA_DIRECTORY));

        configDirectory = new File(zooKeeperDirectory, "conf");
        log4jJarPath = findJar(new File(zooKeeperDirectory, "lib"), "log4j");
        zooKeeperJarPath = findJar(this.zooKeeperDirectory, "zookeeper");

        properties = new Properties();
        if ( isValid() )
        {
            EncodedConfigParser     parser = new EncodedConfigParser(exhibitor.getConfigManager().getConfig().getString(StringConfigs.ZOO_CFG_EXTRA));
            properties.putAll(parser.getValues());
            properties.put("dataDir", dataDirectory.getPath());
        }
    }

    boolean isValid()
    {
        return isValidPath(zooKeeperDirectory)
            && isValidPath(dataDirectory)
            && isValidPath(configDirectory)
            ;
    }

    private boolean isValidPath(File directory)
    {
        return directory.getPath().length() > 0;
    }

    private File findJar(File dir, final String name) throws IOException
    {
        if ( !isValid() )
        {
            return new File("");
        }

        File[]          snapshots = dir.listFiles
        (
            new FileFilter()
            {
                @Override
                public boolean accept(File f)
                {
                    return f.getName().startsWith(name) && f.getName().endsWith(".jar");
                }
            }
        );

        if ( snapshots.length == 0 )
        {
            throw new IOException("Could not find " + name + " jar");
        }
        return snapshots[0];
    }
}
