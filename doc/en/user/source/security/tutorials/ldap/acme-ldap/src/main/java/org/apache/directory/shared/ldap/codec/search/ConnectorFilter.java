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
package org.apache.directory.shared.ldap.codec.search;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.i18n.I18n;


/**
 * This Filter abstract class is used to store a set of filters used by
 * OR/AND/NOT filters.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 912399 $, $Date: 2010-02-21 22:52:31 +0200 (Sun, 21 Feb 2010) $, 
 */
public abstract class ConnectorFilter extends Filter
{
    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** The set of filters used by And/Or filters */
    protected List<Filter> filterSet;

    /** The filters length */
    protected int filtersLength;


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * The constructor. We wont initialize the ArrayList as it may not be used.
     */
    public ConnectorFilter( int tlvId )
    {
        super( tlvId );
    }


    /**
     * The constructor. We wont initialize the ArrayList as it may not be used.
     */
    public ConnectorFilter()
    {
        super();
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Add a new Filter to the list.
     * 
     * @param filter The filter to add
     */
    public void addFilter( Filter filter ) throws DecoderException
    {

        if ( filterSet == null )
        {
            filterSet = new ArrayList<Filter>();
        }

        filterSet.add( filter );
    }


    /**
     * Get the list of filters stored in the composite filter
     * 
     * @return And array of filters
     */
    public List<Filter> getFilterSet()
    {
        return filterSet;
    }


    /**
     * Compute the ConnectorFilter length Length(ConnectorFilter) =
     * sum(filterSet.computeLength())
     */
    public int computeLength()
    {
        int connectorFilterLength = 0;

        if ( ( filterSet != null ) && ( filterSet.size() != 0 ) )
        {
            for ( Filter filter:filterSet )
            {
                connectorFilterLength += filter.computeLength();
            }
        }

        return connectorFilterLength;
    }


    /**
     * Encode the ConnectorFilter message to a PDU. 
     * 
     * ConnectorFilter :
     * filter.encode() ... filter.encode()
     * 
     * @param buffer The buffer where to put the PDU
     * @return The PDU.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( I18n.err( I18n.ERR_04023 ) );
        }

        // encode each filter
        if ( ( filterSet != null ) && ( filterSet.size() != 0 ) )
        {
            for ( Filter filter:filterSet )
            {
                filter.encode( buffer );
            }
        }

        return buffer;
    }


    /**
     * Return a string compliant with RFC 2254 representing a composite filter,
     * one of AND, OR and NOT
     * 
     * @return The composite filter string
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        if ( ( filterSet != null ) && ( filterSet.size() != 0 ) )
        {
            for ( Filter filter : filterSet )
            {
                sb.append( '(' ).append( filter ).append( ')' );
            }
        }

        return sb.toString();
    }
}
