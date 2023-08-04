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
package org.apache.directory.shared.ldap.codec;


import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.util.LdapURL;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A ldapObject to store the LdapResult
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 921600 $, $Date: 2010-03-11 00:37:30 +0200 (Thu, 11 Mar 2010) $, 
 */
public class LdapResultCodec extends AbstractAsn1Object
{
    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /**
     * The result code. The different values are : 
     * 
     * success                                  (0), 
     * operationsError                          (1), 
     * protocolError                            (2), 
     * timeLimitExceeded                        (3), 
     * sizeLimitExceeded                        (4),
     * compareFalse                             (5), 
     * compareTrue                              (6), 
     * authMethodNotSupported                   (7),
     * strongAuthRequired                       (8), 
     *                                          -- 9 reserved -- 
     * referral                                 (10), 
     * adminLimitExceeded                       (11), 
     * unavailableCriticalExtension             (12), 
     * confidentialityRequired                  (13), 
     * saslBindInProgress                       (14),
     * noSuchAttribute                          (16), 
     * undefinedAttributeType                   (17), 
     * inappropriateMatching                    (18), 
     * constraintViolation                      (19), 
     * attributeOrValueExists                   (20),
     * invalidAttributeSyntax                   (21), 
     *                                          -- 22-31 unused -- 
     * noSuchObject                             (32),
     * aliasProblem                             (33), 
     * invalidDNSyntax                          (34), 
     *                                          -- 35 reserved for undefined isLeaf -- 
     * aliasDereferencingProblem                (36), 
     *                                          -- 37-47 unused --
     * inappropriateAuthentication              (48), 
     * invalidCredentials                       (49),
     * insufficientAccessRights                 (50), 
     * busy                                     (51), 
     * unavailable                              (52),
     * unwillingToPerform                       (53), 
     * loopDetect                               (54), 
     *                                          -- 55-63 unused --
     * namingViolation                          (64), 
     * objectClassViolation                     (65), 
     * notAllowedOnNonLeaf                      (66), 
     * notAllowedOnRDN                          (67), 
     * entryAlreadyExists                       (68),
     * objectClassModsProhibited                (69), 
     *                                          -- 70 reserved for CLDAP --
     * affectsMultipleDSAs                      (71), -- new 
     *                                          -- 72-79 unused -- 
     * other                                    (80),
     * ...
     * }
     */
    private ResultCodeEnum resultCode;

    /** The DN that is matched by the Bind */
    private DN matchedDN;

    /** Temporary storage of the byte[] representing the matchedDN */
    private byte[] matchedDNBytes;

    /** The error message */
    private String errorMessage;
    
    /** Temporary storage for message bytes */
    private byte[] errorMessageBytes;

    /** The referrals, if any. This is an optional element */
    private List<LdapURL> referrals;

    /** The inner size of the referrals sequence */
    private int referralsLength;


    // ~ Constructors
    // -------------------------------------------------------------------------------

    /**
     * Creates a new LdapResult object.
     */
    public LdapResultCodec()
    {
        super();
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Initialize the referrals list
     */
    public void initReferrals()
    {
        referrals = new ArrayList<LdapURL>();
    }
    
    /**
     * Get the error message
     * 
     * @return Returns the errorMessage.
     */
    public String getErrorMessage()
    {
        return errorMessage;
    }


    /**
     * Set the error message
     * 
     * @param errorMessage The errorMessage to set.
     */
    public void setErrorMessage( String errorMessage )
    {
        this.errorMessage = errorMessage;
    }


    /**
     * Get the matched DN
     * 
     * @return Returns the matchedDN.
     */
    public String getMatchedDN()
    {
        return ( ( matchedDN == null ) ? "" : matchedDN.getNormName() );
    }


    /**
     * Set the Matched DN
     * 
     * @param matchedDN The matchedDN to set.
     */
    public void setMatchedDN( DN matchedDN )
    {
        this.matchedDN = matchedDN;
    }


    /**
     * Get the referrals
     * 
     * @return Returns the referrals.
     */
    public List<LdapURL> getReferrals()
    {
        return referrals;
    }


    /**
     * Add a referral
     * 
     * @param referral The referral to add.
     */
    public void addReferral( LdapURL referral )
    {
        referrals.add( referral );
    }


    /**
     * Get the result code
     * 
     * @return Returns the resultCode.
     */
    public ResultCodeEnum getResultCode()
    {
        return resultCode;
    }


    /**
     * Set the result code
     * 
     * @param resultCode The resultCode to set.
     */
    public void setResultCode( ResultCodeEnum resultCode )
    {
        this.resultCode = resultCode;
    }


    /**
     * Compute the LdapResult length 
     * 
     * LdapResult : 
     * 0x0A 01 resultCode (0..80)
     *   0x04 L1 matchedDN (L1 = Length(matchedDN)) 
     *   0x04 L2 errorMessage (L2 = Length(errorMessage)) 
     *   [0x83 L3] referrals 
     *     | 
     *     +--> 0x04 L4 referral 
     *     +--> 0x04 L5 referral 
     *     +--> ... 
     *     +--> 0x04 Li referral 
     *     +--> ... 
     *     +--> 0x04 Ln referral 
     *     
     * L1 = Length(matchedDN) 
     * L2 = Length(errorMessage) 
     * L3 = n*Length(0x04) + sum(Length(L4) .. Length(Ln)) + sum(L4..Ln) 
     * L4..n = Length(0x04) + Length(Li) + Li 
     * Length(LdapResult) = Length(0x0x0A) +
     *      Length(0x01) + 1 + Length(0x04) + Length(L1) + L1 + Length(0x04) +
     *      Length(L2) + L2 + Length(0x83) + Length(L3) + L3
     */
    public int computeLength()
    {
        int ldapResultLength = 0;

        // The result code : always 3 bytes
        ldapResultLength = 1 + 1 + 1;

        // The matchedDN length
        if ( matchedDN == null )
        {
            ldapResultLength += 1 + 1;
        }
        else
        {
            matchedDNBytes = StringTools.getBytesUtf8( StringTools.trimLeft( matchedDN.getName() ) );
            ldapResultLength += 1 + TLV.getNbBytes( matchedDNBytes.length ) + matchedDNBytes.length;
        }

        // The errorMessage length
        errorMessageBytes = StringTools.getBytesUtf8( errorMessage ); 
        ldapResultLength += 1 + TLV.getNbBytes( errorMessageBytes.length ) + errorMessageBytes.length;

        if ( ( referrals != null ) && ( referrals.size() != 0 ) )
        {
            referralsLength = 0;

            // Each referral
            for ( LdapURL referral:referrals )
            {
                referralsLength += 1 + TLV.getNbBytes( referral.getNbBytes() ) + referral.getNbBytes();
            }

            // The referrals
            ldapResultLength += 1 + TLV.getNbBytes( referralsLength ) + referralsLength;
        }

        return ldapResultLength;
    }


    /**
     * Encode the LdapResult message to a PDU.
     * 
     * @param buffer The buffer where to put the PDU
     * @return The PDU.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        if ( buffer == null )
        {
            throw new EncoderException( I18n.err( I18n.ERR_04023 ) );
        }

        try
        {
            // The result code
            buffer.put( UniversalTag.ENUMERATED_TAG );
            buffer.put( ( byte ) 1 );
            buffer.put( ( byte ) resultCode.getValue() );
        }
        catch ( BufferOverflowException boe )
        {
            throw new EncoderException( I18n.err( I18n.ERR_04005 ) );
        }

        // The matchedDN
        Value.encode( buffer, matchedDNBytes );

        // The error message
        Value.encode( buffer, errorMessageBytes );

        // The referrals, if any
        if ( ( referrals != null ) && ( referrals.size() != 0 ) )
        {
            // Encode the referrals sequence
            // The referrals length MUST have been computed before !
            buffer.put( ( byte ) LdapConstants.LDAP_RESULT_REFERRAL_SEQUENCE_TAG );
            buffer.put( TLV.getBytes( referralsLength ) );

            // Each referral
            for ( LdapURL referral:referrals )
            {
                // Encode the current referral
                Value.encode( buffer, referral.getBytesReference() );
            }
        }

        return buffer;
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

            case SUCCESS:
                sb.append( " success\n" );
                break;

            case OPERATIONS_ERROR:
                sb.append( " operationsError\n" );
                break;

            case PROTOCOL_ERROR:
                sb.append( " protocolError\n" );
                break;

            case TIME_LIMIT_EXCEEDED:
                sb.append( " timeLimitExceeded\n" );
                break;

            case SIZE_LIMIT_EXCEEDED:
                sb.append( " sizeLimitExceeded\n" );
                break;

            case COMPARE_FALSE:
                sb.append( " compareFalse\n" );
                break;

            case COMPARE_TRUE:
                sb.append( " compareTrue\n" );
                break;

            case AUTH_METHOD_NOT_SUPPORTED:
                sb.append( " authMethodNotSupported\n" );
                break;

            case STRONG_AUTH_REQUIRED:
                sb.append( " strongAuthRequired\n" );
                break;

            case REFERRAL:
                sb.append( " referral -- new\n" );
                break;

            case ADMIN_LIMIT_EXCEEDED:
                sb.append( " adminLimitExceeded -- new\n" );
                break;

            case UNAVAILABLE_CRITICAL_EXTENSION:
                sb.append( " unavailableCriticalExtension -- new\n" );
                break;

            case CONFIDENTIALITY_REQUIRED:
                sb.append( " confidentialityRequired -- new\n" );
                break;

            case SASL_BIND_IN_PROGRESS:
                sb.append( " saslBindInProgress -- new\n" );
                break;

            case NO_SUCH_ATTRIBUTE:
                sb.append( " noSuchAttribute\n" );
                break;

            case UNDEFINED_ATTRIBUTE_TYPE:
                sb.append( " undefinedAttributeType\n" );
                break;

            case INAPPROPRIATE_MATCHING:
                sb.append( " inappropriateMatching\n" );
                break;

            case CONSTRAINT_VIOLATION:
                sb.append( " constraintViolation\n" );
                break;

            case ATTRIBUTE_OR_VALUE_EXISTS:
                sb.append( " attributeOrValueExists\n" );
                break;

            case INVALID_ATTRIBUTE_SYNTAX:
                sb.append( " invalidAttributeSyntax\n" );
                break;

            case NO_SUCH_OBJECT:
                sb.append( " noSuchObject\n" );
                break;

            case ALIAS_PROBLEM:
                sb.append( " aliasProblem\n" );
                break;

            case INVALID_DN_SYNTAX:
                sb.append( " invalidDNSyntax\n" );
                break;

            case ALIAS_DEREFERENCING_PROBLEM:
                sb.append( " aliasDereferencingProblem\n" );
                break;

            case INAPPROPRIATE_AUTHENTICATION:
                sb.append( " inappropriateAuthentication\n" );
                break;

            case INVALID_CREDENTIALS:
                sb.append( " invalidCredentials\n" );
                break;

            case INSUFFICIENT_ACCESS_RIGHTS:
                sb.append( " insufficientAccessRights\n" );
                break;

            case BUSY:
                sb.append( " busy\n" );
                break;

            case UNAVAILABLE:
                sb.append( " unavailable\n" );
                break;

            case UNWILLING_TO_PERFORM:
                sb.append( " unwillingToPerform\n" );
                break;

            case LOOP_DETECT:
                sb.append( " loopDetect\n" );
                break;

            case NAMING_VIOLATION:
                sb.append( " namingViolation\n" );
                break;

            case OBJECT_CLASS_VIOLATION:
                sb.append( " objectClassViolation\n" );
                break;

            case NOT_ALLOWED_ON_NON_LEAF:
                sb.append( " notAllowedOnNonLeaf\n" );
                break;

            case NOT_ALLOWED_ON_RDN:
                sb.append( " notAllowedOnRDN\n" );
                break;

            case ENTRY_ALREADY_EXISTS:
                sb.append( " entryAlreadyExists\n" );
                break;

            case OBJECT_CLASS_MODS_PROHIBITED:
                sb.append( " objectClassModsProhibited\n" );
                break;

            case AFFECTS_MULTIPLE_DSAS:
                sb.append( " affectsMultipleDSAs -- new\n" );
                break;

            case OTHER:
                sb.append( " other\n" );
                break;

            default:
                switch ( resultCode.getResultCode() )
                {
                    case 9:
                        sb.append( " -- 9 reserved --\n" );
                        break;
    
                    case 22:
                    case 23:
                    case 24:
                    case 25:
                    case 26:
                    case 27:
                    case 28:
                    case 29:
                    case 30:
                    case 31:
                        sb.append( " -- 22-31 unused --\n" );
                        break;
                        
                    case 35 :
                        sb.append( " -- 35 reserved for undefined isLeaf --\n" );
                        break;
                        
                    case 37:
                    case 38:
                    case 39:
                    case 40:
                    case 41:
                    case 42:
                    case 43:
                    case 44:
                    case 45:
                    case 46:
                    case 47:
                        sb.append( " -- 37-47 unused --\n" );
                        break;
    
                    case 55:
                    case 56:
                    case 57:
                    case 58:
                    case 59:
                    case 60:
                    case 61:
                    case 62:
                    case 63:
                        sb.append( " -- 55-63 unused --\n" );
                        break;
    
                    case 70:
                        sb.append( " -- 70 reserved for CLDAP --\n" );
                        break;
                        
                    case 72:
                    case 73:
                    case 74:
                    case 75:
                    case 76:
                    case 77:
                    case 78:
                    case 79:
                        sb.append( " -- 72-79 unused --\n" );
                        break;
    
                    case 81:
                    case 82:
                    case 83:
                    case 84:
                    case 85:
                    case 86:
                    case 87:
                    case 88:
                    case 89:
                    case 90:
                        sb.append( " -- 81-90 reserved for APIs --" );
                        break;
                        
                    default :
                        sb.append( "Unknown error code : " ).append( resultCode );
                        break;
                }
            
            break;
        }

        sb.append( "            Matched DN : '" ).append( matchedDN == null ? "": matchedDN.toString() ).append( "'\n" );
        sb.append( "            Error message : '" ).append( errorMessage == null ? "" : errorMessage ).append( "'\n" );

        
        if ( ( referrals != null ) && ( referrals.size() != 0 ) )
        {
            sb.append( "            Referrals :\n" );
            int i = 0;

            for ( LdapURL referral:referrals )
            {

                sb.append( "                Referral[" ).
                    append( i++ ).
                    append( "] :" ).
                    append( referral ).
                    append( '\n' );
            }
        }

        return sb.toString();
    }
}
