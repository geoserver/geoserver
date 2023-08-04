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
package org.apache.directory.shared.ldap.message.internal;

import org.apache.directory.shared.ldap.codec.MessageTypeEnum;
import org.apache.directory.shared.ldap.message.SingleReplyRequest;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.name.RDN;


/**
 * Modify DN request protocol message used to rename or move an existing entry
 * in the directory. Here's what <a
 * href="http://www.faqs.org/rfcs/rfc2251.html">RFC 2251</a> has to say about
 * it:
 * 
 * <pre>
 *  4.9. Modify DN Operation
 * 
 *   The Modify DN Operation allows a client to change the leftmost (least
 *   significant) component of the name of an entry in the directory, or
 *   to move a subtree of entries to a new location in the directory.  The
 *   Modify DN Request is defined as follows:
 * 
 *        ModifyDNRequest ::= [APPLICATION 12] SEQUENCE {
 *                entry           LDAPDN,
 *                newrdn          RelativeLDAPDN,
 *                deleteoldrdn    BOOLEAN,
 *                newSuperior     [0] LDAPDN OPTIONAL }
 * 
 *   Parameters of the Modify DN Request are:
 * 
 *   - entry: the Distinguished Name of the entry to be changed.  This
 *     entry may or may not have subordinate entries.
 * 
 *   - newrdn: the RDN that will form the leftmost component of the new
 *     name of the entry.
 * 
 *   - deleteoldrdn: a boolean parameter that controls whether the old RDN
 *     attribute values are to be retained as attributes of the entry, or
 *     deleted from the entry.
 * 
 *   - newSuperior: if present, this is the Distinguished Name of the entry
 *     which becomes the immediate superior of the existing entry.
 * </pre>
 * 
 * Note that this operation can move an entry and change its Rdn at the same
 * time in fact it might have no choice to comply with name forms.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision: 918756 $
 */
public interface InternalModifyDnRequest extends SingleReplyRequest, InternalAbandonableRequest
{
    /** Modify DN request message type enumeration value */
    MessageTypeEnum TYPE = MessageTypeEnum.MODIFYDN_REQUEST;

    /** Modify DN response message type enumeration value */
    MessageTypeEnum RESP_TYPE = InternalModifyDnResponse.TYPE;


    /**
     * Gets the entry's distinguished name representing the <b>entry</b> PDU
     * field.
     * 
     * @return the distinguished name of the entry.
     */
    DN getName();


    /**
     * Sets the entry's distinguished name representing the <b>entry</b> PDU
     * field.
     * 
     * @param name
     *            the distinguished name of the entry.
     */
    void setName( DN name );


    /**
     * Gets the new relative distinguished name for the entry which represents
     * the PDU's <b>newrdn</b> field.
     * 
     * @return the relative dn with one component
     */
    RDN getNewRdn();


    /**
     * Sets the new relative distinguished name for the entry which represents
     * the PDU's <b>newrdn</b> field.
     * 
     * @param newRdn
     *            the relative dn with one component
     */
    void setNewRdn( RDN newRdn );


    /**
     * Gets the flag which determines if the old Rdn attribute is to be removed
     * from the entry when the new Rdn is used in its stead. This property
     * corresponds to the <b>deleteoldrdn
     * </p>
     * PDU field.
     * 
     * @return true if the old rdn is to be deleted, false if it is not
     */
    boolean getDeleteOldRdn();


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
    void setDeleteOldRdn( boolean deleteOldRdn );


    /**
     * Gets the optional distinguished name of the new superior entry where the
     * candidate entry is to be moved. This property corresponds to the PDU's
     * <b>newSuperior</b> field. May be null representing a simple Rdn change
     * rather than a move operation.
     * 
     * @return the dn of the superior entry the candidate entry is moved under.
     */
    DN getNewSuperior();


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
    void setNewSuperior( DN newSuperior );


    /**
     * Gets whether or not this request is a DN change resulting in a move
     * operation. Setting the newSuperior property to a non-null name, toggles
     * this flag.
     * 
     * @return true if the newSuperior property is <b>NOT</b> null, false
     *         otherwise.
     */
    boolean isMove();
}
