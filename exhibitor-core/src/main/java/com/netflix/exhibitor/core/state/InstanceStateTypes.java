package com.netflix.exhibitor.core.state;

public enum InstanceStateTypes
{
    UNKNOWN(0),
    LATENT(1),
    NOT_SERVING(2),
    SERVING(3),
    DOWN_BECAUSE_UNLISTED(4),
    DOWN_BECAUSE_RESTARTS_TURNED_OFF(5)
    ;

    private final int code;

    public int getCode()
    {
        return code;
    }

    public String   getDescription()
    {
        return name().toLowerCase().replace("_", " ");
    }

    private InstanceStateTypes(int code)
    {
        this.code = code;
    }
}
