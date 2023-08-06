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


import org.apache.directory.shared.ldap.csn.Csn;
import org.apache.directory.shared.ldap.schema.LdapComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A comparator for CSN.
 *
 * The CSN are ordered depending on an evaluation of its component, in this order :
 * - time, 
 * - changeCount,
 * - sid
 * - modifierNumber
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class CsnComparator extends LdapComparator<String>
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( CsnComparator.class );

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /**
     * The CsnComparator constructor. Its OID is the CsnMatch matching
     * rule OID.
     */
    public CsnComparator( String oid )
    {
        super( oid );
    }


    /**
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare( String csnStr1, String csnStr2 )
    {
        LOG.debug( "comparing CSN objects '{}' with '{}'", csnStr1, csnStr2 );

        // -------------------------------------------------------------------
        // Handle some basis cases
        // -------------------------------------------------------------------
        if ( csnStr1 == null )
        {
            return ( csnStr2 == null ) ? 0 : -1;
        }
        
        if ( csnStr2 == null )
        {
            return 1;
        }
        
        Csn csn1 = new Csn( csnStr1 );
        Csn csn2 = new Csn( csnStr2 );
        
        if ( csn1.getTimestamp() != csn2.getTimestamp() )
        {
            return ( csn1.getTimestamp() < csn2.getTimestamp() ? -1 : 1 );
        }
        
        if ( csn1.getChangeCount() != csn2.getChangeCount() )
        {
            return ( csn1.getChangeCount() < csn2.getChangeCount() ? -1 : 1 );
        }
        
        if ( csn1.getReplicaId() != csn2.getReplicaId() )
        {
            return ( csn1.getReplicaId() < csn2.getReplicaId() ? -1 : 1 );
        }
        
        if ( csn1.getOperationNumber() != csn2.getOperationNumber() )
        {
            return ( csn1.getOperationNumber() < csn2.getOperationNumber() ? -1 : 1 );
        }
        
        return 0;
    }
}
