package com.netflix.exhibitor.s3;

class S3Common
{
    private static final String     SEPARATOR = ",";

    static String       nameToKey(String name)
    {
        return System.currentTimeMillis() + SEPARATOR + name;  // so I can sort by time
    }
    
    static String       keyToName(String key)
    {
        return key.split(SEPARATOR)[0];
    }
    
    private S3Common()
    {
    }
}
