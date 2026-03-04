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
import org.apache.directory.shared.ldap.codec.LdapMessageCodec;
import org.apache.directory.shared.ldap.codec.LdapMessageContainer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The action used to initialize a control.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$, 
 */
public class ControlsInitAction extends GrammarAction
{
    /** The logger */
    private static final Logger log = LoggerFactory.getLogger( ControlsInitAction.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    public ControlsInitAction()
    {
        super( "Initialize a control" );
    }

    /**
     * The initialization action
     */
    public void action( IAsn1Container container ) throws DecoderException
    {

        LdapMessageContainer ldapMessageContainer = ( LdapMessageContainer ) container;
        LdapMessageCodec message = ldapMessageContainer.getLdapMessage();

        TLV tlv = ldapMessageContainer.getCurrentTLV();
        int expectedLength = tlv.getLength();

        // The Length should be null
        if ( expectedLength == 0 )
        {
            log.error( "The length of controls must not be null" );
            
            // This will generate a PROTOCOL_ERROR
            throw new DecoderException( "The length of controls must not be null" );
        }
        
        if ( IS_DEBUG )
        {
            log.debug( "A new list of controls has been initialized" );
        }
        
        // We can initialize the controls array
        message.initControls();
    }
}
