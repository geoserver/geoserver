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
package org.apache.directory.shared.ldap.codec.controls.replication.syncRequestValue;


import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.asn1.ber.grammar.IStates;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.util.BooleanDecoder;
import org.apache.directory.shared.asn1.util.BooleanDecoderException;
import org.apache.directory.shared.asn1.util.IntegerDecoder;
import org.apache.directory.shared.asn1.util.IntegerDecoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.message.control.replication.SynchronizationModeEnum;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the SyncRequestValueControl. All the actions are declared in
 * this class. As it is a singleton, these declaration are only done once.
 * 
 * The decoded grammar is the following :
 * 
 * syncRequestValue ::= SEQUENCE {
 *     mode ENUMERATED {
 *     -- 0 unused
 *     refreshOnly       (1),
 *     -- 2 reserved
 *     refreshAndPersist (3)
 *     },
 *     cookie     syncCookie OPTIONAL,
 *     reloadHint BOOLEAN DEFAULT FALSE
 * }
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 741888 $, $Date: 2009-02-07 13:57:03 +0100 (Sat, 07 Feb 2009) $, 
 */
public class SyncRequestValueControlGrammar extends AbstractGrammar
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( SyncRequestValueControlGrammar.class );

    /** Speedup for logs */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. SyncRequestValueControlGrammar is a singleton */
    private static IGrammar instance = new SyncRequestValueControlGrammar();


    /**
     * Creates a new SyncRequestValueControlGrammar object.
     */
    private SyncRequestValueControlGrammar()
    {
        name = SyncRequestValueControlGrammar.class.getName();
        statesEnum = SyncRequestValueControlStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[SyncRequestValueControlStatesEnum.LAST_SYNC_REQUEST_VALUE_STATE][256];

        /** 
         * Transition from initial state to SyncRequestValue sequence
         * SyncRequestValue ::= SEQUENCE OF {
         *     ...
         *     
         * Initialize the syncRequestValue object
         */
        super.transitions[IStates.INIT_GRAMMAR_STATE][UniversalTag.SEQUENCE_TAG] = 
            new GrammarTransition( IStates.INIT_GRAMMAR_STATE, 
                                    SyncRequestValueControlStatesEnum.SYNC_REQUEST_VALUE_SEQUENCE_STATE, 
                                    UniversalTag.SEQUENCE_TAG, 
                null );


        /** 
         * Transition from SyncRequestValue sequence to Change types
         * SyncRequestValue ::= SEQUENCE OF {
         *     mode ENUMERATED {
         *         -- 0 unused
         *         refreshOnly       (1),
         *         -- 2 reserved
         *         refreshAndPersist (3)
         *     },
         *     ...
         *     
         * Stores the mode value
         */
        super.transitions[SyncRequestValueControlStatesEnum.SYNC_REQUEST_VALUE_SEQUENCE_STATE][UniversalTag.ENUMERATED_TAG] = 
            new GrammarTransition( SyncRequestValueControlStatesEnum.SYNC_REQUEST_VALUE_SEQUENCE_STATE, 
                SyncRequestValueControlStatesEnum.MODE_STATE, 
                UniversalTag.ENUMERATED_TAG,
                new GrammarAction( "Set SyncRequestValueControl mode" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    SyncRequestValueControlContainer SyncRequestValueContainer = ( SyncRequestValueControlContainer ) container;
                    Value value = SyncRequestValueContainer.getCurrentTLV().getValue();

                    try
                    {
                        // Check that the value is into the allowed interval
                        int mode = IntegerDecoder.parse( value, 
                            SynchronizationModeEnum.UNUSED.getValue(), 
                            SynchronizationModeEnum.REFRESH_AND_PERSIST.getValue() );
                        
                        SynchronizationModeEnum modeEnum = SynchronizationModeEnum.getSyncMode( mode );
                        
                        if ( IS_DEBUG )
                        {
                            LOG.debug( "Mode = " + modeEnum );
                        }

                        SyncRequestValueContainer.getSyncRequestValueControl().setMode( modeEnum );

                        // We can have an END transition
                        SyncRequestValueContainer.grammarEndAllowed( true );
                    }
                    catch ( IntegerDecoderException e )
                    {
                        String msg = I18n.err( I18n.ERR_04028 );
                        LOG.error( msg, e );
                        throw new DecoderException( msg );
                    }
                }
            } );


        /** 
         * Transition from mode to cookie
         * SyncRequestValue ::= SEQUENCE OF {
         *     ...
         *     cookie     syncCookie OPTIONAL,
         *     ...
         *     
         * Stores the cookie
         */
        super.transitions[SyncRequestValueControlStatesEnum.MODE_STATE][UniversalTag.OCTET_STRING_TAG] = 
            new GrammarTransition( SyncRequestValueControlStatesEnum.MODE_STATE,
                                    SyncRequestValueControlStatesEnum.COOKIE_STATE, UniversalTag.OCTET_STRING_TAG,
                new GrammarAction( "Set SyncRequestValueControl cookie" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    SyncRequestValueControlContainer SyncRequestValueContainer = ( SyncRequestValueControlContainer ) container;
                    Value value = SyncRequestValueContainer.getCurrentTLV().getValue();

                    byte[] cookie = value.getData();

                    if ( IS_DEBUG )
                    {
                        LOG.debug( "cookie = " + StringTools.dumpBytes( cookie ) );
                    }

                    SyncRequestValueContainer.getSyncRequestValueControl().setCookie( cookie );

                    // We can have an END transition
                    SyncRequestValueContainer.grammarEndAllowed( true );
                }
            } );


        /** 
         * Transition from mode to reloadHint
         * SyncRequestValue ::= SEQUENCE OF {
         *     ...
         *     reloadHint BOOLEAN DEFAULT FALSE
         * }
         *     
         * Stores the reloadHint flag
         */
        super.transitions[SyncRequestValueControlStatesEnum.MODE_STATE][UniversalTag.BOOLEAN_TAG] = 
            new GrammarTransition( SyncRequestValueControlStatesEnum.MODE_STATE,
                                    SyncRequestValueControlStatesEnum.RELOAD_HINT_STATE, UniversalTag.BOOLEAN_TAG,
                new GrammarAction( "Set SyncRequestValueControl reloadHint flag" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    SyncRequestValueControlContainer SyncRequestValueContainer = ( SyncRequestValueControlContainer ) container;
                    Value value = SyncRequestValueContainer.getCurrentTLV().getValue();

                    try
                    {
                        boolean reloadHint = BooleanDecoder.parse( value );

                        if ( IS_DEBUG )
                        {
                            LOG.debug( "reloadHint = " + reloadHint );
                        }

                        SyncRequestValueContainer.getSyncRequestValueControl().setReloadHint( reloadHint );

                        // We can have an END transition
                        SyncRequestValueContainer.grammarEndAllowed( true );
                    }
                    catch ( BooleanDecoderException e )
                    {
                        String msg = I18n.err( I18n.ERR_04029 );
                        LOG.error( msg, e );
                        throw new DecoderException( msg );
                    }
                }
            } );


        /** 
         * Transition from cookie to reloadHint
         * SyncRequestValue ::= SEQUENCE OF {
         *     ...
         *     reloadHint BOOLEAN DEFAULT FALSE
         * }
         *     
         * Stores the reloadHint flag
         */
        super.transitions[SyncRequestValueControlStatesEnum.COOKIE_STATE][UniversalTag.BOOLEAN_TAG] = 
            new GrammarTransition( SyncRequestValueControlStatesEnum.COOKIE_STATE,
                                    SyncRequestValueControlStatesEnum.RELOAD_HINT_STATE, UniversalTag.BOOLEAN_TAG,
                new GrammarAction( "Set SyncRequestValueControl reloadHint flag" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    SyncRequestValueControlContainer SyncRequestValueContainer = ( SyncRequestValueControlContainer ) container;
                    Value value = SyncRequestValueContainer.getCurrentTLV().getValue();

                    try
                    {
                        boolean reloadHint = BooleanDecoder.parse( value );

                        if ( IS_DEBUG )
                        {
                            LOG.debug( "reloadHint = " + reloadHint );
                        }

                        SyncRequestValueContainer.getSyncRequestValueControl().setReloadHint( reloadHint );

                        // We can have an END transition
                        SyncRequestValueContainer.grammarEndAllowed( true );
                    }
                    catch ( BooleanDecoderException e )
                    {
                        String msg = I18n.err( I18n.ERR_04029 );
                        LOG.error( msg, e );
                        throw new DecoderException( msg );
                    }
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
