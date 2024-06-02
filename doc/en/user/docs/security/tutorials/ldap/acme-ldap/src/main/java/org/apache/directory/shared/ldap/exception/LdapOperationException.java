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


import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.DN;


/**
 * An class for LDAP operation exceptions which add LDAP specific information to
 * Exceptions.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 437007 $
 */
public class LdapOperationException extends LdapException
{
    /** The serial version UUID */
    private static final long serialVersionUID = 1L;

    /** The operation resultCode */
    protected ResultCodeEnum resultCode;
    
    /** The resolved DN */
    protected DN resolvedDn;

    /**
     * @return the resolvedDn
     */
    public DN getResolvedDn()
    {
        return resolvedDn;
    }


    /**
     * @param resolvedDn the resolvedDn to set
     */
    public void setResolvedDn( DN resolvedDn )
    {
        this.resolvedDn = resolvedDn;
    }


    /**
     * Creates a new instance of LdapOperationException.
     *
     * @param resultCode The operation resultCode
     * @param message The exception message
     */
    public LdapOperationException( ResultCodeEnum resultCode, String message )
    {
        super( message );
        this.resultCode = resultCode;
    }

    
    /**
     * Creates a new instance of LdapOperationException.
     *
     * @param message The exception message
     */
    public LdapOperationException( String message )
    {
        super( message );
    }
    
    
    /**
     * Gets the LDAP result code that would be associated with this exception.
     * 
     * @return the LDAP result code corresponding to this exception type.
     */
    public ResultCodeEnum getResultCode()
    {
        return resultCode;
    }
}
