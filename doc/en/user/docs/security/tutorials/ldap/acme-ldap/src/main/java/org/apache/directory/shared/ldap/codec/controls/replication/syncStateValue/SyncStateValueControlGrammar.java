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
package org.apache.directory.shared.ldap.codec.controls.replication.syncStateValue;


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
import org.apache.directory.shared.ldap.message.control.replication.SyncStateTypeEnum;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the SyncStateValueControl. All the actions are declared in
 * this class. As it is a singleton, these declaration are only done once.
 * 
 * The decoded grammar is the following :
 * 
 *  syncStateValue ::= SEQUENCE {
 *       state ENUMERATED {
 *            present (0),
 *            add (1),
 *            modify (2),
 *            delete (3)
 *       },
 *       entryUUID syncUUID,
 *       cookie    syncCookie OPTIONAL
 *  }
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 741888 $, $Date: 2009-02-07 13:57:03 +0100 (Sat, 07 Feb 2009) $, 
 */
public class SyncStateValueControlGrammar extends AbstractGrammar
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( SyncStateValueControlGrammar.class );

    /** Speedup for logs */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. SyncStateValueControlGrammar is a singleton */
    private static IGrammar instance = new SyncStateValueControlGrammar();


    /**
     * Creates a new SyncStateValueControlGrammar object.
     */
    private SyncStateValueControlGrammar()
    {
        name = SyncStateValueControlGrammar.class.getName();
        statesEnum = SyncStateValueControlStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[SyncStateValueControlStatesEnum.LAST_SYNC_STATE_VALUE_STATE][256];

        /** 
         * Transition from initial state to SyncStateValue sequence
         * SyncRequestValue ::= SEQUENCE OF {
         *     ...
         *     
         * Initialize the syncStateValue object
         */
        super.transitions[IStates.INIT_GRAMMAR_STATE][UniversalTag.SEQUENCE_TAG] = new GrammarTransition(
            IStates.INIT_GRAMMAR_STATE, SyncStateValueControlStatesEnum.SYNC_STATE_VALUE_SEQUENCE_STATE,
            UniversalTag.SEQUENCE_TAG, null );

        /** 
         * Transition from SyncStateValue sequence to state type enum
         * SyncRequestValue ::= SEQUENCE OF {
         *       state ENUMERATED {
         *            present (0),
         *            add (1),
         *            modify (2),
         *            delete (3)
         *       },
         *     ...
         *     
         * Stores the sync state type value
         */
        super.transitions[SyncStateValueControlStatesEnum.SYNC_STATE_VALUE_SEQUENCE_STATE][UniversalTag.ENUMERATED_TAG] = new GrammarTransition(
            SyncStateValueControlStatesEnum.SYNC_STATE_VALUE_SEQUENCE_STATE,
            SyncStateValueControlStatesEnum.SYNC_TYPE_STATE, UniversalTag.ENUMERATED_TAG, new GrammarAction(
                "Set SyncStateValueControl state type" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    SyncStateValueControlContainer syncStateValueContainer = ( SyncStateValueControlContainer ) container;
                    Value value = syncStateValueContainer.getCurrentTLV().getValue();

                    try
                    {
                        // Check that the value is into the allowed interval
                        int syncStateType = IntegerDecoder.parse( value, SyncStateTypeEnum.PRESENT.getValue(),
                            SyncStateTypeEnum.DELETE.getValue() );

                        SyncStateTypeEnum syncStateTypeEnum = SyncStateTypeEnum.getSyncStateType( syncStateType );

                        if ( IS_DEBUG )
                        {
                            LOG.debug( "SyncStateType = {}", syncStateTypeEnum );
                        }

                        syncStateValueContainer.getSyncStateValueControl().setSyncStateType( syncStateTypeEnum );

                        // move on to the entryUUID transistion
                        syncStateValueContainer.grammarEndAllowed( false );
                    }
                    catch ( IntegerDecoderException e )
                    {
                        String msg = I18n.err( I18n.ERR_04030 );
                        LOG.error( msg, e );
                        throw new DecoderException( msg );
                    }
                }
            } );

        /** 
         * Transition from sync state tpe to entryUUID
         * SyncStateValue ::= SEQUENCE OF {
         *     ...
         *     entryUUID     syncUUID
         *     ...
         *     
         * Stores the entryUUID
         */
        super.transitions[SyncStateValueControlStatesEnum.SYNC_TYPE_STATE][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            SyncStateValueControlStatesEnum.SYNC_TYPE_STATE, SyncStateValueControlStatesEnum.SYNC_UUID_STATE,
            UniversalTag.OCTET_STRING_TAG, new GrammarAction( "Set SyncStateValueControl entryUUID" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    SyncStateValueControlContainer syncStateValueContainer = ( SyncStateValueControlContainer ) container;
                    Value value = syncStateValueContainer.getCurrentTLV().getValue();

                    byte[] entryUUID = value.getData();

                    if ( IS_DEBUG )
                    {
                        LOG.debug( "entryUUID = {}", StringTools.dumpBytes( entryUUID ) );
                    }

                    syncStateValueContainer.getSyncStateValueControl().setEntryUUID( entryUUID );

                    // We can have an END transition
                    syncStateValueContainer.grammarEndAllowed( true );
                }
            } );

        /** 
         * Transition from entryUUID to cookie
         * SyncRequestValue ::= SEQUENCE OF {
         *     ...
         *     cookie    syncCookie OPTIONAL
         * }
         *     
         * Stores the reloadHint flag
         */
        super.transitions[SyncStateValueControlStatesEnum.SYNC_UUID_STATE][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            SyncStateValueControlStatesEnum.SYNC_UUID_STATE, SyncStateValueControlStatesEnum.COOKIE_STATE,
            UniversalTag.OCTET_STRING_TAG, new GrammarAction( "Set SyncStateValueControl cookie value" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    SyncStateValueControlContainer syncStateValueContainer = ( SyncStateValueControlContainer ) container;
                    Value value = syncStateValueContainer.getCurrentTLV().getValue();

                    byte[] cookie = value.getData();

                    if ( IS_DEBUG )
                    {
                        LOG.debug( "cookie = {}", cookie );
                    }

                    syncStateValueContainer.getSyncStateValueControl().setCookie( cookie );

                    // terminal state
                    syncStateValueContainer.grammarEndAllowed( true );
                }
            } );
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
