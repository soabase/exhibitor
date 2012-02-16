package com.netflix.exhibitor.core.backup;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.netflix.exhibitor.core.BackupProvider;
import com.sun.deploy.net.URLEncoder;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.Set;

public class BackupConfigParser
{
    private final Map<String, String>       values;
    
    public BackupConfigParser(String encodedValue, BackupProvider backupProvider)
    {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

        Set<String>     usedKeys = Sets.newHashSet();
        String[]        parts = encodedValue.split("&");
        try
        {
            for ( String part : parts )
            {
                String[]      subParts = part.split("=");
                if ( subParts.length == 2 )
                {
                    usedKeys.add(subParts[0]);
                    builder.put
                    (
                        URLDecoder.decode(subParts[0], "UTF-8"),
                        URLDecoder.decode(subParts[1], "UTF-8")
                    );
                }
            }
        }
        catch ( UnsupportedEncodingException e )
        {
            // should never get here
            throw new Error(e);
        }

        for ( BackupConfig config : backupProvider.getConfigs() )
        {
            if ( !usedKeys.contains(config.getKey()) )
            {
                builder.put(config.getKey(), config.getDefaultValue());
            }
        }

        values = builder.build();
    }

    public BackupConfigParser(Map<String, String> values)
    {
        this.values = ImmutableMap.copyOf(values);
    }

    public Map<String, String> getValues()
    {
        return values;
    }

    public String   toEncoded()
    {
        StringBuilder       str = new StringBuilder();
        try
        {
            for ( Map.Entry<String, String> entry : values.entrySet() )
            {
                if ( str.length() > 0 )
                {
                    str.append("&");
                }
                str.append(URLEncoder.encode(entry.getKey(), "UTF8"));
                str.append("=");
                str.append(URLEncoder.encode(entry.getValue(), "UTF8"));
            }
        }
        catch ( UnsupportedEncodingException e )
        {
            // should never get here
            throw new Error(e);
        }

        return str.toString();
    }
}
