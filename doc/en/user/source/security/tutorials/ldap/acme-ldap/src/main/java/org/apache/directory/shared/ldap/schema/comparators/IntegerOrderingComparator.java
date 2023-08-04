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


import java.io.IOException;
import java.math.BigInteger;

import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.schema.LdapComparator;
import org.apache.directory.shared.ldap.schema.PrepareString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A class for the integerOrderingMatch matchingRule (RFC 4517, par. 4.2.20)
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 437007 $
 */
public class IntegerOrderingComparator extends LdapComparator<String>
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( IntegerOrderingComparator.class );

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;


    /**
     * The IntegerOrderingComparator constructor. Its OID is the IntegerOrderingMatch matching
     * rule OID.
     */
    public IntegerOrderingComparator( String oid )
    {
        super( oid );
    }


    /**
     * Implementation of the Compare method
     */
    public int compare( String backendValue, String assertValue )
    {
        LOG.debug( "comparing IntegerOrdering objects '{}' with '{}'", backendValue, assertValue );

        // First, shortcut the process by comparing
        // references. If they are equals, then o1 and o2
        // reference the same object
        if ( backendValue == assertValue )
        {
            return 0;
        }

        // Then, deal with one of o1 or o2 being null
        // Both can't be null, because then they would 
        // have been caught by the previous test
        if ( ( backendValue == null ) || ( assertValue == null ) )
        {
            return ( backendValue == null ? -1 : 1 );
        }

        // Both objects must be stored as String for numeric.
        // But we need to normalize the values first.
        try
        {
            backendValue = PrepareString.normalize( backendValue, PrepareString.StringType.NUMERIC_STRING );
        }
        catch ( IOException e )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04224, backendValue ) );
        }
        try
        {
            assertValue = PrepareString.normalize( assertValue, PrepareString.StringType.NUMERIC_STRING );
        }
        catch ( IOException e )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04224, assertValue ) );
        }

        BigInteger b1 = new BigInteger( backendValue );
        BigInteger b2 = new BigInteger( assertValue );
        return b1.compareTo( b2 );
    }
}
