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
package org.apache.directory.shared.ldap.exception;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.ReferralException;

import org.apache.directory.shared.ldap.NotImplementedException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.DN;


/**
 * A {@link LdapOperationException} which associates a resultCode namely the
 * {@link ResultCodeEnum#REFERRAL} resultCode with the exception.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 925499 $
 */
public class LdapReferralException extends AbstractLdapReferralException
{
    /** The serial version UUID */
    static final long serialVersionUID = 1L;

    /** The list of referrals */
    private final List<String> refs;

    /** The current index in the list of referrals */
    private int index = 0;
    
    /** The remaining DN */
    private DN remainingDn;
    
    /** TODO */
    private Object resolvedObject;


    /**
     * @see ReferralException#ReferralException()
     */
    public LdapReferralException( Collection<String> refs )
    {
        super( null );
        this.refs = new ArrayList<String>( refs );
    }


    /**
     * @see ReferralException#ReferralException(java.lang.String)
     */
    public LdapReferralException( Collection<String> refs, String explanation )
    {
        super( explanation );
        this.refs = new ArrayList<String>( refs );
    }


    /**
     * Always returns {@link ResultCodeEnum#REFERRAL}
     * 
     * @see LdapException#getResultCode()
     */
    public ResultCodeEnum getResultCode()
    {
        return ResultCodeEnum.REFERRAL;
    }


    public String getReferralInfo()
    {
        return refs.get( index );
    }


    public Context getReferralContext() throws NamingException
    {
        throw new NotImplementedException();
    }


    public Context getReferralContext( Hashtable<?, ?> arg ) throws NamingException
    {
        throw new NotImplementedException();
    }


    public boolean skipReferral()
    {
        index++;
        return index < refs.size();
    }


    public void retryReferral()
    {
        throw new NotImplementedException();
    }
    
    
    /**
     * @return the remainingDn
     */
    public DN getRemainingDn()
    {
        return remainingDn;
    }


    /**
     * @param remainingDn the remainingName to set
     */
    public void setRemainingDn( DN remainingDn )
    {
        this.remainingDn = remainingDn;
    }


    /**
     * @return the resolvedObject
     */
    public Object getResolvedObject()
    {
        return resolvedObject;
    }


    /**
     * @param resolvedObject the resolvedObject to set
     */
    public void setResolvedObject( Object resolvedObject )
    {
        this.resolvedObject = resolvedObject;
    }
}
