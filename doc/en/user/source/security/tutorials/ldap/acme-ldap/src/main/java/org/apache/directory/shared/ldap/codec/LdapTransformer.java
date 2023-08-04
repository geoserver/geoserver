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
package org.apache.directory.shared.ldap.codec;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.directory.shared.asn1.Asn1Object;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.primitives.OID;
import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.codec.abandon.AbandonRequestCodec;
import org.apache.directory.shared.ldap.codec.add.AddRequestCodec;
import org.apache.directory.shared.ldap.codec.add.AddResponseCodec;
import org.apache.directory.shared.ldap.codec.bind.BindRequestCodec;
import org.apache.directory.shared.ldap.codec.bind.BindResponseCodec;
import org.apache.directory.shared.ldap.codec.bind.SaslCredentials;
import org.apache.directory.shared.ldap.codec.bind.SimpleAuthentication;
import org.apache.directory.shared.ldap.codec.compare.CompareRequestCodec;
import org.apache.directory.shared.ldap.codec.compare.CompareResponseCodec;
import org.apache.directory.shared.ldap.codec.del.DelRequestCodec;
import org.apache.directory.shared.ldap.codec.del.DelResponseCodec;
import org.apache.directory.shared.ldap.codec.extended.ExtendedRequestCodec;
import org.apache.directory.shared.ldap.codec.extended.ExtendedResponseCodec;
import org.apache.directory.shared.ldap.codec.intermediate.IntermediateResponseCodec;
import org.apache.directory.shared.ldap.codec.modify.ModifyRequestCodec;
import org.apache.directory.shared.ldap.codec.modify.ModifyResponseCodec;
import org.apache.directory.shared.ldap.codec.modifyDn.ModifyDNRequestCodec;
import org.apache.directory.shared.ldap.codec.modifyDn.ModifyDNResponseCodec;
import org.apache.directory.shared.ldap.codec.search.AndFilter;
import org.apache.directory.shared.ldap.codec.search.AttributeValueAssertionFilter;
import org.apache.directory.shared.ldap.codec.search.ConnectorFilter;
import org.apache.directory.shared.ldap.codec.search.ExtensibleMatchFilter;
import org.apache.directory.shared.ldap.codec.search.Filter;
import org.apache.directory.shared.ldap.codec.search.NotFilter;
import org.apache.directory.shared.ldap.codec.search.OrFilter;
import org.apache.directory.shared.ldap.codec.search.PresentFilter;
import org.apache.directory.shared.ldap.codec.search.SearchRequestCodec;
import org.apache.directory.shared.ldap.codec.search.SearchResultDoneCodec;
import org.apache.directory.shared.ldap.codec.search.SearchResultEntryCodec;
import org.apache.directory.shared.ldap.codec.search.SearchResultReferenceCodec;
import org.apache.directory.shared.ldap.codec.search.SubstringFilter;
import org.apache.directory.shared.ldap.codec.unbind.UnBindRequestCodec;
import org.apache.directory.shared.ldap.codec.util.LdapURLEncodingException;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.filter.AndNode;
import org.apache.directory.shared.ldap.filter.ApproximateNode;
import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.ExtensibleNode;
import org.apache.directory.shared.ldap.filter.GreaterEqNode;
import org.apache.directory.shared.ldap.filter.LeafNode;
import org.apache.directory.shared.ldap.filter.LessEqNode;
import org.apache.directory.shared.ldap.filter.NotNode;
import org.apache.directory.shared.ldap.filter.OrNode;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.filter.SimpleNode;
import org.apache.directory.shared.ldap.filter.SubstringNode;
import org.apache.directory.shared.ldap.message.AbandonRequestImpl;
import org.apache.directory.shared.ldap.message.AddRequestImpl;
import org.apache.directory.shared.ldap.message.AddResponseImpl;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.message.BindRequestImpl;
import org.apache.directory.shared.ldap.message.BindResponseImpl;
import org.apache.directory.shared.ldap.message.CompareRequestImpl;
import org.apache.directory.shared.ldap.message.CompareResponseImpl;
import org.apache.directory.shared.ldap.message.DeleteRequestImpl;
import org.apache.directory.shared.ldap.message.DeleteResponseImpl;
import org.apache.directory.shared.ldap.message.ExtendedRequestImpl;
import org.apache.directory.shared.ldap.message.ExtendedResponseImpl;
import org.apache.directory.shared.ldap.message.IntermediateResponseImpl;
import org.apache.directory.shared.ldap.message.LdapResultImpl;
import org.apache.directory.shared.ldap.message.ModifyDnRequestImpl;
import org.apache.directory.shared.ldap.message.ModifyDnResponseImpl;
import org.apache.directory.shared.ldap.message.ModifyRequestImpl;
import org.apache.directory.shared.ldap.message.ModifyResponseImpl;
import org.apache.directory.shared.ldap.message.ReferralImpl;
import org.apache.directory.shared.ldap.message.SearchRequestImpl;
import org.apache.directory.shared.ldap.message.SearchResponseDoneImpl;
import org.apache.directory.shared.ldap.message.SearchResponseEntryImpl;
import org.apache.directory.shared.ldap.message.SearchResponseReferenceImpl;
import org.apache.directory.shared.ldap.message.UnbindRequestImpl;
import org.apache.directory.shared.ldap.message.control.Control;
import org.apache.directory.shared.ldap.message.extended.GracefulShutdownRequest;
import org.apache.directory.shared.ldap.message.internal.InternalLdapResult;
import org.apache.directory.shared.ldap.message.internal.InternalMessage;
import org.apache.directory.shared.ldap.message.internal.InternalReferral;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.util.LdapURL;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A Codec to Internal Message transformer.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 * @version $Rev: 923473 $, $Date: 2010-03-16 00:25:03 +0200 (Tue, 16 Mar 2010) $, 
 */
public class LdapTransformer
{
    /** The logger */
    private static Logger LOG = LoggerFactory.getLogger( LdapTransformer.class );

    /** A speedup for logger */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();


    /**
     * Transform an AbandonRequest message from a codec Message to an
     * InternalMessage
     * 
     * @param codecMessage The message to transform
     * @param messageId The message Id
     * @return An internal AbandonRequest message
     */
    private static InternalMessage transformAbandonRequest( AbandonRequestCodec abandonRequest, int messageId )
    {
        AbandonRequestImpl internalMessage = new AbandonRequestImpl( messageId );

        // Codec : int abandonnedMessageId -> Internal : int abandonId
        internalMessage.setAbandoned( abandonRequest.getAbandonedMessageId() );

        return internalMessage;
    }


    /**
     * Transform an AddRequest message from a addRequest to a InternalMessage
     * 
     * @param addRequest The message to transform
     * @param messageId The message Id
     * @return A Internal AddRequestImpl
     */
    private static InternalMessage transformAddRequest( AddRequestCodec addRequest, int messageId )
    {
        AddRequestImpl internalMessage = new AddRequestImpl( messageId );

        // Codec : DN entry -> Internal : String name
        internalMessage.setEntry( addRequest.getEntry() );

        // Codec : Attributes attributes -> Internal : Attributes entry
        internalMessage.setEntry( addRequest.getEntry() );

        return internalMessage;
    }


    /**
     * Transform a BindRequest message from a CodecMessage to a InternalMessage
     * 
     * @param bindRequest The message to transform
     * @param messageId The message Id
     * @return A Internal BindRequestImpl
     */
    private static InternalMessage transformBindRequest( BindRequestCodec bindRequest, int messageId )
    {
        BindRequestImpl internalMessage = new BindRequestImpl( messageId );

        // Codec : int version -> Internal : boolean isVersion3
        internalMessage.setVersion3( bindRequest.isLdapV3() );

        // Codec : DN name -> Internal : DN name
        internalMessage.setName( bindRequest.getName() );

        // Codec : Asn1Object authentication instanceOf SimpleAuthentication ->
        // Internal : boolean isSimple
        // Codec : SimpleAuthentication OctetString simple -> Internal : byte []
        // credentials
        Asn1Object authentication = bindRequest.getAuthentication();

        if ( authentication instanceof SimpleAuthentication )
        {
            internalMessage.setSimple( true );
            internalMessage.setCredentials( ( ( SimpleAuthentication ) authentication ).getSimple() );
        }
        else
        {
            internalMessage.setSimple( false );
            internalMessage.setCredentials( ( ( SaslCredentials ) authentication ).getCredentials() );
            internalMessage.setSaslMechanism( ( ( SaslCredentials ) authentication ).getMechanism() );
        }

        return internalMessage;
    }


    /**
     * Transform a BindResponse message from a CodecMessage to a 
     * InternalMessage.  This is used by clients which are receiving a 
     * BindResponse PDU and must decode it to return the Internal 
     * representation.
     * 
     * @param bindResponse The message to transform
     * @param messageId The message Id
     * @return a Internal BindResponseImpl
     */
    private static InternalMessage transformBindResponse( BindResponseCodec bindResponse, int messageId )
    {
        BindResponseImpl internalMessage = new BindResponseImpl( messageId );

        // Codec : byte[] serverSaslcreds -> Internal : byte[] serverSaslCreds
        internalMessage.setServerSaslCreds( bindResponse.getServerSaslCreds() );
        //transformControlsCodecToInternal( codecMessage, internalMessage );
        transformLdapResultCodecToInternal( bindResponse.getLdapResult(), internalMessage.getLdapResult() );
        
        return internalMessage;
    }

    
    /**
     * Transforms parameters of a Codec LdapResult into a Internal LdapResult.
     *
     * @param codecLdapResult the codec LdapResult representation
     * @param InternalResult the Internal LdapResult representation
     */
    private static void transformLdapResultCodecToInternal( LdapResultCodec codecLdapResult, InternalLdapResult internalLdapResult )
    {
        internalLdapResult.setErrorMessage( codecLdapResult.getErrorMessage() );
        
        try
        {
            internalLdapResult.setMatchedDn( new DN( codecLdapResult.getMatchedDN() ) );
        }
        catch ( LdapInvalidDnException e )
        {
            LOG.error( I18n.err( I18n.ERR_04111, codecLdapResult.getMatchedDN() ) );
            internalLdapResult.setMatchedDn( new DN() );
        }
        
        internalLdapResult.setResultCode( codecLdapResult.getResultCode() );

        if ( codecLdapResult.getReferrals() == null )
        {
            
        }
        else
        {
            ReferralImpl referral = new ReferralImpl();
            
            for ( LdapURL url : codecLdapResult.getReferrals() )
            {
                referral.addLdapUrl( url.toString() );
            }
            
            internalLdapResult.setReferral( referral );
        }
    }
    

    /**
     * Transform a CompareRequest message from a CodecMessage to a
     * InternalMessage
     * 
     * @param compareRequest The message to transform
     * @param messageId The message Id
     * @return A Internal CompareRequestImpl
     */
    private static InternalMessage transformCompareRequest( CompareRequestCodec compareRequest, int messageId )
    {
        CompareRequestImpl internalMessage = new CompareRequestImpl( messageId );

        // Codec : DN entry -> Internal : private DN
        internalMessage.setName( compareRequest.getEntry() );

        // Codec : LdapString attributeDesc -> Internal : String attrId
        internalMessage.setAttributeId( compareRequest.getAttributeDesc() );

        // Codec : OctetString assertionValue -> Internal : byte[] attrVal
        if ( compareRequest.getAssertionValue() instanceof String )
        {
            internalMessage.setAssertionValue( ( String ) compareRequest.getAssertionValue() );
        }
        else
        {
            internalMessage.setAssertionValue( ( byte[] ) compareRequest.getAssertionValue() );
        }

        return internalMessage;
    }


    /**
     * Transform a DelRequest message from a CodecMessage to a InternalMessage
     * 
     * @param delRequest The message to transform
     * @param messageId The message Id
     * @return A Internal DeleteRequestImpl
     */
    private static InternalMessage transformDelRequest( DelRequestCodec delRequest, int messageId )
    {
        DeleteRequestImpl internalMessage = new DeleteRequestImpl( messageId );

        // Codec : DN entry -> Internal : DN
        internalMessage.setName( delRequest.getEntry() );

        return internalMessage;
    }


    /**
     * Transform an ExtendedRequest message from a CodecMessage to a
     * InternalMessage
     * 
     * @param extendedRequest The message to transform
     * @param messageId The message Id
     * @return A Internal ExtendedRequestImpl
     */
    private static InternalMessage transformExtendedRequest( ExtendedRequestCodec extendedRequest, int messageId )
    {
        ExtendedRequestImpl internalMessage;

        if ( extendedRequest.getRequestName().equals( GracefulShutdownRequest.EXTENSION_OID ) )
        {
            internalMessage = new GracefulShutdownRequest( messageId );
        }
        else
        {
            internalMessage = new ExtendedRequestImpl( messageId );
        }

        // Codec : OID requestName -> Internal : String oid
        internalMessage.setOid( extendedRequest.getRequestName() );

        // Codec : OctetString requestValue -> Internal : byte [] payload
        internalMessage.setPayload( extendedRequest.getRequestValue() );

        return internalMessage;
    }


    /**
     * Transform a ModifyDNRequest message from a CodecMessage to a
     * InternalMessage
     * 
     * @param modifyDNRequest The message to transform
     * @param messageId The message Id
     * @return A Internal ModifyDNRequestImpl
     */
    private static InternalMessage transformModifyDNRequest( ModifyDNRequestCodec modifyDNRequest, int messageId )
    {
        ModifyDnRequestImpl internalMessage = new ModifyDnRequestImpl( messageId );

        // Codec : DN entry -> Internal : DN m_name
        internalMessage.setName( modifyDNRequest.getEntry() );

        // Codec : RelativeDN newRDN -> Internal : DN m_newRdn
        internalMessage.setNewRdn( modifyDNRequest.getNewRDN() );

        // Codec : boolean deleteOldRDN -> Internal : boolean m_deleteOldRdn
        internalMessage.setDeleteOldRdn( modifyDNRequest.isDeleteOldRDN() );

        // Codec : DN newSuperior -> Internal : DN m_newSuperior
        internalMessage.setNewSuperior( modifyDNRequest.getNewSuperior() );

        return internalMessage;
    }


    /**
     * Transform a ModifyRequest message from a CodecMessage to a InternalMessage
     * 
     * @param modifyRequest The message to transform
     * @param messageId The message Id
     * @return A Internal ModifyRequestImpl
     */
    private static InternalMessage transformModifyRequest( ModifyRequestCodec modifyRequest, int messageId )
    {
        ModifyRequestImpl internalMessage = new ModifyRequestImpl( messageId );

        // Codec : DN object -> Internal : String name
        internalMessage.setName( modifyRequest.getObject() );

        // Codec : ArrayList modifications -> Internal : ArrayList mods
        if ( modifyRequest.getModifications() != null )
        {
            // Loop through the modifications
            for ( Modification modification:modifyRequest.getModifications() )
            {
                internalMessage.addModification( modification );
            }
        }

        return internalMessage;
    }


    /**
     * Transform the Filter part of a SearchRequest to an ExprNode
     * 
     * @param codecFilter The filter to be transformed
     * @return An ExprNode
     */
    private static ExprNode transformFilter( Filter codecFilter )
    {
        if ( codecFilter != null )
        {
            // Transform OR, AND or NOT leaves
            if ( codecFilter instanceof ConnectorFilter )
            {
                BranchNode branch = null;

                if ( codecFilter instanceof AndFilter )
                {
                    branch = new AndNode();
                }
                else if ( codecFilter instanceof OrFilter )
                {
                    branch = new OrNode();
                }
                else if ( codecFilter instanceof NotFilter )
                {
                    branch = new NotNode();
                }

                List<Filter> filtersSet = ( ( ConnectorFilter ) codecFilter ).getFilterSet();

                // Loop on all AND/OR children
                if ( filtersSet != null )
                {
                    for ( Filter filter:filtersSet )
                    {
                        branch.addNode( transformFilter( filter ) );
                    }
                }

                return branch;
            }
            else
            {
                // Transform PRESENT or ATTRIBUTE_VALUE_ASSERTION
                LeafNode branch = null;

                if ( codecFilter instanceof PresentFilter )
                {
                    branch = new PresenceNode( ( ( PresentFilter ) codecFilter ).getAttributeDescription() );
                }
                else if ( codecFilter instanceof AttributeValueAssertionFilter )
                {
                    AttributeValueAssertion ava = ( ( AttributeValueAssertionFilter ) codecFilter ).getAssertion();

                    // Transform =, >=, <=, ~= filters
                    switch ( ( ( AttributeValueAssertionFilter ) codecFilter ).getFilterType() )
                    {
                        case LdapConstants.EQUALITY_MATCH_FILTER:
                            branch = new EqualityNode( ava.getAttributeDesc(), 
                                ava.getAssertionValue() );
                            
                            break;

                        case LdapConstants.GREATER_OR_EQUAL_FILTER:
                            branch = new GreaterEqNode( ava.getAttributeDesc(),
                                ava.getAssertionValue() );

                            break;

                        case LdapConstants.LESS_OR_EQUAL_FILTER:
                            branch = new LessEqNode( ava.getAttributeDesc(), 
                                ava.getAssertionValue() );

                            break;

                        case LdapConstants.APPROX_MATCH_FILTER:
                            branch = new ApproximateNode( ava.getAttributeDesc(), 
                                ava.getAssertionValue() );

                            break;
                    }

                }
                else if ( codecFilter instanceof SubstringFilter )
                {
                    // Transform Substring filters
                    SubstringFilter filter = ( SubstringFilter ) codecFilter;
                    String initialString = null;
                    String finalString = null;
                    List<String> anyString = null;

                    if ( filter.getInitialSubstrings() != null )
                    {
                        initialString = filter.getInitialSubstrings();
                    }

                    if ( filter.getFinalSubstrings() != null )
                    {
                        finalString = filter.getFinalSubstrings();
                    }

                    if ( filter.getAnySubstrings() != null )
                    {
                        anyString = new ArrayList<String>();

                        for ( String any:filter.getAnySubstrings() )
                        {
                            anyString.add( any );
                        }
                    }

                    branch = new SubstringNode( anyString, filter.getType(), initialString, finalString );
                }
                else if ( codecFilter instanceof ExtensibleMatchFilter )
                {
                    // Transform Extensible Match Filter
                    ExtensibleMatchFilter filter = ( ExtensibleMatchFilter ) codecFilter;
                    String attribute = null;
                    String matchingRule = null;

                    if ( filter.getType() != null )
                    {
                        attribute = filter.getType();
                    }

                    Value<?> value = filter.getMatchValue();

                    if ( filter.getMatchingRule() != null )
                    {
                        matchingRule = filter.getMatchingRule();
                    }

                    branch = new ExtensibleNode( attribute, value, matchingRule, filter.isDnAttributes() );
                }

                return branch;
            }
        }
        else
        {
            // We have found nothing to transform. Return null then.
            return null;
        }
    }


    /**
     * Transform an ExprNode filter to a CodecFilter
     * 
     * @param exprNode The filter to be transformed
     * @return A Codec filter
     */
    public static Filter transformFilter( ExprNode exprNode )
    {
        if ( exprNode != null )
        {
            Filter filter  = null;

            // Transform OR, AND or NOT leaves
            if ( exprNode instanceof BranchNode )
            {
                if ( exprNode instanceof AndNode )
                {
                    filter = new AndFilter();
                }
                else if ( exprNode instanceof OrNode )
                {
                    filter = new OrFilter();
                }
                else if ( exprNode instanceof NotNode )
                {
                    filter = new NotFilter();
                }

                List<ExprNode> children = ((BranchNode)exprNode).getChildren();

                // Loop on all AND/OR children
                if ( children != null )
                {
                    for ( ExprNode child:children )
                    {
                        try
                        {
                            ((ConnectorFilter)filter).addFilter( transformFilter( child ) );
                        }
                        catch ( DecoderException de )
                        {
                            LOG.error( I18n.err( I18n.ERR_04112, de.getLocalizedMessage() ) );
                            return null;
                        }
                    }
                }
            }
            else
            {
                if ( exprNode instanceof PresenceNode )
                {
                    // Transform Presence Node
                    filter = new PresentFilter();
                    ((PresentFilter)filter).setAttributeDescription( ((PresenceNode)exprNode).getAttribute() );
                }
                else if ( exprNode instanceof SimpleNode<?> )
                {
                    if ( exprNode instanceof EqualityNode<?> )
                    {
                        filter = new AttributeValueAssertionFilter( LdapConstants.EQUALITY_MATCH_FILTER );
                        AttributeValueAssertion assertion = new AttributeValueAssertion();
                        assertion.setAttributeDesc( ((EqualityNode<?>)exprNode).getAttribute() );
                        assertion.setAssertionValue( ((EqualityNode<?>)exprNode).getValue() );
                        ((AttributeValueAssertionFilter)filter).setAssertion( assertion );
                    }
                    else if ( exprNode instanceof GreaterEqNode<?> ) 
                    {
                        filter = new AttributeValueAssertionFilter( LdapConstants.GREATER_OR_EQUAL_FILTER );
                        AttributeValueAssertion assertion = new AttributeValueAssertion();
                        assertion.setAttributeDesc( ((EqualityNode<?>)exprNode).getAttribute() );
                        assertion.setAssertionValue( ((EqualityNode<?>)exprNode).getValue() );
                        ((AttributeValueAssertionFilter)filter).setAssertion( assertion );
                    }
                    else if ( exprNode instanceof LessEqNode<?> ) 
                    {
                        filter = new AttributeValueAssertionFilter( LdapConstants.LESS_OR_EQUAL_FILTER );
                        AttributeValueAssertion assertion = new AttributeValueAssertion();
                        assertion.setAttributeDesc( ((EqualityNode<?>)exprNode).getAttribute() );
                        assertion.setAssertionValue( ((EqualityNode<?>)exprNode).getValue() );
                        ((AttributeValueAssertionFilter)filter).setAssertion( assertion );
                    }
                    else if ( exprNode instanceof ApproximateNode<?> )
                    {
                        filter = new AttributeValueAssertionFilter( LdapConstants.APPROX_MATCH_FILTER );
                        AttributeValueAssertion assertion = new AttributeValueAssertion();
                        assertion.setAttributeDesc( ((EqualityNode<?>)exprNode).getAttribute() );
                        assertion.setAssertionValue( ((EqualityNode<?>)exprNode).getValue() );
                        ((AttributeValueAssertionFilter)filter).setAssertion( assertion );
                    }
                }
                else if ( exprNode instanceof SubstringNode )
                {
                    // Transform Substring Nodes
                    filter = new SubstringFilter();

                    String initialString = ((SubstringNode)exprNode).getInitial();
                    String finalString = ((SubstringNode)exprNode).getFinal();
                    List<String> anyStrings = ((SubstringNode)exprNode).getAny();

                    if ( initialString != null )
                    {
                        ((SubstringFilter)filter).setInitialSubstrings( initialString );
                    }

                    if ( finalString != null )
                    {
                        ((SubstringFilter)filter).setFinalSubstrings( finalString );
                    }

                    if ( anyStrings != null )
                    {
                        for ( String any:anyStrings )
                        {
                            ((SubstringFilter)filter).addAnySubstrings( any );
                        }
                    }
                }
                else if ( exprNode instanceof ExtensibleNode )
                {
                    // Transform Extensible Node
                    filter = new ExtensibleMatchFilter();
                    
                    String attribute = ((ExtensibleNode)exprNode).getAttribute();
                    String matchingRule = ((ExtensibleNode)exprNode).getMatchingRuleId();
                    boolean dnAttributes = ((ExtensibleNode)exprNode).hasDnAttributes();
                    Value<?> value = ((ExtensibleNode)exprNode).getValue();

                    if ( attribute != null )
                    {
                        ((ExtensibleMatchFilter)filter).setType( attribute );
                    }

                    if ( matchingRule != null )
                    {
                        ((ExtensibleMatchFilter)filter).setMatchingRule( matchingRule );
                    }

                    ((ExtensibleMatchFilter)filter).setMatchValue( value );
                    ((ExtensibleMatchFilter)filter).setDnAttributes( dnAttributes );
                }
            }

            return filter;
        }
        else
        {
            // We have found nothing to transform. Return null then.
            return null;
        }
    }


    /**
     * Transform a SearchRequest message from a CodecMessage to a InternalMessage
     * 
     * @param searchRequest The message to transform
     * @param messageId The message Id
     * @return A Internal SearchRequestImpl
     */
    private static InternalMessage transformSearchRequest( SearchRequestCodec searchRequest, int messageId )
    {
        SearchRequestImpl internalMessage = new SearchRequestImpl( messageId );

        // Codec : DN baseObject -> Internal : String baseDn
        internalMessage.setBase( searchRequest.getBaseObject() );

        // Codec : int scope -> Internal : ScopeEnum scope
        internalMessage.setScope( searchRequest.getScope() );

        // Codec : int derefAliases -> Internal : AliasDerefMode derefAliases
        switch ( searchRequest.getDerefAliases() )
        {
            case LdapConstants.DEREF_ALWAYS:
                internalMessage.setDerefAliases( AliasDerefMode.DEREF_ALWAYS );
                break;

            case LdapConstants.DEREF_FINDING_BASE_OBJ:
                internalMessage.setDerefAliases( AliasDerefMode.DEREF_FINDING_BASE_OBJ );
                break;

            case LdapConstants.DEREF_IN_SEARCHING:
                internalMessage.setDerefAliases( AliasDerefMode.DEREF_IN_SEARCHING );
                break;

            case LdapConstants.NEVER_DEREF_ALIASES:
                internalMessage.setDerefAliases( AliasDerefMode.NEVER_DEREF_ALIASES );
                break;
        }

        // Codec : int sizeLimit -> Internal : int sizeLimit
        internalMessage.setSizeLimit( searchRequest.getSizeLimit() );

        // Codec : int timeLimit -> Internal : int timeLimit
        internalMessage.setTimeLimit( searchRequest.getTimeLimit() );

        // Codec : boolean typesOnly -> Internal : boolean typesOnly
        internalMessage.setTypesOnly( searchRequest.isTypesOnly() );

        // Codec : Filter filter -> Internal : ExprNode filter
        Filter codecFilter = searchRequest.getFilter();

        internalMessage.setFilter( transformFilter( codecFilter ) );

        // Codec : ArrayList attributes -> Internal : ArrayList attributes
        if ( searchRequest.getAttributes() != null )
        {
            List<EntryAttribute> attributes = searchRequest.getAttributes();

            if ( ( attributes != null ) && ( attributes.size() != 0 ) )
            {
                for ( EntryAttribute attribute:attributes )
                {
                    if ( attribute != null )
                    {
                        internalMessage.addAttribute( attribute.getId() );
                    }
                }
            }
        }

        return internalMessage;
    }


    /**
     * Transform an UnBindRequest message from a CodecMessage to a
     * InternalMessage
     * 
     * @param codecMessage The message to transform
     * @param messageId The message Id
     * @return A Internal UnBindRequestImpl
     */
    private static InternalMessage transformUnBindRequest( LdapMessageCodec codecMessage, int messageId )
    {
        return new UnbindRequestImpl( messageId );
    }


    /**
     * Transform the Codec message to a internal message.
     * 
     * @param obj the object to transform
     * @return the object transformed
     */
    public static InternalMessage transform( Object obj )
    {
        LdapMessageCodec codecMessage = ( LdapMessageCodec ) obj;
        int messageId = codecMessage.getMessageId();

        if ( IS_DEBUG )
        {
            LOG.debug( "Transforming LdapMessage <" + messageId + ", " + codecMessage.getMessageTypeName()
                + "> from Codec to nternal." );
        }

        InternalMessage internalMessage = null;

        MessageTypeEnum messageType = codecMessage.getMessageType();

        switch ( messageType )
        {
            case BIND_REQUEST :
                internalMessage = transformBindRequest( (BindRequestCodec)codecMessage, messageId );
                break;

            case UNBIND_REQUEST :
                internalMessage = transformUnBindRequest( (UnBindRequestCodec)codecMessage, messageId );
                break;

            case SEARCH_REQUEST :
                internalMessage = transformSearchRequest( (SearchRequestCodec)codecMessage, messageId );
                break;

            case MODIFY_REQUEST :
                internalMessage = transformModifyRequest( (ModifyRequestCodec)codecMessage, messageId );
                break;

            case ADD_REQUEST :
                internalMessage = transformAddRequest( (AddRequestCodec)codecMessage, messageId );
                break;

            case DEL_REQUEST :
                internalMessage = transformDelRequest( (DelRequestCodec)codecMessage, messageId );
                break;

            case MODIFYDN_REQUEST :
                internalMessage = transformModifyDNRequest( (ModifyDNRequestCodec)codecMessage, messageId );
                break;

            case COMPARE_REQUEST :
                internalMessage = transformCompareRequest( (CompareRequestCodec)codecMessage, messageId );
                break;

            case ABANDON_REQUEST :
                internalMessage = transformAbandonRequest( (AbandonRequestCodec)codecMessage, messageId );
                break;

            case EXTENDED_REQUEST :
                internalMessage = transformExtendedRequest( (ExtendedRequestCodec)codecMessage, messageId );
                break;
                
            case BIND_RESPONSE :
                internalMessage = transformBindResponse( (BindResponseCodec)codecMessage, messageId );
                break;

            case SEARCH_RESULT_ENTRY :
            case SEARCH_RESULT_DONE :
            case SEARCH_RESULT_REFERENCE :
            case MODIFY_RESPONSE :
            case ADD_RESPONSE :
            case DEL_RESPONSE :
            case MODIFYDN_RESPONSE :
            case COMPARE_RESPONSE :
            case EXTENDED_RESPONSE :
            case INTERMEDIATE_RESPONSE :
                // Nothing to do !
                break;


            default:
                throw new IllegalStateException( I18n.err( I18n.ERR_04113 ) );
        }

        // Transform the controls, too
        transformControlsCodecToInternal( codecMessage, internalMessage );

        return internalMessage;
    }


    /**
     * Transform a Ldapresult part of a Internal Response to a Codec LdapResult
     * 
     * @param InternalLdapResult the Internal LdapResult to transform
     * @return A Codec LdapResult
     */
    private static LdapResultCodec transformLdapResult( LdapResultImpl internalLdapResult )
    {
        LdapResultCodec codecLdapResult = new LdapResultCodec();

        // Internal : ResultCodeEnum resultCode -> Codec : int resultCode
        codecLdapResult.setResultCode( internalLdapResult.getResultCode() );

        // Internal : String errorMessage -> Codec : LdapString errorMessage
        String errorMessage = internalLdapResult.getErrorMessage();
        
        codecLdapResult.setErrorMessage( StringTools.isEmpty( errorMessage ) ? "" : errorMessage );

        // Internal : String matchedDn -> Codec : DN matchedDN
        codecLdapResult.setMatchedDN( internalLdapResult.getMatchedDn() );

        // Internal : Referral referral -> Codec : ArrayList referrals
        ReferralImpl internalReferrals = ( ReferralImpl ) internalLdapResult.getReferral();

        if ( internalReferrals != null )
        {
            codecLdapResult.initReferrals();

            for ( String referral:internalReferrals.getLdapUrls() )
            {
                try
                {
                    LdapURL ldapUrl = new LdapURL( referral.getBytes() );
                    codecLdapResult.addReferral( ldapUrl );
                }
                catch ( LdapURLEncodingException lude )
                {
                    LOG.warn( "The referral " + referral + " is invalid : " + lude.getMessage() );
                    codecLdapResult.addReferral( LdapURL.EMPTY_URL );
                }
            }
        }

        return codecLdapResult;
    }


    /**
     * Transform a Internal AddResponse to a Codec AddResponse
     * 
     * @param internalMessage The incoming Internal AddResponse
     * @return The AddResponseCodec instance
     */
    private static LdapMessageCodec transformAddResponse( InternalMessage internalMessage )
    {
        AddResponseImpl internalAddResponse = ( AddResponseImpl ) internalMessage;

        AddResponseCodec addResponse = new AddResponseCodec();

        // Transform the ldapResult
        addResponse.setLdapResult( transformLdapResult( ( LdapResultImpl ) internalAddResponse.getLdapResult() ) );

        return addResponse;
    }


    /**
     * Transform a Internal BindResponse to a Codec BindResponse
     * 
     * @param internalMessage The incoming Internal BindResponse
     * @return The BindResponseCodec instance
     */
    private static LdapMessageCodec transformBindResponse( InternalMessage internalMessage )
    {
        BindResponseImpl internalBindResponse = ( BindResponseImpl ) internalMessage;

        BindResponseCodec bindResponseCodec = new BindResponseCodec();

        // Internal : byte [] serverSaslCreds -> Codec : OctetString
        // serverSaslCreds
        byte[] serverSaslCreds = internalBindResponse.getServerSaslCreds();

        if ( serverSaslCreds != null )
        {
            bindResponseCodec.setServerSaslCreds( serverSaslCreds );
        }

        // Transform the ldapResult
        bindResponseCodec.setLdapResult( transformLdapResult( ( LdapResultImpl ) internalBindResponse.getLdapResult() ) );

        return bindResponseCodec;
    }


    /**
     * Transform a Internal BindRequest to a Codec BindRequest
     * 
     * @param internalMessage The incoming Internal BindRequest
     * @return The BindRequestCodec instance
     */
    private static LdapMessageCodec transformBindRequest( InternalMessage internalMessage )
    {
        BindRequestImpl internalBindRequest = ( BindRequestImpl ) internalMessage;

        BindRequestCodec bindRequest = new BindRequestCodec();

        if ( internalBindRequest.isSimple() )
        {
            SimpleAuthentication simple = new SimpleAuthentication();
            simple.setSimple( internalBindRequest.getCredentials() );
            bindRequest.setAuthentication( simple );
        }
        else
        {
            SaslCredentials sasl = new SaslCredentials();
            sasl.setCredentials( internalBindRequest.getCredentials() );
            sasl.setMechanism( internalBindRequest.getSaslMechanism() );
            bindRequest.setAuthentication( sasl );
        }
        
        bindRequest.setMessageId( internalBindRequest.getMessageId() );
        bindRequest.setName( internalBindRequest.getName() );
        bindRequest.setVersion( internalBindRequest.isVersion3() ? 3 : 2 );
        
        return bindRequest;
    }


    /**
     * Transform a Internal CompareResponse to a Codec CompareResponse
     * 
     * @param internalMessage The incoming Internal CompareResponse
     * @return The CompareResponseCodec instance
     */
    private static LdapMessageCodec transformCompareResponse( InternalMessage internalMessage )
    {
        CompareResponseImpl internalCompareResponse = ( CompareResponseImpl ) internalMessage;

        CompareResponseCodec compareResponse = new CompareResponseCodec();

        // Transform the ldapResult
        compareResponse
            .setLdapResult( transformLdapResult( ( LdapResultImpl ) internalCompareResponse.getLdapResult() ) );

        return compareResponse;
    }


    /**
     * Transform a Internal DelResponse to a Codec DelResponse
     * 
     * @param internalMessage The incoming Internal DelResponse
     * @return The DelResponseCodec instance
     */
    private static LdapMessageCodec transformDelResponse( InternalMessage internalMessage )
    {
        DeleteResponseImpl internalDelResponse = ( DeleteResponseImpl ) internalMessage;

        DelResponseCodec delResponse = new DelResponseCodec();

        // Transform the ldapResult
        delResponse.setLdapResult( transformLdapResult( ( LdapResultImpl ) internalDelResponse.getLdapResult() ) );

        return delResponse;
    }


    /**
     * Transform a Internal ExtendedResponse to a Codec ExtendedResponse
     * 
     * @param internalMessage The incoming Internal ExtendedResponse
     * @return The ExtendedResponseCodec instance
     */
    private static LdapMessageCodec transformExtendedResponse( InternalMessage internalMessage )
    {
        ExtendedResponseImpl internalExtendedResponse = ( ExtendedResponseImpl ) internalMessage;
        ExtendedResponseCodec extendedResponse = new ExtendedResponseCodec();

        // Internal : String oid -> Codec : OID responseName
        try
        {
            extendedResponse.setResponseName( new OID( internalExtendedResponse.getResponseName() ) );
        }
        catch ( DecoderException de )
        {
            LOG.warn( "The OID " + internalExtendedResponse.getResponseName() + " is invalid : " + de.getMessage() );
            extendedResponse.setResponseName( null );
        }

        // Internal : byte [] value -> Codec : Object response
        extendedResponse.setResponse( internalExtendedResponse.getResponse() );

        // Transform the ldapResult
        extendedResponse.setLdapResult( transformLdapResult( ( LdapResultImpl ) internalExtendedResponse
            .getLdapResult() ) );

        return extendedResponse;
    }


    /**
     * Transform a Internal IntermediateResponse to a Codec IntermediateResponse
     * 
     * @param internalMessage The incoming Internal IntermediateResponse
     * @return The IntermediateResponseCodec instance
     */
    private static LdapMessageCodec transformIntermediateResponse( InternalMessage internalMessage )
    {
        IntermediateResponseImpl internalIntermediateResponse = (IntermediateResponseImpl) internalMessage;
        IntermediateResponseCodec intermediateResponse = new IntermediateResponseCodec();

        // Internal : String oid -> Codec : String responseName
        try
        {
            intermediateResponse.setResponseName( new OID( internalIntermediateResponse.getResponseName() ) );
        }
        catch ( DecoderException de )
        {
            LOG.warn( "The OID " + internalIntermediateResponse.getResponseName() + " is invalid : " + de.getMessage() );
            intermediateResponse.setResponseName( null );
        }

        // Internal : byte [] value -> Codec : byte[] value
        intermediateResponse.setResponseValue( internalIntermediateResponse.getResponseValue() );

        // Transform the ldapResult
        intermediateResponse.setLdapResult( transformLdapResult( ( LdapResultImpl ) internalIntermediateResponse
            .getLdapResult() ) );

        return intermediateResponse;
    }


    /**
     * Transform a Internal ModifyResponse to a Codec ModifyResponse
     * 
     * @param internalMessage The incoming Internal ModifyResponse
     * @return The ModifyResponseCodec instance
     */
    private static LdapMessageCodec transformModifyResponse( InternalMessage internalMessage )
    {
        ModifyResponseImpl internalModifyResponse = ( ModifyResponseImpl ) internalMessage;

        ModifyResponseCodec modifyResponse = new ModifyResponseCodec();

        // Transform the ldapResult
        modifyResponse.setLdapResult( transformLdapResult( ( LdapResultImpl ) internalModifyResponse.getLdapResult() ) );

        return modifyResponse;
    }


    /**
     * Transform a Internal ModifyDNResponse to a Codec ModifyDNResponse
     * 
     * @param internalMessage The incoming Internal ModifyDNResponse
     * @return The ModifyDnResponseCodec instance
     */
    private static LdapMessageCodec transformModifyDNResponse( InternalMessage internalMessage )
    {
        ModifyDnResponseImpl internalModifyDNResponse = ( ModifyDnResponseImpl ) internalMessage;

        ModifyDNResponseCodec modifyDNResponse = new ModifyDNResponseCodec();

        // Transform the ldapResult
        modifyDNResponse.setLdapResult( transformLdapResult( ( LdapResultImpl ) internalModifyDNResponse
            .getLdapResult() ) );

        return modifyDNResponse;
    }


    /**
     * Transform a Internal SearchResponseDone to a Codec SearchResultDone
     * 
     * @param internalMessage The incoming Internal SearchResponseDone
     * @return The SearchResultDone instance
     */
    private static LdapMessageCodec transformSearchResultDone( InternalMessage internalMessage )
    {
        SearchResponseDoneImpl internalSearchResponseDone = ( SearchResponseDoneImpl ) internalMessage;
        SearchResultDoneCodec searchResultDone = new SearchResultDoneCodec();

        // Transform the ldapResult
        searchResultDone.setLdapResult( transformLdapResult( ( LdapResultImpl ) internalSearchResponseDone
            .getLdapResult() ) );

        // Set the operation into the LdapMessage
        return searchResultDone;
    }


    /**
     * Transform a Internal SearchResponseEntry to a Codec SearchResultEntry
     * 
     * @param internalMessage The incoming Internal SearchResponseEntry
     */
    private static LdapMessageCodec transformSearchResultEntry( InternalMessage internalMessage )
    {
        SearchResponseEntryImpl internalSearchResultResponse = ( SearchResponseEntryImpl ) internalMessage;
        SearchResultEntryCodec searchResultEntry = new SearchResultEntryCodec();

        // Internal : DN dn -> Codec : DN objectName
        searchResultEntry.setObjectName( internalSearchResultResponse.getObjectName() );

        // Internal : Attributes attributes -> Codec : ArrayList
        // partialAttributeList
        searchResultEntry.setEntry( internalSearchResultResponse.getEntry() );

        return searchResultEntry;
    }


    /**
     * Transform a Internal SearchResponseReference to a Codec
     * SearchResultReference
     * 
     * @param internalMessage The incoming Internal SearchResponseReference
     */
    private static LdapMessageCodec transformSearchResultReference( InternalMessage internalMessage )
    {
        SearchResponseReferenceImpl internalSearchResponseReference = ( SearchResponseReferenceImpl ) internalMessage;
        SearchResultReferenceCodec searchResultReference = new SearchResultReferenceCodec();

        // Internal : Referral m_referral -> Codec: ArrayList
        // searchResultReferences
        InternalReferral referrals = internalSearchResponseReference.getReferral();

        // Loop on all referals
        if ( referrals != null )
        {
            Collection<String> urls = referrals.getLdapUrls();

            if ( urls != null )
            {
                for ( String url:urls)
                {
                    try
                    {
                        searchResultReference.addSearchResultReference( new LdapURL( url ) );
                    }
                    catch ( LdapURLEncodingException luee )
                    {
                        LOG.warn( "The LdapURL " + url + " is incorrect : " + luee.getMessage() );
                    }
                }
            }
        }

        return searchResultReference;
    }


    /**
     * Transform the internal message to a codec message.
     * 
     * @param msg the message to transform
     * @return the msg transformed
     */
    public static Object transform( InternalMessage msg )
    {
        if ( IS_DEBUG )
        {
            LOG.debug( "Transforming message type " + msg.getType() );
        }

        LdapMessageCodec codecMessage = null;

        switch ( msg.getType() )
        {
            case SEARCH_RESULT_ENTRY :
                codecMessage = transformSearchResultEntry( msg );
                break;
                
            case SEARCH_RESULT_DONE :
                codecMessage = transformSearchResultDone( msg );
                break;
                
            case SEARCH_RESULT_REFERENCE :
                codecMessage = transformSearchResultReference( msg );
                break;
                
            case BIND_RESPONSE :
                codecMessage = transformBindResponse( msg );
                break;
                
            case BIND_REQUEST :
                codecMessage = transformBindRequest( msg );
                break;
                
            case ADD_RESPONSE :
                codecMessage = transformAddResponse( msg );
                break;
                
            case COMPARE_RESPONSE :
                codecMessage = transformCompareResponse( msg );
                break;
                
            case DEL_RESPONSE :
                codecMessage = transformDelResponse( msg );
                break;
         
            case MODIFY_RESPONSE :
                codecMessage = transformModifyResponse( msg );
                break;

            case MODIFYDN_RESPONSE :
                codecMessage = transformModifyDNResponse( msg );
                break;
                
            case EXTENDED_RESPONSE :
                codecMessage = transformExtendedResponse( msg );
                break;
                
            case INTERMEDIATE_RESPONSE :
                codecMessage = transformIntermediateResponse( msg );
                break;
        }

        codecMessage.setMessageId( msg.getMessageId() );

        // We also have to transform the controls...
        if ( !msg.getControls().isEmpty() )
        {
            transformControlsInternalToCodec( codecMessage, msg );
        }

        if ( IS_DEBUG )
        {
            LOG.debug( "Transformed message : " + codecMessage );
        }

        return codecMessage;
    }


    /**
     * TODO finish this implementation. Takes Codec Controls, transforming 
     * them to Internal Controls and populates the Internal message with them.
     *
     * @param codecMessage the Codec message
     * @param msg the Internal message
     */
    private static void transformControlsCodecToInternal( LdapMessageCodec codecMessage, InternalMessage internalMessage )
    {
        if ( codecMessage.getControls() == null )
        {
            return;
        }
        
        for ( final Control codecControl:codecMessage.getControls() )
        {
            internalMessage.add( codecControl );
        }
    }
    
    
    /**
     * Transforms the controls
     * @param codecMessage The Codec SearchResultReference to produce
     * @param msg The incoming Internal Message
     */
    private static void transformControlsInternalToCodec( LdapMessageCodec codecMessage, InternalMessage internalMessage )
    {
        if ( internalMessage.getControls() == null )
        {
            return;
        }
        
        for ( Control control:internalMessage.getControls().values() )
        {
            codecMessage.addControl( control );
        }
    }
}
