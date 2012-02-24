package com.netflix.exhibitor.core.entities;

import com.google.common.collect.ImmutableList;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;

@XmlRootElement
@SuppressWarnings("UnusedDeclaration")
public class VersionedInstanceStatus
{
    private long                        version;
    private Collection<InstanceStatus>  status;

    public VersionedInstanceStatus()
    {
        this(0, ImmutableList.<InstanceStatus>of());
    }

    public VersionedInstanceStatus(long version, Collection<InstanceStatus> status)
    {
        this.version = version;
        this.status = status;
    }

    public long getVersion()
    {
        return version;
    }

    public void setVersion(long version)
    {
        this.version = version;
    }

    public Collection<InstanceStatus> getStatus()
    {
        return status;
    }

    public void setStatus(Collection<InstanceStatus> status)
    {
        this.status = status;
    }
}
