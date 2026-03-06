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
package org.apache.directory.shared.ldap.schema.comparators;


import org.apache.directory.shared.ldap.schema.LdapComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A comparator for CSN SID.
 *
 * The SID is supposed to be an hexadecimal number between 0x0 and 0xfff
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class CsnSidComparator extends LdapComparator<String>
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( CsnSidComparator.class );

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /**
     * The CsnSidComparator constructor. Its OID is the CsnSidMatch matching
     * rule OID.
     */
    public CsnSidComparator( String oid )
    {
        super( oid );
    }
    
    
    /**
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare( String sidStr1, String sidStr2 )
    {
        LOG.debug( "comparing CSN SID objects '{}' with '{}'", sidStr1, sidStr2 );

        // -------------------------------------------------------------------
        // Handle some basis cases
        // -------------------------------------------------------------------
        if ( sidStr1 == null )
        {
            return ( sidStr2 == null ) ? 0 : -1;
        }
        
        if ( sidStr2 == null )
        {
            return 1;
        }
        
        int sid1 = 0;
        int sid2 = 0;
        
        try
        {
            sid1 = Integer.parseInt( sidStr1, 16 );
        }
        catch ( NumberFormatException nfe )
        {
            return -1;
        }
        
        try
        {
            sid2 = Integer.parseInt( sidStr2, 16 );
        }
        catch ( NumberFormatException nfe )
        {
            return 1;
        }
        
        if ( sid1 > sid2 )
        {
            return 1;
        }
        else if ( sid2 > sid1 )
        {
            return -1;
        }
        
        return 0;
    }
}
