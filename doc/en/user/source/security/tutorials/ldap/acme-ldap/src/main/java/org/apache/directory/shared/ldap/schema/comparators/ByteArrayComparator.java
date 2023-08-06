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
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A comparator for byte[]s.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ByteArrayComparator extends LdapComparator<byte[]>
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( ByteArrayComparator.class );

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /**
     * The ByteArrayComparator constructor. Its OID is the OctetStringMatch matching
     * rule OID.
     */
    public ByteArrayComparator( String oid )
    {
        super( oid );
    }

    /**
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare( byte[] b1, byte[] b2 )
    {
        LOG.debug( "comparing OctetString objects '{}' with '{}'", 
            StringTools.dumpBytes( b1 ), StringTools.dumpBytes( b2 ) );

        // -------------------------------------------------------------------
        // Handle some basis cases
        // -------------------------------------------------------------------

        if ( b1 == null )
        {
            return ( b2 == null ) ? 0 : -1;
        }
        
        if ( b2 == null )
        {
            return 1;
        }
        
        if ( b1.length == b2.length )
        {
            for ( int i = 0; i < b1.length; i++ )
            {
                if ( b1[i] > b2[i] )
                {
                    return 1;
                }
                else if ( b1[i] < b2[i] )
                {
                    return -1;
                }
            }
            
            return 0;
        }
        
        int minLength = Math.min( b1.length, b2.length );
        
        for ( int i = 0; i < minLength; i++ )
        {
            if ( b1[i] > b2[i] )
            {
                return 1;
            }
            else if ( b1[i] < b2[i] )
            {
                return -1;
            }
        }
        
        // b2 is longer w/ b1 as prefix 
        if ( b1.length == minLength )
        {
            return -1;
        }
        
        // b1 is longer w/ b2 as prefix
        if ( b2.length == minLength )
        {
            return 1;
        }
        
        return 0;
    }
}
