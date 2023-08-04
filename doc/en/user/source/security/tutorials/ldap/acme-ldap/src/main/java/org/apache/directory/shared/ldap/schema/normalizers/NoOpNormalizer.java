/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.shared.ldap.schema.normalizers;


import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.schema.Normalizer;


/**
 * No op (pass through or do nothing) normalizer returning what its given.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 896579 $
 */
public class NoOpNormalizer extends Normalizer
{
    /** The serial UID */
    public static final long serialVersionUID = 1L;


    /**
     * Creates a new instance of NoOpNormalizer.
     * 
     * @param oid The MR OID to use with this Normalizer
     */
    public NoOpNormalizer( String oid )
    {
        super( oid );
    }

    
    /**
     * Default constructor for NoOpNormalizer used when we must set the OID
     * after instantiating the Normalizer.
     */
    public NoOpNormalizer()
    {
    }

    
    /**
     * Returns the value argument as-is without alterations all the time.
     * 
     * @param value any value
     * @return the value argument returned as-is
     * @see org.apache.directory.shared.ldap.schema.Normalizer#normalize(java.lang.Object)
     */
    public Value<?> normalize( Value<?> value )
    {
        return value;
    }
    
    
    /**
     * Returns the value argument as-is without alterations all the time.
     * 
     * @param value any value
     * @return the value argument returned as-is
     * @see org.apache.directory.shared.ldap.schema.Normalizer#normalize(java.lang.Object)
     */
    public String normalize( String value )
    {
        return value;
    }
}
