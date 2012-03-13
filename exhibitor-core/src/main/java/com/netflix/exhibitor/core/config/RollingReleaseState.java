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

package com.netflix.exhibitor.core.config;

import com.google.common.collect.Sets;
import com.netflix.exhibitor.core.state.InstanceState;
import com.netflix.exhibitor.core.state.ServerList;
import java.util.Set;

class RollingReleaseState
{
    private final String            currentRollingHostname;
    private final ServerList        targetServerList;
    private final Set<String>       targetHostnames;
    private final InstanceState     currentInstanceState;
    private final ConfigCollection  config;

    RollingReleaseState(InstanceState currentInstanceState, ConfigCollection config)
    {
        this.currentInstanceState = currentInstanceState;
        this.config = config;

        String          currentRollingHostname = null;
        ServerList      targetServerList = null;
        Set<String>     targetHostnames = null;
        if ( config.isRolling() )
        {
            currentRollingHostname = (config.getRollingHostNames().size() > 0) ? config.getRollingHostNames().get(config.getRollingHostNames().size() - 1) : null;

            targetServerList = new ServerList(config.getRollingConfig().getString(StringConfigs.SERVERS_SPEC));
            targetHostnames = Sets.newHashSet(targetServerList.getHostnames());
        }

        this.currentRollingHostname = currentRollingHostname;
        this.targetServerList = targetServerList;
        this.targetHostnames = targetHostnames;
    }

    String getCurrentRollingHostname()
    {
        return currentRollingHostname;
    }
    
    String getNextRollingHostname()
    {
        if ( !config.isRolling() )
        {
            return null;
        }

        Set<String>     completedHostnames = Sets.newHashSet(config.getRollingHostNames());
        Set<String>     targetHostnames = Sets.newHashSet(targetServerList.getHostnames());

        Set<String>     remainingHostnames = Sets.difference(targetHostnames, completedHostnames);
        if ( remainingHostnames.size() > 0 )
        {
            return remainingHostnames.iterator().next();
        }
        else
        {
            ServerList              priorServerList = new ServerList(config.getRollingConfig().getString(StringConfigs.SERVERS_SPEC));
            Set<String>             priorHostnames = Sets.newHashSet(priorServerList.getHostnames());
            Set<String>             remainingPriorHostnames = Sets.difference(priorHostnames, completedHostnames);
            if ( remainingPriorHostnames.size() > 0 )
            {
                return remainingPriorHostnames.iterator().next();
            }
        }
        return null;
    }

    Set<String> getTargetHostnames()
    {
        return targetHostnames;
    }

    boolean serverListHasSynced()
    {
        return (targetServerList != null) && targetServerList.equals(currentInstanceState.getServerList());
    }
}
