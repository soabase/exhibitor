package com.netflix.exhibitor.core.config;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Converts between the stored version of encoded config and the runtime version
 */
public class EncodedConfigParser
{
    private final Map<String, String>       values;
    
    public EncodedConfigParser(String encodedValue)
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

        values = builder.build();
    }

    /**
     * @param values runtime values
     */
    public EncodedConfigParser(Map<String, String> values)
    {
        TreeMap<String, String>     localSorted = Maps.newTreeMap();
        localSorted.putAll(values);
        this.values = ImmutableMap.copyOf(localSorted);
    }

    /**
     * Return the values
     *
     * @return values
     */
    public Map<String, String> getValues()
    {
        return values;
    }

    /**
     * @return storable value
     */
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
