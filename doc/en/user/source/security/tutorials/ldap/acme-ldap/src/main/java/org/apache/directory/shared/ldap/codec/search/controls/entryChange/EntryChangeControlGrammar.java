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
package org.apache.directory.shared.ldap.codec.search.controls.entryChange;


import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.util.IntegerDecoder;
import org.apache.directory.shared.asn1.util.IntegerDecoderException;
import org.apache.directory.shared.asn1.util.LongDecoder;
import org.apache.directory.shared.asn1.util.LongDecoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.codec.search.controls.ChangeType;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the EntryChangeControl. All the actions are declared in
 * this class. As it is a singleton, these declaration are only done once.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 923473 $, $Date: 2010-03-16 00:25:03 +0200 (Tue, 16 Mar 2010) $, 
 */
public class EntryChangeControlGrammar extends AbstractGrammar
{
    /** The logger */
    static final Logger log = LoggerFactory.getLogger( EntryChangeControlGrammar.class );

    /** Speedup for logs */
    static final boolean IS_DEBUG = log.isDebugEnabled();

    /** The instance of grammar. EntryChangeControlGrammar is a singleton */
    private static IGrammar instance = new EntryChangeControlGrammar();


    /**
     * Creates a new EntryChangeControlGrammar object.
     */
    private EntryChangeControlGrammar()
    {
        name = EntryChangeControlGrammar.class.getName();
        statesEnum = EntryChangeControlStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[EntryChangeControlStatesEnum.LAST_EC_STATE][256];

        // ============================================================================================
        // Transition from start state to Entry Change sequence
        // ============================================================================================
        // EntryChangeNotification ::= SEQUENCE {
        //     ...
        //
        // Initialization of the structure
        super.transitions[EntryChangeControlStatesEnum.START_STATE][UniversalTag.SEQUENCE_TAG] = 
            new GrammarTransition( EntryChangeControlStatesEnum.START_STATE, 
                                    EntryChangeControlStatesEnum.EC_SEQUENCE_STATE, 
                                    UniversalTag.SEQUENCE_TAG, null );

        // ============================================================================================
        // transition from Entry Change sequence to Change Type
        // ============================================================================================
        // EntryChangeNotification ::= SEQUENCE {
        //     changeType ENUMERATED {
        //     ...
        //
        // Evaluates the changeType
        super.transitions[EntryChangeControlStatesEnum.EC_SEQUENCE_STATE][UniversalTag.ENUMERATED_TAG] = 
            new GrammarTransition( EntryChangeControlStatesEnum.EC_SEQUENCE_STATE,
                                    EntryChangeControlStatesEnum.CHANGE_TYPE_STATE, 
                                    UniversalTag.ENUMERATED_TAG,
            new GrammarAction( "Set EntryChangeControl changeType" )
        {
            public void action( IAsn1Container container ) throws DecoderException
            {
                EntryChangeControlContainer entryChangeContainer = ( EntryChangeControlContainer ) container;
                Value value = entryChangeContainer.getCurrentTLV().getValue();

                try
                {
                    int change = IntegerDecoder.parse( value, 1, 8 );

                    switch ( change )
                    {
                        case ChangeType.ADD_VALUE:
                        case ChangeType.DELETE_VALUE:
                        case ChangeType.MODDN_VALUE:
                        case ChangeType.MODIFY_VALUE:
                            ChangeType changeType = ChangeType.getChangeType( change );

                            if ( IS_DEBUG )
                            {
                                log.debug( "changeType = " + changeType );
                            }

                            entryChangeContainer.getEntryChangeControl().setChangeType( changeType );
                            break;

                        default:
                            String msg = I18n.err( I18n.ERR_04044 );
                            log.error( msg );
                            throw new DecoderException( msg );
                    }

                    // We can have an END transition
                    entryChangeContainer.grammarEndAllowed( true );
                }
                catch ( IntegerDecoderException e )
                {
                    String msg = I18n.err( I18n.ERR_04044 );
                    log.error( msg, e );
                    throw new DecoderException( msg );
                }
            }
        } );

        // ============================================================================================
        // Transition from Change Type to Previous DN
        // ============================================================================================
        // EntryChangeNotification ::= SEQUENCE {
        //     ...
        //     previousDN LDAPDN OPTIONAL,
        //     ...
        //
        // Set the previousDN into the structure. We first check that it's a
        // valid DN
        super.transitions[EntryChangeControlStatesEnum.CHANGE_TYPE_STATE][UniversalTag.OCTET_STRING_TAG] = 
            new GrammarTransition( EntryChangeControlStatesEnum.CHANGE_TYPE_STATE, 
                                    EntryChangeControlStatesEnum.PREVIOUS_DN_STATE,
                                    UniversalTag.OCTET_STRING_TAG,
            new GrammarAction( "Set EntryChangeControl previousDN" )
        {
            public void action( IAsn1Container container ) throws DecoderException
            {
                EntryChangeControlContainer entryChangeContainer = ( EntryChangeControlContainer ) container;

                ChangeType changeType = entryChangeContainer.getEntryChangeControl().getChangeType();

                if ( changeType != ChangeType.MODDN )
                {
                    log.error( I18n.err( I18n.ERR_04045 ) );
                    throw new DecoderException( I18n.err( I18n.ERR_04046 ));
                }
                else
                {
                    Value value = entryChangeContainer.getCurrentTLV().getValue();
                    DN previousDn = null;

                    try
                    {
                        previousDn = new DN( StringTools.utf8ToString( value.getData() ) );
                    }
                    catch ( LdapInvalidDnException ine )
                    {
                        log.error( I18n.err( I18n.ERR_04047, StringTools.dumpBytes( value.getData() ) ) );
                        throw new DecoderException( I18n.err( I18n.ERR_04048 ) );
                    }

                    if ( IS_DEBUG )
                    {
                        log.debug( "previousDN = " + previousDn );
                    }

                    entryChangeContainer.getEntryChangeControl().setPreviousDn( previousDn );

                    // We can have an END transition
                    entryChangeContainer.grammarEndAllowed( true );
                }
            }
        } );

        // Change Number action
        GrammarAction setChangeNumberAction = new GrammarAction( "Set EntryChangeControl changeNumber" )
        {
            public void action( IAsn1Container container ) throws DecoderException
            {
                EntryChangeControlContainer entryChangeContainer = ( EntryChangeControlContainer ) container;
                Value value = entryChangeContainer.getCurrentTLV().getValue();

                try
                {
                    long changeNumber = LongDecoder.parse( value );

                    if ( IS_DEBUG )
                    {
                        log.debug( "changeNumber = " + changeNumber );
                    }

                    entryChangeContainer.getEntryChangeControl().setChangeNumber( changeNumber );

                    // We can have an END transition
                    entryChangeContainer.grammarEndAllowed( true );
                }
                catch ( LongDecoderException e )
                {
                    String msg = I18n.err( I18n.ERR_04049 );
                    log.error( msg, e );
                    throw new DecoderException( msg );
                }
            }
        };

        // ============================================================================================
        // Transition from Previous DN to Change Number
        // ============================================================================================
        // EntryChangeNotification ::= SEQUENCE {
        //     ...
        //     changeNumber INTEGER OPTIONAL
        // }
        //
        // Set the changeNumber into the structure
        super.transitions[EntryChangeControlStatesEnum.PREVIOUS_DN_STATE][UniversalTag.INTEGER_TAG] = 
            new GrammarTransition( EntryChangeControlStatesEnum.PREVIOUS_DN_STATE, 
                                    EntryChangeControlStatesEnum.CHANGE_NUMBER_STATE, 
                                    UniversalTag.INTEGER_TAG,
                setChangeNumberAction );

        // ============================================================================================
        // Transition from Previous DN to Change Number
        // ============================================================================================
        // EntryChangeNotification ::= SEQUENCE {
        //     ...
        //     changeNumber INTEGER OPTIONAL
        // }
        //
        // Set the changeNumber into the structure
        super.transitions[EntryChangeControlStatesEnum.CHANGE_TYPE_STATE][UniversalTag.INTEGER_TAG] = 
            new GrammarTransition( EntryChangeControlStatesEnum.CHANGE_TYPE_STATE, 
                                    EntryChangeControlStatesEnum.CHANGE_NUMBER_STATE, 
                                    UniversalTag.INTEGER_TAG,
                setChangeNumberAction );
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
