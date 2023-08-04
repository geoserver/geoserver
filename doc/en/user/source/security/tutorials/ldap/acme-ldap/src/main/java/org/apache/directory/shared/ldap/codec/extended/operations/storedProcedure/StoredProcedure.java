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

package org.apache.directory.shared.ldap.codec.extended.operations.storedProcedure;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * Stored Procedure Extended Operation bean
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$, 
 */
public class StoredProcedure extends AbstractAsn1Object
{
    private String language;

    private byte[] procedure;

    private List<StoredProcedureParameter> parameters = new ArrayList<StoredProcedureParameter>();

    private StoredProcedureParameter currentParameter;
    
    /** The stored procedure length */
    private int storedProcedureLength;
    
    /** The parameters length */
    private int parametersLength;

    /** The list of all parameter lengths */
    private List<Integer> parameterLength;

    /** The list of all parameter type lengths */
    private List<Integer> paramTypeLength;

    /** The list of all parameter value lengths */
    private List<Integer> paramValueLength;

    public String getLanguage()
    {
        return language;
    }


    public void setLanguage( String language )
    {
        this.language = language;
    }


    public byte[] getProcedure()
    {
        if ( procedure == null )
        {
            return null;
        }

        final byte[] copy = new byte[ procedure.length ];
        System.arraycopy( procedure, 0, copy, 0, procedure.length );
        return copy;
    }


    public void setProcedure( byte[] procedure )
    {
        if ( procedure != null )
        {
            this.procedure = new byte[ procedure.length ];
            System.arraycopy( procedure, 0, this.procedure, 0, procedure.length );
        } else {
            this.procedure = null;
        }
    }


    public List<StoredProcedureParameter> getParameters()
    {
        return parameters;
    }


    public void addParameter( StoredProcedureParameter parameter )
    {
        parameters.add( parameter );
    }


    public StoredProcedureParameter getCurrentParameter()
    {
        return currentParameter;
    }


    public void setCurrentParameter( StoredProcedureParameter currentParameter )
    {
        this.currentParameter = currentParameter;
    }
    

    /**
     * Bean for representing a Stored Procedure Parameter
     */
    public static class StoredProcedureParameter
    {
        byte[] type;

        byte[] value;


        public byte[] getType()
        {
            if ( type == null )
            {
                return null;
            }

            final byte[] copy = new byte[ type.length ];
            System.arraycopy( type, 0, copy, 0, type.length );
            return copy;
        }


        public void setType( byte[] type )
        {
            if ( type != null )
            {
                this.type = new byte[ type.length ];
                System.arraycopy( type, 0, this.type, 0, type.length );
            } else {
                this.type = null;
            }
        }


        public byte[] getValue()
        {
            if ( value == null )
            {
                return null;
            }

            final byte[] copy = new byte[ value.length ];
            System.arraycopy( value, 0, copy, 0, value.length );
            return copy;
        }


        public void setValue( byte[] value )
        {
            if ( value != null )
            {
                this.value = new byte[ value.length ];
                System.arraycopy( value, 0, this.value, 0, value.length );
            } else {
                this.value = null;
            }
        }
    }

    /**
     * Compute the StoredProcedure length 
     * 
     * 0x30 L1 
     *   | 
     *   +--> 0x04 L2 language
     *   +--> 0x04 L3 procedure
     *  [+--> 0x30 L4 (parameters)
     *          |
     *          +--> 0x30 L5-1 (parameter)
     *          |      |
     *          |      +--> 0x04 L6-1 type
     *          |      +--> 0x04 L7-1 value
     *          |      
     *          +--> 0x30 L5-2 (parameter)
     *          |      |
     *          |      +--> 0x04 L6-2 type
     *          |      +--> 0x04 L7-2 value
     *          |
     *          +--> ...
     *          |      
     *          +--> 0x30 L5-m (parameter)
     *                 |
     *                 +--> 0x04 L6-m type
     *                 +--> 0x04 L7-m value
     */
    public int computeLength()
    {
        // The language
        byte[] languageBytes = StringTools.getBytesUtf8( language );
        
        int languageLength = 1 + TLV.getNbBytes( languageBytes.length )
            + languageBytes.length;
        
        // The procedure
        int procedureLength = 1 + TLV.getNbBytes( procedure.length )
            + procedure.length;
        
        // Compute parameters length value
        if ( parameters != null )
        {
            parameterLength = new LinkedList<Integer>();
            paramTypeLength = new LinkedList<Integer>();
            paramValueLength = new LinkedList<Integer>();
            
            for ( StoredProcedureParameter spParam:parameters )
            {
                int localParameterLength = 0;
                int localParamTypeLength = 0;
                int localParamValueLength = 0;
                
                localParamTypeLength = 1 + TLV.getNbBytes( spParam.type.length ) + spParam.type.length;
                localParamValueLength = 1 + TLV.getNbBytes( spParam.value.length ) + spParam.value.length;
                
                localParameterLength = localParamTypeLength + localParamValueLength;
                
                parametersLength += 1 + TLV.getNbBytes( localParameterLength ) + localParameterLength;
                
                parameterLength.add( localParameterLength );
                paramTypeLength.add( localParamTypeLength );
                paramValueLength.add( localParamValueLength );
            }
        }
        
        int localParametersLength = 1 + TLV.getNbBytes( parametersLength ) + parametersLength; 
        storedProcedureLength = languageLength + procedureLength + localParametersLength;

        return 1 + TLV.getNbBytes( storedProcedureLength ) + storedProcedureLength; 
    }

    /**
     * Encode the StoredProcedure message to a PDU. 
     * 
     * @return The PDU.
     */
    public ByteBuffer encode() throws EncoderException
    {
        // Allocate the bytes buffer.
        ByteBuffer bb = ByteBuffer.allocate( computeLength() );

        try
        {
            // The StoredProcedure Tag
            bb.put( UniversalTag.SEQUENCE_TAG );
            bb.put( TLV.getBytes( storedProcedureLength ) );

            // The language
            Value.encode( bb, language );

            // The procedure
            Value.encode( bb, procedure );
            
            // The parameters sequence
            bb.put( UniversalTag.SEQUENCE_TAG );
            bb.put( TLV.getBytes( parametersLength ) );

            // The parameters list
            if ( ( parameters != null ) && ( parameters.size() != 0 ) )
            {
                int parameterNumber = 0;

                for ( StoredProcedureParameter spParam:parameters )
                {
                    // The parameter sequence
                    bb.put( UniversalTag.SEQUENCE_TAG );
                    int localParameterLength = parameterLength.get( parameterNumber );
                    bb.put( TLV.getBytes( localParameterLength ) );

                    // The parameter type
                    Value.encode( bb, spParam.type );

                    // The parameter value
                    Value.encode( bb, spParam.value );

                    // Go to the next parameter;
                    parameterNumber++;
                }
            }
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( I18n.err( I18n.ERR_04005 ) );
        }

        return bb;
    }


    /**
     * Returns the StoredProcedure string
     * 
     * @return The StoredProcedure string
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer();

        sb.append( "    StoredProcedure\n" );
        sb.append( "        Language : '" ).append( language ).append( "'\n" );
        sb.append( "        Procedure\n" ).append( StringTools.utf8ToString( procedure ) ).append( "'\n" );

        if ( ( parameters == null ) || ( parameters.size() == 0 ) )
        {
            sb.append( "        No parameters\n" );
        }
        else
        {
            sb.append( "        Parameters\n" );

            int i = 1;
            
            for ( StoredProcedureParameter spParam:parameters )
            {
                sb.append( "            type[" ).append( i ) .append( "] : '" ).
                    append( StringTools.utf8ToString( spParam.type ) ).append( "'\n" );
                sb.append( "            value[" ).append( i ) .append( "] : '" ).
                    append( StringTools.dumpBytes( spParam.value ) ).append( "'\n" );
            }
        }

        return sb.toString();
    }
}
