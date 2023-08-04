// $ANTLR 2.7.4: "ACIItem.g" -> "AntlrACIItemParser.java"$

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


package org.apache.directory.shared.ldap.aci;


import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Enumeration;

import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;

import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.AndNode;
import org.apache.directory.shared.ldap.filter.OrNode;
import org.apache.directory.shared.ldap.filter.NotNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.LeafNode;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.FilterParser;
import org.apache.directory.shared.ldap.name.NameComponentNormalizer;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecification;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecificationModifier;
import org.apache.directory.shared.ldap.util.ComponentsMonitor;
import org.apache.directory.shared.ldap.util.MandatoryAndOptionalComponentsMonitor;
import org.apache.directory.shared.ldap.util.MandatoryComponentsMonitor;
import org.apache.directory.shared.ldap.util.NamespaceTools;
import org.apache.directory.shared.ldap.util.NoDuplicateKeysMap;
import org.apache.directory.shared.ldap.util.OptionalComponentsMonitor;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.name.RDN;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.schema.normalizers.OidNormalizer;
import org.apache.directory.shared.ldap.entry.StringValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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
 * The antlr generated ACIItem parser.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AntlrACIItemParser extends antlr.LLkParser       implements AntlrACIItemParserTokenTypes
 {

    private static final Logger log = LoggerFactory.getLogger( AntlrACIItemParser.class );
    
    NameComponentNormalizer normalizer;
    
    // nonshared global data needed to avoid extensive pass/return stuff
    // these are only used by three first order components
    private String identificationTag;
    private AuthenticationLevel authenticationLevel;
    private int aciPrecedence = -1;
    
    private boolean isItemFirstACIItem;
    
    // shared global data needed to avoid extensive pass/return stuff
    private Set<ProtectedItem> protectedItems;
    private Map<String, ProtectedItem> protectedItemsMap;
    private Set<UserClass> userClasses;
    private Map<String, UserClass> userClassesMap;
    private Set<ItemPermission> itemPermissions;
    private int precedence = -1;
    private Set<GrantAndDenial> grantsAndDenials;
    private Set<UserPermission> userPermissions;
    private Map<String, OidNormalizer> oidsMap;
    
    private Set<DN> chopBeforeExclusions;
    private Set<DN> chopAfterExclusions;
    private SubtreeSpecificationModifier ssModifier = null;
    
    private ComponentsMonitor mainACIItemComponentsMonitor;
    private ComponentsMonitor itemPermissionComponentsMonitor;
    private ComponentsMonitor userPermissionComponentsMonitor;
    private ComponentsMonitor subtreeSpecificationComponentsMonitor;
    
    
    /**
     * Creates a (normalizing) subordinate DnParser for parsing Names.
     * This method MUST be called for each instance while we cannot do
     * constructor overloading for this class.
     *
     * @return the DnParser to be used for parsing Names
     */
    public void init( Map<String, OidNormalizer> oidsMap )
    {
        this.oidsMap = oidsMap;
    }

    /**
     * Sets the NameComponentNormalizer for this parser's dnParser.
     */
    public void setNormalizer(NameComponentNormalizer normalizer)
    {
        this.normalizer = normalizer;
    }

    private int token2Integer( Token token ) throws RecognitionException
    {
        int i = 0;
        
        try
        {
            i = Integer.parseInt( token.getText());
        }
        catch ( NumberFormatException e )
        {
            throw new RecognitionException( "Value of INTEGER token " +
                                            token.getText() +
                                            " cannot be converted to an Integer" );
        }
        
        return i;
    }

protected AntlrACIItemParser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public AntlrACIItemParser(TokenBuffer tokenBuf) {
  this(tokenBuf,1);
}

protected AntlrACIItemParser(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public AntlrACIItemParser(TokenStream lexer) {
  this(lexer,1);
}

public AntlrACIItemParser(ParserSharedInputState state) {
  super(state,1);
  tokenNames = _tokenNames;
}

	public final  ACIItem  wrapperEntryPoint() throws RecognitionException, TokenStreamException {
		 ACIItem l_ACIItem ;
		
		
		log.debug( "entered wrapperEntryPoint()" );
		l_ACIItem = null;
		
		
		{
		_loop3:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop3;
			}
			
		} while (true);
		}
		l_ACIItem=theACIItem();
		{
		_loop5:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop5;
			}
			
		} while (true);
		}
		match(Token.EOF_TYPE);
		return l_ACIItem ;
	}
	
	public final  ACIItem  theACIItem() throws RecognitionException, TokenStreamException {
		 ACIItem ACIItem ;
		
		
		log.debug( "entered theACIItem()" );
		ACIItem = null;
		mainACIItemComponentsMonitor = new MandatoryComponentsMonitor( 
		new String [] { "identificationTag", "precedence", "authenticationLevel", "itemOrUserFirst" } );
		
		
		match(OPEN_CURLY);
		{
		_loop8:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop8;
			}
			
		} while (true);
		}
		mainACIItemComponent();
		{
		_loop10:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop10;
			}
			
		} while (true);
		}
		{
		_loop16:
		do {
			if ((LA(1)==SEP)) {
				match(SEP);
				{
				_loop13:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop13;
					}
					
				} while (true);
				}
				mainACIItemComponent();
				{
				_loop15:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop15;
					}
					
				} while (true);
				}
			}
			else {
				break _loop16;
			}
			
		} while (true);
		}
		match(CLOSE_CURLY);
		
		if ( !mainACIItemComponentsMonitor.finalStateValid() )
		{
		throw new RecognitionException( "Missing mandatory ACIItem components: " 
		+ mainACIItemComponentsMonitor.getRemainingComponents() );
		}
		
		if ( isItemFirstACIItem )
		{
		ACIItem = new ItemFirstACIItem(
		identificationTag,
		aciPrecedence,
		authenticationLevel,
		protectedItems,
		itemPermissions );
		}
		else
		{
		ACIItem = new UserFirstACIItem(
		identificationTag,
		aciPrecedence,
		authenticationLevel,
		userClasses,
		userPermissions );
		}
		
		
		return ACIItem ;
	}
	
	public final void mainACIItemComponent() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered mainACIItemComponent()" );
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case ID_identificationTag:
			{
				aci_identificationTag();
				
				mainACIItemComponentsMonitor.useComponent( "identificationTag" );
				
				break;
			}
			case ID_precedence:
			{
				aci_precedence();
				
				mainACIItemComponentsMonitor.useComponent( "precedence" );
				
				break;
			}
			case ID_authenticationLevel:
			{
				aci_authenticationLevel();
				
				mainACIItemComponentsMonitor.useComponent( "authenticationLevel" );
				
				break;
			}
			case ID_itemOrUserFirst:
			{
				aci_itemOrUserFirst();
				
				mainACIItemComponentsMonitor.useComponent( "itemOrUserFirst" );
				
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (IllegalArgumentException e) {
			
			throw new RecognitionException( e.getMessage() );
			
		}
	}
	
	public final void aci_identificationTag() throws RecognitionException, TokenStreamException {
		
		Token  token = null;
		
		log.debug( "entered aci_identificationTag()" );
		
		
		match(ID_identificationTag);
		{
		int _cnt20=0;
		_loop20:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt20>=1 ) { break _loop20; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt20++;
		} while (true);
		}
		token = LT(1);
		match(SAFEUTF8STRING);
		
		identificationTag = token.getText();
		
	}
	
	public final void aci_precedence() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered aci_precedence()" );
		
		
		precedence();
		
		aciPrecedence = precedence;
		precedence = -1;
		
	}
	
	public final void aci_authenticationLevel() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered aci_authenticationLevel()" );
		
		
		match(ID_authenticationLevel);
		{
		int _cnt27=0;
		_loop27:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt27>=1 ) { break _loop27; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt27++;
		} while (true);
		}
		authenticationLevel();
	}
	
	public final void aci_itemOrUserFirst() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered aci_itemOrUserFirst()" );
		
		
		match(ID_itemOrUserFirst);
		{
		int _cnt31=0;
		_loop31:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt31>=1 ) { break _loop31; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt31++;
		} while (true);
		}
		itemOrUserFirst();
	}
	
	public final void precedence() throws RecognitionException, TokenStreamException {
		
		Token  token = null;
		
		log.debug( "entered precedence()" );
		
		
		match(ID_precedence);
		{
		int _cnt24=0;
		_loop24:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt24>=1 ) { break _loop24; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt24++;
		} while (true);
		}
		token = LT(1);
		match(INTEGER);
		
		precedence = token2Integer( token );
		
		if ( ( precedence < 0 ) || ( precedence > 255 ) )
		{
		throw new RecognitionException( "Expecting INTEGER token having an Integer value between 0 and 255, found " + precedence );
		}
		
	}
	
	public final void authenticationLevel() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered authenticationLevel()" );
		
		
		switch ( LA(1)) {
		case ID_none:
		{
			match(ID_none);
			
			authenticationLevel = AuthenticationLevel.NONE;
			
			break;
		}
		case ID_simple:
		{
			match(ID_simple);
			
			authenticationLevel = AuthenticationLevel.SIMPLE;
			
			break;
		}
		case ID_strong:
		{
			match(ID_strong);
			
			authenticationLevel = AuthenticationLevel.STRONG;
			
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
	}
	
	public final void itemOrUserFirst() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered itemOrUserFirst()" );
		
		
		switch ( LA(1)) {
		case ID_itemFirst:
		{
			itemFirst();
			break;
		}
		case ID_userFirst:
		{
			userFirst();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
	}
	
	public final void itemFirst() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered itemFirst()" );
		
		
		match(ID_itemFirst);
		{
		_loop35:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop35;
			}
			
		} while (true);
		}
		match(COLON);
		{
		_loop37:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop37;
			}
			
		} while (true);
		}
		match(OPEN_CURLY);
		{
		_loop39:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop39;
			}
			
		} while (true);
		}
		{
		switch ( LA(1)) {
		case ID_protectedItems:
		{
			protectedItems();
			{
			_loop42:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop42;
				}
				
			} while (true);
			}
			match(SEP);
			{
			_loop44:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop44;
				}
				
			} while (true);
			}
			itemPermissions();
			break;
		}
		case ID_itemPermissions:
		{
			itemPermissions();
			{
			_loop46:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop46;
				}
				
			} while (true);
			}
			match(SEP);
			{
			_loop48:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop48;
				}
				
			} while (true);
			}
			protectedItems();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		_loop50:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop50;
			}
			
		} while (true);
		}
		match(CLOSE_CURLY);
		
		isItemFirstACIItem = true;
		
	}
	
	public final void userFirst() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered userFirst()" );
		
		
		match(ID_userFirst);
		{
		_loop53:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop53;
			}
			
		} while (true);
		}
		match(COLON);
		{
		_loop55:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop55;
			}
			
		} while (true);
		}
		match(OPEN_CURLY);
		{
		_loop57:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop57;
			}
			
		} while (true);
		}
		{
		switch ( LA(1)) {
		case ID_userClasses:
		{
			userClasses();
			{
			_loop60:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop60;
				}
				
			} while (true);
			}
			match(SEP);
			{
			_loop62:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop62;
				}
				
			} while (true);
			}
			userPermissions();
			break;
		}
		case ID_userPermissions:
		{
			userPermissions();
			{
			_loop64:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop64;
				}
				
			} while (true);
			}
			match(SEP);
			{
			_loop66:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop66;
				}
				
			} while (true);
			}
			userClasses();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		_loop68:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop68;
			}
			
		} while (true);
		}
		match(CLOSE_CURLY);
		
		isItemFirstACIItem = false;
		
	}
	
	public final void protectedItems() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered protectedItems()" );
		protectedItemsMap = new NoDuplicateKeysMap();
		
		
		try {      // for error handling
			match(ID_protectedItems);
			{
			_loop71:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop71;
				}
				
			} while (true);
			}
			match(OPEN_CURLY);
			{
			_loop73:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop73;
				}
				
			} while (true);
			}
			{
			switch ( LA(1)) {
			case ATTRIBUTE_VALUE_CANDIDATE:
			case RANGE_OF_VALUES_CANDIDATE:
			case ID_entry:
			case ID_allUserAttributeTypes:
			case ID_attributeType:
			case ID_allAttributeValues:
			case ID_allUserAttributeTypesAndValues:
			case ID_selfValue:
			case ID_maxValueCount:
			case ID_maxImmSub:
			case ID_restrictedBy:
			case ID_classes:
			{
				protectedItem();
				{
				_loop76:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop76;
					}
					
				} while (true);
				}
				{
				_loop82:
				do {
					if ((LA(1)==SEP)) {
						match(SEP);
						{
						_loop79:
						do {
							if ((LA(1)==SP)) {
								match(SP);
							}
							else {
								break _loop79;
							}
							
						} while (true);
						}
						protectedItem();
						{
						_loop81:
						do {
							if ((LA(1)==SP)) {
								match(SP);
							}
							else {
								break _loop81;
							}
							
						} while (true);
						}
					}
					else {
						break _loop82;
					}
					
				} while (true);
				}
				break;
			}
			case CLOSE_CURLY:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(CLOSE_CURLY);
			
			protectedItems = new HashSet<ProtectedItem>( protectedItemsMap.values() );
			
		}
		catch (IllegalArgumentException e) {
			
			throw new RecognitionException( "Protected Items cannot be duplicated. " + e.getMessage() );
			
		}
	}
	
	public final void itemPermissions() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered itemPermissions()" );
		itemPermissions = new HashSet<ItemPermission>();
		ItemPermission itemPermission = null;
		
		
		match(ID_itemPermissions);
		{
		int _cnt187=0;
		_loop187:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt187>=1 ) { break _loop187; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt187++;
		} while (true);
		}
		match(OPEN_CURLY);
		{
		_loop189:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop189;
			}
			
		} while (true);
		}
		{
		switch ( LA(1)) {
		case OPEN_CURLY:
		{
			itemPermission=itemPermission();
			{
			_loop192:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop192;
				}
				
			} while (true);
			}
			
			itemPermissions.add( itemPermission );
			
			{
			_loop198:
			do {
				if ((LA(1)==SEP)) {
					match(SEP);
					{
					_loop195:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop195;
						}
						
					} while (true);
					}
					itemPermission=itemPermission();
					{
					_loop197:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop197;
						}
						
					} while (true);
					}
					
					itemPermissions.add( itemPermission );
					
				}
				else {
					break _loop198;
				}
				
			} while (true);
			}
			break;
		}
		case CLOSE_CURLY:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		match(CLOSE_CURLY);
	}
	
	public final void userClasses() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered userClasses()" );
		userClassesMap = new NoDuplicateKeysMap();
		
		
		try {      // for error handling
			match(ID_userClasses);
			{
			int _cnt228=0;
			_loop228:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					if ( _cnt228>=1 ) { break _loop228; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt228++;
			} while (true);
			}
			match(OPEN_CURLY);
			{
			_loop230:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop230;
				}
				
			} while (true);
			}
			{
			switch ( LA(1)) {
			case ID_allUsers:
			case ID_thisEntry:
			case ID_parentOfEntry:
			case ID_name:
			case ID_userGroup:
			case ID_subtree:
			{
				userClass();
				{
				_loop233:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop233;
					}
					
				} while (true);
				}
				{
				_loop239:
				do {
					if ((LA(1)==SEP)) {
						match(SEP);
						{
						_loop236:
						do {
							if ((LA(1)==SP)) {
								match(SP);
							}
							else {
								break _loop236;
							}
							
						} while (true);
						}
						userClass();
						{
						_loop238:
						do {
							if ((LA(1)==SP)) {
								match(SP);
							}
							else {
								break _loop238;
							}
							
						} while (true);
						}
					}
					else {
						break _loop239;
					}
					
				} while (true);
				}
				break;
			}
			case CLOSE_CURLY:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(CLOSE_CURLY);
			
			userClasses  = new HashSet<UserClass>( userClassesMap.values() );
			
		}
		catch (IllegalArgumentException e) {
			
			throw new RecognitionException( "User Classes cannot be duplicated. " + e.getMessage() );
			
		}
	}
	
	public final void userPermissions() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered userPermissions()" );
		userPermissions = new HashSet<UserPermission>();
		UserPermission userPermission = null;
		
		
		match(ID_userPermissions);
		{
		int _cnt285=0;
		_loop285:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt285>=1 ) { break _loop285; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt285++;
		} while (true);
		}
		match(OPEN_CURLY);
		{
		_loop287:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop287;
			}
			
		} while (true);
		}
		{
		switch ( LA(1)) {
		case OPEN_CURLY:
		{
			userPermission=userPermission();
			{
			_loop290:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop290;
				}
				
			} while (true);
			}
			
			userPermissions.add( userPermission );
			
			{
			_loop296:
			do {
				if ((LA(1)==SEP)) {
					match(SEP);
					{
					_loop293:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop293;
						}
						
					} while (true);
					}
					userPermission=userPermission();
					{
					_loop295:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop295;
						}
						
					} while (true);
					}
					
					userPermissions.add( userPermission );
					
				}
				else {
					break _loop296;
				}
				
			} while (true);
			}
			break;
		}
		case CLOSE_CURLY:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		match(CLOSE_CURLY);
	}
	
	public final void protectedItem() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered protectedItem()" );
		
		
		switch ( LA(1)) {
		case ID_entry:
		{
			entry();
			break;
		}
		case ID_allUserAttributeTypes:
		{
			allUserAttributeTypes();
			break;
		}
		case ID_attributeType:
		{
			attributeType();
			break;
		}
		case ID_allAttributeValues:
		{
			allAttributeValues();
			break;
		}
		case ID_allUserAttributeTypesAndValues:
		{
			allUserAttributeTypesAndValues();
			break;
		}
		case ATTRIBUTE_VALUE_CANDIDATE:
		{
			attributeValue();
			break;
		}
		case ID_selfValue:
		{
			selfValue();
			break;
		}
		case RANGE_OF_VALUES_CANDIDATE:
		{
			rangeOfValues();
			break;
		}
		case ID_maxValueCount:
		{
			maxValueCount();
			break;
		}
		case ID_maxImmSub:
		{
			maxImmSub();
			break;
		}
		case ID_restrictedBy:
		{
			restrictedBy();
			break;
		}
		case ID_classes:
		{
			classes();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
	}
	
	public final void entry() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered entry()" );  
		
		
		match(ID_entry);
		
		protectedItemsMap.put( "entry", ProtectedItem.ENTRY );
		
	}
	
	public final void allUserAttributeTypes() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered allUserAttributeTypes()" );
		
		
		match(ID_allUserAttributeTypes);
		
		protectedItemsMap.put( "allUserAttributeTypes", ProtectedItem.ALL_USER_ATTRIBUTE_TYPES );
		
	}
	
	public final void attributeType() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered attributeType()" );
		Set<String> attributeTypeSet = null;
		
		
		match(ID_attributeType);
		{
		int _cnt88=0;
		_loop88:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt88>=1 ) { break _loop88; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt88++;
		} while (true);
		}
		attributeTypeSet=attributeTypeSet();
		
		protectedItemsMap.put( "attributeType", new ProtectedItem.AttributeType(attributeTypeSet ) );
		
	}
	
	public final void allAttributeValues() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered allAttributeValues()" );
		Set<String> attributeTypeSet = null;
		
		
		match(ID_allAttributeValues);
		{
		int _cnt91=0;
		_loop91:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt91>=1 ) { break _loop91; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt91++;
		} while (true);
		}
		attributeTypeSet=attributeTypeSet();
		
		protectedItemsMap.put( "allAttributeValues", new ProtectedItem.AllAttributeValues( attributeTypeSet ) );
		
	}
	
	public final void allUserAttributeTypesAndValues() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered allUserAttributeTypesAndValues()" );
		
		
		match(ID_allUserAttributeTypesAndValues);
		
		protectedItemsMap.put( "allUserAttributeTypesAndValues", ProtectedItem.ALL_USER_ATTRIBUTE_TYPES_AND_VALUES );
		
	}
	
	public final void attributeValue() throws RecognitionException, TokenStreamException {
		
		Token  token = null;
		
		log.debug( "entered attributeValue()" );
		String attributeTypeAndValue = null;
		String attributeType = null;
		String attributeValue = null;
		Set<Attribute> attributeSet = new HashSet<Attribute>();
		
		
		try {      // for error handling
			token = LT(1);
			match(ATTRIBUTE_VALUE_CANDIDATE);
			
			// A Dn can be considered as a set of attributeTypeAndValues
			// So, parse the set as a Dn and extract each attributeTypeAndValue
			DN attributeTypeAndValueSetAsDn = new DN( token.getText() );
			
			if ( oidsMap != null )
			{        
			attributeTypeAndValueSetAsDn.normalize( oidsMap );
			}
			
			for ( RDN rdn :attributeTypeAndValueSetAsDn.getRdns() )
			{
			attributeTypeAndValue = rdn.getNormName();
			attributeType = NamespaceTools.getRdnAttribute( attributeTypeAndValue );
			attributeValue = NamespaceTools.getRdnValue( attributeTypeAndValue );
			attributeSet.add( new BasicAttribute( attributeType, attributeValue ) );
			log.debug( "An attributeTypeAndValue from the set: " + attributeType + "=" +  attributeValue);
			}
			
			protectedItemsMap.put( "attributeValue", new ProtectedItem.AttributeValue( attributeSet ) );
			
		}
		catch (Exception e) {
			
			throw new RecognitionException( "dnParser failed for " + token.getText() + " , " + e.getMessage() );
			
		}
	}
	
	public final void selfValue() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered selfValue()" );
		Set<String> attributeTypeSet = null;
		
		
		match(ID_selfValue);
		{
		int _cnt96=0;
		_loop96:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt96>=1 ) { break _loop96; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt96++;
		} while (true);
		}
		attributeTypeSet=attributeTypeSet();
		
		protectedItemsMap.put( "sefValue", new ProtectedItem.SelfValue( attributeTypeSet ) );
		
	}
	
	public final void rangeOfValues() throws RecognitionException, TokenStreamException {
		
		Token  token = null;
		
		log.debug( "entered rangeOfValues()" );
		
		
		try {      // for error handling
			token = LT(1);
			match(RANGE_OF_VALUES_CANDIDATE);
			
			protectedItemsMap.put( "rangeOfValues",
			new ProtectedItem.RangeOfValues(
			FilterParser.parse( token.getText() ) ) );
			log.debug( "filterParser parsed " + token.getText() );
			
		}
		catch (Exception e) {
			
			throw new RecognitionException( "filterParser failed. " + e.getMessage() );
			
		}
	}
	
	public final void maxValueCount() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered maxValueCount()" );
		ProtectedItem.MaxValueCountItem maxValueCount = null;
		Set<ProtectedItem.MaxValueCountItem> maxValueCountSet = new HashSet<ProtectedItem.MaxValueCountItem>();
		
		
		match(ID_maxValueCount);
		{
		int _cnt100=0;
		_loop100:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt100>=1 ) { break _loop100; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt100++;
		} while (true);
		}
		match(OPEN_CURLY);
		{
		_loop102:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop102;
			}
			
		} while (true);
		}
		maxValueCount=aMaxValueCount();
		{
		_loop104:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop104;
			}
			
		} while (true);
		}
		
		maxValueCountSet.add( maxValueCount );
		
		{
		_loop110:
		do {
			if ((LA(1)==SEP)) {
				match(SEP);
				{
				_loop107:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop107;
					}
					
				} while (true);
				}
				maxValueCount=aMaxValueCount();
				{
				_loop109:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop109;
					}
					
				} while (true);
				}
				
				maxValueCountSet.add( maxValueCount );
				
			}
			else {
				break _loop110;
			}
			
		} while (true);
		}
		match(CLOSE_CURLY);
		
		protectedItemsMap.put( "maxValueCount", new ProtectedItem.MaxValueCount( maxValueCountSet ) );
		
	}
	
	public final void maxImmSub() throws RecognitionException, TokenStreamException {
		
		Token  token = null;
		
		log.debug( "entered maxImmSub()" );
		
		
		match(ID_maxImmSub);
		{
		int _cnt135=0;
		_loop135:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt135>=1 ) { break _loop135; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt135++;
		} while (true);
		}
		token = LT(1);
		match(INTEGER);
		
		
		protectedItemsMap.put( "maxImmSub",
		new ProtectedItem.MaxImmSub(
		token2Integer( token ) ) );
		
	}
	
	public final void restrictedBy() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered restrictedBy()" );
		ProtectedItem.RestrictedByItem restrictedValue = null;
		Set<ProtectedItem.RestrictedByItem> restrictedBy = new HashSet<ProtectedItem.RestrictedByItem>();
		
		
		match(ID_restrictedBy);
		{
		int _cnt138=0;
		_loop138:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt138>=1 ) { break _loop138; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt138++;
		} while (true);
		}
		match(OPEN_CURLY);
		{
		_loop140:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop140;
			}
			
		} while (true);
		}
		restrictedValue=restrictedValue();
		{
		_loop142:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop142;
			}
			
		} while (true);
		}
		
		restrictedBy.add( restrictedValue );
		
		{
		_loop148:
		do {
			if ((LA(1)==SEP)) {
				match(SEP);
				{
				_loop145:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop145;
					}
					
				} while (true);
				}
				restrictedValue=restrictedValue();
				{
				_loop147:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop147;
					}
					
				} while (true);
				}
				
				restrictedBy.add( restrictedValue );
				
			}
			else {
				break _loop148;
			}
			
		} while (true);
		}
		match(CLOSE_CURLY);
		
		protectedItemsMap.put( "restrictedBy", new ProtectedItem.RestrictedBy( restrictedBy ) );
		
	}
	
	public final void classes() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered classes()" );
		ExprNode classes = null;
		
		
		match(ID_classes);
		{
		int _cnt184=0;
		_loop184:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt184>=1 ) { break _loop184; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt184++;
		} while (true);
		}
		classes=refinement();
		
		protectedItemsMap.put( "classes", new ProtectedItem.Classes( classes ) );
		
	}
	
	public final  Set<String>  attributeTypeSet() throws RecognitionException, TokenStreamException {
		 Set<String> attributeTypeSet ;
		
		
		log.debug( "entered attributeTypeSet()" );
		String oid = null;
		attributeTypeSet = new HashSet<String>();
		
		
		match(OPEN_CURLY);
		{
		_loop173:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop173;
			}
			
		} while (true);
		}
		oid=oid();
		{
		_loop175:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop175;
			}
			
		} while (true);
		}
		
		attributeTypeSet.add( oid );
		
		{
		_loop181:
		do {
			if ((LA(1)==SEP)) {
				match(SEP);
				{
				_loop178:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop178;
					}
					
				} while (true);
				}
				oid=oid();
				{
				_loop180:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop180;
					}
					
				} while (true);
				}
				
				attributeTypeSet.add( oid );
				
			}
			else {
				break _loop181;
			}
			
		} while (true);
		}
		match(CLOSE_CURLY);
		return attributeTypeSet ;
	}
	
	public final  ProtectedItem.MaxValueCountItem  aMaxValueCount() throws RecognitionException, TokenStreamException {
		 ProtectedItem.MaxValueCountItem maxValueCount ;
		
		Token  token1 = null;
		Token  token2 = null;
		
		log.debug( "entered aMaxValueCount()" );
		maxValueCount = null;
		String oid = null;
		Token token = null;
		
		
		match(OPEN_CURLY);
		{
		_loop113:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop113;
			}
			
		} while (true);
		}
		{
		switch ( LA(1)) {
		case ID_type:
		{
			match(ID_type);
			{
			int _cnt116=0;
			_loop116:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					if ( _cnt116>=1 ) { break _loop116; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt116++;
			} while (true);
			}
			oid=oid();
			{
			_loop118:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop118;
				}
				
			} while (true);
			}
			match(SEP);
			{
			_loop120:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop120;
				}
				
			} while (true);
			}
			match(ID_maxCount);
			{
			int _cnt122=0;
			_loop122:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					if ( _cnt122>=1 ) { break _loop122; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt122++;
			} while (true);
			}
			token1 = LT(1);
			match(INTEGER);
			token = token1;
			break;
		}
		case ID_maxCount:
		{
			match(ID_maxCount);
			{
			int _cnt124=0;
			_loop124:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					if ( _cnt124>=1 ) { break _loop124; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt124++;
			} while (true);
			}
			token2 = LT(1);
			match(INTEGER);
			{
			_loop126:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop126;
				}
				
			} while (true);
			}
			match(SEP);
			{
			_loop128:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop128;
				}
				
			} while (true);
			}
			match(ID_type);
			{
			int _cnt130=0;
			_loop130:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					if ( _cnt130>=1 ) { break _loop130; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt130++;
			} while (true);
			}
			oid=oid();
			token = token2;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		_loop132:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop132;
			}
			
		} while (true);
		}
		match(CLOSE_CURLY);
		
		maxValueCount = new ProtectedItem.MaxValueCountItem( oid, token2Integer( token ) );
		
		return maxValueCount ;
	}
	
	public final  String  oid() throws RecognitionException, TokenStreamException {
		 String result ;
		
		
		log.debug( "entered oid()" );
		result = null;
		Token token = null;
		
		
		token = LT( 1 );
		{
		switch ( LA(1)) {
		case DESCR:
		{
			match(DESCR);
			break;
		}
		case NUMERICOID:
		{
			match(NUMERICOID);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		
		result = token.getText();
		log.debug( "recognized an oid: " + result );
		
		return result ;
	}
	
	public final  ProtectedItem.RestrictedByItem  restrictedValue() throws RecognitionException, TokenStreamException {
		 ProtectedItem.RestrictedByItem restrictedValue ;
		
		
		log.debug( "entered restrictedValue()" );
		String typeOid = null;
		String valuesInOid = null;
		restrictedValue = null;
		
		
		match(OPEN_CURLY);
		{
		_loop151:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop151;
			}
			
		} while (true);
		}
		{
		switch ( LA(1)) {
		case ID_type:
		{
			match(ID_type);
			{
			int _cnt154=0;
			_loop154:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					if ( _cnt154>=1 ) { break _loop154; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt154++;
			} while (true);
			}
			typeOid=oid();
			{
			_loop156:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop156;
				}
				
			} while (true);
			}
			match(SEP);
			{
			_loop158:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop158;
				}
				
			} while (true);
			}
			match(ID_valuesIn);
			{
			int _cnt160=0;
			_loop160:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					if ( _cnt160>=1 ) { break _loop160; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt160++;
			} while (true);
			}
			valuesInOid=oid();
			break;
		}
		case ID_valuesIn:
		{
			match(ID_valuesIn);
			{
			int _cnt162=0;
			_loop162:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					if ( _cnt162>=1 ) { break _loop162; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt162++;
			} while (true);
			}
			valuesInOid=oid();
			{
			_loop164:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop164;
				}
				
			} while (true);
			}
			match(SEP);
			{
			_loop166:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop166;
				}
				
			} while (true);
			}
			match(ID_type);
			{
			int _cnt168=0;
			_loop168:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					if ( _cnt168>=1 ) { break _loop168; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt168++;
			} while (true);
			}
			typeOid=oid();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		_loop170:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop170;
			}
			
		} while (true);
		}
		match(CLOSE_CURLY);
		
		restrictedValue = new ProtectedItem.RestrictedByItem( typeOid, valuesInOid );
		
		return restrictedValue ;
	}
	
	public final  ExprNode  refinement() throws RecognitionException, TokenStreamException {
		 ExprNode node ;
		
		
		log.debug( "entered refinement()" );
		node = null;
		
		
		switch ( LA(1)) {
		case ID_item:
		{
			node=item();
			break;
		}
		case ID_and:
		{
			node=and();
			break;
		}
		case ID_or:
		{
			node=or();
			break;
		}
		case ID_not:
		{
			node=not();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return node ;
	}
	
	public final  ItemPermission  itemPermission() throws RecognitionException, TokenStreamException {
		 ItemPermission itemPermission ;
		
		
		log.debug( "entered itemPermission()" );
		itemPermission = null;
		itemPermissionComponentsMonitor = new MandatoryAndOptionalComponentsMonitor( 
		new String [] { "userClasses", "grantsAndDenials" }, new String [] { "precedence" } );
		
		
		match(OPEN_CURLY);
		{
		_loop201:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop201;
			}
			
		} while (true);
		}
		anyItemPermission();
		{
		_loop203:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop203;
			}
			
		} while (true);
		}
		{
		_loop209:
		do {
			if ((LA(1)==SEP)) {
				match(SEP);
				{
				_loop206:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop206;
					}
					
				} while (true);
				}
				anyItemPermission();
				{
				_loop208:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop208;
					}
					
				} while (true);
				}
			}
			else {
				break _loop209;
			}
			
		} while (true);
		}
		match(CLOSE_CURLY);
		
		if ( !itemPermissionComponentsMonitor.finalStateValid() )
		{
		throw new RecognitionException( "Missing mandatory itemPermission components: " 
		+ itemPermissionComponentsMonitor.getRemainingComponents() );
		}
		
		itemPermission = new ItemPermission( precedence, grantsAndDenials, userClasses );
		precedence = -1;
		
		return itemPermission ;
	}
	
	public final void anyItemPermission() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case ID_precedence:
			{
				precedence();
				
				itemPermissionComponentsMonitor.useComponent( "precedence" );
				
				break;
			}
			case ID_userClasses:
			{
				userClasses();
				
				itemPermissionComponentsMonitor.useComponent( "userClasses" );
				
				break;
			}
			case ID_grantsAndDenials:
			{
				grantsAndDenials();
				
				itemPermissionComponentsMonitor.useComponent( "grantsAndDenials" );
				
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (IllegalArgumentException e) {
			
			throw new RecognitionException( e.getMessage() );
			
		}
	}
	
	public final void grantsAndDenials() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered grantsAndDenials()" );
		grantsAndDenials = new HashSet<GrantAndDenial>();
		GrantAndDenial grantAndDenial = null;
		
		
		match(ID_grantsAndDenials);
		{
		int _cnt213=0;
		_loop213:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt213>=1 ) { break _loop213; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt213++;
		} while (true);
		}
		match(OPEN_CURLY);
		{
		_loop215:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop215;
			}
			
		} while (true);
		}
		{
		switch ( LA(1)) {
		case ID_grantAdd:
		case ID_denyAdd:
		case ID_grantDiscloseOnError:
		case ID_denyDiscloseOnError:
		case ID_grantRead:
		case ID_denyRead:
		case ID_grantRemove:
		case ID_denyRemove:
		case ID_grantBrowse:
		case ID_denyBrowse:
		case ID_grantExport:
		case ID_denyExport:
		case ID_grantImport:
		case ID_denyImport:
		case ID_grantModify:
		case ID_denyModify:
		case ID_grantRename:
		case ID_denyRename:
		case ID_grantReturnDN:
		case ID_denyReturnDN:
		case ID_grantCompare:
		case ID_denyCompare:
		case ID_grantFilterMatch:
		case ID_denyFilterMatch:
		case ID_grantInvoke:
		case ID_denyInvoke:
		{
			grantAndDenial=grantAndDenial();
			{
			_loop218:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop218;
				}
				
			} while (true);
			}
			
			if ( !grantsAndDenials.add( grantAndDenial ))
			{
			throw new RecognitionException( "Duplicated GrantAndDenial bit: " + grantAndDenial );
			}
			
			{
			_loop224:
			do {
				if ((LA(1)==SEP)) {
					match(SEP);
					{
					_loop221:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop221;
						}
						
					} while (true);
					}
					grantAndDenial=grantAndDenial();
					{
					_loop223:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop223;
						}
						
					} while (true);
					}
					
					if ( !grantsAndDenials.add( grantAndDenial ))
					{
					throw new RecognitionException( "Duplicated GrantAndDenial bit: " + grantAndDenial );
					}
					
				}
				else {
					break _loop224;
				}
				
			} while (true);
			}
			break;
		}
		case CLOSE_CURLY:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		match(CLOSE_CURLY);
	}
	
	public final  GrantAndDenial  grantAndDenial() throws RecognitionException, TokenStreamException {
		 GrantAndDenial l_grantAndDenial ;
		
		
		log.debug( "entered grantAndDenialsBit()" );
		l_grantAndDenial = null;
		
		
		switch ( LA(1)) {
		case ID_grantAdd:
		{
			match(ID_grantAdd);
			l_grantAndDenial = GrantAndDenial.GRANT_ADD;
			break;
		}
		case ID_denyAdd:
		{
			match(ID_denyAdd);
			l_grantAndDenial = GrantAndDenial.DENY_ADD;
			break;
		}
		case ID_grantDiscloseOnError:
		{
			match(ID_grantDiscloseOnError);
			l_grantAndDenial = GrantAndDenial.GRANT_DISCLOSE_ON_ERROR;
			break;
		}
		case ID_denyDiscloseOnError:
		{
			match(ID_denyDiscloseOnError);
			l_grantAndDenial = GrantAndDenial.DENY_DISCLOSE_ON_ERROR;
			break;
		}
		case ID_grantRead:
		{
			match(ID_grantRead);
			l_grantAndDenial = GrantAndDenial.GRANT_READ;
			break;
		}
		case ID_denyRead:
		{
			match(ID_denyRead);
			l_grantAndDenial = GrantAndDenial.DENY_READ;
			break;
		}
		case ID_grantRemove:
		{
			match(ID_grantRemove);
			l_grantAndDenial = GrantAndDenial.GRANT_REMOVE;
			break;
		}
		case ID_denyRemove:
		{
			match(ID_denyRemove);
			l_grantAndDenial = GrantAndDenial.DENY_REMOVE;
			break;
		}
		case ID_grantBrowse:
		{
			match(ID_grantBrowse);
			l_grantAndDenial = GrantAndDenial.GRANT_BROWSE;
			break;
		}
		case ID_denyBrowse:
		{
			match(ID_denyBrowse);
			l_grantAndDenial = GrantAndDenial.DENY_BROWSE;
			break;
		}
		case ID_grantExport:
		{
			match(ID_grantExport);
			l_grantAndDenial = GrantAndDenial.GRANT_EXPORT;
			break;
		}
		case ID_denyExport:
		{
			match(ID_denyExport);
			l_grantAndDenial = GrantAndDenial.DENY_EXPORT;
			break;
		}
		case ID_grantImport:
		{
			match(ID_grantImport);
			l_grantAndDenial = GrantAndDenial.GRANT_IMPORT;
			break;
		}
		case ID_denyImport:
		{
			match(ID_denyImport);
			l_grantAndDenial = GrantAndDenial.DENY_IMPORT;
			break;
		}
		case ID_grantModify:
		{
			match(ID_grantModify);
			l_grantAndDenial = GrantAndDenial.GRANT_MODIFY;
			break;
		}
		case ID_denyModify:
		{
			match(ID_denyModify);
			l_grantAndDenial = GrantAndDenial.DENY_MODIFY;
			break;
		}
		case ID_grantRename:
		{
			match(ID_grantRename);
			l_grantAndDenial = GrantAndDenial.GRANT_RENAME;
			break;
		}
		case ID_denyRename:
		{
			match(ID_denyRename);
			l_grantAndDenial = GrantAndDenial.DENY_RENAME;
			break;
		}
		case ID_grantReturnDN:
		{
			match(ID_grantReturnDN);
			l_grantAndDenial = GrantAndDenial.GRANT_RETURN_DN;
			break;
		}
		case ID_denyReturnDN:
		{
			match(ID_denyReturnDN);
			l_grantAndDenial = GrantAndDenial.DENY_RETURN_DN;
			break;
		}
		case ID_grantCompare:
		{
			match(ID_grantCompare);
			l_grantAndDenial = GrantAndDenial.GRANT_COMPARE;
			break;
		}
		case ID_denyCompare:
		{
			match(ID_denyCompare);
			l_grantAndDenial = GrantAndDenial.DENY_COMPARE;
			break;
		}
		case ID_grantFilterMatch:
		{
			match(ID_grantFilterMatch);
			l_grantAndDenial = GrantAndDenial.GRANT_FILTER_MATCH;
			break;
		}
		case ID_denyFilterMatch:
		{
			match(ID_denyFilterMatch);
			l_grantAndDenial = GrantAndDenial.DENY_FILTER_MATCH;
			break;
		}
		case ID_grantInvoke:
		{
			match(ID_grantInvoke);
			l_grantAndDenial = GrantAndDenial.GRANT_INVOKE;
			break;
		}
		case ID_denyInvoke:
		{
			match(ID_denyInvoke);
			l_grantAndDenial = GrantAndDenial.DENY_INVOKE;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return l_grantAndDenial ;
	}
	
	public final void userClass() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered userClasses()" );
		
		
		switch ( LA(1)) {
		case ID_allUsers:
		{
			allUsers();
			break;
		}
		case ID_thisEntry:
		{
			thisEntry();
			break;
		}
		case ID_parentOfEntry:
		{
			parentOfEntry();
			break;
		}
		case ID_name:
		{
			name();
			break;
		}
		case ID_userGroup:
		{
			userGroup();
			break;
		}
		case ID_subtree:
		{
			subtree();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
	}
	
	public final void allUsers() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered allUsers()" );
		
		
		match(ID_allUsers);
		
		userClassesMap.put( "allUsers", UserClass.ALL_USERS );
		
	}
	
	public final void thisEntry() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered thisEntry()" );
		
		
		match(ID_thisEntry);
		
		userClassesMap.put( "thisEntry", UserClass.THIS_ENTRY );
		
	}
	
	public final void parentOfEntry() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered parentOfEntry()" );
		
		
		match(ID_parentOfEntry);
		
		userClassesMap.put( "parentOfEntry", UserClass.PARENT_OF_ENTRY );
		
	}
	
	public final void name() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered name()" );
		Set<DN> names = new HashSet<DN>();
		DN distinguishedName = null;
		
		
		match(ID_name);
		{
		int _cnt246=0;
		_loop246:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt246>=1 ) { break _loop246; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt246++;
		} while (true);
		}
		match(OPEN_CURLY);
		{
		_loop248:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop248;
			}
			
		} while (true);
		}
		distinguishedName=distinguishedName();
		{
		_loop250:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop250;
			}
			
		} while (true);
		}
		
		names.add( distinguishedName );
		
		{
		_loop256:
		do {
			if ((LA(1)==SEP)) {
				match(SEP);
				{
				_loop253:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop253;
					}
					
				} while (true);
				}
				distinguishedName=distinguishedName();
				{
				_loop255:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop255;
					}
					
				} while (true);
				}
				
				names.add( distinguishedName );
				
			}
			else {
				break _loop256;
			}
			
		} while (true);
		}
		match(CLOSE_CURLY);
		
		userClassesMap.put( "name", new UserClass.Name( names ) );
		
	}
	
	public final void userGroup() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered userGroup()" );
		Set<DN> userGroup = new HashSet<DN>();
		DN distinguishedName = null;
		
		
		match(ID_userGroup);
		{
		int _cnt259=0;
		_loop259:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt259>=1 ) { break _loop259; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt259++;
		} while (true);
		}
		match(OPEN_CURLY);
		{
		_loop261:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop261;
			}
			
		} while (true);
		}
		distinguishedName=distinguishedName();
		{
		_loop263:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop263;
			}
			
		} while (true);
		}
		
		userGroup.add( distinguishedName );
		
		{
		_loop269:
		do {
			if ((LA(1)==SEP)) {
				match(SEP);
				{
				_loop266:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop266;
					}
					
				} while (true);
				}
				distinguishedName=distinguishedName();
				{
				_loop268:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop268;
					}
					
				} while (true);
				}
				
				userGroup.add( distinguishedName );
				
			}
			else {
				break _loop269;
			}
			
		} while (true);
		}
		match(CLOSE_CURLY);
		
		userClassesMap.put( "userGroup", new UserClass.UserGroup( userGroup ) );
		
	}
	
	public final void subtree() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered subtree()" );
		Set<SubtreeSpecification> subtrees = new HashSet<SubtreeSpecification>();
		SubtreeSpecification subtreeSpecification = null;    
		
		
		match(ID_subtree);
		{
		int _cnt272=0;
		_loop272:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt272>=1 ) { break _loop272; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt272++;
		} while (true);
		}
		match(OPEN_CURLY);
		{
		_loop274:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop274;
			}
			
		} while (true);
		}
		subtreeSpecification=subtreeSpecification();
		{
		_loop276:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop276;
			}
			
		} while (true);
		}
		
		subtrees.add( subtreeSpecification );
		
		{
		_loop282:
		do {
			if ((LA(1)==SEP)) {
				match(SEP);
				{
				_loop279:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop279;
					}
					
				} while (true);
				}
				subtreeSpecification=subtreeSpecification();
				{
				_loop281:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop281;
					}
					
				} while (true);
				}
				
				subtrees.add( subtreeSpecification );
				
			}
			else {
				break _loop282;
			}
			
		} while (true);
		}
		match(CLOSE_CURLY);
		
		userClassesMap.put( "subtree", new UserClass.Subtree( subtrees ) );
		
	}
	
	public final  DN  distinguishedName() throws RecognitionException, TokenStreamException {
		 DN name ;
		
		Token  token = null;
		
		log.debug( "entered distinguishedName()" );
		name = null;
		
		
		try {      // for error handling
			token = LT(1);
			match(SAFEUTF8STRING);
			
			name = new DN( token.getText() );
			if ( oidsMap != null )
			{
			name.normalize( oidsMap );
			}
			log.debug( "recognized a DistinguishedName: " + token.getText() );
			
		}
		catch (Exception e) {
			
			throw new RecognitionException( "dnParser failed for " + token.getText() + " " + e.getMessage() );
			
		}
		return name ;
	}
	
	public final SubtreeSpecification  subtreeSpecification() throws RecognitionException, TokenStreamException {
		SubtreeSpecification ss;
		
		
		log.debug( "entered subtreeSpecification()" );
		// clear out ss, ssModifier, chopBeforeExclusions and chopAfterExclusions
		// in case something is left from the last parse
		ss = null;
		ssModifier = new SubtreeSpecificationModifier();
		chopBeforeExclusions = new HashSet<DN>();
		chopAfterExclusions = new HashSet<DN>();
		subtreeSpecificationComponentsMonitor = new OptionalComponentsMonitor( 
		new String [] { "base", "specificExclusions", "minimum", "maximum" } );
		
		
		match(OPEN_CURLY);
		{
		_loop311:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop311;
			}
			
		} while (true);
		}
		{
		switch ( LA(1)) {
		case ID_base:
		case ID_specificExclusions:
		case ID_minimum:
		case ID_maximum:
		{
			subtreeSpecificationComponent();
			{
			_loop314:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop314;
				}
				
			} while (true);
			}
			{
			_loop320:
			do {
				if ((LA(1)==SEP)) {
					match(SEP);
					{
					_loop317:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop317;
						}
						
					} while (true);
					}
					subtreeSpecificationComponent();
					{
					_loop319:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop319;
						}
						
					} while (true);
					}
				}
				else {
					break _loop320;
				}
				
			} while (true);
			}
			break;
		}
		case CLOSE_CURLY:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		match(CLOSE_CURLY);
		
		ss = ssModifier.getSubtreeSpecification();
		
		return ss;
	}
	
	public final  UserPermission  userPermission() throws RecognitionException, TokenStreamException {
		 UserPermission userPermission ;
		
		
		log.debug( "entered userPermission()" );
		userPermission = null;
		userPermissionComponentsMonitor = new MandatoryAndOptionalComponentsMonitor( 
		new String [] { "protectedItems", "grantsAndDenials" }, new String [] { "precedence" } );
		
		
		match(OPEN_CURLY);
		{
		_loop299:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop299;
			}
			
		} while (true);
		}
		anyUserPermission();
		{
		_loop301:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop301;
			}
			
		} while (true);
		}
		{
		_loop307:
		do {
			if ((LA(1)==SEP)) {
				match(SEP);
				{
				_loop304:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop304;
					}
					
				} while (true);
				}
				anyUserPermission();
				{
				_loop306:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop306;
					}
					
				} while (true);
				}
			}
			else {
				break _loop307;
			}
			
		} while (true);
		}
		match(CLOSE_CURLY);
		
		if ( !userPermissionComponentsMonitor.finalStateValid() )
		{
		throw new RecognitionException( "Missing mandatory userPermission components: " 
		+ userPermissionComponentsMonitor.getRemainingComponents() );
		}
		
		userPermission = new UserPermission( precedence, grantsAndDenials, protectedItems );
		precedence = -1;
		
		return userPermission ;
	}
	
	public final void anyUserPermission() throws RecognitionException, TokenStreamException {
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case ID_precedence:
			{
				precedence();
				
				userPermissionComponentsMonitor.useComponent( "precedence" );
				
				break;
			}
			case ID_protectedItems:
			{
				protectedItems();
				
				userPermissionComponentsMonitor.useComponent( "protectedItems" );
				
				break;
			}
			case ID_grantsAndDenials:
			{
				grantsAndDenials();
				
				userPermissionComponentsMonitor.useComponent( "grantsAndDenials" );
				
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (IllegalArgumentException e) {
			
			throw new RecognitionException( e.getMessage() );
			
		}
	}
	
	public final void subtreeSpecificationComponent() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered subtreeSpecification()" );
		
		
		try {      // for error handling
			switch ( LA(1)) {
			case ID_base:
			{
				ss_base();
				
				subtreeSpecificationComponentsMonitor.useComponent( "base" );
				
				break;
			}
			case ID_specificExclusions:
			{
				ss_specificExclusions();
				
				subtreeSpecificationComponentsMonitor.useComponent( "specificExclusions" );
				
				break;
			}
			case ID_minimum:
			{
				ss_minimum();
				
				subtreeSpecificationComponentsMonitor.useComponent( "minimum" );
				
				break;
			}
			case ID_maximum:
			{
				ss_maximum();
				
				subtreeSpecificationComponentsMonitor.useComponent( "maximum" );
				
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
		}
		catch (IllegalArgumentException e) {
			
			throw new RecognitionException( e.getMessage() );
			
		}
	}
	
	public final void ss_base() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered ss_base()" );
		DN base = null;
		
		
		match(ID_base);
		{
		int _cnt324=0;
		_loop324:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt324>=1 ) { break _loop324; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt324++;
		} while (true);
		}
		base=distinguishedName();
		
		ssModifier.setBase( base );
		
	}
	
	public final void ss_specificExclusions() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered ss_specificExclusions()" );
		
		
		match(ID_specificExclusions);
		{
		int _cnt327=0;
		_loop327:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt327>=1 ) { break _loop327; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt327++;
		} while (true);
		}
		specificExclusions();
		
		ssModifier.setChopBeforeExclusions( chopBeforeExclusions );
		ssModifier.setChopAfterExclusions( chopAfterExclusions );
		
	}
	
	public final void ss_minimum() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered ss_minimum()" );
		int minimum = 0;
		
		
		match(ID_minimum);
		{
		int _cnt353=0;
		_loop353:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt353>=1 ) { break _loop353; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt353++;
		} while (true);
		}
		minimum=baseDistance();
		
		ssModifier.setMinBaseDistance( minimum );
		
	}
	
	public final void ss_maximum() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered ss_maximum()" );
		int maximum = 0;
		
		
		match(ID_maximum);
		{
		int _cnt356=0;
		_loop356:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt356>=1 ) { break _loop356; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt356++;
		} while (true);
		}
		maximum=baseDistance();
		
		ssModifier.setMaxBaseDistance( maximum );
		
	}
	
	public final void specificExclusions() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered specificExclusions()" );
		
		
		match(OPEN_CURLY);
		{
		_loop330:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop330;
			}
			
		} while (true);
		}
		{
		switch ( LA(1)) {
		case ID_chopBefore:
		case ID_chopAfter:
		{
			specificExclusion();
			{
			_loop333:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop333;
				}
				
			} while (true);
			}
			{
			_loop339:
			do {
				if ((LA(1)==SEP)) {
					match(SEP);
					{
					_loop336:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop336;
						}
						
					} while (true);
					}
					specificExclusion();
					{
					_loop338:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop338;
						}
						
					} while (true);
					}
				}
				else {
					break _loop339;
				}
				
			} while (true);
			}
			break;
		}
		case CLOSE_CURLY:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		match(CLOSE_CURLY);
	}
	
	public final void specificExclusion() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered specificExclusion()" );
		
		
		switch ( LA(1)) {
		case ID_chopBefore:
		{
			chopBefore();
			break;
		}
		case ID_chopAfter:
		{
			chopAfter();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
	}
	
	public final void chopBefore() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered chopBefore()" );
		DN chopBeforeExclusion = null;
		
		
		match(ID_chopBefore);
		{
		_loop343:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop343;
			}
			
		} while (true);
		}
		match(COLON);
		{
		_loop345:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop345;
			}
			
		} while (true);
		}
		chopBeforeExclusion=distinguishedName();
		
		chopBeforeExclusions.add( chopBeforeExclusion );
		
	}
	
	public final void chopAfter() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered chopAfter()" );
		DN chopAfterExclusion = null;
		
		
		match(ID_chopAfter);
		{
		_loop348:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop348;
			}
			
		} while (true);
		}
		match(COLON);
		{
		_loop350:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop350;
			}
			
		} while (true);
		}
		chopAfterExclusion=distinguishedName();
		
		chopAfterExclusions.add( chopAfterExclusion );
		
	}
	
	public final  int  baseDistance() throws RecognitionException, TokenStreamException {
		 int distance ;
		
		Token  token = null;
		
		log.debug( "entered baseDistance()" );
		distance = 0;
		
		
		token = LT(1);
		match(INTEGER);
		
		distance = token2Integer( token );
		
		return distance ;
	}
	
	public final  LeafNode  item() throws RecognitionException, TokenStreamException {
		 LeafNode node ;
		
		
		log.debug( "entered item()" );
		node = null;
		String oid = null;
		
		
		match(ID_item);
		{
		_loop364:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop364;
			}
			
		} while (true);
		}
		match(COLON);
		{
		_loop366:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop366;
			}
			
		} while (true);
		}
		oid=oid();
		
		node = new EqualityNode( SchemaConstants.OBJECT_CLASS_AT , new StringValue( oid ) );
		
		return node ;
	}
	
	public final  BranchNode  and() throws RecognitionException, TokenStreamException {
		 BranchNode node ;
		
		
		log.debug( "entered and()" );
		node = null;
		List<ExprNode> children = null; 
		
		
		match(ID_and);
		{
		_loop369:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop369;
			}
			
		} while (true);
		}
		match(COLON);
		{
		_loop371:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop371;
			}
			
		} while (true);
		}
		children=refinements();
		
		node = new AndNode( children );
		
		return node ;
	}
	
	public final  BranchNode  or() throws RecognitionException, TokenStreamException {
		 BranchNode node ;
		
		
		log.debug( "entered or()" );
		node = null;
		List<ExprNode> children = null; 
		
		
		match(ID_or);
		{
		_loop374:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop374;
			}
			
		} while (true);
		}
		match(COLON);
		{
		_loop376:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop376;
			}
			
		} while (true);
		}
		children=refinements();
		
		node = new OrNode( children );
		
		return node ;
	}
	
	public final  BranchNode  not() throws RecognitionException, TokenStreamException {
		 BranchNode node ;
		
		
		log.debug( "entered not()" );
		node = null;
		List<ExprNode> children = null;
		
		
		match(ID_not);
		{
		_loop379:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop379;
			}
			
		} while (true);
		}
		match(COLON);
		{
		_loop381:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop381;
			}
			
		} while (true);
		}
		children=refinements();
		
		node = new NotNode( children );
		
		return node ;
	}
	
	public final  List<ExprNode>  refinements() throws RecognitionException, TokenStreamException {
		 List<ExprNode> children ;
		
		
		log.debug( "entered refinements()" );
		children = null;
		ExprNode child = null;
		List<ExprNode> tempChildren = new ArrayList<ExprNode>();
		
		
		match(OPEN_CURLY);
		{
		_loop384:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop384;
			}
			
		} while (true);
		}
		{
		switch ( LA(1)) {
		case ID_item:
		case ID_and:
		case ID_or:
		case ID_not:
		{
			child=refinement();
			{
			_loop387:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop387;
				}
				
			} while (true);
			}
			
			tempChildren.add( child );
			
			{
			_loop393:
			do {
				if ((LA(1)==SEP)) {
					match(SEP);
					{
					_loop390:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop390;
						}
						
					} while (true);
					}
					child=refinement();
					{
					_loop392:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop392;
						}
						
					} while (true);
					}
					
					tempChildren.add( child );
					
				}
				else {
					break _loop393;
				}
				
			} while (true);
			}
			break;
		}
		case CLOSE_CURLY:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		match(CLOSE_CURLY);
		
		children = tempChildren;
		
		return children ;
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"ATTRIBUTE_VALUE_CANDIDATE",
		"RANGE_OF_VALUES_CANDIDATE",
		"SP",
		"OPEN_CURLY",
		"SEP",
		"CLOSE_CURLY",
		"\"identificationTag\"",
		"SAFEUTF8STRING",
		"\"precedence\"",
		"INTEGER",
		"\"authenticationLevel\"",
		"\"none\"",
		"\"simple\"",
		"\"strong\"",
		"\"itemOrUserFirst\"",
		"\"itemFirst\"",
		"COLON",
		"\"userFirst\"",
		"\"protectedItems\"",
		"\"entry\"",
		"\"allUserAttributeTypes\"",
		"\"attributeType\"",
		"\"allAttributeValues\"",
		"\"allUserAttributeTypesAndValues\"",
		"\"selfValue\"",
		"\"maxValueCount\"",
		"\"type\"",
		"\"maxCount\"",
		"\"maxImmSub\"",
		"\"restrictedBy\"",
		"\"valuesIn\"",
		"\"classes\"",
		"\"itemPermissions\"",
		"\"grantsAndDenials\"",
		"\"grantAdd\"",
		"\"denyAdd\"",
		"\"grantDiscloseOnError\"",
		"\"denyDiscloseOnError\"",
		"\"grantRead\"",
		"\"denyRead\"",
		"\"grantRemove\"",
		"\"denyRemove\"",
		"\"grantBrowse\"",
		"\"denyBrowse\"",
		"\"grantExport\"",
		"\"denyExport\"",
		"\"grantImport\"",
		"\"denyImport\"",
		"\"grantModify\"",
		"\"denyModify\"",
		"\"grantRename\"",
		"\"denyRename\"",
		"\"grantReturnDN\"",
		"\"denyReturnDN\"",
		"\"grantCompare\"",
		"\"denyCompare\"",
		"\"grantFilterMatch\"",
		"\"denyFilterMatch\"",
		"\"grantInvoke\"",
		"\"denyInvoke\"",
		"\"userClasses\"",
		"\"allUsers\"",
		"\"thisEntry\"",
		"\"parentOfEntry\"",
		"\"name\"",
		"\"userGroup\"",
		"\"subtree\"",
		"\"userPermissions\"",
		"\"base\"",
		"\"specificExclusions\"",
		"\"chopBefore\"",
		"\"chopAfter\"",
		"\"minimum\"",
		"\"maximum\"",
		"DESCR",
		"NUMERICOID",
		"\"item\"",
		"\"and\"",
		"\"or\"",
		"\"not\"",
		"\"FALSE\"",
		"\"TRUE\"",
		"\"level\"",
		"\"basicLevels\"",
		"\"localQualifier\"",
		"\"signed\"",
		"\"rangeOfValues\"",
		"\"specificationFilter\"",
		"SAFEUTF8CHAR",
		"DIGIT",
		"LDIGIT",
		"ALPHA",
		"HYPHEN",
		"DOT",
		"INTEGER_OR_NUMERICOID",
		"FILTER",
		"FILTER_VALUE"
	};
	
	
	}
