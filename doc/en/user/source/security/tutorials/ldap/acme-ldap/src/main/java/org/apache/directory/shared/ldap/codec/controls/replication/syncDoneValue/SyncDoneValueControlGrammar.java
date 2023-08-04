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
package org.apache.directory.shared.ldap.codec.controls.replication.syncDoneValue;


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
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * Implementation of SyncDoneValueControl. All the actions are declared in
 * this class. As it is a singleton, these declaration are only done once.
 *
 *  The decoded grammar is as follows :
 *  
 *  syncDoneValue ::= SEQUENCE 
 *  {
 *       cookie          syncCookie OPTIONAL,
 *       refreshDeletes  BOOLEAN DEFAULT FALSE
 *  }
 *  
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SyncDoneValueControlGrammar extends AbstractGrammar
{

    /** the logger */
    private static final Logger LOG = LoggerFactory.getLogger( SyncDoneValueControlGrammar.class );

    /** speedup for logger */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** SyncDoneValueControlGrammar singleton instance */
    private static final SyncDoneValueControlGrammar INSTANCE = new SyncDoneValueControlGrammar();


    /**
     * 
     * Creates a new instance of SyncDoneValueControlGrammar.
     *
     */
    private SyncDoneValueControlGrammar()
    {
        name = SyncDoneValueControlGrammar.class.getName();
        statesEnum = SyncDoneValueControlStatesEnum.getInstance();

        super.transitions = new GrammarTransition[SyncDoneValueControlStatesEnum.LAST_SYNC_DONE_VALUE_STATE][256];

        /** 
         * Transition from initial state to SyncDoneValue sequence
         * SyncDoneValue ::= SEQUENCE {
         *     ...
         *     
         * Initialize the syncDoneValue object
         */
        super.transitions[IStates.INIT_GRAMMAR_STATE][UniversalTag.SEQUENCE_TAG] = new GrammarTransition(
            IStates.INIT_GRAMMAR_STATE, SyncDoneValueControlStatesEnum.SYNC_DONE_VALUE_SEQUENCE_STATE, UniversalTag.SEQUENCE_TAG,
            new GrammarAction( "Initiaization" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    SyncDoneValueControlContainer syncDoneValueContainer = ( SyncDoneValueControlContainer ) container;

                    // As all the values are optional or defaulted, we can end here
                    syncDoneValueContainer.grammarEndAllowed( true );
                }
            }  );

        /**
         * transition from start to cookie
         * {
         *    cookie          syncCookie OPTIONAL
         *    ....
         * }
         */
        super.transitions[SyncDoneValueControlStatesEnum.SYNC_DONE_VALUE_SEQUENCE_STATE][UniversalTag.OCTET_STRING_TAG] = new GrammarTransition(
            SyncDoneValueControlStatesEnum.SYNC_DONE_VALUE_SEQUENCE_STATE, SyncDoneValueControlStatesEnum.COOKIE_STATE,
            UniversalTag.OCTET_STRING_TAG, new GrammarAction( "Set SyncDoneValueControl cookie" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    SyncDoneValueControlContainer syncDoneValueContainer = ( SyncDoneValueControlContainer ) container;
                    Value value = syncDoneValueContainer.getCurrentTLV().getValue();

                    byte[] cookie = value.getData();

                    if ( IS_DEBUG )
                    {
                        LOG.debug( "cookie = {}", StringTools.dumpBytes( cookie ) );
                    }

                    syncDoneValueContainer.getSyncDoneValueControl().setCookie( cookie );

                    syncDoneValueContainer.grammarEndAllowed( true );
                }
            } );

        GrammarAction refreshDeletesTagAction = new GrammarAction( "set SyncDoneValueControl refreshDeletes flag" )
        {
            public void action( IAsn1Container container ) throws DecoderException
            {
                SyncDoneValueControlContainer syncDoneValueContainer = ( SyncDoneValueControlContainer ) container;
                Value value = syncDoneValueContainer.getCurrentTLV().getValue();

                try
                {
                    boolean refreshDeletes = BooleanDecoder.parse( value );

                    if ( IS_DEBUG )
                    {
                        LOG.debug( "refreshDeletes = {}", refreshDeletes );
                    }

                    syncDoneValueContainer.getSyncDoneValueControl().setRefreshDeletes( refreshDeletes );

                    // the END transition for grammar
                    syncDoneValueContainer.grammarEndAllowed( true );
                }
                catch ( BooleanDecoderException be )
                {
                    String msg = I18n.err( I18n.ERR_04024 );
                    LOG.error( msg, be );
                    throw new DecoderException( msg );
                }

            }
        }; 
        /**
         * transition from cookie to refreshDeletes
         * {
         *    ....
         *    refreshDeletes BOOLEAN DEFAULT FALSE
         * }
         */
        super.transitions[SyncDoneValueControlStatesEnum.COOKIE_STATE][UniversalTag.BOOLEAN_TAG] = new GrammarTransition(
            SyncDoneValueControlStatesEnum.COOKIE_STATE, SyncDoneValueControlStatesEnum.REFRESH_DELETES_STATE,
            UniversalTag.BOOLEAN_TAG, refreshDeletesTagAction );
        
        /**
         * transition from SEQUENCE to refreshDeletes
         * {
         *    ....
         *    refreshDeletes BOOLEAN DEFAULT FALSE
         * }
         */
        super.transitions[SyncDoneValueControlStatesEnum.SYNC_DONE_VALUE_SEQUENCE_STATE][UniversalTag.BOOLEAN_TAG] = new GrammarTransition(
            SyncDoneValueControlStatesEnum.SYNC_DONE_VALUE_SEQUENCE_STATE, SyncDoneValueControlStatesEnum.REFRESH_DELETES_STATE,
            UniversalTag.BOOLEAN_TAG, refreshDeletesTagAction );

    }


    /**
     * @return the singleton instance of the SyncDoneValueControlGrammar
     */
    public static IGrammar getInstance()
    {
        return INSTANCE;
    }
}
