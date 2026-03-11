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
package org.apache.directory.shared.ldap.codec.search.controls.subentries;


import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.asn1.ber.tlv.TLV;
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
 * This class implements the SubEntryControl. All the actions are declared in
 * this class. As it is a singleton, these declaration are only done once.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 912399 $, $Date: 2010-02-21 22:52:31 +0200 (Sun, 21 Feb 2010) $, 
 */
public class SubentriesControlGrammar extends AbstractGrammar
{
    /** The logger */
    static final Logger log = LoggerFactory.getLogger( SubentriesControlGrammar.class );

    /** The instance of grammar. SubEntryControlGrammar is a singleton */
    private static IGrammar instance = new SubentriesControlGrammar();


    /**
     * Creates a new SubEntryGrammar object.
     */
    private SubentriesControlGrammar()
    {
        name = SubentriesControlGrammar.class.getName();
        statesEnum = SubentriesControlStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[SubentriesControlStatesEnum.LAST_SUB_ENTRY_STATE][256];

        super.transitions[SubentriesControlStatesEnum.START_STATE][UniversalTag.BOOLEAN_TAG] = 
            new GrammarTransition( SubentriesControlStatesEnum.START_STATE, 
                                    SubentriesControlStatesEnum.SUB_ENTRY_VISIBILITY_STATE, UniversalTag.BOOLEAN_TAG, 
                new GrammarAction( "SubEntryControl visibility" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    SubentriesControlContainer subEntryContainer = ( SubentriesControlContainer ) container;
                    SubentriesControl control = subEntryContainer.getSubEntryControl();

                    TLV tlv = subEntryContainer.getCurrentTLV();

                    // We get the value. If it's a 0, it's a FALSE. If it's
                    // a FF, it's a TRUE. Any other value should be an error,
                    // but we could relax this constraint. So if we have
                    // something
                    // which is not 0, it will be interpreted as TRUE, but we
                    // will generate a warning.
                    Value value = tlv.getValue();

                    try
                    {
                        control.setVisibility( BooleanDecoder.parse( value ) );

                        // We can have an END transition
                        container.grammarEndAllowed( true );
                    }
                    catch ( BooleanDecoderException bde )
                    {
                        log.error( I18n.err( I18n.ERR_04054, StringTools.dumpBytes( value.getData() ), bde.getMessage() ) );

                        // This will generate a PROTOCOL_ERROR
                        throw new DecoderException( bde.getMessage() );
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
