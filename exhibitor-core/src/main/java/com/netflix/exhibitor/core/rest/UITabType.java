/*
 * Copyright 2014 Netflix, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.netflix.exhibitor.core.rest;

import javax.ws.rs.core.UriInfo;

public enum UITabType
{
    /**
     * {@link UITab#getContent(UriInfo)} will get called periodically and the tab's
     * contents will be replaced
     */
    SIMPLE,

    /**
     * {@link UITab#getContent(UriInfo)} will get called exactly once when the page
     * is displayed
     */
    STATIC
}
