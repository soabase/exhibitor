package com.netflix.exhibitor.core.rest;

import org.codehaus.jackson.map.ObjectMapper;
import java.io.IOException;
import java.io.StringWriter;

class JsonUtil
{
    static String writeValueAsString(Object obj)
    {
        try
        {
            ObjectMapper mapper = new ObjectMapper();
            StringWriter str = new StringWriter();
            mapper.getJsonFactory().createJsonGenerator(str).writeObject(obj);
            return str.toString();
        }
        catch ( IOException e )
        {
            throw new RuntimeException(e);
        }
    }
}
