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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.directory.shared.ldap.codec.MessageTypeEnum;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.client.ClientModification;
import org.apache.directory.shared.ldap.message.internal.InternalModifyRequest;
import org.apache.directory.shared.ldap.message.internal.InternalModifyResponse;
import org.apache.directory.shared.ldap.message.internal.InternalResultResponse;
import org.apache.directory.shared.ldap.name.DN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Lockable ModifyRequest implementation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 918756 $
 */
public class ModifyRequestImpl extends AbstractAbandonableRequest implements InternalModifyRequest
{
    static final long serialVersionUID = -505803669028990304L;

    /** The logger */
    private static final transient Logger LOG = LoggerFactory.getLogger( ModifyRequestImpl.class );

    /** Dn of the entry to modify or PDU's <b>object</b> field */
    private DN name;

    /** Sequence of modifications or PDU's <b>modification</b> seqence field */
    private List<Modification> mods = new ArrayList<Modification>();

    private InternalModifyResponse response;


    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Creates a Lockable ModifyRequest implementing object used to modify the
     * attributes of an entry.
     * 
     * @param id
     *            the sequential message identifier
     */
    public ModifyRequestImpl(final int id)
    {
        super( id, TYPE );
    }


    // ------------------------------------------------------------------------
    // ModifyRequest Interface Method Implementations
    // ------------------------------------------------------------------------
    /**
     * Gets an immutable Collection of modification items representing the
     * atomic changes to perform on the candidate entry to modify.
     * 
     * @return an immutable Collection of Modification instances.
     */
    public Collection<Modification> getModificationItems()
    {
        return Collections.unmodifiableCollection( mods );
    }


    /**
     * Gets the distinguished name of the entry to be modified by this request.
     * This property represents the PDU's <b>object</b> field.
     * 
     * @return the DN of the modified entry.
     */
    public DN getName()
    {
        return name;
    }


    /**
     * Sets the distinguished name of the entry to be modified by this request.
     * This property represents the PDU's <b>object</b> field.
     * 
     * @param name
     *            the DN of the modified entry.
     */
    public void setName( DN name )
    {
        this.name = name;
    }


    /**
     * Adds a Modification to the set of modifications composing this modify
     * request.
     * 
     * @param mod a Modification to add
     */
    public void addModification( Modification mod )
    {
        mods.add( mod );
    }


    /**
     * Removes a Modification to the set of modifications composing this
     * modify request.
     * 
     * @param mod a Modification to remove.
     */
    public void removeModification( Modification mod )
    {
        mods.remove( mod );
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
            response = new ModifyResponseImpl( getMessageId() );
        }

        return response;
    }


    /**
     * Checks to see if ModifyRequest stub equals another by factoring in checks
     * for the name and modification items of the request.
     * 
     * @param obj
     *            the object to compare this ModifyRequest to
     * @return true if obj equals this ModifyRequest, false otherwise
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

        InternalModifyRequest req = ( InternalModifyRequest ) obj;

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

        if ( req.getModificationItems().size() != mods.size() )
        {
            return false;
        }

        Iterator<Modification> list = req.getModificationItems().iterator();

        for ( int i = 0; i < mods.size(); i++ )
        {
            Modification item = list.next();

            if ( item == null )
            {
                if ( mods.get( i ) != null )
                {
                    return false;
                }
            }
            else
                
            if ( !item.equals((ClientModification) mods.get( i ) ) )
            {
                return false;
            }
        }

        return true;
    }


    /**
     * Get a String representation of a ModifyRequest
     * 
     * @return A ModifyRequest String
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer();

        sb.append( "    Modify Request\n" );
        sb.append( "        Object : '" ).append( name ).append( "'\n" );

        if ( mods != null )
        {

            for ( int i = 0; i < mods.size(); i++ )
            {

                ClientModification modification = ( ClientModification ) mods.get( i );

                sb.append( "            Modification[" ).append( i ).append( "]\n" );
                sb.append( "                Operation : " );

                switch ( modification.getOperation() )
                {
                    case ADD_ATTRIBUTE:
                        sb.append( " add\n" );
                        break;

                    case REPLACE_ATTRIBUTE:
                        sb.append( " replace\n" );
                        break;

                    case REMOVE_ATTRIBUTE:
                        sb.append( " delete\n" );
                        break;
                }

                sb.append( "                Modification\n" );
                sb.append( modification.getAttribute() );
            }
        }

        return sb.toString();
    }
}
