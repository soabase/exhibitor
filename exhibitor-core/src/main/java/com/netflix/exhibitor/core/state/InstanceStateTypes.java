package com.netflix.exhibitor.core.state;

public enum InstanceStateTypes
{
    LATENT(0),
    DOWN(1),
    NOT_SERVING(2),
    SERVING(3),
    UNLISTED_DOWN(4),
    NO_RESTARTS_DOWN(5)
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
