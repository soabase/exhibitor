package com.netflix.exhibitor.core.backup.s3;

import com.google.common.io.Closeables;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyBasedS3Credential implements S3Credential
{
    private final String accessKeyId;
    private final String accessSecretKey;

    public static final String PROPERTY_S3_KEY_ID = "com.netflix.exhibitor.s3.access-key-id";
    public static final String PROPERTY_S3_SECRET_KEY = "com.netflix.exhibitor.s3.access-secret-key";

    public PropertyBasedS3Credential(File propertiesFile) throws IOException
    {
        this(loadProperties(propertiesFile));
    }

    public PropertyBasedS3Credential(Properties properties)
    {
        accessKeyId = properties.getProperty(PROPERTY_S3_KEY_ID);
        accessSecretKey = properties.getProperty(PROPERTY_S3_SECRET_KEY);
    }

    @Override
    public String getAccessKeyId()
    {
        return accessKeyId;
    }

    @Override
    public String getSecretAccessKey()
    {
        return accessSecretKey;
    }

    private static Properties loadProperties(File propertiesFile) throws IOException
    {
        Properties      properties = new Properties();
        InputStream     in = new BufferedInputStream(new FileInputStream(propertiesFile));
        try
        {
            properties.load(in);
        }
        finally
        {
            Closeables.closeQuietly(in);
        }
        return properties;
    }
}
