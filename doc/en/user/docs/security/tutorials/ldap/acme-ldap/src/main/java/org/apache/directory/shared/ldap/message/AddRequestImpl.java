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
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.client.DefaultClientEntry;
import org.apache.directory.shared.ldap.message.internal.InternalAddRequest;
import org.apache.directory.shared.ldap.message.internal.InternalAddResponse;
import org.apache.directory.shared.ldap.message.internal.InternalResultResponse;
import org.apache.directory.shared.ldap.name.DN;


/**
 * Lockable add request implemenation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 918756 $
 */
public class AddRequestImpl extends AbstractAbandonableRequest implements InternalAddRequest
{
    static final long serialVersionUID = 7534132448349520346L;

    /** A MultiMap of the new entry's attributes and their values */
    private Entry entry;

    private InternalAddResponse response;


    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Creates an AddRequest implementation to create a new entry.
     * 
     * @param id
     *            the sequence identifier of the AddRequest message.
     */
    public AddRequestImpl(final int id)
    {
        super( id, TYPE );
        entry = new DefaultClientEntry();
    }


    // ------------------------------------------------------------------------
    // AddRequest Interface Method Implementations
    // ------------------------------------------------------------------------

    /**
     * Gets the distinguished name of the entry to add.
     * 
     * @return the Dn of the added entry.
     */
    public DN getEntryDn()
    {
        return entry.getDn();
    }


    /**
     * Sets the distinguished name of the entry to add.
     * 
     * @param entry the Dn of the added entry.
     */
    public void setEntryDn( DN dn )
    {
        entry.setDn( dn );
    }


    /**
     * Gets the entry to add.
     * 
     * @return the added Entry
     */
    public Entry getEntry()
    {
        return entry;
    }


    /**
     * Sets the Entry to add.
     * 
     * @param entry the added Entry
     */
    public void setEntry( Entry entry )
    {
        this.entry = entry;
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
            response = new AddResponseImpl( getMessageId() );
        }

        return response;
    }


    /**
     * Checks to see if an object is equivalent to this AddRequest. First
     * there's a quick test to see if the obj is the same object as this one -
     * if so true is returned. Next if the super method fails false is returned.
     * Then the name of the entry is compared - if not the same false is
     * returned. Lastly the attributes of the entry are compared. If they are
     * not the same false is returned otherwise the method exists returning
     * true.
     * 
     * @param obj
     *            the object to test for equality to this
     * @return true if the obj is equal to this AddRequest, false otherwise
     */
    public boolean equals( Object obj )
    {
        // Short circuit
        if ( this == obj )
        {
            return true;
        }
        
        // Check the object class. If null, it will exit.
        if ( !( obj instanceof InternalAddRequest ) )
        {
            return false;
        }

        if ( !super.equals( obj ) )
        {
            return false;
        }

        InternalAddRequest req = ( InternalAddRequest ) obj;

        // Check the entry
        if ( entry == null )
        {
            return ( req.getEntry() == null );
        }
        else
        {
            return ( entry.equals( req.getEntry() ) );
        }
    }

    /**
     * @see Object#hashCode()
     * @return the instance's hash code 
     */
    public int hashCode()
    {
        int hash = 37;
        hash = hash*17 + ( entry == null ? 0 : entry.hashCode() );
        hash = hash*17 + ( response == null ? 0 : response.hashCode() );
        hash = hash*17 + super.hashCode();
        
        return hash;
    }

    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( "    Add Request :\n" );
        
        if ( entry == null )
        {
            sb.append( "            No entry\n" );
        }
        else
        {
            sb.append( entry.toString() );
        }

        return sb.toString();
    }
}
