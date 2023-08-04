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
import java.text.ParseException;

import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.schema.LdapComparator;
import org.apache.directory.shared.ldap.schema.PrepareString;
import org.apache.directory.shared.ldap.util.GeneralizedTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A class for the generalizedTimeOrderingMatch matchingRule (RFC 4517, par. 4.2.17)
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 437007 $
 */
public class GeneralizedTimeComparator extends LdapComparator<String>
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( GeneralizedTimeComparator.class );

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;


    /**
     * The GeneralizedTimeComparator constructor. Its OID is the 
     * generalizedTimeOrderingMatch matching rule OID.
     */
    public GeneralizedTimeComparator( String oid )
    {
        super( oid );
    }


    /**
     * Implementation of the Compare method
     */
    public int compare( String backendValue, String assertValue )
    {
        LOG.debug( "comparing generalizedTimeOrdering objects '{}' with '{}'", backendValue, assertValue );

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

        // Both objects must be stored as String for generalized tim.
        // But we need to normalize the values first.
        GeneralizedTime backendTime;
        try
        {
            String prepared = PrepareString.normalize( backendValue, PrepareString.StringType.DIRECTORY_STRING );
            backendTime = new GeneralizedTime( prepared );
        }
        catch ( IOException ioe )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04224, backendValue ) );
        }
        catch ( ParseException pe )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04224, backendValue ) );
        }

        GeneralizedTime assertTime;
        try
        {
            String prepared = PrepareString.normalize( assertValue, PrepareString.StringType.DIRECTORY_STRING );
            assertTime = new GeneralizedTime( prepared );
        }
        catch ( IOException ioe )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04224, assertValue ) );
        }
        catch ( ParseException pe )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04224, assertValue ) );
        }

        return backendTime.compareTo( assertTime );
    }
}
