package com.netflix.exhibitor.core.state;

public enum InstanceStateTypes
{
    LATENT,
    NOT_SERVING,
    UNKNOWN,
    SERVING,
    DOWN_BECAUSE_UNLISTED,
    DOWN_BECAUSE_RESTARTS_TURNED_OFF
}
