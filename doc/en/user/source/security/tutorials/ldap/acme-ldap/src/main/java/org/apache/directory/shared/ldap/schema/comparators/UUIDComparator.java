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
 * A comparator for UUID. We simply use the UUID compareTo method.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class UUIDComparator extends LdapComparator<String>
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( UUIDComparator.class );

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /**
     * The UUIDComparator constructor. Its OID is the UUIDMatch matching
     * rule OID.
     */
    public UUIDComparator( String oid )
    {
        super( oid );
    }

    
    /**
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare( String uuid1, String uuid2 )
    {
        LOG.debug( "comparing UUID objects '{}' with '{}'", uuid1, uuid2 );

        // -------------------------------------------------------------------
        // Handle some basis cases
        // -------------------------------------------------------------------
        if ( uuid1 == null )
        {
            return ( uuid2 == null ) ? 0 : -1;
        }
        
        if ( uuid2 == null )
        {
            return 1;
        }
        
        return uuid1.compareTo( uuid2 );
    }
}
