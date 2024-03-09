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

import org.apache.directory.shared.ldap.message.internal.InternalAbstractResponse;
import org.apache.directory.shared.ldap.message.internal.InternalReferral;
import org.apache.directory.shared.ldap.message.internal.InternalSearchResponseReference;


/**
 * SearchResponseReference implementation
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision: 905344 $
 */
public class SearchResponseReferenceImpl extends InternalAbstractResponse implements InternalSearchResponseReference
{
    static final long serialVersionUID = 7423807019951309810L;

    /** Referral holding the reference urls */
    private InternalReferral referral;


    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Creates a Lockable SearchResponseReference as a reply to an SearchRequest
     * to indicate the end of a search operation.
     * 
     * @param id
     *            the session unique message id
     */
    public SearchResponseReferenceImpl(final int id)
    {
        super( id, TYPE );
    }


    // ------------------------------------------------------------------------
    // SearchResponseReference Interface Method Implementations
    // ------------------------------------------------------------------------

    /**
     * Gets the sequence of LdapUrls as a Referral instance.
     * 
     * @return the sequence of LdapUrls
     */
    public InternalReferral getReferral()
    {
        return this.referral;
    }


    /**
     * Sets the sequence of LdapUrls as a Referral instance.
     * 
     * @param referral
     *            the sequence of LdapUrls
     */
    public void setReferral( InternalReferral referral )
    {
        this.referral = referral;
    }


    /**
     * Checks to see if an object is equal to this SearchResponseReference stub.
     * 
     * @param obj
     *            the object to compare to this response stub
     * @return true if the objects are equivalent false otherwise
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

        InternalSearchResponseReference resp = ( InternalSearchResponseReference ) obj;

        if ( this.referral != null && resp.getReferral() == null )
        {
            return false;
        }

        if ( this.referral == null && resp.getReferral() != null )
        {
            return false;
        }

        if ( this.referral != null && resp.getReferral() != null )
        {
            if ( !this.referral.equals( resp.getReferral() ) )
            {
                return false;
            }
        }

        return true;
    }
}
