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


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.codec.LdapConstants;


/**
 * Not Filter Object to store the Not filter.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 912399 $, $Date: 2010-02-21 22:52:31 +0200 (Sun, 21 Feb 2010) $, 
 */
public class NotFilter extends ConnectorFilter
{
    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * The constructor.
     */
    public NotFilter( int tlvId )
    {
        super( tlvId );
    }
    
    /**
     * The constructor.
     */
    public NotFilter()
    {
        super();
    }


    /**
     * Subclass the addFilterMethod, as this is specific for a NotFilter (we
     * cannot have more than one elements).
     * 
     * @param filter The Filter to add
     */
    public void addFilter( Filter filter ) throws DecoderException
    {
        if ( filterSet != null )
        {
            throw new DecoderException( I18n.err( I18n.ERR_04057 ) );
        }

        super.addFilter( filter );
    }


    /**
     * Get the NotFilter
     * 
     * @return Returns the notFilter.
     */
    public Filter getNotFilter()
    {
        return filterSet.get( 0 );
    }


    /**
     * Set the NotFilter
     * 
     * @param notFilter The notFilter to set.
     */
    public void setNotFilter( Filter notFilter ) throws DecoderException
    {
        if ( filterSet != null )
        {
            throw new DecoderException( I18n.err( I18n.ERR_04057 ) );
        }

        super.addFilter( notFilter );
    }


    /**
     * Compute the NotFilter length 
     * NotFilter : 
     * 0xA2 L1 super.computeLength()
     * 
     * Length(NotFilter) = Length(0xA2) + Length(super.computeLength()) +
     *      super.computeLength()
     */
    public int computeLength()
    {
        filtersLength = super.computeLength();

        return 1 + TLV.getNbBytes( filtersLength ) + filtersLength;
    }


    /**
     * Encode the NotFilter message to a PDU. 
     * NotFilter : 
     * 0xA2 LL filter.encode()
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

        try
        {
            // The NotFilter Tag
            buffer.put( ( byte ) LdapConstants.NOT_FILTER_TAG );
            buffer.put( TLV.getBytes( filtersLength ) );
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( I18n.err( I18n.ERR_04005 ) );
        }

        super.encode( buffer );

        return buffer;
    }


    /**
     * Return a string compliant with RFC 2254 representing a NOT filter
     * 
     * @return The NOT filter string
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append( '!' ).append( super.toString() );

        return sb.toString();
    }
}
