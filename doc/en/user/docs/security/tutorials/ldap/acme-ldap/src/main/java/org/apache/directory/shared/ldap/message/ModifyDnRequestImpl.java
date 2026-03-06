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
import org.apache.directory.shared.ldap.message.internal.InternalModifyDnRequest;
import org.apache.directory.shared.ldap.message.internal.InternalModifyDnResponse;
import org.apache.directory.shared.ldap.message.internal.InternalResultResponse;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.name.RDN;


/**
 * Lockable ModifyDNRequest implementation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 918756 $
 */
public class ModifyDnRequestImpl extends AbstractAbandonableRequest implements InternalModifyDnRequest
{
    static final long serialVersionUID = 1233507339633051696L;

    /** PDU's modify Dn candidate <b>entry</b> distinguished name property */
    private DN name;

    /** PDU's <b>newrdn</b> relative distinguished name property */
    private RDN newRdn;

    /** PDU's <b>newSuperior</b> distinguished name property */
    private DN newSuperior;

    /** PDU's <b>deleteOldRdn</b> flag */
    private boolean deleteOldRdn = false;

    private InternalModifyDnResponse response;


    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    /**
     * Creates a Lockable ModifyDnRequest implementing object used to perform a
     * dn change on an entry potentially resulting in an entry move.
     * 
     * @param id
     *            the seq id of this message
     */
    public ModifyDnRequestImpl(final int id)
    {
        super( id, TYPE );
    }


    // -----------------------------------------------------------------------
    // ModifyDnRequest Interface Method Implementations
    // -----------------------------------------------------------------------

    /**
     * Gets the flag which determines if the old Rdn attribute is to be removed
     * from the entry when the new Rdn is used in its stead. This property
     * corresponds to the <b>deleteoldrdn
     * </p>
     * PDU field.
     * 
     * @return true if the old rdn is to be deleted, false if it is not
     */
    public boolean getDeleteOldRdn()
    {
        return deleteOldRdn;
    }


    /**
     * Sets the flag which determines if the old Rdn attribute is to be removed
     * from the entry when the new Rdn is used in its stead. This property
     * corresponds to the <b>deleteoldrdn
     * </p>
     * PDU field.
     * 
     * @param deleteOldRdn
     *            true if the old rdn is to be deleted, false if it is not
     */
    public void setDeleteOldRdn( boolean deleteOldRdn )
    {
        this.deleteOldRdn = deleteOldRdn;
    }


    /**
     * Gets whether or not this request is a DN change resulting in a move
     * operation. Setting the newSuperior property to a non-null name, toggles
     * this flag.
     * 
     * @return true if the newSuperior property is <b>NOT</b> null, false
     *         otherwise.
     */
    public boolean isMove()
    {
        return newSuperior != null;
    }


    /**
     * Gets the entry's distinguished name representing the <b>entry</b> PDU
     * field.
     * 
     * @return the distinguished name of the entry.
     */
    public DN getName()
    {
        return name;
    }


    /**
     * Sets the entry's distinguished name representing the <b>entry</b> PDU
     * field.
     * 
     * @param name
     *            the distinguished name of the entry.
     */
    public void setName( DN name )
    {
        this.name = name;
    }


    /**
     * Gets the new relative distinguished name for the entry which represents
     * the PDU's <b>newrdn</b> field.
     * 
     * @return the relative dn with one component
     */
    public RDN getNewRdn()
    {
        return newRdn;
    }


    /**
     * Sets the new relative distinguished name for the entry which represents
     * the PDU's <b>newrdn</b> field.
     * 
     * @param newRdn
     *            the relative dn with one component
     */
    public void setNewRdn( RDN newRdn )
    {
        this.newRdn = newRdn;
    }


    /**
     * Gets the optional distinguished name of the new superior entry where the
     * candidate entry is to be moved. This property corresponds to the PDU's
     * <b>newSuperior</b> field. May be null representing a simple Rdn change
     * rather than a move operation.
     * 
     * @return the dn of the superior entry the candidate entry is moved under.
     */
    public DN getNewSuperior()
    {
        return newSuperior;
    }


    /**
     * Sets the optional distinguished name of the new superior entry where the
     * candidate entry is to be moved. This property corresponds to the PDU's
     * <b>newSuperior</b> field. May be null representing a simple Rdn change
     * rather than a move operation. Setting this property to a non-null value
     * toggles the move flag obtained via the <code>isMove</code> method.
     * 
     * @param newSuperior
     *            the dn of the superior entry the candidate entry for DN
     *            modification is moved under.
     */
    public void setNewSuperior( DN newSuperior )
    {
        this.newSuperior = newSuperior;
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
            response = new ModifyDnResponseImpl( getMessageId() );
        }

        return response;
    }


    /**
     * Checks to see of an object equals this ModifyDnRequest stub. The equality
     * presumes all ModifyDnRequest specific properties are the same.
     * 
     * @param obj
     *            the object to compare with this stub
     * @return true if the obj is equal to this stub, false otherwise
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

        InternalModifyDnRequest req = ( InternalModifyDnRequest ) obj;

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

        if ( deleteOldRdn != req.getDeleteOldRdn() )
        {
            return false;
        }

        if ( newRdn != null && req.getNewRdn() == null )
        {
            return false;
        }

        if ( newRdn == null && req.getNewRdn() != null )
        {
            return false;
        }

        if ( newRdn != null && req.getNewRdn() != null )
        {
            if ( !newRdn.equals( req.getNewRdn() ) )
            {
                return false;
            }
        }

        if ( newSuperior != null && req.getNewSuperior() == null )
        {
            return false;
        }

        if ( newSuperior == null && req.getNewSuperior() != null )
        {
            return false;
        }

        if ( newSuperior != null && req.getNewSuperior() != null )
        {
            if ( !newSuperior.equals( req.getNewSuperior() ) )
            {
                return false;
            }
        }

        return true;
    }


    /**
     * Get a String representation of a ModifyDNRequest
     * 
     * @return A ModifyDNRequest String
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer();

        sb.append( "    ModifyDN Response\n" );
        sb.append( "        Entry : '" ).append( name ).append( "'\n" );
        if( newRdn != null )
        {
            sb.append( "        New RDN : '" ).append( newRdn.toString() ).append( "'\n" );
        }
        sb.append( "        Delete old RDN : " ).append( deleteOldRdn ).append( "\n" );

        if ( newSuperior != null )
        {
            sb.append( "        New superior : '" ).append( newSuperior.toString() ).append( "'\n" );
        }

        return sb.toString();
    }
}
