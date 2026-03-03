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


import java.util.Comparator;

import org.apache.directory.shared.ldap.exception.LdapException;

import org.apache.directory.shared.ldap.schema.LdapComparator;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A comparator which normalizes a value first before using a subordinate
 * comparator to compare them.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 923524 $
 */
/* no qualifier*/ class NormalizingComparator extends LdapComparator<String>
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( NormalizingComparator.class );

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** the Normalizer to normalize values with before comparing */
    private Normalizer normalizer;

    /** the underlying comparator to use for comparisons */
    private LdapComparator<String> comparator;


    /**
     * A comparator which normalizes a value first before comparing them.
     * 
     * @param normalizer the Normalizer to normalize values with before comparing
     * @param comparator the underlying comparator to use for comparisons
     */
    public NormalizingComparator( String oid, Normalizer normalizer, LdapComparator<String> comparator )
    {
        super( oid );
        this.normalizer = normalizer;
        this.comparator = comparator;
    }


    /**
     * If any normalization attempt fails we compare using the unnormalized
     * object.
     * 
     * @see Comparator#compare(Object, Object)
     */
    public int compare( String o1, String o2 )
    {
        String n1;
        String n2;

        try
        {
            n1 = normalizer.normalize( o1 );
        }
        catch ( LdapException e )
        {
            LOG.warn( "Failed to normalize: " + o1, e );
            n1 = o1;
        }

        try
        {
            n2 = normalizer.normalize( o2 );
        }
        catch ( LdapException e )
        {
            LOG.warn( "Failed to normalize: " + o2, e );
            n2 = o2;
        }

        return comparator.compare( n1, n2 );
    }
    
    
    /**
     * Makes sure we update the oid property of the contained normalizer and 
     * comparator.
     * 
     * @param oid the object identifier
     */
    @Override
    public void setOid( String oid )
    {
    	super.setOid( oid );
    	normalizer.setOid( oid );
    	comparator.setOid( oid );
    }    
}
