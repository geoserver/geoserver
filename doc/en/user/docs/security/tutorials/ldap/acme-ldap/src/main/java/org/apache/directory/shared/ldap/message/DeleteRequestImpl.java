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
import org.apache.directory.shared.ldap.message.internal.InternalDeleteRequest;
import org.apache.directory.shared.ldap.message.internal.InternalDeleteResponse;
import org.apache.directory.shared.ldap.message.internal.InternalResultResponse;
import org.apache.directory.shared.ldap.name.DN;


/**
 * Delete request implementation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 918756 $
 */
public class DeleteRequestImpl extends AbstractAbandonableRequest implements InternalDeleteRequest
{
    static final long serialVersionUID = 3187847454305567542L;

    /** The distinguished name of the entry to delete */
    private DN name;

    private InternalDeleteResponse response;


    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Creates a Lockable DeleteRequest implementing object used to delete a
     * leaf entry from the DIT.
     * 
     * @param id
     *            the sequential message identifier
     */
    public DeleteRequestImpl(final int id)
    {
        super( id, TYPE );
    }


    // ------------------------------------------------------------------------
    // DeleteRequest Interface Method Implementations
    // ------------------------------------------------------------------------

    /**
     * Gets the distinguished name of the leaf entry to be deleted by this
     * request.
     * 
     * @return the DN of the leaf entry to delete.
     */
    public DN getName()
    {
        return name;
    }


    /**
     * Sets the distinguished name of the leaf entry to be deleted by this
     * request.
     * 
     * @param name
     *            the DN of the leaf entry to delete.
     */
    public void setName( DN name )
    {
        this.name = name;
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
            response = new DeleteResponseImpl( getMessageId() );
        }

        return response;
    }


    /**
     * Checks to see if an object is equivalent to this DeleteRequest. First
     * there's a quick test to see if the obj is the same object as this one -
     * if so true is returned. Next if the super method fails false is returned.
     * Then the name of the entry is compared - if not the same false is
     * returned. Finally the method exists returning true.
     * 
     * @param obj
     *            the object to test for equality to this
     * @return true if the obj is equal to this DeleteRequest, false otherwise
     */
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( !super.equals( obj ) )
        {
            return false;
        }

        InternalDeleteRequest req = ( InternalDeleteRequest ) obj;

        if ( name != null && req.getName() == null )
        {
            return false;
        }

        if ( name == null && req.getName() != null )
        {
            return false;
        }

        if ( name != null && req.getName() != null )
        {
            if ( !name.equals( req.getName() ) )
            {
                return false;
            }
        }

        return true;
    }


    /**
     * Return a String representing a DelRequest
     * 
     * @return A DelRequest String
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer();

        sb.append( "    Del request\n" );
        sb.append( "        Entry : '" ).append( name.toString() ).append( "'\n" );

        return sb.toString();
    }
}
