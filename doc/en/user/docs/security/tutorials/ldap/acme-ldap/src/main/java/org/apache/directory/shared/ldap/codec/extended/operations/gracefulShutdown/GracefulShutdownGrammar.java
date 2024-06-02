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
package org.apache.directory.shared.ldap.codec.extended.operations.gracefulShutdown;


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
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the Graceful shutdown. All the actions are declared in
 * this class. As it is a singleton, these declaration are only done once. The
 * grammar is :
 * 
 * <pre>
 *  GracefulShutdwon ::= SEQUENCE {
 *      timeOffline INTEGER (0..720) DEFAULT 0,
 *      delay [0] INTEGER (0..86400) DEFAULT 0
 *  }
 * </pre>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 912399 $, $Date: 2010-02-21 22:52:31 +0200 (Sun, 21 Feb 2010) $, 
 */
public class GracefulShutdownGrammar extends AbstractGrammar
{
    /** The logger */
    static final Logger log = LoggerFactory.getLogger( GracefulShutdownGrammar.class );

    /** Speedup for logs */
    static final boolean IS_DEBUG = log.isDebugEnabled();

    /** The instance of grammar. GracefulShutdownGrammar is a singleton */
    private static IGrammar instance = new GracefulShutdownGrammar();


    /**
     * Creates a new GracefulShutdownGrammar object.
     */
    private GracefulShutdownGrammar()
    {
        name = GracefulShutdownGrammar.class.getName();
        statesEnum = GracefulShutdownStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[GracefulShutdownStatesEnum.LAST_GRACEFUL_SHUTDOWN_STATE][256];

        /**
         * Transition from init state to graceful shutdown
         * 
         * GracefulShutdown ::= SEQUENCE {
         *     ...
         *     
         * Creates the GracefulShutdown object
         */
        super.transitions[IStates.INIT_GRAMMAR_STATE][UniversalTag.SEQUENCE_TAG] = 
            new GrammarTransition( IStates.INIT_GRAMMAR_STATE, 
                GracefulShutdownStatesEnum.GRACEFUL_SHUTDOWN_SEQUENCE_STATE, 
                UniversalTag.SEQUENCE_TAG,
                new GrammarAction( "Init GracefulShutdown" )
            {
                public void action( IAsn1Container container )
                {
                    GracefulShutdownContainer gracefulShutdownContainer = ( GracefulShutdownContainer ) container;
                    GracefulShutdown gracefulShutdown = new GracefulShutdown();
                    gracefulShutdownContainer.setGracefulShutdown( gracefulShutdown );
                    gracefulShutdownContainer.grammarEndAllowed( true );
                }
            } );

        /**
         * Transition from graceful shutdown to time offline
         *
         * GracefulShutdown ::= SEQUENCE { 
         *     timeOffline INTEGER (0..720) DEFAULT 0,
         *     ...
         *     
         * Set the time offline value into the GracefulShutdown
         * object.
         */
        super.transitions[GracefulShutdownStatesEnum.GRACEFUL_SHUTDOWN_SEQUENCE_STATE][UniversalTag.INTEGER_TAG] = 
            new GrammarTransition( GracefulShutdownStatesEnum.GRACEFUL_SHUTDOWN_SEQUENCE_STATE, 
                                    GracefulShutdownStatesEnum.TIME_OFFLINE_STATE, 
                                    UniversalTag.INTEGER_TAG, 
                new GrammarAction( "Set Graceful Shutdown time offline" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    GracefulShutdownContainer gracefulShutdownContainer = ( GracefulShutdownContainer ) container;
                    Value value = gracefulShutdownContainer.getCurrentTLV().getValue();

                    try
                    {
                        int timeOffline = IntegerDecoder.parse( value, 0, 720 );

                        if ( IS_DEBUG )
                        {
                            log.debug( "Time Offline = " + timeOffline );
                        }

                        gracefulShutdownContainer.getGracefulShutdown().setTimeOffline( timeOffline );
                        gracefulShutdownContainer.grammarEndAllowed( true );
                    }
                    catch ( IntegerDecoderException e )
                    {
                        String msg = I18n.err( I18n.ERR_04037, StringTools.dumpBytes( value.getData() ) );
                        log.error( msg );
                        throw new DecoderException( msg );
                    }
                }
            } );

        /**
         * Transition from time offline to delay
         * 
         * GracefulShutdown ::= SEQUENCE { 
         *     ... 
         *     delay [0] INTEGER (0..86400) DEFAULT 0 }
         * 
         * Set the delay value into the GracefulShutdown
         * object.
         */
        super.transitions[GracefulShutdownStatesEnum.TIME_OFFLINE_STATE][GracefulActionConstants.GRACEFUL_ACTION_DELAY_TAG] = 
            new GrammarTransition( GracefulShutdownStatesEnum.TIME_OFFLINE_STATE, 
                                    GracefulShutdownStatesEnum.DELAY_STATE, 
                                    GracefulActionConstants.GRACEFUL_ACTION_DELAY_TAG, 

                new GrammarAction( "Set Graceful Shutdown Delay" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    GracefulShutdownContainer gracefulShutdownContainer = ( GracefulShutdownContainer ) container;
                    Value value = gracefulShutdownContainer.getCurrentTLV().getValue();

                    try
                    {
                        int delay = IntegerDecoder.parse( value, 0, 86400 );

                        if ( IS_DEBUG )
                        {
                            log.debug( "Delay = " + delay );
                        }

                        gracefulShutdownContainer.getGracefulShutdown().setDelay( delay );
                        gracefulShutdownContainer.grammarEndAllowed( true );
                    }
                    catch ( IntegerDecoderException e )
                    {
                        String msg = I18n.err( I18n.ERR_04036, StringTools.dumpBytes( value.getData() ) );
                        log.error( msg );
                        throw new DecoderException( msg );
                    }
                }
            } );
        
        /**
         * Transition from graceful shutdown to delay
         * 
         * GracefulShutdown ::= SEQUENCE { 
         *     ... 
         *     delay [0] INTEGER (0..86400) DEFAULT 0 }
         * 
         * Set the delay value into the GracefulShutdown
         * object.
         */
        super.transitions[GracefulShutdownStatesEnum.GRACEFUL_SHUTDOWN_SEQUENCE_STATE]
                         [GracefulActionConstants.GRACEFUL_ACTION_DELAY_TAG] = 
            new GrammarTransition( GracefulShutdownStatesEnum.GRACEFUL_SHUTDOWN_SEQUENCE_STATE, 
                                    GracefulShutdownStatesEnum.DELAY_STATE, 
                                    GracefulActionConstants.GRACEFUL_ACTION_DELAY_TAG, 

                new GrammarAction( "Set Graceful Shutdown Delay" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    GracefulShutdownContainer gracefulShutdownContainer = ( GracefulShutdownContainer ) container;
                    Value value = gracefulShutdownContainer.getCurrentTLV().getValue();

                    try
                    {
                        int delay = IntegerDecoder.parse( value, 0, 86400 );

                        if ( IS_DEBUG )
                        {
                            log.debug( "Delay = " + delay );
                        }

                        gracefulShutdownContainer.getGracefulShutdown().setDelay( delay );
                        gracefulShutdownContainer.grammarEndAllowed( true );
                    }
                    catch ( IntegerDecoderException e )
                    {
                        String msg = I18n.err( I18n.ERR_04036, StringTools.dumpBytes( value.getData() ) );
                        log.error( msg );
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
