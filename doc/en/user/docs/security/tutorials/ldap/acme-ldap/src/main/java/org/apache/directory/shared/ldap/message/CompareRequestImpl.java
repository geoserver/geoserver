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


import org.apache.directory.shared.ldap.codec.MessageTypeEnum;
import org.apache.directory.shared.ldap.entry.BinaryValue;
import org.apache.directory.shared.ldap.entry.StringValue;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.message.internal.InternalCompareRequest;
import org.apache.directory.shared.ldap.message.internal.InternalCompareResponse;
import org.apache.directory.shared.ldap.message.internal.InternalResultResponse;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * Comparison request implementation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 928945 $
 */
public class CompareRequestImpl extends AbstractAbandonableRequest implements InternalCompareRequest
{
    static final long serialVersionUID = 1699731530016468977L;

    /** Distinguished name identifying the compared entry */
    private DN name;

    /** The id of the attribute used in the comparison */
    private String attrId;

    /** The value of the attribute used in the comparison */
    private Value<?> attrVal;

    private InternalCompareResponse response;


    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Creates an CompareRequest implementation to compare a named entry with an
     * attribute value assertion pair.
     * 
     * @param id
     *            the sequence identifier of the CompareRequest message.
     */
    public CompareRequestImpl( final int id )
    {
        super( id, TYPE );
    }


    // ------------------------------------------------------------------------
    // ComparisonRequest Interface Method Implementations
    // ------------------------------------------------------------------------

    /**
     * Gets the distinguished name of the entry to be compared using the
     * attribute value assertion.
     * 
     * @return the DN of the compared entry.
     */
    public DN getName()
    {
        return name;
    }


    /**
     * Sets the distinguished name of the entry to be compared using the
     * attribute value assertion.
     * 
     * @param name
     *            the DN of the compared entry.
     */
    public void setName( DN name )
    {
        this.name = name;
    }


    /**
     * Gets the attribute value to use in making the comparison.
     * 
     * @return the attribute value to used in comparison.
     */
    public Value<?> getAssertionValue()
    {
        return attrVal;
    }


    /**
     * Sets the attribute value to use in the comparison.
     * 
     * @param attrVal
     *            the attribute value used in comparison.
     */
    public void setAssertionValue( String attrVal )
    {
        this.attrVal = new StringValue( attrVal );
    }


    /**
     * Sets the attribute value to use in the comparison.
     * 
     * @param attrVal
     *            the attribute value used in comparison.
     */
    public void setAssertionValue( byte[] attrVal )
    {
        if ( attrVal != null )
        {
            this.attrVal = new BinaryValue( attrVal );
        }
        else
        {
            this.attrVal = null;
        }
    }


    /**
     * Gets the attribute id use in making the comparison.
     * 
     * @return the attribute id used in comparison.
     */
    public String getAttributeId()
    {
        return attrId;
    }


    /**
     * Sets the attribute id used in the comparison.
     * 
     * @param attrId
     *            the attribute id used in comparison.
     */
    public void setAttributeId( String attrId )
    {
        this.attrId = attrId;
    }


    // ------------------------------------------------------------------------
    // SingleReplyRequest Interface Method Implementations
    // ------------------------------------------------------------------------

    /**
     * Gets the protocol response message type for this request which produces
     * at least one response.
     * 
     * @return the message type of the response.
     */
    public MessageTypeEnum getResponseType()
    {
        return RESP_TYPE;
    }


    /**
     * The result containing response for this request.
     * 
     * @return the result containing response for this request
     */
    public InternalResultResponse getResultResponse()
    {
        if ( response == null )
        {
            response = new CompareResponseImpl( getMessageId() );
        }

        return response;
    }


    /**
     * Checks to see if an object is equivalent to this CompareRequest.
     * 
     * @param obj the obj to compare with this CompareRequest
     * @return true if the obj is equal to this request, false otherwise
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

        InternalCompareRequest req = ( InternalCompareRequest ) obj;
        DN reqName = req.getName();

        if ( ( name != null ) && ( reqName == null ) )
        {
            return false;
        }

        if ( ( name == null ) && ( reqName != null ) )
        {
            return false;
        }

        if ( ( name != null ) && ( reqName != null ) )
        {
            if ( !name.equals( req.getName() ) )
            {
                return false;
            }
        }

        String reqId = req.getAttributeId();

        if ( ( attrId != null ) && ( reqId == null ) )
        {
            return false;
        }

        if ( ( attrId == null ) && ( reqId != null ) )
        {
            return false;
        }

        if ( ( attrId != null ) && ( reqId != null ) )
        {
            if ( !attrId.equals( reqId ) )
            {
                return false;
            }
        }

        Value<?> reqVal = req.getAssertionValue();

        if ( attrVal != null )
        {
            if ( reqVal != null )
            {
                return attrVal.equals( reqVal );
            }
            else
            {
                return false;
            }
        }
        else
        {
            return reqVal == null;
        }
    }


    /**
     * Get a String representation of a Compare Request
     * 
     * @return A Compare Request String
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "    Compare request\n" );
        sb.append( "        Entry : '" ).append( name.toString() ).append( "'\n" );
        sb.append( "        Attribute description : '" ).append( attrId ).append( "'\n" );
        sb.append( "        Attribute value : '" );
        
        if ( !attrVal.isBinary() )
        {
            sb.append( attrVal.get() );
        }
        else
        {
            byte[] binVal = attrVal.getBytes();
            sb.append( StringTools.utf8ToString( binVal ) ).append( '/' ).append(
                StringTools.dumpBytes( binVal ) ).append( "'\n" );
        }

        return sb.toString();
    }
}
