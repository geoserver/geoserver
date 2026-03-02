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
import org.apache.directory.shared.ldap.codec.LdapMessageContainer;
import org.apache.directory.shared.ldap.codec.modify.ModifyRequestCodec;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to store a Value to an modifyRequest
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$, 
 */
public class ModifyAttributeValueAction extends GrammarAction
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( ModifyAttributeValueAction.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    public ModifyAttributeValueAction()
    {
        super( "Stores AttributeValue" );
    }

    /**
     * The initialization action
     */
    public void action( IAsn1Container container )
    {
        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
        ModifyRequestCodec modifyRequest = ldapMessageContainer.getModifyRequest();

        TLV tlv = ldapMessageContainer.getCurrentTLV();

        // Store the value. It can't be null
        byte[] value = StringTools.EMPTY_BYTES;

        if ( tlv.getLength() == 0 )
        {
            modifyRequest.addAttributeValue( "" );
        }
        else
        {
            value = tlv.getValue().getData();

            if ( ldapMessageContainer.isBinary( modifyRequest.getCurrentAttributeType() ) )
            {
                modifyRequest.addAttributeValue( value );
            }
            else
            {
                modifyRequest.addAttributeValue( StringTools.utf8ToString( ( byte[] ) value ) );
            }
        }

        // We can have an END transition
        ldapMessageContainer.grammarEndAllowed( true );

        if ( IS_DEBUG )
        {
            log.debug( "Value modified : {}", value );
        }
    }
}
