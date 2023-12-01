// $ANTLR 2.7.4: "schema.g" -> "AntlrSchemaParser.java"$

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
package org.apache.directory.shared.ldap.schema.syntax;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.directory.shared.ldap.schema.parsers.LdapComparatorDescription;
import org.apache.directory.shared.ldap.schema.DITContentRule;
import org.apache.directory.shared.ldap.schema.DITStructureRule;
import org.apache.directory.shared.ldap.schema.LdapSyntax;
import org.apache.directory.shared.ldap.schema.MatchingRule;
import org.apache.directory.shared.ldap.schema.MatchingRuleUse;
import org.apache.directory.shared.ldap.schema.NameForm;
import org.apache.directory.shared.ldap.schema.parsers.NormalizerDescription;
import org.apache.directory.shared.ldap.schema.parsers.ParserMonitor;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.parsers.SyntaxCheckerDescription;
import org.apache.directory.shared.ldap.schema.syntaxCheckers.OpenLdapObjectIdentifierMacro;
import org.apache.directory.shared.ldap.schema.ObjectClassTypeEnum;
import org.apache.directory.shared.ldap.schema.UsageEnum;


import antlr.TokenBuffer;
import antlr.TokenStreamException;
import antlr.TokenStreamIOException;
import antlr.ANTLRException;
import antlr.LLkParser;
import antlr.Token;
import antlr.TokenStream;
import antlr.RecognitionException;
import antlr.NoViableAltException;
import antlr.MismatchedTokenException;
import antlr.SemanticException;
import antlr.ParserSharedInputState;
import antlr.collections.impl.BitSet;

/**
 * An antlr generated schema main parser.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AntlrSchemaParser extends antlr.LLkParser       implements AntlrSchemaTokenTypes
 {

    private ParserMonitor monitor = null;
    private boolean isQuirksModeEnabled = false;
    public void setParserMonitor( ParserMonitor monitor )
    {
        this.monitor = monitor;
    }
    private void matchedProduction( String msg )
    {
        if ( null != monitor )
        {
            monitor.matchedProduction( msg );
        }
    }
    public void setQuirksMode( boolean enabled )
    {
        this.isQuirksModeEnabled = enabled;
    }
    public boolean isQuirksMode()
    {
        return this.isQuirksModeEnabled;
    }
    static class Extension
    {
        String key = "";
        List<String> values = new ArrayList<String>();
        
        public void addValue( String value )
        {
            this.values.add( value );
        }
    }
    static class NoidLen
    {
        String noid = "";
        int len = 0;
    }
    static class ElementTracker
    {
        Map<String, Integer> elementMap = new HashMap<String, Integer>();
        void track(String element, Token token) throws SemanticException 
        {
            if(elementMap.containsKey(element))
            {
                throw new SemanticException( element + " appears twice.", token.getFilename(), token.getLine() , token.getColumn() );
            }
            elementMap.put(element, new Integer(1));
        }
        boolean contains(String element) 
        {
            return elementMap.containsKey(element);
        }
    }


protected AntlrSchemaParser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public AntlrSchemaParser(TokenBuffer tokenBuf) {
  this(tokenBuf,3);
}

protected AntlrSchemaParser(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public AntlrSchemaParser(TokenStream lexer) {
  this(lexer,3);
}

public AntlrSchemaParser(ParserSharedInputState state) {
  super(state,3);
  tokenNames = _tokenNames;
}

	public final List<Object>  openLdapSchema() throws RecognitionException, TokenStreamException {
		List<Object> list = new ArrayList<Object>();
		
		
		AttributeType attributeType = null;
		ObjectClass objectClass = null;
		OpenLdapObjectIdentifierMacro oloid = null;
		
		
		{
		_loop1280:
		do {
			switch ( LA(1)) {
			case OBJECTIDENTIFIER:
			{
				oloid=openLdapObjectIdentifier();
				list.add( oloid );
				break;
			}
			case ATTRIBUTETYPE:
			{
				attributeType=openLdapAttributeType();
				list.add( attributeType );
				break;
			}
			case OBJECTCLASS:
			{
				objectClass=openLdapObjectClass();
				list.add( objectClass );
				break;
			}
			default:
			{
				break _loop1280;
			}
			}
		} while (true);
		}
		return list;
	}
	
	public final OpenLdapObjectIdentifierMacro  openLdapObjectIdentifier() throws RecognitionException, TokenStreamException {
		OpenLdapObjectIdentifierMacro oloid;
		
		Token  oi = null;
		
		matchedProduction( "openLdapObjectIdentifier()" );
		
		
		{
		oi = LT(1);
		match(OBJECTIDENTIFIER);
		
		String[] nameAndValue = oi.getText().split( " " );
		oloid = new OpenLdapObjectIdentifierMacro();
		oloid.setName( nameAndValue[0] );
		oloid.setRawOidOrNameSuffix( nameAndValue[1] );
		
		}
		return oloid;
	}
	
	public final AttributeType  openLdapAttributeType() throws RecognitionException, TokenStreamException {
		AttributeType attributeType;
		
		
		matchedProduction( "openLdapAttributeType()" );
		
		
		{
		match(ATTRIBUTETYPE);
		{
		attributeType=attributeTypeDescription();
		}
		}
		return attributeType;
	}
	
	public final ObjectClass  openLdapObjectClass() throws RecognitionException, TokenStreamException {
		ObjectClass objectClass;
		
		
		matchedProduction( "openLdapObjectClass()" );
		
		
		{
		match(OBJECTCLASS);
		{
		objectClass=objectClassDescription();
		}
		}
		return objectClass;
	}
	
/**
     * Production for matching object class descriptions. It is fault-tolerant
     * against element ordering.
     *
     * <pre>
     * ObjectClassDescription = LPAREN WSP
     *     numericoid                 ; object identifier
     *     [ SP "NAME" SP qdescrs ]   ; short names (descriptors)
     *     [ SP "DESC" SP qdstring ]  ; description
     *     [ SP "OBSOLETE" ]          ; not active
     *     [ SP "SUP" SP oids ]       ; superior object classes
     *     [ SP kind ]                ; kind of class
     *     [ SP "MUST" SP oids ]      ; attribute types
     *     [ SP "MAY" SP oids ]       ; attribute types
     *     extensions WSP RPAREN
     *
     * kind = "ABSTRACT" / "STRUCTURAL" / "AUXILIARY"
     * 
     * extensions = *( SP xstring SP qdstrings )
     * xstring = "X" HYPHEN 1*( ALPHA / HYPHEN / USCORE ) 
     * </pre>
    */
	public final ObjectClass  objectClassDescription() throws RecognitionException, TokenStreamException {
		ObjectClass objectClass;
		
		Token  oid = null;
		Token  name = null;
		Token  desc = null;
		Token  obsolete = null;
		Token  sup = null;
		Token  kind1 = null;
		Token  kind2 = null;
		Token  kind3 = null;
		Token  must = null;
		Token  may = null;
		Token  extension = null;
		
		matchedProduction( "objectClassDescription()" );
		ElementTracker et = new ElementTracker();
		
		
		{
		oid = LT(1);
		match(STARTNUMERICOID);
		objectClass = new ObjectClass(numericoid(oid.getText()));
		}
		{
		_loop1300:
		do {
			switch ( LA(1)) {
			case NAME:
			{
				{
				name = LT(1);
				match(NAME);
				et.track("NAME", name); objectClass.setNames(qdescrs(name.getText()));
				}
				break;
			}
			case DESC:
			{
				{
				desc = LT(1);
				match(DESC);
				et.track("DESC", desc); objectClass.setDescription(qdstring(desc.getText()));
				}
				break;
			}
			case OBSOLETE:
			{
				{
				obsolete = LT(1);
				match(OBSOLETE);
				et.track("OBSOLETE", obsolete); objectClass.setObsolete( true );
				}
				break;
			}
			case SUP:
			{
				{
				sup = LT(1);
				match(SUP);
				et.track("SUP", sup); objectClass.setSuperiorOids(oids(sup.getText()));
				}
				break;
			}
			case ABSTRACT:
			case STRUCTURAL:
			case AUXILIARY:
			{
				{
				switch ( LA(1)) {
				case ABSTRACT:
				{
					kind1 = LT(1);
					match(ABSTRACT);
					et.track("KIND", kind1); objectClass.setType( ObjectClassTypeEnum.ABSTRACT );
					break;
				}
				case STRUCTURAL:
				{
					kind2 = LT(1);
					match(STRUCTURAL);
					et.track("KIND", kind2); objectClass.setType( ObjectClassTypeEnum.STRUCTURAL );
					break;
				}
				case AUXILIARY:
				{
					kind3 = LT(1);
					match(AUXILIARY);
					et.track("KIND", kind3); objectClass.setType( ObjectClassTypeEnum.AUXILIARY );
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				break;
			}
			case MUST:
			{
				{
				must = LT(1);
				match(MUST);
				et.track("MUST", must); objectClass.setMustAttributeTypeOids(oids(must.getText()));
				}
				break;
			}
			case MAY:
			{
				{
				may = LT(1);
				match(MAY);
				et.track("MAY", may); objectClass.setMayAttributeTypeOids(oids(may.getText()));
				}
				break;
			}
			case EXTENSION:
			{
				{
				extension = LT(1);
				match(EXTENSION);
				
				Extension ex = extension(extension.getText());
				et.track(ex.key, extension); 
				objectClass.addExtension(ex.key, ex.values); 
				
				}
				break;
			}
			default:
			{
				break _loop1300;
			}
			}
		} while (true);
		}
		match(RPAR);
		return objectClass;
	}
	
/**
     * Production for matching attribute type descriptions. It is fault-tolerant
     * against element ordering.
     *
     * <pre>
     * AttributeTypeDescription = LPAREN WSP
     *     numericoid                    ; object identifier
     *     [ SP "NAME" SP qdescrs ]      ; short names (descriptors)
     *     [ SP "DESC" SP qdstring ]     ; description
     *     [ SP "OBSOLETE" ]             ; not active
     *     [ SP "SUP" SP oid ]           ; supertype
     *     [ SP "EQUALITY" SP oid ]      ; equality matching rule
     *     [ SP "ORDERING" SP oid ]      ; ordering matching rule
     *     [ SP "SUBSTR" SP oid ]        ; substrings matching rule
     *     [ SP "SYNTAX" SP noidlen ]    ; value syntax
     *     [ SP "SINGLE-VALUE" ]         ; single-value
     *     [ SP "COLLECTIVE" ]           ; collective
     *     [ SP "NO-USER-MODIFICATION" ] ; not user modifiable
     *     [ SP "USAGE" SP usage ]       ; usage
     *     extensions WSP RPAREN         ; extensions
     * 
     * usage = "userApplications"     /  ; user
     *         "directoryOperation"   /  ; directory operational
     *         "distributedOperation" /  ; DSA-shared operational
     *         "dSAOperation"            ; DSA-specific operational     
     * 
     * extensions = *( SP xstring SP qdstrings )
     * xstring = "X" HYPHEN 1*( ALPHA / HYPHEN / USCORE ) 
     * </pre>
    */
	public final AttributeType  attributeTypeDescription() throws RecognitionException, TokenStreamException {
		AttributeType attributeType;
		
		Token  oid = null;
		Token  name = null;
		Token  desc = null;
		Token  obsolete = null;
		Token  superior = null;
		Token  equality = null;
		Token  ordering = null;
		Token  substring = null;
		Token  syntax = null;
		Token  singleValued = null;
		Token  collective = null;
		Token  noUserModification = null;
		Token  usage1 = null;
		Token  usage2 = null;
		Token  usage3 = null;
		Token  usage4 = null;
		Token  extension = null;
		
		matchedProduction( "attributeTypeDescription()" );
		ElementTracker et = new ElementTracker();
		
		
		{
		oid = LT(1);
		match(STARTNUMERICOID);
		attributeType = new AttributeType(numericoid(oid.getText()));
		}
		{
		_loop1319:
		do {
			switch ( LA(1)) {
			case NAME:
			{
				{
				name = LT(1);
				match(NAME);
				et.track("NAME", name); attributeType.setNames(qdescrs(name.getText()));
				}
				break;
			}
			case DESC:
			{
				{
				desc = LT(1);
				match(DESC);
				et.track("DESC", desc); attributeType.setDescription(qdstring(desc.getText()));
				}
				break;
			}
			case OBSOLETE:
			{
				{
				obsolete = LT(1);
				match(OBSOLETE);
				et.track("OBSOLETE", obsolete); attributeType.setObsolete( true );
				}
				break;
			}
			case SUP:
			{
				{
				superior = LT(1);
				match(SUP);
				et.track("SUP", superior); attributeType.setSuperiorOid(oid(superior.getText()));
				}
				break;
			}
			case EQUALITY:
			{
				{
				equality = LT(1);
				match(EQUALITY);
				et.track("EQUALITY", equality); attributeType.setEqualityOid(oid(equality.getText()));
				}
				break;
			}
			case ORDERING:
			{
				{
				ordering = LT(1);
				match(ORDERING);
				et.track("ORDERING", ordering); attributeType.setOrderingOid(oid(ordering.getText()));
				}
				break;
			}
			case SUBSTR:
			{
				{
				substring = LT(1);
				match(SUBSTR);
				et.track("SUBSTR", substring); attributeType.setSubstringOid(oid(substring.getText()));
				}
				break;
			}
			case SYNTAX:
			{
				{
				syntax = LT(1);
				match(SYNTAX);
				
				et.track("SYNTAX", syntax); 
				NoidLen noidlen = noidlen(syntax.getText());
				attributeType.setSyntaxOid(noidlen.noid); 
				attributeType.setSyntaxLength(noidlen.len);
				
				}
				break;
			}
			case SINGLE_VALUE:
			{
				{
				singleValued = LT(1);
				match(SINGLE_VALUE);
				et.track("SINGLE_VALUE", singleValued); attributeType.setSingleValued( true );
				}
				break;
			}
			case COLLECTIVE:
			{
				{
				collective = LT(1);
				match(COLLECTIVE);
				et.track("COLLECTIVE", collective); attributeType.setCollective( true );
				}
				break;
			}
			case NO_USER_MODIFICATION:
			{
				{
				noUserModification = LT(1);
				match(NO_USER_MODIFICATION);
				et.track("NO_USER_MODIFICATION", noUserModification); attributeType.setUserModifiable( false );
				}
				break;
			}
			case USAGE:
			{
				{
				if ((LA(1)==USAGE) && (LA(2)==WHSP||LA(2)==USER_APPLICATIONS)) {
					usage1 = LT(1);
					match(USAGE);
					{
					_loop1317:
					do {
						if ((LA(1)==WHSP)) {
							match(WHSP);
						}
						else {
							break _loop1317;
						}
						
					} while (true);
					}
					match(USER_APPLICATIONS);
					et.track("USAGE", usage1); attributeType.setUsage( UsageEnum.USER_APPLICATIONS );
				}
				else if ((LA(1)==USAGE) && (LA(2)==DIRECTORY_OPERATION)) {
					usage2 = LT(1);
					match(USAGE);
					match(DIRECTORY_OPERATION);
					et.track("USAGE", usage2); attributeType.setUsage( UsageEnum.DIRECTORY_OPERATION );
				}
				else if ((LA(1)==USAGE) && (LA(2)==DISTRIBUTED_OPERATION)) {
					usage3 = LT(1);
					match(USAGE);
					match(DISTRIBUTED_OPERATION);
					et.track("USAGE", usage3); attributeType.setUsage( UsageEnum.DISTRIBUTED_OPERATION );
				}
				else if ((LA(1)==USAGE) && (LA(2)==DSA_OPERATION)) {
					usage4 = LT(1);
					match(USAGE);
					match(DSA_OPERATION);
					et.track("USAGE", usage4); attributeType.setUsage( UsageEnum.DSA_OPERATION );
				}
				else {
					throw new NoViableAltException(LT(1), getFilename());
				}
				
				}
				break;
			}
			case EXTENSION:
			{
				{
				extension = LT(1);
				match(EXTENSION);
				
				Extension ex = extension(extension.getText());
				et.track(ex.key, extension); 
				attributeType.addExtension(ex.key, ex.values); 
				
				}
				break;
			}
			default:
			{
				break _loop1319;
			}
			}
		} while (true);
		}
		match(RPAR);
		
		if( !isQuirksModeEnabled )
		{
		// semantic check: required elements
		if( !et.contains("SYNTAX") && !et.contains("SUP") ) 
		{
		throw new SemanticException( "One of SYNTAX or SUP is required", null, 0, 0 );
		}
		
		// COLLECTIVE requires USAGE userApplications
		if ( attributeType.isCollective() && ( attributeType.getUsage() != UsageEnum.USER_APPLICATIONS ) )
		{
		throw new SemanticException( "COLLECTIVE requires USAGE userApplications", null, 0, 0 );
		}
		
		// NO-USER-MODIFICATION requires an operational USAGE.
		if ( !attributeType.isUserModifiable() && ( attributeType.getUsage() == UsageEnum.USER_APPLICATIONS ) )
		{
		throw new SemanticException( "NO-USER-MODIFICATION requires an operational USAGE", null, 0, 0 );
		}
		}
		
		return attributeType;
	}
	
/**
     * Production for matching ldap syntax descriptions. It is fault-tolerant
     * against element ordering.
     *
     * <pre>
     * SyntaxDescription = LPAREN WSP
     *    numericoid                 ; object identifier
     *    [ SP "DESC" SP qdstring ]  ; description
     *    extensions WSP RPAREN      ; extensions
     * </pre>
    */
	public final LdapSyntax  ldapSyntaxDescription() throws RecognitionException, TokenStreamException {
		LdapSyntax ldapSyntax;
		
		Token  oid = null;
		Token  name = null;
		Token  desc = null;
		Token  extension = null;
		
		matchedProduction( "ldapSyntaxDescription()" );
		ElementTracker et = new ElementTracker();
		
		
		{
		oid = LT(1);
		match(STARTNUMERICOID);
		ldapSyntax = new LdapSyntax(numericoid(oid.getText()));
		}
		{
		_loop1326:
		do {
			switch ( LA(1)) {
			case NAME:
			{
				{
				name = LT(1);
				match(NAME);
				et.track("NAME", name); ldapSyntax.setNames(qdescrs(name.getText()));
				}
				break;
			}
			case DESC:
			{
				{
				desc = LT(1);
				match(DESC);
				et.track("DESC", desc); ldapSyntax.setDescription(qdstring(desc.getText()));
				}
				break;
			}
			case EXTENSION:
			{
				{
				extension = LT(1);
				match(EXTENSION);
				
				Extension ex = extension(extension.getText());
				et.track(ex.key, extension); 
				ldapSyntax.addExtension(ex.key, ex.values); 
				
				}
				break;
			}
			default:
			{
				break _loop1326;
			}
			}
		} while (true);
		}
		match(RPAR);
		return ldapSyntax;
	}
	
/**
     * Production for matching rule descriptions. It is fault-tolerant
     * against element ordering.
     *
     * <pre>
     * MatchingRuleDescription = LPAREN WSP
     *    numericoid                 ; object identifier
     *    [ SP "NAME" SP qdescrs ]   ; short names (descriptors)
     *    [ SP "DESC" SP qdstring ]  ; description
     *    [ SP "OBSOLETE" ]          ; not active
     *    SP "SYNTAX" SP numericoid  ; assertion syntax
     *    extensions WSP RPAREN      ; extensions
     * </pre>
    */
	public final MatchingRule  matchingRuleDescription() throws RecognitionException, TokenStreamException {
		MatchingRule matchingRule;
		
		Token  oid = null;
		Token  name = null;
		Token  desc = null;
		Token  obsolete = null;
		Token  syntax = null;
		Token  extension = null;
		
		matchedProduction( "matchingRuleDescription()" );
		ElementTracker et = new ElementTracker();
		
		
		{
		oid = LT(1);
		match(STARTNUMERICOID);
		matchingRule = new MatchingRule(numericoid(oid.getText()));
		}
		{
		_loop1335:
		do {
			switch ( LA(1)) {
			case NAME:
			{
				{
				name = LT(1);
				match(NAME);
				et.track("NAME", name); matchingRule.setNames(qdescrs(name.getText()));
				}
				break;
			}
			case DESC:
			{
				{
				desc = LT(1);
				match(DESC);
				et.track("DESC", desc); matchingRule.setDescription(qdstring(desc.getText()));
				}
				break;
			}
			case OBSOLETE:
			{
				{
				obsolete = LT(1);
				match(OBSOLETE);
				et.track("OBSOLETE", obsolete); matchingRule.setObsolete( true );
				}
				break;
			}
			case SYNTAX:
			{
				{
				syntax = LT(1);
				match(SYNTAX);
				et.track("SYNTAX", syntax); matchingRule.setSyntaxOid(numericoid(syntax.getText()));
				}
				break;
			}
			case EXTENSION:
			{
				{
				extension = LT(1);
				match(EXTENSION);
				
				Extension ex = extension(extension.getText());
				et.track(ex.key, extension); 
				matchingRule.addExtension(ex.key, ex.values); 
				
				}
				break;
			}
			default:
			{
				break _loop1335;
			}
			}
		} while (true);
		}
		match(RPAR);
		
		if( !isQuirksModeEnabled )
		{    
		// semantic check: required elements
		if( !et.contains("SYNTAX") ) {
		throw new SemanticException( "SYNTAX is required", null, 0, 0 );
		}
		}
		
		return matchingRule;
	}
	
/**
     * Production for matching rule use descriptions. It is fault-tolerant
     * against element ordering.
     *
     * <pre>
     * MatchingRuleUseDescription = LPAREN WSP
     *    numericoid                 ; object identifier
     *    [ SP "NAME" SP qdescrs ]   ; short names (descriptors)
     *    [ SP "DESC" SP qdstring ]  ; description
     *    [ SP "OBSOLETE" ]          ; not active
     *    SP "APPLIES" SP oids       ; attribute types
     *    extensions WSP RPAREN      ; extensions
     * </pre>
    */
	public final MatchingRuleUse  matchingRuleUseDescription() throws RecognitionException, TokenStreamException {
		MatchingRuleUse matchingRuleUse;
		
		Token  oid = null;
		Token  name = null;
		Token  desc = null;
		Token  obsolete = null;
		Token  applies = null;
		Token  extension = null;
		
		matchedProduction( "matchingRuleUseDescription()" );
		ElementTracker et = new ElementTracker();
		
		
		{
		oid = LT(1);
		match(STARTNUMERICOID);
		matchingRuleUse = new MatchingRuleUse(numericoid(oid.getText()));
		}
		{
		_loop1344:
		do {
			switch ( LA(1)) {
			case NAME:
			{
				{
				name = LT(1);
				match(NAME);
				et.track("NAME", name); matchingRuleUse.setNames(qdescrs(name.getText()));
				}
				break;
			}
			case DESC:
			{
				{
				desc = LT(1);
				match(DESC);
				et.track("DESC", desc); matchingRuleUse.setDescription(qdstring(desc.getText()));
				}
				break;
			}
			case OBSOLETE:
			{
				{
				obsolete = LT(1);
				match(OBSOLETE);
				et.track("OBSOLETE", obsolete); matchingRuleUse.setObsolete( true );
				}
				break;
			}
			case APPLIES:
			{
				{
				applies = LT(1);
				match(APPLIES);
				et.track("APPLIES", applies); matchingRuleUse.setApplicableAttributeOids(oids(applies.getText()));
				}
				break;
			}
			case EXTENSION:
			{
				{
				extension = LT(1);
				match(EXTENSION);
				
				Extension ex = extension(extension.getText());
				et.track(ex.key, extension); 
				matchingRuleUse.addExtension(ex.key, ex.values); 
				
				}
				break;
			}
			default:
			{
				break _loop1344;
			}
			}
		} while (true);
		}
		match(RPAR);
		
		if( !isQuirksModeEnabled )
		{
		// semantic check: required elements
		if( !et.contains("APPLIES") ) {
		throw new SemanticException( "APPLIES is required", null, 0, 0 );
		}
		}
		
		return matchingRuleUse;
	}
	
/**
     * Production for DIT content rule descriptions. It is fault-tolerant
     * against element ordering.
     *
     * <pre>
     * DITContentRuleDescription = LPAREN WSP
     *    numericoid                 ; object identifier
     *    [ SP "NAME" SP qdescrs ]   ; short names (descriptors)
     *    [ SP "DESC" SP qdstring ]  ; description
     *    [ SP "OBSOLETE" ]          ; not active
     *    [ SP "AUX" SP oids ]       ; auxiliary object classes
     *    [ SP "MUST" SP oids ]      ; attribute types
     *    [ SP "MAY" SP oids ]       ; attribute types
     *    [ SP "NOT" SP oids ]       ; attribute types
     *    extensions WSP RPAREN      ; extensions
     * </pre>
    */
	public final DITContentRule  ditContentRuleDescription() throws RecognitionException, TokenStreamException {
		DITContentRule ditContentRule;
		
		Token  oid = null;
		Token  name = null;
		Token  desc = null;
		Token  obsolete = null;
		Token  aux = null;
		Token  must = null;
		Token  may = null;
		Token  not = null;
		Token  extension = null;
		
		matchedProduction( "ditContentRuleDescription()" );
		ElementTracker et = new ElementTracker();
		
		
		{
		oid = LT(1);
		match(STARTNUMERICOID);
		ditContentRule = new DITContentRule(numericoid(oid.getText()));
		}
		{
		_loop1356:
		do {
			switch ( LA(1)) {
			case NAME:
			{
				{
				name = LT(1);
				match(NAME);
				et.track("NAME", name); ditContentRule.setNames(qdescrs(name.getText()));
				}
				break;
			}
			case DESC:
			{
				{
				desc = LT(1);
				match(DESC);
				et.track("DESC", desc); ditContentRule.setDescription(qdstring(desc.getText()));
				}
				break;
			}
			case OBSOLETE:
			{
				{
				obsolete = LT(1);
				match(OBSOLETE);
				et.track("OBSOLETE", obsolete); ditContentRule.setObsolete( true );
				}
				break;
			}
			case AUX:
			{
				{
				aux = LT(1);
				match(AUX);
				et.track("AUX", aux); ditContentRule.setAuxObjectClassOids(oids(aux.getText()));
				}
				break;
			}
			case MUST:
			{
				{
				must = LT(1);
				match(MUST);
				et.track("MUST", must); ditContentRule.setMustAttributeTypeOids(oids(must.getText()));
				}
				break;
			}
			case MAY:
			{
				{
				may = LT(1);
				match(MAY);
				et.track("MAY", may); ditContentRule.setMayAttributeTypeOids(oids(may.getText()));
				}
				break;
			}
			case NOT:
			{
				{
				not = LT(1);
				match(NOT);
				et.track("NOT", not); ditContentRule.setNotAttributeTypeOids(oids(not.getText()));
				}
				break;
			}
			case EXTENSION:
			{
				{
				extension = LT(1);
				match(EXTENSION);
				
				Extension ex = extension(extension.getText());
				et.track(ex.key, extension); 
				ditContentRule.addExtension(ex.key, ex.values); 
				
				}
				break;
			}
			default:
			{
				break _loop1356;
			}
			}
		} while (true);
		}
		match(RPAR);
		return ditContentRule;
	}
	
/**
     * Production for DIT structure rules descriptions. It is fault-tolerant
     * against element ordering.
     *
     * <pre>
     * DITStructureRuleDescription = LPAREN WSP
     *   ruleid                     ; rule identifier
     *   [ SP "NAME" SP qdescrs ]   ; short names (descriptors)
     *   [ SP "DESC" SP qdstring ]  ; description
     *   [ SP "OBSOLETE" ]          ; not active
     *   SP "FORM" SP oid           ; NameForm
     *   [ SP "SUP" ruleids ]       ; superior rules
     *   extensions WSP RPAREN      ; extensions
     *
     * ruleids = ruleid / ( LPAREN WSP ruleidlist WSP RPAREN )
     * ruleidlist = ruleid *( SP ruleid )
     * ruleid = number
     * </pre>
    */
	public final DITStructureRule  ditStructureRuleDescription() throws RecognitionException, TokenStreamException {
		DITStructureRule ditStructureRule;
		
		Token  ruleid = null;
		Token  name = null;
		Token  desc = null;
		Token  obsolete = null;
		Token  form = null;
		Token  sup = null;
		Token  extension = null;
		
		matchedProduction( "ditStructureRuleDescription()" );
		ElementTracker et = new ElementTracker();
		
		
		{
		ruleid = LT(1);
		match(STARTNUMERICOID);
		ditStructureRule = new DITStructureRule(ruleid(ruleid.getText()));
		}
		{
		_loop1366:
		do {
			switch ( LA(1)) {
			case NAME:
			{
				{
				name = LT(1);
				match(NAME);
				et.track("NAME", name); ditStructureRule.setNames(qdescrs(name.getText()));
				}
				break;
			}
			case DESC:
			{
				{
				desc = LT(1);
				match(DESC);
				et.track("DESC", desc); ditStructureRule.setDescription(qdstring(desc.getText()));
				}
				break;
			}
			case OBSOLETE:
			{
				{
				obsolete = LT(1);
				match(OBSOLETE);
				et.track("OBSOLETE", obsolete); ditStructureRule.setObsolete( true );
				}
				break;
			}
			case FORM:
			{
				{
				form = LT(1);
				match(FORM);
				et.track("FORM", form); ditStructureRule.setForm(oid(form.getText()));
				}
				break;
			}
			case SUP:
			{
				{
				sup = LT(1);
				match(SUP);
				et.track("SUP", sup); ditStructureRule.setSuperRules(ruleids(sup.getText()));
				}
				break;
			}
			case EXTENSION:
			{
				{
				extension = LT(1);
				match(EXTENSION);
				
				Extension ex = extension(extension.getText());
				et.track(ex.key, extension); 
				ditStructureRule.addExtension(ex.key, ex.values); 
				
				}
				break;
			}
			default:
			{
				break _loop1366;
			}
			}
		} while (true);
		}
		match(RPAR);
		
		if( !isQuirksModeEnabled )
		{
		// semantic check: required elements
		if( !et.contains("FORM") ) {
		throw new SemanticException( "FORM is required", null, 0, 0 );
		}
		}
		
		return ditStructureRule;
	}
	
/**
     * Production for name form descriptions. It is fault-tolerant
     * against element ordering.
     *
     * <pre>
     * NameFormDescription = LPAREN WSP
     *    numericoid                 ; object identifier
     *    [ SP "NAME" SP qdescrs ]   ; short names (descriptors)
     *    [ SP "DESC" SP qdstring ]  ; description
     *    [ SP "OBSOLETE" ]          ; not active
     *    SP "OC" SP oid             ; structural object class
     *    SP "MUST" SP oids          ; attribute types
     *    [ SP "MAY" SP oids ]       ; attribute types
     *    extensions WSP RPAREN      ; extensions
     * </pre>
    */
	public final NameForm  nameFormDescription() throws RecognitionException, TokenStreamException {
		NameForm nameForm;
		
		Token  oid = null;
		Token  name = null;
		Token  desc = null;
		Token  obsolete = null;
		Token  oc = null;
		Token  must = null;
		Token  may = null;
		Token  extension = null;
		
		matchedProduction( "nameFormDescription()" );
		ElementTracker et = new ElementTracker();
		
		
		{
		oid = LT(1);
		match(STARTNUMERICOID);
		nameForm = new NameForm(numericoid(oid.getText()));
		}
		{
		_loop1377:
		do {
			switch ( LA(1)) {
			case NAME:
			{
				{
				name = LT(1);
				match(NAME);
				et.track("NAME", name); nameForm.setNames(qdescrs(name.getText()));
				}
				break;
			}
			case DESC:
			{
				{
				desc = LT(1);
				match(DESC);
				et.track("DESC", desc); nameForm.setDescription(qdstring(desc.getText()));
				}
				break;
			}
			case OBSOLETE:
			{
				{
				obsolete = LT(1);
				match(OBSOLETE);
				et.track("OBSOLETE", obsolete); nameForm.setObsolete( true );
				}
				break;
			}
			case OC:
			{
				{
				oc = LT(1);
				match(OC);
				et.track("OC", oc); nameForm.setStructuralObjectClassOid(oid(oc.getText()));
				}
				break;
			}
			case MUST:
			{
				{
				must = LT(1);
				match(MUST);
				et.track("MUST", must); nameForm.setMustAttributeTypeOids(oids(must.getText()));
				}
				break;
			}
			case MAY:
			{
				{
				may = LT(1);
				match(MAY);
				et.track("MAY", may); nameForm.setMayAttributeTypeOids(oids(may.getText()));
				}
				break;
			}
			case EXTENSION:
			{
				{
				extension = LT(1);
				match(EXTENSION);
				
				Extension ex = extension(extension.getText());
				et.track(ex.key, extension); 
				nameForm.addExtension(ex.key, ex.values); 
				
				}
				break;
			}
			default:
			{
				break _loop1377;
			}
			}
		} while (true);
		}
		match(RPAR);
		
		if( !isQuirksModeEnabled )
		{
		// semantic check: required elements
		if( !et.contains("MUST") ) {
		throw new SemanticException( "MUST is required", null, 0, 0 );
		}
		if( !et.contains("OC") ) {
		throw new SemanticException( "OC is required", null, 0, 0 );
		}
		
		// semantic check: MUST and MAY must be disjoint
		//List<String> aList = new ArrayList<String>( nfd.getMustAttributeTypes() );
		//aList.retainAll( nfd.getMayAttributeTypes() );
		//if( !aList.isEmpty() ) 
		//{
		//    throw new SemanticException( "MUST and MAY must be disjoint, "+aList.get( 0 )+" appears in both", null, 0, 0 );
		//}
		}
		
		return nameForm;
	}
	
/**
     * Production for comparator descriptions. It is fault-tolerant
     * against element ordering.
     *
     * <pre>
     * LdapComparator = LPAREN WSP
     *       numericoid                           ; object identifier
     *       [ SP "DESC" SP qdstring ]            ; description
     *       SP "FQCN" SP fqcn                    ; fully qualified class name
     *       [ SP "BYTECODE" SP base64 ]          ; optional base64 encoded bytecode
     *       extensions WSP RPAREN                ; extensions
     * 
     * base64          = *(4base64-char)
     * base64-char     = ALPHA / DIGIT / "+" / "/"
     * fqcn = fqcnComponent 1*( DOT fqcnComponent )
     * fqcnComponent = ???
     * </pre>
    */
	public final LdapComparatorDescription  ldapComparator() throws RecognitionException, TokenStreamException {
		LdapComparatorDescription lcd;
		
		Token  oid = null;
		Token  desc = null;
		Token  fqcn = null;
		Token  bytecode = null;
		Token  extension = null;
		
		matchedProduction( "ldapComparator()" );
		ElementTracker et = new ElementTracker();
		
		
		{
		oid = LT(1);
		match(STARTNUMERICOID);
		lcd = new LdapComparatorDescription(numericoid(oid.getText()));
		}
		{
		_loop1385:
		do {
			switch ( LA(1)) {
			case DESC:
			{
				{
				desc = LT(1);
				match(DESC);
				et.track("DESC", desc); lcd.setDescription(qdstring(desc.getText()));
				}
				break;
			}
			case FQCN:
			{
				{
				fqcn = LT(1);
				match(FQCN);
				et.track("FQCN", fqcn); lcd.setFqcn(fqcn.getText());
				}
				break;
			}
			case BYTECODE:
			{
				{
				bytecode = LT(1);
				match(BYTECODE);
				et.track("BYTECODE", bytecode); lcd.setBytecode(bytecode.getText());
				}
				break;
			}
			case EXTENSION:
			{
				{
				extension = LT(1);
				match(EXTENSION);
				
				Extension ex = extension(extension.getText());
				et.track(ex.key, extension); 
				lcd.addExtension(ex.key, ex.values); 
				
				}
				break;
			}
			default:
			{
				break _loop1385;
			}
			}
		} while (true);
		}
		match(RPAR);
		
		if( !isQuirksModeEnabled )
		{
		// semantic check: required elements
		if( !et.contains("FQCN") ) {
		throw new SemanticException( "FQCN is required", null, 0, 0 );
		}
		
		// semantic check: length should be divisible by 4
		if( ( lcd.getBytecode() != null ) && ( lcd.getBytecode().length() % 4 != 0 ) ) {
		throw new SemanticException( "BYTECODE must be divisible by 4", null, 0, 0 );
		}
		}
		
		return lcd;
	}
	
/**
     * Production for normalizer descriptions. It is fault-tolerant
     * against element ordering.
     *
     * <pre>
     * NormalizerDescription = LPAREN WSP
     *       numericoid                           ; object identifier
     *       [ SP "DESC" SP qdstring ]            ; description
     *       SP "FQCN" SP fqcn                    ; fully qualified class name
     *       [ SP "BYTECODE" SP base64 ]          ; optional base64 encoded bytecode
     *       extensions WSP RPAREN                ; extensions
     * 
     * base64          = *(4base64-char)
     * base64-char     = ALPHA / DIGIT / "+" / "/"
     * fqcn = fqcnComponent 1*( DOT fqcnComponent )
     * fqcnComponent = ???
     * </pre>
    */
	public final NormalizerDescription  normalizerDescription() throws RecognitionException, TokenStreamException {
		NormalizerDescription nd;
		
		Token  oid = null;
		Token  desc = null;
		Token  fqcn = null;
		Token  bytecode = null;
		Token  extension = null;
		
		matchedProduction( "normalizerDescription()" );
		ElementTracker et = new ElementTracker();
		
		
		{
		oid = LT(1);
		match(STARTNUMERICOID);
		nd = new NormalizerDescription(numericoid(oid.getText()));
		}
		{
		_loop1393:
		do {
			switch ( LA(1)) {
			case DESC:
			{
				{
				desc = LT(1);
				match(DESC);
				et.track("DESC", desc); nd.setDescription(qdstring(desc.getText()));
				}
				break;
			}
			case FQCN:
			{
				{
				fqcn = LT(1);
				match(FQCN);
				et.track("FQCN", fqcn); nd.setFqcn(fqcn.getText());
				}
				break;
			}
			case BYTECODE:
			{
				{
				bytecode = LT(1);
				match(BYTECODE);
				et.track("BYTECODE", bytecode); nd.setBytecode(bytecode.getText());
				}
				break;
			}
			case EXTENSION:
			{
				{
				extension = LT(1);
				match(EXTENSION);
				
				Extension ex = extension(extension.getText());
				et.track(ex.key, extension); 
				nd.addExtension(ex.key, ex.values); 
				
				}
				break;
			}
			default:
			{
				break _loop1393;
			}
			}
		} while (true);
		}
		match(RPAR);
		
		if( !isQuirksModeEnabled )
		{
		// semantic check: required elements
		if( !et.contains("FQCN") ) {
		throw new SemanticException( "FQCN is required", null, 0, 0 );
		}
		
		// semantic check: length should be divisible by 4
		if( nd.getBytecode() != null && ( nd.getBytecode().length() % 4 != 0 ) ) {
		throw new SemanticException( "BYTECODE must be divisible by 4", null, 0, 0 );
		}     
		}   
		
		return nd;
	}
	
/**
     * Production for syntax checker descriptions. It is fault-tolerant
     * against element ordering.
     *
     * <pre>
     * SyntaxCheckerDescription = LPAREN WSP
     *       numericoid                           ; object identifier
     *       [ SP "DESC" SP qdstring ]            ; description
     *       SP "FQCN" SP fqcn                    ; fully qualified class name
     *       [ SP "BYTECODE" SP base64 ]          ; optional base64 encoded bytecode
     *       extensions WSP RPAREN                ; extensions
     * 
     * base64          = *(4base64-char)
     * base64-char     = ALPHA / DIGIT / "+" / "/"
     * fqcn = fqcnComponent 1*( DOT fqcnComponent )
     * fqcnComponent = ???
     * </pre>
    */
	public final SyntaxCheckerDescription  syntaxCheckerDescription() throws RecognitionException, TokenStreamException {
		SyntaxCheckerDescription scd;
		
		Token  oid = null;
		Token  desc = null;
		Token  fqcn = null;
		Token  bytecode = null;
		Token  extension = null;
		
		matchedProduction( "syntaxCheckerDescription()" );
		ElementTracker et = new ElementTracker();
		
		
		{
		oid = LT(1);
		match(STARTNUMERICOID);
		scd = new SyntaxCheckerDescription(numericoid(oid.getText()));
		}
		{
		_loop1401:
		do {
			switch ( LA(1)) {
			case DESC:
			{
				{
				desc = LT(1);
				match(DESC);
				et.track("DESC", desc); scd.setDescription(qdstring(desc.getText()));
				}
				break;
			}
			case FQCN:
			{
				{
				fqcn = LT(1);
				match(FQCN);
				et.track("FQCN", fqcn); scd.setFqcn(fqcn.getText());
				}
				break;
			}
			case BYTECODE:
			{
				{
				bytecode = LT(1);
				match(BYTECODE);
				et.track("BYTECODE", bytecode); scd.setBytecode(bytecode.getText());
				}
				break;
			}
			case EXTENSION:
			{
				{
				extension = LT(1);
				match(EXTENSION);
				
				Extension ex = extension(extension.getText());
				et.track(ex.key, extension); 
				scd.addExtension(ex.key, ex.values); 
				
				}
				break;
			}
			default:
			{
				break _loop1401;
			}
			}
		} while (true);
		}
		match(RPAR);
		
		if( !isQuirksModeEnabled )
		{
		// semantic check: required elements
		if( !et.contains("FQCN") ) {
		throw new SemanticException( "FQCN is required", null, 0, 0 );
		}
		
		// semantic check: length should be divisible by 4
		if( scd.getBytecode() != null && ( scd.getBytecode().length() % 4 != 0 ) ) {
		throw new SemanticException( "BYTECODE must be divisible by 4", null, 0, 0 );
		}  
		}      
		
		return scd;
	}
	
	public final NoidLen  noidlen(
		String s
	) throws RecognitionException, TokenStreamException {
		NoidLen noidlen;
		
		
		matchedProduction( "noidlen()" );
		AntlrSchemaValueLexer lexer = new AntlrSchemaValueLexer(new StringReader(s));
		AntlrSchemaValueParser parser = new AntlrSchemaValueParser(lexer);
		parser.setParserMonitor(monitor);
		noidlen = isQuirksModeEnabled ? parser.quirksNoidlen() : parser.noidlen();
		
		
		return noidlen;
	}
	
	public final Extension  extension(
		String s
	) throws RecognitionException, TokenStreamException {
		Extension extension;
		
		
		matchedProduction( "extension()" );
		AntlrSchemaExtensionLexer lexer = new AntlrSchemaExtensionLexer(new StringReader(s));
		AntlrSchemaExtensionParser parser = new AntlrSchemaExtensionParser(lexer);
		extension = parser.extension();
		
		
		return extension;
	}
	
	public final String  numericoid(
		String s
	) throws RecognitionException, TokenStreamException {
		String numericoid;
		
		
		matchedProduction( "numericoid()");
		if(isQuirksModeEnabled)
		{
		numericoid = oid(s);
		}
		else
		{
			        AntlrSchemaValueLexer lexer = new AntlrSchemaValueLexer(new StringReader(s));
			        AntlrSchemaValueParser parser = new AntlrSchemaValueParser(lexer);
			        parser.setParserMonitor(monitor);
			        numericoid = parser.numericoid();
		}
		
		
		return numericoid;
	}
	
	public final String  oid(
		String s
	) throws RecognitionException, TokenStreamException {
		String oid;
		
		
		matchedProduction( "oid()" );
		List<String> oids = oids(s);
		if( oids.size() != 1 ) 
		{
		throw new SemanticException( "Exactly one OID expected", null, 0, 0 );
		}
		oid = oids.get(0);
		
		
		return oid;
	}
	
	public final List<String>  oids(
		String s
	) throws RecognitionException, TokenStreamException {
		List<String> oids;
		
		
		matchedProduction( "oids()" );
		if(isQuirksModeEnabled)
		{
		oids = qdescrs(s);
		}
		else
		{
			        AntlrSchemaValueLexer lexer = new AntlrSchemaValueLexer(new StringReader(s));
			        AntlrSchemaValueParser parser = new AntlrSchemaValueParser(lexer);
			        parser.setParserMonitor(monitor);
			        oids = parser.oids();
			    }
		
		
		return oids;
	}
	
	public final String  qdescr(
		String s
	) throws RecognitionException, TokenStreamException {
		String qdescr;
		
		
		matchedProduction( "qdescr()" );
		List<String> qdescrs = qdescrs(s);
		if( qdescrs.size() != 1 ) 
		{
		throw new SemanticException( "Exactly one qdescrs expected", null, 0, 0 );
		}
		qdescr = qdescrs.get(0);
		
		
		return qdescr;
	}
	
	public final List<String>  qdescrs(
		String s
	) throws RecognitionException, TokenStreamException {
		List<String> qdescrs;
		
		
		matchedProduction( "qdescrs()" );
		AntlrSchemaValueLexer lexer = new AntlrSchemaValueLexer(new StringReader(s));
		AntlrSchemaValueParser parser = new AntlrSchemaValueParser(lexer);
		parser.setParserMonitor(monitor);
		qdescrs = isQuirksModeEnabled ? parser.quirksQdescrs() : parser.qdescrs();
		
		
		return qdescrs;
	}
	
	public final String  qdstring(
		String s
	) throws RecognitionException, TokenStreamException {
		String qdstring;
		
		
		matchedProduction( "qdstring()" );
		List<String> qdstrings = qdstrings(s);
		if( qdstrings.size() != 1 ) 
		{
		throw new SemanticException( "Exactly one qdstrings expected", null, 0, 0 );
		}
		qdstring = qdstrings.get(0);
		
		
		return qdstring;
	}
	
	public final List<String>  qdstrings(
		String s
	) throws RecognitionException, TokenStreamException {
		List<String> qdstrings;
		
		
		matchedProduction( "qdstrings()" );
		AntlrSchemaQdstringLexer lexer = new AntlrSchemaQdstringLexer(new StringReader(s));
		AntlrSchemaQdstringParser parser = new AntlrSchemaQdstringParser(lexer);
		parser.setParserMonitor(monitor);
		qdstrings = parser.qdstrings();
		
		
		return qdstrings;
	}
	
	public final Integer  ruleid(
		String s
	) throws RecognitionException, TokenStreamException {
		Integer ruleid;
		
		
		matchedProduction( "ruleid()" );
		AntlrSchemaValueLexer lexer = new AntlrSchemaValueLexer(new StringReader(s));
		AntlrSchemaValueParser parser = new AntlrSchemaValueParser(lexer);
		parser.setParserMonitor(monitor);
		ruleid = parser.ruleid();
		
		
		return ruleid;
	}
	
	public final List<Integer>  ruleids(
		String s
	) throws RecognitionException, TokenStreamException {
		List<Integer> ruleids;
		
		
		matchedProduction( "ruleids()" );
		AntlrSchemaValueLexer lexer = new AntlrSchemaValueLexer(new StringReader(s));
		AntlrSchemaValueParser parser = new AntlrSchemaValueParser(lexer);
		parser.setParserMonitor(monitor);
		ruleids = parser.ruleids();
		
		
		return ruleids;
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"WHSP",
		"LPAR",
		"RPAR",
		"QUOTE",
		"DOLLAR",
		"LBRACKET",
		"RBRACKET",
		"LEN",
		"SINGLE_VALUE",
		"COLLECTIVE",
		"NO_USER_MODIFICATION",
		"OBSOLETE",
		"ABSTRACT",
		"STRUCTURAL",
		"AUXILIARY",
		"OBJECTIDENTIFIER",
		"OBJECTCLASS",
		"ATTRIBUTETYPE",
		"STARTNUMERICOID",
		"NAME",
		"DESC",
		"SUP",
		"MUST",
		"MAY",
		"AUX",
		"NOT",
		"FORM",
		"OC",
		"EQUALITY",
		"ORDERING",
		"SUBSTR",
		"SYNTAX",
		"APPLIES",
		"EXTENSION",
		"FQCN",
		"BYTECODE",
		"AUX_OR_AUXILIARY",
		"VALUES",
		"VALUE",
		"UNQUOTED_STRING",
		"QUOTED_STRING",
		"FQCN_VALUE",
		"FQCN_IDENTIFIER",
		"FQCN_LETTER",
		"FQCN_LETTERORDIGIT",
		"BYTECODE_VALUE",
		"USAGE",
		"USER_APPLICATIONS",
		"DIRECTORY_OPERATION",
		"DISTRIBUTED_OPERATION",
		"DSA_OPERATION"
	};
	
	
	}
