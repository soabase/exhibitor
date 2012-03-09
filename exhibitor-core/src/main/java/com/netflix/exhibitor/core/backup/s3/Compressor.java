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

package com.netflix.exhibitor.core.backup.s3;

import java.io.File;
import java.io.InputStream;

/**
 * Pluggable method for compressing/decompressing in chunks
 */
public interface Compressor
{
    /**
     * Compress the given file
     *
     * @param f file
     * @return compression chunks
     * @throws Exception errors
     */
    public CompressorIterator     compress(File f) throws Exception;

    /**
     * Decompress the given stream
     *
     * @param in strema
     * @return compression chunks
     * @throws Exception errors
     */
    public CompressorIterator     decompress(InputStream in) throws Exception;
}
