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
package org.apache.directory.shared.ldap.codec.extended.operations.cancel;


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
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the Cancel operation. All the actions are declared
 * in this class. As it is a singleton, these declaration are only done once.
 * The grammar is :
 * 
 * <pre>
 *  cancelRequestValue ::= SEQUENCE {
 *      cancelId     MessageID 
 *                   -- MessageID is as defined in [RFC2251]
 * }
 * </pre>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 687720 $, $Date: 2008-08-21 14:05:50 +0200 (Thu, 21 Aug 2008) $, 
 */
public class CancelGrammar extends AbstractGrammar
{
    /** The logger */
    static final Logger LOG = LoggerFactory.getLogger( CancelGrammar.class );

    /** Speedup for logs */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. CancelGrammar is a singleton */
    private static IGrammar instance = new CancelGrammar();


    /**
     * Creates a new GracefulDisconnectGrammar object.
     */
    private CancelGrammar()
    {
        name = CancelGrammar.class.getName();
        statesEnum = CancelStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[CancelStatesEnum.LAST_CANCEL_STATE][256];

        /**
         * Transition from init state to cancel sequence
         * cancelRequestValue ::= SEQUENCE {
         *     ... 
         * 
         * Creates the Cancel object
         */
        super.transitions[IStates.INIT_GRAMMAR_STATE][UniversalTag.SEQUENCE_TAG] = 
            new GrammarTransition( IStates.INIT_GRAMMAR_STATE,
                                    CancelStatesEnum.CANCEL_SEQUENCE_STATE, 
                                    UniversalTag.SEQUENCE_TAG,
                new GrammarAction(
                "Init Cancel" )
            {
                public void action( IAsn1Container container )
                {
                    CancelContainer cancelContainer = ( CancelContainer ) container;
                    Cancel cancel = new Cancel();
                    cancelContainer.setCancel( cancel );
                }
            } );

        /**
         * Transition from cancel SEQ to cancelId
         * 
         * cancelRequestValue ::= SEQUENCE {
         *     cancelId   MessageID 
         * }
         *     
         * Set the cancelId value into the Cancel object.    
         */
        super.transitions[CancelStatesEnum.CANCEL_SEQUENCE_STATE][UniversalTag.INTEGER_TAG] = 
            new GrammarTransition( CancelStatesEnum.CANCEL_SEQUENCE_STATE,
                                    CancelStatesEnum.CANCEL_ID_STATE, 
                                    UniversalTag.INTEGER_TAG, 
                new GrammarAction( "Stores CancelId" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    CancelContainer cancelContainer = ( CancelContainer ) container;
                    Value value = cancelContainer.getCurrentTLV().getValue();
    
                    try
                    {
                        int cancelId = IntegerDecoder.parse( value, 0, Integer.MAX_VALUE );
        
                        if ( IS_DEBUG )
                        {
                            LOG.debug( "CancelId = " + cancelId );
                        }
        
                        cancelContainer.getCancel().setCancelId( cancelId );
                        cancelContainer.grammarEndAllowed( true );
                    }
                    catch ( IntegerDecoderException e )
                    {
                        String msg = I18n.err( I18n.ERR_04031, StringTools.dumpBytes( value.getData() ) );
                        LOG.error( msg );
                        throw new DecoderException( msg );
                    }
                }
            });
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
