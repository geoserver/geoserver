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
package org.apache.directory.shared.ldap.codec.extended.operations.gracefulDisconnect;


import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.asn1.ber.grammar.IStates;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.util.IntegerDecoder;
import org.apache.directory.shared.asn1.util.IntegerDecoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.codec.extended.operations.GracefulActionConstants;
import org.apache.directory.shared.ldap.codec.util.LdapURLEncodingException;
import org.apache.directory.shared.ldap.util.LdapURL;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the Graceful Disconnect. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once.
 * The grammar is :
 * 
 * <pre>
 *  GracefulDisconnect ::= SEQUENCE {
 *      timeOffline INTEGER (0..720) DEFAULT 0,
 *      delay [0] INTEGER (0..86400) DEFAULT 0,
 *      replicatedContexts Referral OPTIONAL
 * }
 *  
 *  Referral ::= SEQUENCE OF LDAPURL
 *  
 *  LDAPURL ::= LDAPString -- limited to characters permitted in URLs
 *  
 *  LDAPString ::= OCTET STRING
 * </pre>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 912399 $, $Date: 2010-02-21 22:52:31 +0200 (Sun, 21 Feb 2010) $, 
 */
public class GracefulDisconnectGrammar extends AbstractGrammar
{
    /** The logger */
    static final Logger log = LoggerFactory.getLogger( GracefulDisconnectGrammar.class );

    /** Speedup for logs */
    static final boolean IS_DEBUG = log.isDebugEnabled();

    /** The instance of grammar. GracefulDisconnectnGrammar is a singleton */
    private static IGrammar instance = new GracefulDisconnectGrammar();


    /**
     * The action used to store a Time Offline.
     */
    GrammarAction storeDelay = new GrammarAction( "Set Graceful Disconnect Delay" )
    {
        public void action( IAsn1Container container ) throws DecoderException
        {
            GracefulDisconnectContainer gracefulDisconnectContainer = ( GracefulDisconnectContainer ) container;
            Value value = gracefulDisconnectContainer.getCurrentTLV().getValue();
    
            try
            {
                int delay = IntegerDecoder.parse( value, 0, 86400 );
    
                if ( IS_DEBUG )
                {
                    log.debug( "Delay = " + delay );
                }
    
                gracefulDisconnectContainer.getGracefulDisconnect().setDelay( delay );
                gracefulDisconnectContainer.grammarEndAllowed( true );
            }
            catch ( IntegerDecoderException e )
            {
                String msg = I18n.err( I18n.ERR_04036, StringTools.dumpBytes( value.getData() ) );
                log.error( msg );
                throw new DecoderException( msg );
            }
        }
    };
    
    /**
     * The action used to store a referral.
     */
    GrammarAction storeReferral = new GrammarAction( "Stores a referral" )
    {
        public void action( IAsn1Container container ) throws DecoderException
        {
            GracefulDisconnectContainer gracefulDisconnectContainer = ( GracefulDisconnectContainer ) container;
            Value value = gracefulDisconnectContainer.getCurrentTLV().getValue();

            try
            {
                LdapURL url = new LdapURL( value.getData() );
                gracefulDisconnectContainer.getGracefulDisconnect().addReplicatedContexts( url );
                gracefulDisconnectContainer.grammarEndAllowed( true );
                
                if ( IS_DEBUG )
                {
                    log.debug( "Stores a referral : {}", url );
                }
            }
            catch ( LdapURLEncodingException e )
            {
                String msg = "failed to decode the URL '" + StringTools.dumpBytes( value.getData() ) + "'";
                log.error( msg );
                throw new DecoderException( msg );
            }
        }
    };
    
    /**
     * The action used to store a Time Offline.
     */
    GrammarAction storeTimeOffline = new GrammarAction( "Set Graceful Disconnect time offline" )
    {
        public void action( IAsn1Container container ) throws DecoderException
        {
            GracefulDisconnectContainer gracefulDisconnectContainer = ( GracefulDisconnectContainer ) container;
            Value value = gracefulDisconnectContainer.getCurrentTLV().getValue();

            try
            {
                int timeOffline = IntegerDecoder.parse( value, 0, 720 );

                if ( IS_DEBUG )
                {
                    log.debug( "Time Offline = " + timeOffline );
                }

                gracefulDisconnectContainer.getGracefulDisconnect().setTimeOffline( timeOffline );
                gracefulDisconnectContainer.grammarEndAllowed( true );
            }
            catch ( IntegerDecoderException e )
            {
                String msg = I18n.err( I18n.ERR_04037, StringTools.dumpBytes( value.getData() ) );
                log.error( msg );
                throw new DecoderException( msg );
            }
        }
    };

    /**
     * Creates a new GracefulDisconnectGrammar object.
     */
    private GracefulDisconnectGrammar()
    {
        name = GracefulDisconnectGrammar.class.getName();
        statesEnum = GracefulDisconnectStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[GracefulDisconnectStatesEnum.LAST_GRACEFUL_DISCONNECT_STATE][256];

        /**
         * Transition from init state to graceful disconnect
         * GracefulDisconnect ::= SEQUENCE { 
         *     ... 
         * 
         * Creates the GracefulDisconnect object
         */
        super.transitions[IStates.INIT_GRAMMAR_STATE][UniversalTag.SEQUENCE_TAG] = 
            new GrammarTransition( IStates.INIT_GRAMMAR_STATE,
                                    GracefulDisconnectStatesEnum.GRACEFUL_DISCONNECT_SEQUENCE_STATE, 
                                    UniversalTag.SEQUENCE_TAG,
                new GrammarAction(
                "Init Graceful Disconnect" )
            {
                public void action( IAsn1Container container )
                {
                    GracefulDisconnectContainer gracefulDisconnectContainer = ( GracefulDisconnectContainer ) container;
                    GracefulDisconnect gracefulDisconnect = new GracefulDisconnect();
                    gracefulDisconnectContainer.setGracefulDisconnect( gracefulDisconnect );
                    gracefulDisconnectContainer.grammarEndAllowed( true );
                }
            } );

        /**
         * Transition from graceful disconnect to time offline
         * 
         * GracefulDisconnect ::= SEQUENCE { 
         *     timeOffline INTEGER (0..720) DEFAULT 0, 
         *     ... 
         *     
         * Set the time offline value into the GracefulDisconnect object.    
         */
        super.transitions[GracefulDisconnectStatesEnum.GRACEFUL_DISCONNECT_SEQUENCE_STATE][UniversalTag.INTEGER_TAG] = 
            new GrammarTransition( GracefulDisconnectStatesEnum.GRACEFUL_DISCONNECT_SEQUENCE_STATE,
                                    GracefulDisconnectStatesEnum.TIME_OFFLINE_STATE, 
                                    UniversalTag.INTEGER_TAG, 
                storeTimeOffline );
        
        /**
         * Transition from graceful disconnect to delay
         * 
         * GracefulDisconnect ::= SEQUENCE { 
         *     ... 
         *     delay [0] INTEGER (0..86400) DEFAULT 0,
         *     ... 
         *     
         * Set the delay value into the GracefulDisconnect object.    
         */
        super.transitions[GracefulDisconnectStatesEnum.GRACEFUL_DISCONNECT_SEQUENCE_STATE]
                         [GracefulActionConstants.GRACEFUL_ACTION_DELAY_TAG] = 
            new GrammarTransition( GracefulDisconnectStatesEnum.GRACEFUL_DISCONNECT_SEQUENCE_STATE,
                                    GracefulDisconnectStatesEnum.DELAY_STATE, 
                                    GracefulActionConstants.GRACEFUL_ACTION_DELAY_TAG, 
                storeDelay );
        
        /**
         * Transition from graceful disconnect to replicated Contexts
         * 
         * GracefulDisconnect ::= SEQUENCE { 
         *     ... 
         *     replicatedContexts Referral OPTIONAL } 
         *     
         * Referral ::= SEQUENCE OF LDAPURL
         *     
         * Get some replicated contexts. Nothing to do    
         */
        super.transitions[GracefulDisconnectStatesEnum.GRACEFUL_DISCONNECT_SEQUENCE_STATE][UniversalTag.SEQUENCE_TAG] = 
            new GrammarTransition( GracefulDisconnectStatesEnum.GRACEFUL_DISCONNECT_SEQUENCE_STATE,
                                    GracefulDisconnectStatesEnum.REPLICATED_CONTEXTS_STATE,
                                    UniversalTag.SEQUENCE_TAG, null );
        
        /**
         * Transition from time offline to delay
         * 
         * GracefulDisconnect ::= SEQUENCE { 
         *     ... 
         *     delay [0] INTEGER (0..86400) DEFAULT 0,
         *     ... 
         *     
         * Set the delay value into the GracefulDisconnect object.    
         */
        super.transitions[GracefulDisconnectStatesEnum.TIME_OFFLINE_STATE][GracefulActionConstants.GRACEFUL_ACTION_DELAY_TAG] = 
            new GrammarTransition( GracefulDisconnectStatesEnum.TIME_OFFLINE_STATE,
                                    GracefulDisconnectStatesEnum.DELAY_STATE, 
                                    GracefulActionConstants.GRACEFUL_ACTION_DELAY_TAG,
                storeDelay );

        /**
         * Transition from time offline to replicated Contexts
         * 
         * GracefulDisconnect ::= SEQUENCE { 
         *     ... 
         *     replicatedContexts Referral OPTIONAL } 
         *     
         * Referral ::= SEQUENCE OF LDAPURL
         *     
         * Get some replicated contexts. Nothing to do    
         */
        super.transitions[GracefulDisconnectStatesEnum.TIME_OFFLINE_STATE][UniversalTag.SEQUENCE_TAG] = 
            new GrammarTransition( GracefulDisconnectStatesEnum.TIME_OFFLINE_STATE,
                                    GracefulDisconnectStatesEnum.REPLICATED_CONTEXTS_STATE,
                                    UniversalTag.SEQUENCE_TAG, null );
        
        /**
         * Transition from delay to replicated contexts
         * 
         * GracefulDisconnect ::= SEQUENCE { 
         *     ... 
         *     replicatedContexts Referral OPTIONAL } 
         *     
         * Referral ::= SEQUENCE OF LDAPURL
         *     
         * Get some replicated contexts. Nothing to do    
         */
        super.transitions[GracefulDisconnectStatesEnum.DELAY_STATE][UniversalTag.SEQUENCE_TAG] = 
            new GrammarTransition( GracefulDisconnectStatesEnum.DELAY_STATE,
                                    GracefulDisconnectStatesEnum.REPLICATED_CONTEXTS_STATE, 
                                    UniversalTag.SEQUENCE_TAG, null );
        
        /**
         * Transition from replicated contexts to referral
         * 
         * GracefulDisconnect ::= SEQUENCE { 
         *     ... 
         *     replicatedContexts Referral OPTIONAL } 
         *     
         * Referral ::= SEQUENCE OF LDAPURL
         *     
         * Stores the referral
         */
        super.transitions[GracefulDisconnectStatesEnum.REPLICATED_CONTEXTS_STATE][UniversalTag.OCTET_STRING_TAG] = 
            new GrammarTransition( GracefulDisconnectStatesEnum.REPLICATED_CONTEXTS_STATE,
                                    GracefulDisconnectStatesEnum.REFERRAL_STATE, 
                                    UniversalTag.OCTET_STRING_TAG,
                storeReferral );

        /**
         * Transition from referral to referral
         * 
         * GracefulDisconnect ::= SEQUENCE { 
         *     ... 
         *     replicatedContexts Referral OPTIONAL } 
         *     
         * Referral ::= SEQUENCE OF LDAPURL
         *     
         * Stores the referral
         */
        super.transitions[GracefulDisconnectStatesEnum.REFERRAL_STATE][UniversalTag.OCTET_STRING_TAG] = 
            new GrammarTransition( GracefulDisconnectStatesEnum.REFERRAL_STATE,
                                    GracefulDisconnectStatesEnum.REFERRAL_STATE, 
                                    UniversalTag.OCTET_STRING_TAG,
                storeReferral );

    }


    /**
     * This class is a singleton.
     * 
     * @return An instance on this grammar
     */
    public static IGrammar getInstance()
    {
        return instance;
    }
}
