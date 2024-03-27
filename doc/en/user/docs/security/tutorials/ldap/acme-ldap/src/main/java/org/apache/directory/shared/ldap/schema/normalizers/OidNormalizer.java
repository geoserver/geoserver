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

import org.apache.directory.shared.ldap.schema.Normalizer;


/**
 * The OidNomalizer class contains a tuple: an OID with its Normalizer.  It itself
 * is not a normalizer.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class OidNormalizer
{
    /** The oid */
    private String attributeTypeOid;

    /** The normalizer to be used with this OID */
    private Normalizer normalizer;


    /**
     * A constructor which accept two parameters
     * 
     * @param attributeTypeOid the oid of the attributeType mapped to the normalizer
     * @param normalizer the associated equality match based normalizer
     */
    public OidNormalizer( String attributeTypeOid, Normalizer normalizer )
    {
        this.attributeTypeOid = attributeTypeOid;
        this.normalizer = normalizer;
    }


    /**
     * A copy constructor.
     * 
     * @param oidNormalizer the OidNormalizer to copy from
     */
    public OidNormalizer( OidNormalizer oidNormalizer )
    {
        attributeTypeOid = oidNormalizer.attributeTypeOid;
        normalizer = oidNormalizer.normalizer;
    }


    /**
     * Get the normalizer
     * 
     * @return The normalizer associated to the current OID
     */
    public Normalizer getNormalizer()
    {
        return normalizer;
    }


    /**
     * Get the current name
     * 
     * @return The current name
     */
    public String getAttributeTypeOid()
    {
        return attributeTypeOid;
    }
    
    
    /**
     * Copy an OidNormalizer(). The contained Normalizer will be cloned too.
     * 
     * @return A deep clone of the current OidNormalizer
     */
    public OidNormalizer copy() throws CloneNotSupportedException
    {
        OidNormalizer copy = new OidNormalizer( attributeTypeOid, normalizer );

        // Copy the SchemaObject common data
        copy.copy();
        
        return copy;
    }


    /**
     * Return a String representation of this class
     */
    public String toString()
    {
        return "OidNormalizer : { " + attributeTypeOid + ", " + normalizer.toString() + "}";
    }
}
