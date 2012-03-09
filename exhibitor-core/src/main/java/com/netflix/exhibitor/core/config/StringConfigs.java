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

/**
 * Config selectors for string values
 */
public enum StringConfigs
{
    /**
     * Path to stored indexed log files
     */
    LOG_INDEX_DIRECTORY,

    /**
     * The base director of the zookeeper installation
     */
    ZOOKEEPER_INSTALL_DIRECTORY,

    /**
     * Where to store ZooKeeper data
     */
    ZOOKEEPER_DATA_DIRECTORY,

    /**
     * List of servers in the cluster of the form [id]:[hostname],[id]:[hostname],...
     */
    SERVERS_SPEC,

    /**
     * Used to store the provider-specific backup config
     */
    BACKUP_EXTRA,

    /**
     * Additional properties to add to zoo.cfg
     */
    ZOO_CFG_EXTRA
}
