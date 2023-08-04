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
package org.apache.directory.shared.ldap.codec.controls.replication.syncInfoValue;


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
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.message.control.replication.SynchronizationInfoEnum;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the SyncInfoValueControl. All the actions are declared in
 * this class. As it is a singleton, these declaration are only done once.
 * 
 * The decoded grammar is the following :
 * 
 * syncInfoValue ::= CHOICE {
 *     newcookie      [0] syncCookie,
 *     refreshDelete  [1] SEQUENCE {
 *         cookie         syncCookie OPTIONAL,
 *         refreshDone    BOOLEAN DEFAULT TRUE
 *     },
 *     refreshPresent [2] SEQUENCE {
 *         cookie         syncCookie OPTIONAL,
 *         refreshDone    BOOLEAN DEFAULT TRUE
 *     },
 *     syncIdSet      [3] SEQUENCE {
 *         cookie         syncCookie OPTIONAL,
 *         refreshDeletes BOOLEAN DEFAULT FALSE,
 *         syncUUIDs      SET OF syncUUID
 *     }
 * }
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 741888 $, $Date: 2009-02-07 13:57:03 +0100 (Sat, 07 Feb 2009) $, 
 */
public class SyncInfoValueControlGrammar extends AbstractGrammar
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( SyncInfoValueControlGrammar.class );

    /** Speedup for logs */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. SyncInfoValueControlGrammar is a singleton */
    private static IGrammar instance = new SyncInfoValueControlGrammar();


    /**
     * Creates a new SyncInfoValueControlGrammar object.
     */
    private SyncInfoValueControlGrammar()
    {
        name = SyncInfoValueControlGrammar.class.getName();
        statesEnum = SyncInfoValueControlStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[SyncInfoValueControlStatesEnum.LAST_SYNC_INFO_VALUE_STATE][256];

        /** 
         * Transition from initial state to SyncInfoValue newCookie choice
         * SyncInfoValue ::= CHOICE {
         *     newCookie [0] syncCookie,
         *     ...
         *     
         * Initialize the syncInfoValue object
         */
        super.transitions[IStates.INIT_GRAMMAR_STATE][SyncInfoValueTags.NEW_COOKIE_TAG.getValue()] = 
            new GrammarTransition( IStates.INIT_GRAMMAR_STATE, 
                                    SyncInfoValueControlStatesEnum.NEW_COOKIE_STATE, 
                                    SyncInfoValueTags.NEW_COOKIE_TAG.getValue(), 
                new GrammarAction( "NewCookie choice for SyncInfoValueControl" )
            {
                public void action( IAsn1Container container )
                {
                    SyncInfoValueControlContainer syncInfoValueContainer = 
                        ( SyncInfoValueControlContainer ) container;
                    SyncInfoValueControl control = 
                        new SyncInfoValueControl( SynchronizationInfoEnum.NEW_COOKIE);
                    
                    Value value = syncInfoValueContainer.getCurrentTLV().getValue();

                    byte[] newCookie = value.getData();

                    if ( IS_DEBUG )
                    {
                        LOG.debug( "newcookie = " + StringTools.dumpBytes( newCookie ) );
                    }

                    control.setCookie( newCookie );

                    // We can have an END transition
                    syncInfoValueContainer.grammarEndAllowed( true );
                    
                    syncInfoValueContainer.setSyncInfoValueControl( control );
                }
            } );


        /** 
         * Transition from initial state to SyncInfoValue refreshDelete choice
         * SyncInfoValue ::= CHOICE {
         *     ...
         *     refreshDelete [1] SEQUENCE {
         *     ...
         *     
         * Initialize the syncInfoValue object
         */
        super.transitions[IStates.INIT_GRAMMAR_STATE][SyncInfoValueTags.REFRESH_DELETE_TAG.getValue()] = 
            new GrammarTransition( IStates.INIT_GRAMMAR_STATE, 
                                    SyncInfoValueControlStatesEnum.REFRESH_DELETE_STATE, 
                                    SyncInfoValueTags.REFRESH_DELETE_TAG.getValue(), 
                new GrammarAction( "RefreshDelete choice for SyncInfoValueControl" )
            {
                public void action( IAsn1Container container )
                {
                    SyncInfoValueControlContainer syncInfoValueContainer = 
                        ( SyncInfoValueControlContainer ) container;
                    SyncInfoValueControl control = 
                        new SyncInfoValueControl( SynchronizationInfoEnum.REFRESH_DELETE);
                    
                    syncInfoValueContainer.setSyncInfoValueControl( control );

                    // We can have an END transition
                    syncInfoValueContainer.grammarEndAllowed( true );
                }
            } );


        /** 
         * Transition from refreshDelete state to cookie
         *     refreshDelete [1] SEQUENCE {
         *         cookie syncCookie OPTIONAL,
         *     ...
         *     
         * Load the cookie object
         */
        super.transitions[SyncInfoValueControlStatesEnum.REFRESH_DELETE_STATE][UniversalTag.OCTET_STRING_TAG] = 
            new GrammarTransition( SyncInfoValueControlStatesEnum.REFRESH_DELETE_STATE, 
                                    SyncInfoValueControlStatesEnum.REFRESH_DELETE_COOKIE_STATE, 
                                    UniversalTag.OCTET_STRING_TAG, 
                new GrammarAction( "RefreshDelete cookie" )
            {
                public void action( IAsn1Container container )
                {
                    SyncInfoValueControlContainer syncInfoValueContainer = 
                        ( SyncInfoValueControlContainer ) container;
                    SyncInfoValueControl control = syncInfoValueContainer.getSyncInfoValueControl();
                    
                    Value value = syncInfoValueContainer.getCurrentTLV().getValue();

                    byte[] cookie = value.getData();

                    if ( IS_DEBUG )
                    {
                        LOG.debug( "cookie = " + StringTools.dumpBytes( cookie ) );
                    }

                    syncInfoValueContainer.getSyncInfoValueControl().setCookie( cookie );
                    syncInfoValueContainer.setSyncInfoValueControl( control );

                    // We can have an END transition
                    syncInfoValueContainer.grammarEndAllowed( true );
                }
            } );


        /** 
         * Transition from refreshDelete cookie state to refreshDone
         *     refreshDelete [1] SEQUENCE {
         *         ....
         *         refreshDone BOOLEAN DEFAULT TRUE
         *     }
         *     
         * Load the refreshDone flag
         */
        super.transitions[SyncInfoValueControlStatesEnum.REFRESH_DELETE_COOKIE_STATE][UniversalTag.BOOLEAN_TAG] = 
            new GrammarTransition( SyncInfoValueControlStatesEnum.REFRESH_DELETE_COOKIE_STATE, 
                                    SyncInfoValueControlStatesEnum.LAST_SYNC_INFO_VALUE_STATE, 
                                    UniversalTag.BOOLEAN_TAG, 
                new GrammarAction( "RefreshDelete refreshDone flag" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    SyncInfoValueControlContainer syncInfoValueContainer = 
                        ( SyncInfoValueControlContainer ) container;
                    SyncInfoValueControl control = syncInfoValueContainer.getSyncInfoValueControl();
                    
                    Value value = syncInfoValueContainer.getCurrentTLV().getValue();

                    try
                    {
                        boolean refreshDone = BooleanDecoder.parse( value );

                        if ( IS_DEBUG )
                        {
                            LOG.debug( "refreshDone = {}", refreshDone );
                        }

                        control.setRefreshDone( refreshDone );

                        syncInfoValueContainer.setSyncInfoValueControl( control );

                        // the END transition for grammar
                        syncInfoValueContainer.grammarEndAllowed( true );
                    }
                    catch ( BooleanDecoderException be )
                    {
                        String msg = I18n.err( I18n.ERR_04025 );
                        LOG.error( msg, be );
                        throw new DecoderException( msg );
                    }


                    // We can have an END transition
                    syncInfoValueContainer.grammarEndAllowed( true );
                }
            } );


        /** 
         * Transition from refreshDelete choice state to refreshDone
         *     refreshDelete [1] SEQUENCE {
         *         ....
         *         refreshDone BOOLEAN DEFAULT TRUE
         *     }
         *     
         * Load the refreshDone flag
         */
        super.transitions[SyncInfoValueControlStatesEnum.REFRESH_DELETE_STATE][UniversalTag.BOOLEAN_TAG] = 
            new GrammarTransition( SyncInfoValueControlStatesEnum.REFRESH_DELETE_STATE, 
                                    SyncInfoValueControlStatesEnum.LAST_SYNC_INFO_VALUE_STATE, 
                                    UniversalTag.BOOLEAN_TAG, 
                new GrammarAction( "RefreshDelete refreshDone flag" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    SyncInfoValueControlContainer syncInfoValueContainer = 
                        ( SyncInfoValueControlContainer ) container;
                    SyncInfoValueControl control = syncInfoValueContainer.getSyncInfoValueControl();
                    
                    Value value = syncInfoValueContainer.getCurrentTLV().getValue();

                    try
                    {
                        boolean refreshDone = BooleanDecoder.parse( value );

                        if ( IS_DEBUG )
                        {
                            LOG.debug( "refreshDone = {}", refreshDone );
                        }

                        control.setRefreshDone( refreshDone );

                        syncInfoValueContainer.setSyncInfoValueControl( control );

                        // the END transition for grammar
                        syncInfoValueContainer.grammarEndAllowed( true );
                    }
                    catch ( BooleanDecoderException be )
                    {
                        String msg = I18n.err( I18n.ERR_04025 );
                        LOG.error( msg, be );
                        throw new DecoderException( msg );
                    }


                    // We can have an END transition
                    syncInfoValueContainer.grammarEndAllowed( true );
                }
            } );
        
        
        /** 
         * Transition from initial state to SyncInfoValue refreshPresent choice
         * SyncInfoValue ::= CHOICE {
         *     ...
         *     refreshPresent [2] SEQUENCE {
         *     ...
         *     
         * Initialize the syncInfoValue object
         */
        super.transitions[IStates.INIT_GRAMMAR_STATE][SyncInfoValueTags.REFRESH_PRESENT_TAG.getValue()] = 
            new GrammarTransition( IStates.INIT_GRAMMAR_STATE, 
                                    SyncInfoValueControlStatesEnum.REFRESH_PRESENT_STATE, 
                                    SyncInfoValueTags.REFRESH_PRESENT_TAG.getValue(), 
                new GrammarAction( "RefreshDelete choice for SyncInfoValueControl" )
            {
                public void action( IAsn1Container container )
                {
                    SyncInfoValueControlContainer syncInfoValueContainer = 
                        ( SyncInfoValueControlContainer ) container;
                    SyncInfoValueControl control = 
                        new SyncInfoValueControl( SynchronizationInfoEnum.REFRESH_PRESENT);
                    
                    syncInfoValueContainer.setSyncInfoValueControl( control );

                    // We can have an END transition
                    syncInfoValueContainer.grammarEndAllowed( true );
                }
            } );

    
        /** 
         * Transition from refreshPresent state to cookie
         *     refreshPresent [2] SEQUENCE {
         *         cookie syncCookie OPTIONAL,
         *     ...
         *     
         * Load the cookie object
         */
        super.transitions[SyncInfoValueControlStatesEnum.REFRESH_PRESENT_STATE][UniversalTag.OCTET_STRING_TAG] = 
            new GrammarTransition( SyncInfoValueControlStatesEnum.REFRESH_PRESENT_STATE, 
                                    SyncInfoValueControlStatesEnum.REFRESH_PRESENT_COOKIE_STATE, 
                                    UniversalTag.OCTET_STRING_TAG, 
                new GrammarAction( "RefreshPresent cookie" )
            {
                public void action( IAsn1Container container )
                {
                    SyncInfoValueControlContainer syncInfoValueContainer = 
                        ( SyncInfoValueControlContainer ) container;
                    SyncInfoValueControl control = syncInfoValueContainer.getSyncInfoValueControl();
                    
                    Value value = syncInfoValueContainer.getCurrentTLV().getValue();

                    byte[] cookie = value.getData();

                    if ( IS_DEBUG )
                    {
                        LOG.debug( "cookie = " + StringTools.dumpBytes( cookie ) );
                    }

                    syncInfoValueContainer.getSyncInfoValueControl().setCookie( cookie );
                    syncInfoValueContainer.setSyncInfoValueControl( control );

                    // We can have an END transition
                    syncInfoValueContainer.grammarEndAllowed( true );
                }
            } );
        
        


        /** 
         * Transition from refreshPresent cookie state to refreshDone
         *     refreshPresent [2] SEQUENCE {
         *         ....
         *         refreshDone BOOLEAN DEFAULT TRUE
         *     }
         *     
         * Load the refreshDone flag
         */
        super.transitions[SyncInfoValueControlStatesEnum.REFRESH_PRESENT_COOKIE_STATE][UniversalTag.BOOLEAN_TAG] = 
            new GrammarTransition( SyncInfoValueControlStatesEnum.REFRESH_PRESENT_COOKIE_STATE, 
                                    SyncInfoValueControlStatesEnum.LAST_SYNC_INFO_VALUE_STATE, 
                                    UniversalTag.BOOLEAN_TAG, 
                new GrammarAction( "RefreshPresent refreshDone flag" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    SyncInfoValueControlContainer syncInfoValueContainer = 
                        ( SyncInfoValueControlContainer ) container;
                    SyncInfoValueControl control = syncInfoValueContainer.getSyncInfoValueControl();
                    
                    Value value = syncInfoValueContainer.getCurrentTLV().getValue();

                    try
                    {
                        boolean refreshDone = BooleanDecoder.parse( value );

                        if ( IS_DEBUG )
                        {
                            LOG.debug( "refreshDone = {}", refreshDone );
                        }

                        control.setRefreshDone( refreshDone );

                        syncInfoValueContainer.setSyncInfoValueControl( control );

                        // the END transition for grammar
                        syncInfoValueContainer.grammarEndAllowed( true );
                    }
                    catch ( BooleanDecoderException be )
                    {
                        String msg = I18n.err( I18n.ERR_04025 );
                        LOG.error( msg, be );
                        throw new DecoderException( msg );
                    }


                    // We can have an END transition
                    syncInfoValueContainer.grammarEndAllowed( true );
                }
            } );


        /** 
         * Transition from refreshPresent choice state to refreshDone
         *     refreshPresent [1] SEQUENCE {
         *         ....
         *         refreshDone BOOLEAN DEFAULT TRUE
         *     }
         *     
         * Load the refreshDone flag
         */
        super.transitions[SyncInfoValueControlStatesEnum.REFRESH_PRESENT_STATE][UniversalTag.BOOLEAN_TAG] = 
            new GrammarTransition( SyncInfoValueControlStatesEnum.REFRESH_PRESENT_STATE, 
                                    SyncInfoValueControlStatesEnum.LAST_SYNC_INFO_VALUE_STATE, 
                                    UniversalTag.BOOLEAN_TAG, 
                new GrammarAction( "RefreshPresent refreshDone flag" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    SyncInfoValueControlContainer syncInfoValueContainer = 
                        ( SyncInfoValueControlContainer ) container;
                    SyncInfoValueControl control = syncInfoValueContainer.getSyncInfoValueControl();
                    
                    Value value = syncInfoValueContainer.getCurrentTLV().getValue();

                    try
                    {
                        boolean refreshDone = BooleanDecoder.parse( value );

                        if ( IS_DEBUG )
                        {
                            LOG.debug( "refreshDone = {}", refreshDone );
                        }

                        control.setRefreshDone( refreshDone );

                        syncInfoValueContainer.setSyncInfoValueControl( control );

                        // the END transition for grammar
                        syncInfoValueContainer.grammarEndAllowed( true );
                    }
                    catch ( BooleanDecoderException be )
                    {
                        String msg = I18n.err( I18n.ERR_04025 );
                        LOG.error( msg, be );
                        throw new DecoderException( msg );
                    }

                    // We can have an END transition
                    syncInfoValueContainer.grammarEndAllowed( true );
                }
            } );
        
        
        /** 
         * Transition from initial state to SyncInfoValue syncIdSet choice
         * SyncInfoValue ::= CHOICE {
         *     ...
         *     syncIdSet [3] SEQUENCE {
         *     ...
         *     
         * Initialize the syncInfoValue object
         */
        super.transitions[IStates.INIT_GRAMMAR_STATE][SyncInfoValueTags.SYNC_ID_SET_TAG.getValue()] = 
            new GrammarTransition( IStates.INIT_GRAMMAR_STATE, 
                                    SyncInfoValueControlStatesEnum.SYNC_ID_SET_STATE, 
                                    SyncInfoValueTags.SYNC_ID_SET_TAG.getValue(), 
                new GrammarAction( "SyncIdSet choice for SyncInfoValueControl" )
            {
                public void action( IAsn1Container container )
                {
                    SyncInfoValueControlContainer syncInfoValueContainer = 
                        ( SyncInfoValueControlContainer ) container;
                    SyncInfoValueControl control = 
                        new SyncInfoValueControl( SynchronizationInfoEnum.SYNC_ID_SET);
                    
                    syncInfoValueContainer.setSyncInfoValueControl( control );
                }
            } );
        
        
        /** 
         * Transition from syncIdSet state to cookie
         *     syncIdSet [3] SEQUENCE {
         *         cookie syncCookie OPTIONAL,
         *     ...
         *     
         * Load the cookie object
         */
        super.transitions[SyncInfoValueControlStatesEnum.SYNC_ID_SET_STATE][UniversalTag.OCTET_STRING_TAG] = 
            new GrammarTransition( SyncInfoValueControlStatesEnum.SYNC_ID_SET_STATE, 
                                    SyncInfoValueControlStatesEnum.SYNC_ID_SET_COOKIE_STATE, 
                                    UniversalTag.OCTET_STRING_TAG, 
                new GrammarAction( "SyncIdSet cookie" )
            {
                public void action( IAsn1Container container )
                {
                    SyncInfoValueControlContainer syncInfoValueContainer = 
                        ( SyncInfoValueControlContainer ) container;
                    SyncInfoValueControl control = syncInfoValueContainer.getSyncInfoValueControl();
                    
                    Value value = syncInfoValueContainer.getCurrentTLV().getValue();

                    byte[] cookie = value.getData();

                    if ( IS_DEBUG )
                    {
                        LOG.debug( "cookie = " + StringTools.dumpBytes( cookie ) );
                    }

                    syncInfoValueContainer.getSyncInfoValueControl().setCookie( cookie );
                    syncInfoValueContainer.setSyncInfoValueControl( control );
                }
            } );
        
        
        /** 
         * Transition from syncIdSet state to refreshDeletes
         *     syncIdSet [3] SEQUENCE {
         *         ...
         *         refreshDeletes BOOLEAN DEFAULT FALSE,
         *     ...
         *     
         * Load the refreshDeletes flag
         */
        super.transitions[SyncInfoValueControlStatesEnum.SYNC_ID_SET_STATE][UniversalTag.BOOLEAN_TAG] = 
            new GrammarTransition( SyncInfoValueControlStatesEnum.SYNC_ID_SET_STATE, 
                                    SyncInfoValueControlStatesEnum.SYNC_ID_SET_REFRESH_DELETES_STATE, 
                                    UniversalTag.BOOLEAN_TAG, 
                new GrammarAction( "SyncIdSet refreshDeletes" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    SyncInfoValueControlContainer syncInfoValueContainer = 
                        ( SyncInfoValueControlContainer ) container;
                    SyncInfoValueControl control = syncInfoValueContainer.getSyncInfoValueControl();
                    
                    Value value = syncInfoValueContainer.getCurrentTLV().getValue();

                    try
                    {
                        boolean refreshDeletes = BooleanDecoder.parse( value );

                        if ( IS_DEBUG )
                        {
                            LOG.debug( "refreshDeletes = {}", refreshDeletes );
                        }

                        control.setRefreshDeletes( refreshDeletes );

                        syncInfoValueContainer.setSyncInfoValueControl( control );
                    }
                    catch ( BooleanDecoderException be )
                    {
                        String msg = I18n.err( I18n.ERR_04026 );
                        LOG.error( msg, be );
                        throw new DecoderException( msg );
                    }
                }
            } );
        
        
        /** 
         * Transition from syncIdSet cookie state to refreshDeletes
         *     syncIdSet [3] SEQUENCE {
         *         ...
         *         refreshDeletes BOOLEAN DEFAULT FALSE,
         *     ...
         *     
         * Load the refreshDeletes flag
         */
        super.transitions[SyncInfoValueControlStatesEnum.SYNC_ID_SET_COOKIE_STATE][UniversalTag.BOOLEAN_TAG] = 
            new GrammarTransition( SyncInfoValueControlStatesEnum.SYNC_ID_SET_COOKIE_STATE, 
                                    SyncInfoValueControlStatesEnum.SYNC_ID_SET_REFRESH_DELETES_STATE, 
                                    UniversalTag.BOOLEAN_TAG, 
                new GrammarAction( "SyncIdSet refreshDeletes" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    SyncInfoValueControlContainer syncInfoValueContainer = 
                        ( SyncInfoValueControlContainer ) container;
                    SyncInfoValueControl control = syncInfoValueContainer.getSyncInfoValueControl();
                    
                    Value value = syncInfoValueContainer.getCurrentTLV().getValue();

                    try
                    {
                        boolean refreshDeletes = BooleanDecoder.parse( value );

                        if ( IS_DEBUG )
                        {
                            LOG.debug( "refreshDeletes = {}", refreshDeletes );
                        }

                        control.setRefreshDeletes( refreshDeletes );

                        syncInfoValueContainer.setSyncInfoValueControl( control );
                    }
                    catch ( BooleanDecoderException be )
                    {
                        String msg = I18n.err( I18n.ERR_04024 );
                        LOG.error( msg, be );
                        throw new DecoderException( msg );
                    }
                }
            } );
        
        
        /** 
         * Transition from syncIdSet state to syncUUIDs
         *     syncIdSet [3] SEQUENCE {
         *         ...
         *         syncUUIDs      *SET OF* syncUUID
         *     }
         *     
         * Initialize the UUID set : no action associated, except allowing a grammar end
         */
        super.transitions[SyncInfoValueControlStatesEnum.SYNC_ID_SET_STATE][UniversalTag.SET_TAG] = 
            new GrammarTransition( SyncInfoValueControlStatesEnum.SYNC_ID_SET_STATE, 
                                    SyncInfoValueControlStatesEnum.SYNC_ID_SET_SET_OF_UUIDS_STATE, 
                                    UniversalTag.SET_TAG, 
                new GrammarAction( "SyncIdSet syncUUIDs" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    SyncInfoValueControlContainer syncInfoValueContainer = 
                        ( SyncInfoValueControlContainer ) container;

                    // We can have an END transition
                    syncInfoValueContainer.grammarEndAllowed( true );
                }
            } );
        
        
        /** 
         * Transition from syncIdSet cookie state to syncUUIDs
         *     syncIdSet [3] SEQUENCE {
         *         ...
         *         syncUUIDs      *SET OF* syncUUID
         *     }
         *     
         * Initialize the UUID set : no action associated
         */
        super.transitions[SyncInfoValueControlStatesEnum.SYNC_ID_SET_COOKIE_STATE][UniversalTag.SET_TAG] = 
            new GrammarTransition( SyncInfoValueControlStatesEnum.SYNC_ID_SET_COOKIE_STATE, 
                                    SyncInfoValueControlStatesEnum.SYNC_ID_SET_SET_OF_UUIDS_STATE, 
                                    UniversalTag.SET_TAG,
                new GrammarAction( "SyncIdSet syncUUIDs" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    SyncInfoValueControlContainer syncInfoValueContainer = 
                        ( SyncInfoValueControlContainer ) container;

                    // We can have an END transition
                    syncInfoValueContainer.grammarEndAllowed( true );
                }
            } );
          
        
        /** 
         * Transition from syncIdSet refreshDeletes state to syncUUIDs
         *     syncIdSet [3] SEQUENCE {
         *         ...
         *         syncUUIDs      *SET OF* syncUUID
         *     }
         *     
         * Initialize the UUID set : no action associated
         */
        super.transitions[SyncInfoValueControlStatesEnum.SYNC_ID_SET_REFRESH_DELETES_STATE][UniversalTag.SET_TAG] = 
            new GrammarTransition( SyncInfoValueControlStatesEnum.SYNC_ID_SET_REFRESH_DELETES_STATE, 
                                    SyncInfoValueControlStatesEnum.SYNC_ID_SET_SET_OF_UUIDS_STATE, 
                                    UniversalTag.SET_TAG, 
                new GrammarAction( "SyncIdSet syncUUIDs" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    SyncInfoValueControlContainer syncInfoValueContainer = 
                        ( SyncInfoValueControlContainer ) container;

                    // We can have an END transition
                    syncInfoValueContainer.grammarEndAllowed( true );
                }
            } );
        
        
        /** 
         * Transition from syncIdSet syncUUIDs to syncUUID
         *     syncIdSet [3] SEQUENCE {
         *         ...
         *         syncUUIDs      SET OF *syncUUID*
         *     }
         *     
         * Add the first UUID in the UUIDs list
         */
        super.transitions[SyncInfoValueControlStatesEnum.SYNC_ID_SET_SET_OF_UUIDS_STATE][UniversalTag.OCTET_STRING] = 
            new GrammarTransition( SyncInfoValueControlStatesEnum.SYNC_ID_SET_SET_OF_UUIDS_STATE, 
                                    SyncInfoValueControlStatesEnum.SYNC_ID_SET_UUID_STATE, 
                                    UniversalTag.OCTET_STRING, 
                new GrammarAction( "SyncIdSet first UUID" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    SyncInfoValueControlContainer syncInfoValueContainer = 
                        ( SyncInfoValueControlContainer ) container;
                    SyncInfoValueControl control = syncInfoValueContainer.getSyncInfoValueControl();
                    
                    Value value = syncInfoValueContainer.getCurrentTLV().getValue();

                    byte[] uuid = value.getData();
                    
                    // UUID must be exactly 16 bytes long
                    if ( ( uuid == null ) || ( uuid.length != 16 ) )
                    {
                        String msg = I18n.err( I18n.ERR_04027 );
                        LOG.error( msg );
                        throw new DecoderException( msg );
                    }

                    if ( IS_DEBUG )
                    {
                        LOG.debug( "UUID = " + StringTools.dumpBytes( uuid ) );
                    }

                    // Store the UUID in the UUIDs list
                    control.getSyncUUIDs().add( uuid );
                    
                    // We can have an END transition
                    syncInfoValueContainer.grammarEndAllowed( true );
                }
            } );
        
        
        /** 
         * Transition from syncIdSet syncUUID to syncUUID
         *     syncIdSet [3] SEQUENCE {
         *         ...
         *         syncUUIDs      SET OF *syncUUID*
         *     }
         *     
         * Add a new UUID in the UUIDs list
         */
        super.transitions[SyncInfoValueControlStatesEnum.SYNC_ID_SET_UUID_STATE][UniversalTag.OCTET_STRING] = 
            new GrammarTransition( SyncInfoValueControlStatesEnum.SYNC_ID_SET_UUID_STATE, 
                                    SyncInfoValueControlStatesEnum.SYNC_ID_SET_UUID_STATE, 
                                    UniversalTag.OCTET_STRING, 
                new GrammarAction( "SyncIdSet UUID" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    SyncInfoValueControlContainer syncInfoValueContainer = 
                        ( SyncInfoValueControlContainer ) container;
                    SyncInfoValueControl control = syncInfoValueContainer.getSyncInfoValueControl();
                    
                    Value value = syncInfoValueContainer.getCurrentTLV().getValue();

                    byte[] uuid = value.getData();
                    
                    // UUID must be exactly 16 bytes long
                    if ( ( uuid == null ) || ( uuid.length != 16 ) )
                    {
                        String msg = I18n.err( I18n.ERR_04027 );
                        LOG.error( msg );
                        throw new DecoderException( msg );
                    }

                    if ( IS_DEBUG )
                    {
                        LOG.debug( "UUID = " + StringTools.dumpBytes( uuid ) );
                    }

                    // Store the UUID in the UUIDs list
                    control.getSyncUUIDs().add( uuid );
                    
                    // We can have an END transition
                    syncInfoValueContainer.grammarEndAllowed( true );
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
