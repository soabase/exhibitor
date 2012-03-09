/*
 *
 *  Copyright 2011 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

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
