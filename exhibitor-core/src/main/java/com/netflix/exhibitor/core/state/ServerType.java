package com.netflix.exhibitor.core.state;

public enum ServerType
{
    STANDARD("S", ""),
    OBSERVER("O", ":observer")
    ;

    private final String code;
    private final String zookeeperConfigValue;

    public String getCode()
    {
        return code;
    }

    public String getZookeeperConfigValue()
    {
        return zookeeperConfigValue;
    }

    public static ServerType        fromCode(String code)
    {
        for ( ServerType type : values() )
        {
            if ( code.equalsIgnoreCase(type.getCode()) )
            {
                return type;
            }
        }
        return STANDARD;
    }

    private ServerType(String code, String zookeeperConfigValue)
    {
        this.code = code;
        this.zookeeperConfigValue = zookeeperConfigValue;
    }
}
