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

import org.apache.directory.shared.ldap.message.internal.InternalLdapResult;
import org.apache.directory.shared.ldap.message.internal.InternalReferral;
import org.apache.directory.shared.ldap.name.DN;


/**
 * LdapResult implementation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision: 918756 $
 */
public class LdapResultImpl implements InternalLdapResult
{
    static final long serialVersionUID = -1446626887394613213L;

    /** Lowest matched entry Dn - defaults to empty string */
    private DN matchedDn;

    /** Referral associated with this LdapResult if the errorCode is REFERRAL */
    private InternalReferral referral;

    /** Decriptive error message - defaults to empty string */
    private String errorMessage;

    /** Resultant operation error code - defaults to SUCCESS */
    private ResultCodeEnum resultCode = ResultCodeEnum.SUCCESS;


    // ------------------------------------------------------------------------
    // LdapResult Interface Method Implementations
    // ------------------------------------------------------------------------

    /**
     * Gets the descriptive error message associated with the error code. May be
     * null for SUCCESS, COMPARETRUE, COMPAREFALSE and REFERRAL operations.
     * 
     * @return the descriptive error message.
     */
    public String getErrorMessage()
    {
        return errorMessage;
    }


    /**
     * Sets the descriptive error message associated with the error code. May be
     * null for SUCCESS, COMPARETRUE, and COMPAREFALSE operations.
     * 
     * @param errorMessage
     *            the descriptive error message.
     */
    public void setErrorMessage( String errorMessage )
    {
        this.errorMessage = errorMessage;
    }


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
    public DN getMatchedDn()
    {
        return matchedDn;
    }


    /**
     * Sets the lowest entry in the directory that was matched.
     * 
     * @see #getMatchedDn()
     * @param matchedDn
     *            the Dn of the lowest matched entry.
     */
    public void setMatchedDn( DN matchedDn )
    {
        this.matchedDn = matchedDn;
    }


    /**
     * Gets the result code enumeration associated with the response.
     * Corresponds to the <b> resultCode </b> field within the LDAPResult ASN.1
     * structure.
     * 
     * @return the result code enum value.
     */
    public ResultCodeEnum getResultCode()
    {
        return resultCode;
    }


    /**
     * Sets the result code enumeration associated with the response.
     * Corresponds to the <b> resultCode </b> field within the LDAPResult ASN.1
     * structure.
     * 
     * @param resultCode
     *            the result code enum value.
     */
    public void setResultCode( ResultCodeEnum resultCode )
    {
        this.resultCode = resultCode;
    }


    /**
     * Gets the Referral associated with this LdapResult if the resultCode
     * property is set to the REFERRAL ResultCodeEnum.
     * 
     * @return the referral on REFERRAL errors, null on all others.
     */
    public InternalReferral getReferral()
    {
        return referral;
    }


    /**
     * Gets whether or not this result represents a Referral. For referrals the
     * error code is set to REFERRAL and the referral property is not null.
     * 
     * @return true if this result represents a referral.
     */
    public boolean isReferral()
    {
        return referral != null;
    }


    /**
     * Sets the Referral associated with this LdapResult if the resultCode
     * property is set to the REFERRAL ResultCodeEnum. Setting this property
     * will result in a true return from isReferral and the resultCode should be
     * set to REFERRAL.
     * 
     * @param referral
     *            optional referral on REFERRAL errors.
     */
    public void setReferral( InternalReferral referral )
    {
        this.referral = referral;
    }


    /**
     * @param obj The object to compare with
     * @return <code>true</code> if both objects are equals
     */
    public boolean equals( Object obj )
    {
        // quickly return true if this is the obj
        if ( obj == this )
        {
            return true;
        }

        // return false if object does not implement interface
        if ( !( obj instanceof InternalLdapResult ) )
        {
            return false;
        }

        // compare all the like elements of the two LdapResult objects
        InternalLdapResult result = ( InternalLdapResult ) obj;

        if ( referral == null && result.getReferral() != null )
        {
            return false;
        }

        if ( result.getReferral() == null && referral != null )
        {
            return false;
        }

        if ( referral != null && result.getReferral() != null )
        {
            if ( !referral.equals( result.getReferral() ) )
            {
                return false;
            }
        }

        if ( !resultCode.equals( result.getResultCode() ) )
        {
            return false;
        }

        // Handle Error Messages where "" is considered equivalent to null
        String errMsg0 = errorMessage;
        String errMsg1 = result.getErrorMessage();

        if ( errMsg0 == null )
        {
            errMsg0 = "";
        }

        if ( errMsg1 == null )
        {
            errMsg1 = "";
        }

        if ( !errMsg0.equals( errMsg1 ) )
        {
            return false;
        }

        if ( matchedDn != null )
        {
            if ( !matchedDn.equals( result.getMatchedDn() ) )
            {
                return false;
            }
        }
        else if ( result.getMatchedDn() != null ) // one is null other is not
        {
            return false;
        }

        return true;
    }


    /**
     * Get a String representation of a LdapResult
     * 
     * @return A LdapResult String
     */
    public String toString()
    {

        StringBuffer sb = new StringBuffer();

        sb.append( "        Ldap Result\n" );
        sb.append( "            Result code : (" ).append( resultCode ).append( ')' );

        switch ( resultCode )
        {

            case SUCCESS :
                sb.append( " success\n" );
                break;

            case OPERATIONS_ERROR :
                sb.append( " operationsError\n" );
                break;

            case PROTOCOL_ERROR :
                sb.append( " protocolError\n" );
                break;

            case TIME_LIMIT_EXCEEDED :
                sb.append( " timeLimitExceeded\n" );
                break;

            case SIZE_LIMIT_EXCEEDED :
                sb.append( " sizeLimitExceeded\n" );
                break;

            case COMPARE_FALSE :
                sb.append( " compareFalse\n" );
                break;

            case COMPARE_TRUE :
                sb.append( " compareTrue\n" );
                break;

            case AUTH_METHOD_NOT_SUPPORTED :
                sb.append( " authMethodNotSupported\n" );
                break;

            case STRONG_AUTH_REQUIRED :
                sb.append( " strongAuthRequired\n" );
                break;

            case REFERRAL :
                sb.append( " referral -- new\n" );
                break;

            case ADMIN_LIMIT_EXCEEDED :
                sb.append( " adminLimitExceeded -- new\n" );
                break;

            case UNAVAILABLE_CRITICAL_EXTENSION :
                sb.append( " unavailableCriticalExtension -- new\n" );
                break;

            case CONFIDENTIALITY_REQUIRED :
                sb.append( " confidentialityRequired -- new\n" );
                break;

            case SASL_BIND_IN_PROGRESS :
                sb.append( " saslBindInProgress -- new\n" );
                break;

            case NO_SUCH_ATTRIBUTE :
                sb.append( " noSuchAttribute\n" );
                break;

            case UNDEFINED_ATTRIBUTE_TYPE :
                sb.append( " undefinedAttributeType\n" );
                break;

            case INAPPROPRIATE_MATCHING :
                sb.append( " inappropriateMatching\n" );
                break;

            case CONSTRAINT_VIOLATION :
                sb.append( " constraintViolation\n" );
                break;

            case ATTRIBUTE_OR_VALUE_EXISTS :
                sb.append( " attributeOrValueExists\n" );
                break;

            case INVALID_ATTRIBUTE_SYNTAX :
                sb.append( " invalidAttributeSyntax\n" );
                break;

            case NO_SUCH_OBJECT :
                sb.append( " noSuchObject\n" );
                break;

            case ALIAS_PROBLEM :
                sb.append( " aliasProblem\n" );
                break;

            case INVALID_DN_SYNTAX :
                sb.append( " invalidDNSyntax\n" );
                break;

            case ALIAS_DEREFERENCING_PROBLEM :
                sb.append( " aliasDereferencingProblem\n" );
                break;

            case INAPPROPRIATE_AUTHENTICATION :
                sb.append( " inappropriateAuthentication\n" );
                break;

            case INVALID_CREDENTIALS :
                sb.append( " invalidCredentials\n" );
                break;

            case INSUFFICIENT_ACCESS_RIGHTS :
                sb.append( " insufficientAccessRights\n" );
                break;

            case BUSY :
                sb.append( " busy\n" );
                break;

            case UNAVAILABLE :
                sb.append( " unavailable\n" );
                break;

            case UNWILLING_TO_PERFORM :
                sb.append( " unwillingToPerform\n" );
                break;

            case LOOP_DETECT :
                sb.append( " loopDetect\n" );
                break;

            case NAMING_VIOLATION :
                sb.append( " namingViolation\n" );
                break;

            case OBJECT_CLASS_VIOLATION :
                sb.append( " objectClassViolation\n" );
                break;

            case NOT_ALLOWED_ON_NON_LEAF :
                sb.append( " notAllowedOnNonLeaf\n" );
                break;

            case NOT_ALLOWED_ON_RDN :
                sb.append( " notAllowedOnRDN\n" );
                break;

            case ENTRY_ALREADY_EXISTS :
                sb.append( " entryAlreadyExists\n" );
                break;

            case OBJECT_CLASS_MODS_PROHIBITED :
                sb.append( " objectClassModsProhibited\n" );
                break;

            case AFFECTS_MULTIPLE_DSAS :
                sb.append( " affectsMultipleDSAs -- new\n" );
                break;

            case OTHER :
                sb.append( " other\n" );
                break;
                
            default :
                switch ( resultCode.getResultCode() )
                {
                    case 9 :
                        sb.append( " -- 9 reserved --\n" );
                        break;
                        
                    case 22 :
                    case 23:
                    case 24 :
                    case 25 :
                    case 26 :
                    case 27 :
                    case 28 :
                    case 29 :
                    case 30 :
                    case 31 :
                        sb.append( " -- 22-31 unused --\n" );
                        break;

                    case 35 :
                        sb.append( " -- 35 reserved for undefined isLeaf --\n" );
                        break;
                                
                    case 37 :
                    case 38 :
                    case 39 :
                    case 40 :
                    case 41 :
                    case 42 :
                    case 43 :
                    case 44 :
                    case 45 :
                    case 46 :
                    case 47 :
                        sb.append( " -- 37-47 unused --\n" );
                        break;

                    case 55 :
                    case 56 :
                    case 57 :
                    case 58 :
                    case 59 :
                    case 60 :
                    case 61 :
                    case 62 :
                    case 63 :
                        sb.append( " -- 55-63 unused --\n" );
                        break;

                    case 70 :
                        sb.append( " -- 70 reserved for CLDAP --\n" );
                        break;

                    case 72 :
                    case 73 :
                    case 74 :
                    case 75 :
                    case 76 :
                    case 77 :
                    case 78 :
                    case 79 :
                        sb.append( " -- 72-79 unused --\n" );
                        break;

                    case 81 :
                    case 82 :
                    case 83 :
                    case 84 :
                    case 85 :
                    case 86 :
                    case 87 :
                    case 88 :
                    case 89 :
                    case 90 :
                        sb.append( " -- 81-90 reserved for APIs --" );
                        break;
                        
                    default :
                        sb.append( "Unknown error code : " ).append( resultCode );
                        break;
                }
        }

        sb.append( "            Matched DN : '" ).append( matchedDn ).append( "'\n" );
        sb.append( "            Error message : '" ).append( errorMessage ).append( "'\n" );

        if ( referral != null )
        {
            sb.append( "            Referrals :\n" );

            sb.append( "                Referral :" ).append( referral.toString() ).append( '\n' );
        }

        return sb.toString();
    }
}
