/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.shared.ldap.schema.normalizers;


import org.apache.directory.shared.ldap.schema.registries.Registries;


/**
 * A deep trimming normalizer that caches normalizations to prevent repeat
 * normalizations from occurring needlessly.  Try to use this sparing for only
 * those kinds of attributeTypes using this Normalizer's matchingRule while 
 * requiring heavy parsing activity.  This way there's some advantage to caching 
 * normalized values.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class CachingDeepTrimNormalizer extends CachingNormalizer
{
    /** serial version UID */
    private static final long serialVersionUID = -206263185305284269L;


    public CachingDeepTrimNormalizer()
    {
        super( new DeepTrimNormalizer() );
    }


    /**
     * {@inheritDoc}
     */
    public void setRegistries( Registries registries )
    {
        super.setRegistries( registries );
    }
}
