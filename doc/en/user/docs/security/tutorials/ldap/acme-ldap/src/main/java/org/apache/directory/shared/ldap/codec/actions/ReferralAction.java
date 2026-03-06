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
package org.apache.directory.shared.ldap.codec.actions;


import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.codec.LdapMessageContainer;
import org.apache.directory.shared.ldap.codec.LdapResponseCodec;
import org.apache.directory.shared.ldap.codec.LdapResultCodec;
import org.apache.directory.shared.ldap.codec.util.LdapURLEncodingException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.util.LdapURL;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to add a referral to a LdapTresult
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$, 
 */
public class ReferralAction extends GrammarAction
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( ReferralAction.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    public ReferralAction()
    {
        super( "Add a referral" );
    }

    /**
     * The initialization action
     */
    public void action( IAsn1Container container ) throws DecoderException
    {
        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
        LdapResponseCodec response = ldapMessageContainer.getLdapResponse();
        LdapResultCodec ldapResult = response.getLdapResult();

        TLV tlv = ldapMessageContainer.getCurrentTLV();

        if ( tlv.getLength() == 0 )
        {
            ldapResult.addReferral( LdapURL.EMPTY_URL );
        }
        else
        {
            if ( ldapResult.getResultCode() == ResultCodeEnum.REFERRAL )
            {
                try
                {
                    ldapResult.addReferral( new LdapURL( tlv.getValue().getData() ) );
                }
                catch ( LdapURLEncodingException luee )
                {
                    String badUrl = StringTools.utf8ToString( tlv.getValue().getData() );
                    log.error( I18n.err( I18n.ERR_04015, badUrl, luee.getMessage() ) );
                    throw new DecoderException( I18n.err( I18n.ERR_04016, luee.getMessage() ) );
                }
            }
            else
            {
                log.warn( "The Referral error message is not allowed when havind an error code no equals to REFERRAL" );
                ldapResult.addReferral( LdapURL.EMPTY_URL );
            }
        }

        // We can have an END transition
        ldapMessageContainer.grammarEndAllowed( true );

        if ( IS_DEBUG )
        {
            StringBuffer sb = new StringBuffer();
            boolean isFirst = true;

            for ( LdapURL url:ldapResult.getReferrals() )
            {
                if ( isFirst )
                {
                    isFirst = false;
                }
                else
                {
                    sb.append( ", " );
                }

                sb.append( url );
            }

            log.debug( "The referral error message is set to " + sb.toString() );
        }
    }
}
