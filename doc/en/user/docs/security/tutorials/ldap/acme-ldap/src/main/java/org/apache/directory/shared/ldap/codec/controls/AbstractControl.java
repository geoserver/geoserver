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
package org.apache.directory.shared.ldap.codec.controls;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.message.control.Control;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A Asn1Object to store a Control.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 912399 $, $Date: 2010-02-21 22:52:31 +0200 (Sun, 21 Feb 2010) $, 
 */
public abstract class AbstractControl extends AbstractAsn1Object implements Control, CodecControl
{
    // ~ Instance fields
    // ----------------------------------------------------------------------------
    /** The control type */
    private String oid;

    /** The criticality (default value is false) */
    private boolean criticality = false;

    /** Optional control value */
    protected byte[] value;
    
    /** The encoded value length */
    protected int valueLength;

    /** The control length */
    private int controlLength;
    
    protected ControlDecoder decoder;

    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Default constructor.
     */
    public AbstractControl( String oid )
    {
        this.oid = oid;
    }

    /**
     * Get the OID
     * 
     * @return A string which represent the control oid
     */
    public String getOid()
    {
        return oid == null ? "" : oid;
    }


    /**
     * Get the control value
     * 
     * @return The control value
     */
    public byte[] getValue()
    {
        return value;
    }


    /**
     * Set the encoded control value
     * 
     * @param encodedValue The encoded control value to store
     */
    public void setValue( byte[] value )
    {
        if ( value != null )
        {
            this.value = new byte[ value.length ];
            System.arraycopy( value, 0, this.value, 0, value.length );
        } 
        else 
        {
            this.value = null;
        }
    }


    /**
     * Get the criticality
     * 
     * @return <code>true</code> if the criticality flag is true.
     */
    public boolean isCritical()
    {
        return criticality;
    }


    /**
     * Set the criticality
     * 
     * @param criticality The criticality value
     */
    public void setCritical( boolean criticality )
    {
        this.criticality = criticality;
    }

    
    /**
     * {@inheritDoc}
     */
    public int computeLength()
    {
        return 0;
    }


    /**
     * {@inheritDoc}
     */
    public int computeLength( int valueLength )
    {
        // The OID
        int oidLengh = StringTools.getBytesUtf8( oid ).length;
        controlLength = 1 + TLV.getNbBytes( oidLengh ) + oidLengh;

        // The criticality, only if true
        if ( criticality )
        {
            controlLength += 1 + 1 + 1; // Always 3 for a boolean
        }

        this.valueLength = valueLength;
        
        if ( valueLength != 0 )
        {
            controlLength += 1 + TLV.getNbBytes( valueLength ) + valueLength;
        }
        
        return 1 + TLV.getNbBytes( controlLength ) + controlLength;
    }


    /**
     * {@inheritDoc}
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( I18n.err( I18n.ERR_04023 ) );
        }

        try
        {
            // The LdapMessage Sequence
            buffer.put( UniversalTag.SEQUENCE_TAG );

            // The length has been calculated by the computeLength method
            buffer.put( TLV.getBytes( controlLength ) );
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( I18n.err( I18n.ERR_04005 ) );
        }

        // The control type
        Value.encode( buffer, getOid().getBytes() );

        // The control criticality, if true
        if ( criticality )
        {
            Value.encode( buffer, criticality );
        }

        return buffer;
    }
    
    
    /**
     * {@inheritDoc}
     */
    public boolean hasValue()
    {
        return value != null;
    }
    
    
    public ControlDecoder getDecoder()
    {
        return decoder;
    }


    /**
     * Return a String representing a Control
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "    Control\n" );
        sb.append( "        Control oid : '" ).append( oid ).append(
            "'\n" );
        sb.append( "        Criticality : '" ).append( criticality ).append( "'\n" );

        if ( value != null )
        {
            sb.append( "        Control value : '" ).append( StringTools.dumpBytes( value ) )
                .append( "'\n" );
        }

        return sb.toString();
    }
}
