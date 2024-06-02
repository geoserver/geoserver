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
package org.apache.directory.shared.ldap.message;


import java.util.Arrays;

import org.apache.directory.shared.ldap.message.internal.InternalAbstractResultResponse;
import org.apache.directory.shared.ldap.message.internal.InternalIntermediateResponse;

/**
 * IntermediateResponse implementation
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 * @version $Rev: 905344 $
 */
public class IntermediateResponseImpl extends InternalAbstractResultResponse implements InternalIntermediateResponse
{
    static final long serialVersionUID = -6646752766410531060L;

    /** ResponseName for the intermediate response */
    protected String oid;

    /** Response Value for the intermediate response */
    protected byte[] value;


    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------
    public IntermediateResponseImpl( int id )
    {
        super( id, TYPE );
    }


    // ------------------------------------------------------------------------
    // IntermediateResponse Interface Method Implementations
    // ------------------------------------------------------------------------

    /**
     * Gets the reponseName specific encoded
     * 
     * @return the response value
     */
    public byte[] getResponseValue()
    {
        if ( value == null )
        {
            return null;
        }

        final byte[] copy = new byte[ value.length ];
        System.arraycopy( value, 0, copy, 0, value.length );
        return copy;
    }


    /**
     * Sets the response value
     * 
     * @param value the response value.
     */
    public void setResponseValue( byte[] value )
    {
        if ( value != null )
        {
            this.value = new byte[ value.length ];
            System.arraycopy( value, 0, this.value, 0, value.length );
        } else {
            this.value = null;
        }
    }
    
    
    /**
     * Gets the OID uniquely identifying this Intemediate response (a.k.a. its
     * name).
     * 
     * @return the OID of the Intemediate response type.
     */
    public String getResponseName()
    {
        return oid;
    }


    /**
     * Sets the OID uniquely identifying this Intemediate response (a.k.a. its
     * name).
     * 
     * @param oid the OID of the Intemediate response type.
     */
    public void setResponseName( String oid )
    {
        this.oid = oid;
    }


    /**
     * Checks to see if an object equals this IntemediateResponse.
     * 
     * @param obj the object to be checked for equality
     * @return true if the obj equals this IntemediateResponse, false otherwise
     */
    public boolean equals( Object obj )
    {
        if ( obj == this )
        {
            return true;
        }

        if ( !super.equals( obj ) )
        {
            return false;
        }
        
        if ( !( obj instanceof InternalIntermediateResponse ) )
        {
            return false;
        }

        InternalIntermediateResponse resp = ( InternalIntermediateResponse ) obj;

        if ( ( oid != null ) && ( resp.getResponseName() == null ) )
        {
            return false;
        }

        if ( ( oid == null ) && ( resp.getResponseName() != null ) )
        {
            return false;
        }

        if ( ( oid != null ) && ( resp.getResponseName() != null ) )
        {
            if ( !oid.equals( resp.getResponseName() ) )
            {
                return false;
            }
        }

        if ( ( value != null ) && ( resp.getResponseValue() == null ) )
        {
            return false;
        }

        if ( ( value == null ) && ( resp.getResponseValue() != null ) )
        {
            return false;
        }

        if ( ( value != null ) && ( resp.getResponseValue() != null ) )
        {
            if ( !Arrays.equals( value, resp.getResponseValue() ) )
            {
                return false;
            }
        }

        return true;
    }
}
