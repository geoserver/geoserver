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

import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.DN;


/**
 * LDAPv3 result structure embedded into Responses. See section 4.1.10 in <a
 * href="">RFC 2251</a> for a description of the LDAPResult ASN.1 structure,
 * here's a snippet from it:
 * 
 * <pre>
 *   The LDAPResult is the construct used in this protocol to return
 *   success or failure indications from servers to clients. In response
 *   to various requests servers will return responses containing fields
 *   of type LDAPResult to indicate the final status of a protocol
 *   operation request.
 * </pre>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision: 918756 $
 */
public interface InternalLdapResult
{
    /**
     * Gets the result code enumeration associated with the response.
     * Corresponds to the <b> resultCode </b> field within the LDAPResult ASN.1
     * structure.
     * 
     * @return the result code enum value.
     */
    ResultCodeEnum getResultCode();


    /**
     * Sets the result code enumeration associated with the response.
     * Corresponds to the <b> resultCode </b> field within the LDAPResult ASN.1
     * structure.
     * 
     * @param resultCode
     *            the result code enum value.
     */
    void setResultCode( ResultCodeEnum resultCode );


    /**
     * Gets the lowest entry in the directory that was matched. For result codes
     * of noSuchObject, aliasProblem, invalidDNSyntax and
     * aliasDereferencingProblem, the matchedDN field is set to the name of the
     * lowest entry (object or alias) in the directory that was matched. If no
     * aliases were dereferenced while attempting to locate the entry, this will
     * be a truncated form of the name provided, or if aliases were
     * dereferenced, of the resulting name, as defined in section 12.5 of X.511
     * [8]. The matchedDN field is to be set to a zero length string with all
     * other result codes.
     * 
     * @return the Dn of the lowest matched entry.
     */
    DN getMatchedDn();


    /**
     * Sets the lowest entry in the directory that was matched.
     * 
     * @see #getMatchedDn()
     * @param dn
     *            the Dn of the lowest matched entry.
     */
    void setMatchedDn( DN dn );


    /**
     * Gets the descriptive error message associated with the error code. May be
     * null for SUCCESS, COMPARETRUE, COMPAREFALSE and REFERRAL operations.
     * 
     * @return the descriptive error message.
     */
    String getErrorMessage();


    /**
     * Sets the descriptive error message associated with the error code. May be
     * null for SUCCESS, COMPARETRUE, and COMPAREFALSE operations.
     * 
     * @param errorMessage
     *            the descriptive error message.
     */
    void setErrorMessage( String errorMessage );


    /**
     * Gets whether or not this result represents a Referral. For referrals the
     * error code is set to REFERRAL and the referral property is not null.
     * 
     * @return true if this result represents a referral.
     */
    boolean isReferral();


    /**
     * Gets the Referral associated with this LdapResult if the resultCode
     * property is set to the REFERRAL ResultCodeEnum.
     * 
     * @return the referral on REFERRAL errors, null on all others.
     */
    InternalReferral getReferral();


    /**
     * Sets the Referral associated with this LdapResult if the resultCode
     * property is set to the REFERRAL ResultCodeEnum. Setting this property
     * will result in a true return from isReferral and the resultCode should be
     * set to REFERRAL.
     * 
     * @param referral
     *            optional referral on REFERRAL errors.
     */
    void setReferral( InternalReferral referral );
}
