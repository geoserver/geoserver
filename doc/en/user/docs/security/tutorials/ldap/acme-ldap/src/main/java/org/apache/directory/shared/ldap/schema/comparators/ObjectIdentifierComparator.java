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
 * A comparator for the objectIdentifierMatch matchingRule.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 896579 $
 */
public class ObjectIdentifierComparator extends LdapComparator<Object>
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( ObjectIdentifierComparator.class );

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    
    /**
     * The ObjectIdentifierComparator constructor. Its OID is the ObjectIdentifierMatch matching
     * rule OID.
     */
    public ObjectIdentifierComparator( String oid )
    {
        super( oid );
    }

    
    /**
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare( Object o1, Object o2 )
    {
        LOG.debug( "comparing ObjectIdentifier objects '{}' with '{}'", o1, o2 );

        // -------------------------------------------------------------------
        // Handle some basis cases
        // -------------------------------------------------------------------
        if ( o1 == null )
        {
            return ( o2 == null ) ? 0 : -1;
        }
        else if ( o2 == null )
        {
            return 1;
        }

        if ( o1.equals( o2 ) )
        {
            return 0;
        }

        if ( !( o1 instanceof String && o2 instanceof String ) )
        {
            if ( o1.equals( o2 ) )
            {
                return 0;
            }

            return -1;
        }

        String s1 = ( ( String ) o1 ).trim().toLowerCase(), s2 = ( ( String ) o2 ).trim().toLowerCase();
        return s1.compareTo( s2 );
    }
}
