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
package org.apache.directory.shared.ldap.codec.extended.operations.certGeneration;


import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.GrammarAction;
import org.apache.directory.shared.asn1.ber.grammar.GrammarTransition;
import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.directory.shared.asn1.ber.grammar.IStates;
import org.apache.directory.shared.asn1.ber.tlv.UniversalTag;
import org.apache.directory.shared.asn1.ber.tlv.Value;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class implements the Certificate generation extended operation's ASN.1 grammer. 
 * All the actions are declared in this class. As it is a singleton, 
 * these declaration are only done once. The grammar is :
 * 
 * <pre>
 *   CertGenerateObject ::= SEQUENCE 
 *   {
 *      targetDN        IA5String,
 *      issuerDN        IA5String,
 *      subjectDN       IA5String,
 *      keyAlgorithm    IA5String
 *   }
 * </pre>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 664290 $, $Date: 2008-06-07 11:58:06 +0530 (Sat, 07 Jun 2008) $, 
 */

public class CertGenerationGrammar extends AbstractGrammar
{

    /** logger */
    private static final Logger LOG = LoggerFactory.getLogger( CertGenerationGrammar.class );

    /** Speedup for logs */
    static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** The instance of grammar. CertGenerationObjectGrammar is a singleton */
    private static IGrammar instance = new CertGenerationGrammar();


    public CertGenerationGrammar()
    {
        name = CertGenerationGrammar.class.getName();
        statesEnum = CertGenerationStatesEnum.getInstance();

        // Create the transitions table
        super.transitions = new GrammarTransition[CertGenerationStatesEnum.LAST_CERT_GENERATION_STATE][256];

        /**
         * Transition from init state to certificate generation
         * 
         * CertGenerationObject ::= SEQUENCE {
         *     ...
         *     
         * Creates the CertGenerationObject object
         */
        super.transitions[IStates.INIT_GRAMMAR_STATE][UniversalTag.SEQUENCE_TAG] = new GrammarTransition(
            IStates.INIT_GRAMMAR_STATE, CertGenerationStatesEnum.CERT_GENERATION_REQUEST_SEQUENCE_STATE,
            UniversalTag.SEQUENCE_TAG, new GrammarAction( "Init CertGenerationObject" )
            {
                public void action( IAsn1Container container )
                {
                    CertGenerationContainer certGenContainer = ( CertGenerationContainer ) container;
                    CertGenerationObject certGenerationObject = new CertGenerationObject();
                    certGenContainer.setCertGenerationObject( certGenerationObject );
                }
            } );

        /**
         * Transition from certificate generation request to targetDN
         *
         * CertGenerationObject ::= SEQUENCE { 
         *     targetDN IA5String,
         *     ...
         *     
         * Set the targetDN value into the CertGenerationObject instance.
         */
        super.transitions[CertGenerationStatesEnum.CERT_GENERATION_REQUEST_SEQUENCE_STATE][UniversalTag.OCTET_STRING] = new GrammarTransition(
            CertGenerationStatesEnum.CERT_GENERATION_REQUEST_SEQUENCE_STATE, CertGenerationStatesEnum.TARGETDN_STATE,
            UniversalTag.OCTET_STRING, new GrammarAction( "Set Cert Generation target DN value" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    CertGenerationContainer CertGenContainer = ( CertGenerationContainer ) container;
                    Value value = CertGenContainer.getCurrentTLV().getValue();

                    String targetDN = StringTools.utf8ToString( value.getData() );

                    if ( IS_DEBUG )
                    {
                        LOG.debug( "Target DN = " + targetDN );
                    }

                    if ( ( targetDN != null ) && ( targetDN.trim().length() > 0 ) )
                    {
                        if( !DN.isValid( targetDN ) )
                        {
                            String msg = I18n.err( I18n.ERR_04032, targetDN );
                            LOG.error( msg );
                            throw new DecoderException( msg );
                        }
                        
                        CertGenContainer.getCertGenerationObject().setTargetDN( targetDN );
                    }
                    else
                    {
                        String msg = I18n.err( I18n.ERR_04033, StringTools.dumpBytes( value.getData() ) );
                        LOG.error( msg );
                        throw new DecoderException( msg );
                    }
                }
            } );

        /**
         * Transition from targetDN state to issuerDN
         *
         * CertGenerationObject ::= SEQUENCE { 
         *     ...
         *     issuerDN IA5String,
         *     ...
         *     
         * Set the issuerDN value into the CertGenerationObject instance.
         */
        super.transitions[CertGenerationStatesEnum.TARGETDN_STATE][UniversalTag.OCTET_STRING] = new GrammarTransition(
            CertGenerationStatesEnum.TARGETDN_STATE, CertGenerationStatesEnum.ISSUER_STATE, UniversalTag.OCTET_STRING,
            new GrammarAction( "Set Cert Generation issuer DN value" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    CertGenerationContainer CertGenContainer = ( CertGenerationContainer ) container;
                    Value value = CertGenContainer.getCurrentTLV().getValue();

                    String issuerDN = StringTools.utf8ToString( value.getData() );

                    if ( IS_DEBUG )
                    {
                        LOG.debug( "Issuer DN = " + issuerDN );
                    }

                    if ( ( issuerDN != null ) && ( issuerDN.trim().length() > 0 ) )
                    {
                        if( !DN.isValid( issuerDN ) )
                        {
                            String msg = I18n.err( I18n.ERR_04034, issuerDN );
                            LOG.error( msg );
                            throw new DecoderException( msg );
                        }
                        
                        CertGenContainer.getCertGenerationObject().setIssuerDN( issuerDN );
                    }
                }
            } );

        /**
         * Transition from issuerDN state to subjectDN
         *
         * CertGenerationObject ::= SEQUENCE {
         *     ... 
         *     subjectDN IA5String,
         *     ...
         *     
         * Set the subjectDN value into the CertGenerationObject instance.
         */
        super.transitions[CertGenerationStatesEnum.ISSUER_STATE][UniversalTag.OCTET_STRING] = new GrammarTransition(
            CertGenerationStatesEnum.ISSUER_STATE, CertGenerationStatesEnum.SUBJECT_STATE, UniversalTag.OCTET_STRING,
            new GrammarAction( "Set Cert Generation subject DN value" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    CertGenerationContainer CertGenContainer = ( CertGenerationContainer ) container;
                    Value value = CertGenContainer.getCurrentTLV().getValue();

                    String subjectDN = StringTools.utf8ToString( value.getData() );

                    if ( IS_DEBUG )
                    {
                        LOG.debug( "subject DN = " + subjectDN );
                    }

                    if ( ( subjectDN != null ) && ( subjectDN.trim().length() > 0 ) )
                    {
                        if( !DN.isValid( subjectDN ) )
                        {
                            String msg = I18n.err( I18n.ERR_04035, subjectDN );
                            LOG.error( msg );
                            throw new DecoderException( msg );
                        }

                        CertGenContainer.getCertGenerationObject().setSubjectDN( subjectDN );
                    }
                    else
                    {
                        String msg = I18n.err( I18n.ERR_04033, StringTools.dumpBytes( value.getData() ) );
                        LOG.error( msg );
                        throw new DecoderException( msg );
                    }
                }
            } );

        /**
         * Transition from subjectDN state to keyAlgo
         *
         * CertGenerationObject ::= SEQUENCE { 
         *     ...
         *     keyAlgorithm IA5String
         *     
         * Set the key algorithm value into the CertGenerationObject instance.
         */
        super.transitions[CertGenerationStatesEnum.SUBJECT_STATE][UniversalTag.OCTET_STRING] = new GrammarTransition(
            CertGenerationStatesEnum.SUBJECT_STATE, CertGenerationStatesEnum.KEY_ALGORITHM_STATE,
            UniversalTag.OCTET_STRING, new GrammarAction( "Set Cert Generation key algorithm value" )
            {
                public void action( IAsn1Container container ) throws DecoderException
                {
                    CertGenerationContainer CertGenContainer = ( CertGenerationContainer ) container;
                    Value value = CertGenContainer.getCurrentTLV().getValue();

                    String keyAlgorithm = StringTools.utf8ToString( value.getData() );

                    if ( IS_DEBUG )
                    {
                        LOG.debug( "key algorithm = " + keyAlgorithm );
                    }

                    if ( keyAlgorithm != null && ( keyAlgorithm.trim().length() > 0 ) )
                    {
                        CertGenContainer.getCertGenerationObject().setKeyAlgorithm( keyAlgorithm );
                    }

                    CertGenContainer.grammarEndAllowed( true );
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
