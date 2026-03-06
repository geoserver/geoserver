// $ANTLR 2.7.4: "ACIItemChecker.g" -> "AntlrACIItemChecker.java"$

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


import org.apache.directory.shared.ldap.name.NameComponentNormalizer;

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
 * The antlr generated ACIItem checker.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AntlrACIItemChecker extends antlr.LLkParser       implements AntlrACIItemCheckerTokenTypes
 {

    NameComponentNormalizer normalizer;
    
    /**
     * Creates a (normalizing) subordinate DnParser for parsing Names.
     * This method MUST be called for each instance while we cannot do
     * constructor overloading for this class.
     *
     * @return the DnParser to be used for parsing Names
     */
    public void init()
    {
    }

    /**
     * Sets the NameComponentNormalizer for this parser's dnParser.
     */
    public void setNormalizer(NameComponentNormalizer normalizer)
    {
        this.normalizer = normalizer;
    }

protected AntlrACIItemChecker(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public AntlrACIItemChecker(TokenBuffer tokenBuf) {
  this(tokenBuf,1);
}

protected AntlrACIItemChecker(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public AntlrACIItemChecker(TokenStream lexer) {
  this(lexer,1);
}

public AntlrACIItemChecker(ParserSharedInputState state) {
  super(state,1);
  tokenNames = _tokenNames;
}

	public final void wrapperEntryPoint() throws RecognitionException, TokenStreamException {
		
		
		{
		_loop461:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop461;
			}
			
		} while (true);
		}
		theACIItem();
		{
		_loop463:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop463;
			}
			
		} while (true);
		}
		match(Token.EOF_TYPE);
	}
	
	public final void theACIItem() throws RecognitionException, TokenStreamException {
		
		
		match(OPEN_CURLY);
		{
		_loop466:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop466;
			}
			
		} while (true);
		}
		mainACIItemComponent();
		{
		_loop468:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop468;
			}
			
		} while (true);
		}
		{
		_loop474:
		do {
			if ((LA(1)==SEP)) {
				match(SEP);
				{
				_loop471:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop471;
					}
					
				} while (true);
				}
				mainACIItemComponent();
				{
				_loop473:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop473;
					}
					
				} while (true);
				}
			}
			else {
				break _loop474;
			}
			
		} while (true);
		}
		match(CLOSE_CURLY);
	}
	
	public final void mainACIItemComponent() throws RecognitionException, TokenStreamException {
		
		
		switch ( LA(1)) {
		case ID_identificationTag:
		{
			aci_identificationTag();
			break;
		}
		case ID_precedence:
		{
			aci_precedence();
			break;
		}
		case ID_authenticationLevel:
		{
			aci_authenticationLevel();
			break;
		}
		case ID_itemOrUserFirst:
		{
			aci_itemOrUserFirst();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
	}
	
	public final void aci_identificationTag() throws RecognitionException, TokenStreamException {
		
		
		match(ID_identificationTag);
		{
		int _cnt478=0;
		_loop478:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt478>=1 ) { break _loop478; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt478++;
		} while (true);
		}
		match(SAFEUTF8STRING);
	}
	
	public final void aci_precedence() throws RecognitionException, TokenStreamException {
		
		
		precedence();
	}
	
	public final void aci_authenticationLevel() throws RecognitionException, TokenStreamException {
		
		
		match(ID_authenticationLevel);
		{
		int _cnt485=0;
		_loop485:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt485>=1 ) { break _loop485; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt485++;
		} while (true);
		}
		authenticationLevel();
	}
	
	public final void aci_itemOrUserFirst() throws RecognitionException, TokenStreamException {
		
		
		match(ID_itemOrUserFirst);
		{
		int _cnt489=0;
		_loop489:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt489>=1 ) { break _loop489; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt489++;
		} while (true);
		}
		itemOrUserFirst();
	}
	
	public final void precedence() throws RecognitionException, TokenStreamException {
		
		
		match(ID_precedence);
		{
		int _cnt482=0;
		_loop482:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt482>=1 ) { break _loop482; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt482++;
		} while (true);
		}
		match(INTEGER);
	}
	
	public final void authenticationLevel() throws RecognitionException, TokenStreamException {
		
		
		switch ( LA(1)) {
		case ID_none:
		{
			match(ID_none);
			break;
		}
		case ID_simple:
		{
			match(ID_simple);
			break;
		}
		case ID_strong:
		{
			match(ID_strong);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
	}
	
	public final void itemOrUserFirst() throws RecognitionException, TokenStreamException {
		
		
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
		
		
		match(ID_itemFirst);
		{
		_loop493:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop493;
			}
			
		} while (true);
		}
		match(COLON);
		{
		_loop495:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop495;
			}
			
		} while (true);
		}
		match(OPEN_CURLY);
		{
		_loop497:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop497;
			}
			
		} while (true);
		}
		{
		switch ( LA(1)) {
		case ID_protectedItems:
		{
			protectedItems();
			{
			_loop500:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop500;
				}
				
			} while (true);
			}
			match(SEP);
			{
			_loop502:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop502;
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
			_loop504:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop504;
				}
				
			} while (true);
			}
			match(SEP);
			{
			_loop506:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop506;
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
		_loop508:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop508;
			}
			
		} while (true);
		}
		match(CLOSE_CURLY);
	}
	
	public final void userFirst() throws RecognitionException, TokenStreamException {
		
		
		match(ID_userFirst);
		{
		_loop511:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop511;
			}
			
		} while (true);
		}
		match(COLON);
		{
		_loop513:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop513;
			}
			
		} while (true);
		}
		match(OPEN_CURLY);
		{
		_loop515:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop515;
			}
			
		} while (true);
		}
		{
		switch ( LA(1)) {
		case ID_userClasses:
		{
			userClasses();
			{
			_loop518:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop518;
				}
				
			} while (true);
			}
			match(SEP);
			{
			_loop520:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop520;
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
			_loop522:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop522;
				}
				
			} while (true);
			}
			match(SEP);
			{
			_loop524:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop524;
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
		_loop526:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop526;
			}
			
		} while (true);
		}
		match(CLOSE_CURLY);
	}
	
	public final void protectedItems() throws RecognitionException, TokenStreamException {
		
		
		match(ID_protectedItems);
		{
		_loop529:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop529;
			}
			
		} while (true);
		}
		match(OPEN_CURLY);
		{
		_loop531:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop531;
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
			_loop534:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop534;
				}
				
			} while (true);
			}
			{
			_loop540:
			do {
				if ((LA(1)==SEP)) {
					match(SEP);
					{
					_loop537:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop537;
						}
						
					} while (true);
					}
					protectedItem();
					{
					_loop539:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop539;
						}
						
					} while (true);
					}
				}
				else {
					break _loop540;
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
	
	public final void itemPermissions() throws RecognitionException, TokenStreamException {
		
		
		match(ID_itemPermissions);
		{
		int _cnt645=0;
		_loop645:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt645>=1 ) { break _loop645; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt645++;
		} while (true);
		}
		match(OPEN_CURLY);
		{
		_loop647:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop647;
			}
			
		} while (true);
		}
		{
		switch ( LA(1)) {
		case OPEN_CURLY:
		{
			itemPermission();
			{
			_loop650:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop650;
				}
				
			} while (true);
			}
			{
			_loop656:
			do {
				if ((LA(1)==SEP)) {
					match(SEP);
					{
					_loop653:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop653;
						}
						
					} while (true);
					}
					itemPermission();
					{
					_loop655:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop655;
						}
						
					} while (true);
					}
				}
				else {
					break _loop656;
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
		
		
		match(ID_userClasses);
		{
		int _cnt686=0;
		_loop686:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt686>=1 ) { break _loop686; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt686++;
		} while (true);
		}
		match(OPEN_CURLY);
		{
		_loop688:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop688;
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
			_loop691:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop691;
				}
				
			} while (true);
			}
			{
			_loop697:
			do {
				if ((LA(1)==SEP)) {
					match(SEP);
					{
					_loop694:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop694;
						}
						
					} while (true);
					}
					userClass();
					{
					_loop696:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop696;
						}
						
					} while (true);
					}
				}
				else {
					break _loop697;
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
	
	public final void userPermissions() throws RecognitionException, TokenStreamException {
		
		
		match(ID_userPermissions);
		{
		int _cnt743=0;
		_loop743:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt743>=1 ) { break _loop743; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt743++;
		} while (true);
		}
		match(OPEN_CURLY);
		{
		_loop745:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop745;
			}
			
		} while (true);
		}
		{
		switch ( LA(1)) {
		case OPEN_CURLY:
		{
			userPermission();
			{
			_loop748:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop748;
				}
				
			} while (true);
			}
			{
			_loop754:
			do {
				if ((LA(1)==SEP)) {
					match(SEP);
					{
					_loop751:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop751;
						}
						
					} while (true);
					}
					userPermission();
					{
					_loop753:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop753;
						}
						
					} while (true);
					}
				}
				else {
					break _loop754;
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
		
		
		match(ID_entry);
	}
	
	public final void allUserAttributeTypes() throws RecognitionException, TokenStreamException {
		
		
		match(ID_allUserAttributeTypes);
	}
	
	public final void attributeType() throws RecognitionException, TokenStreamException {
		
		
		match(ID_attributeType);
		{
		int _cnt546=0;
		_loop546:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt546>=1 ) { break _loop546; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt546++;
		} while (true);
		}
		attributeTypeSet();
	}
	
	public final void allAttributeValues() throws RecognitionException, TokenStreamException {
		
		
		match(ID_allAttributeValues);
		{
		int _cnt549=0;
		_loop549:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt549>=1 ) { break _loop549; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt549++;
		} while (true);
		}
		attributeTypeSet();
	}
	
	public final void allUserAttributeTypesAndValues() throws RecognitionException, TokenStreamException {
		
		
		match(ID_allUserAttributeTypesAndValues);
	}
	
	public final void attributeValue() throws RecognitionException, TokenStreamException {
		
		
		match(ATTRIBUTE_VALUE_CANDIDATE);
	}
	
	public final void selfValue() throws RecognitionException, TokenStreamException {
		
		
		match(ID_selfValue);
		{
		int _cnt554=0;
		_loop554:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt554>=1 ) { break _loop554; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt554++;
		} while (true);
		}
		attributeTypeSet();
	}
	
	public final void rangeOfValues() throws RecognitionException, TokenStreamException {
		
		
		match(RANGE_OF_VALUES_CANDIDATE);
	}
	
	public final void maxValueCount() throws RecognitionException, TokenStreamException {
		
		
		match(ID_maxValueCount);
		{
		int _cnt558=0;
		_loop558:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt558>=1 ) { break _loop558; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt558++;
		} while (true);
		}
		match(OPEN_CURLY);
		{
		_loop560:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop560;
			}
			
		} while (true);
		}
		aMaxValueCount();
		{
		_loop562:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop562;
			}
			
		} while (true);
		}
		{
		_loop568:
		do {
			if ((LA(1)==SEP)) {
				match(SEP);
				{
				_loop565:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop565;
					}
					
				} while (true);
				}
				aMaxValueCount();
				{
				_loop567:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop567;
					}
					
				} while (true);
				}
			}
			else {
				break _loop568;
			}
			
		} while (true);
		}
		match(CLOSE_CURLY);
	}
	
	public final void maxImmSub() throws RecognitionException, TokenStreamException {
		
		
		match(ID_maxImmSub);
		{
		int _cnt593=0;
		_loop593:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt593>=1 ) { break _loop593; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt593++;
		} while (true);
		}
		match(INTEGER);
	}
	
	public final void restrictedBy() throws RecognitionException, TokenStreamException {
		
		
		match(ID_restrictedBy);
		{
		int _cnt596=0;
		_loop596:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt596>=1 ) { break _loop596; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt596++;
		} while (true);
		}
		match(OPEN_CURLY);
		{
		_loop598:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop598;
			}
			
		} while (true);
		}
		restrictedValue();
		{
		_loop600:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop600;
			}
			
		} while (true);
		}
		{
		_loop606:
		do {
			if ((LA(1)==SEP)) {
				match(SEP);
				{
				_loop603:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop603;
					}
					
				} while (true);
				}
				restrictedValue();
				{
				_loop605:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop605;
					}
					
				} while (true);
				}
			}
			else {
				break _loop606;
			}
			
		} while (true);
		}
		match(CLOSE_CURLY);
	}
	
	public final void classes() throws RecognitionException, TokenStreamException {
		
		
		match(ID_classes);
		{
		int _cnt642=0;
		_loop642:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt642>=1 ) { break _loop642; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt642++;
		} while (true);
		}
		refinement();
	}
	
	public final void attributeTypeSet() throws RecognitionException, TokenStreamException {
		
		
		match(OPEN_CURLY);
		{
		_loop631:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop631;
			}
			
		} while (true);
		}
		oid();
		{
		_loop633:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop633;
			}
			
		} while (true);
		}
		{
		_loop639:
		do {
			if ((LA(1)==SEP)) {
				match(SEP);
				{
				_loop636:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop636;
					}
					
				} while (true);
				}
				oid();
				{
				_loop638:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop638;
					}
					
				} while (true);
				}
			}
			else {
				break _loop639;
			}
			
		} while (true);
		}
		match(CLOSE_CURLY);
	}
	
	public final void aMaxValueCount() throws RecognitionException, TokenStreamException {
		
		
		match(OPEN_CURLY);
		{
		_loop571:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop571;
			}
			
		} while (true);
		}
		{
		switch ( LA(1)) {
		case ID_type:
		{
			match(ID_type);
			{
			int _cnt574=0;
			_loop574:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					if ( _cnt574>=1 ) { break _loop574; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt574++;
			} while (true);
			}
			oid();
			{
			_loop576:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop576;
				}
				
			} while (true);
			}
			match(SEP);
			{
			_loop578:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop578;
				}
				
			} while (true);
			}
			match(ID_maxCount);
			{
			int _cnt580=0;
			_loop580:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					if ( _cnt580>=1 ) { break _loop580; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt580++;
			} while (true);
			}
			match(INTEGER);
			break;
		}
		case ID_maxCount:
		{
			match(ID_maxCount);
			{
			int _cnt582=0;
			_loop582:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					if ( _cnt582>=1 ) { break _loop582; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt582++;
			} while (true);
			}
			match(INTEGER);
			{
			_loop584:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop584;
				}
				
			} while (true);
			}
			match(SEP);
			{
			_loop586:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop586;
				}
				
			} while (true);
			}
			match(ID_type);
			{
			int _cnt588=0;
			_loop588:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					if ( _cnt588>=1 ) { break _loop588; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt588++;
			} while (true);
			}
			oid();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		_loop590:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop590;
			}
			
		} while (true);
		}
		match(CLOSE_CURLY);
	}
	
	public final void oid() throws RecognitionException, TokenStreamException {
		
		
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
	}
	
	public final void restrictedValue() throws RecognitionException, TokenStreamException {
		
		
		match(OPEN_CURLY);
		{
		_loop609:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop609;
			}
			
		} while (true);
		}
		{
		switch ( LA(1)) {
		case ID_type:
		{
			match(ID_type);
			{
			int _cnt612=0;
			_loop612:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					if ( _cnt612>=1 ) { break _loop612; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt612++;
			} while (true);
			}
			oid();
			{
			_loop614:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop614;
				}
				
			} while (true);
			}
			match(SEP);
			{
			_loop616:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop616;
				}
				
			} while (true);
			}
			match(ID_valuesIn);
			{
			int _cnt618=0;
			_loop618:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					if ( _cnt618>=1 ) { break _loop618; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt618++;
			} while (true);
			}
			oid();
			break;
		}
		case ID_valuesIn:
		{
			match(ID_valuesIn);
			{
			int _cnt620=0;
			_loop620:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					if ( _cnt620>=1 ) { break _loop620; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt620++;
			} while (true);
			}
			oid();
			{
			_loop622:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop622;
				}
				
			} while (true);
			}
			match(SEP);
			{
			_loop624:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop624;
				}
				
			} while (true);
			}
			match(ID_type);
			{
			int _cnt626=0;
			_loop626:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					if ( _cnt626>=1 ) { break _loop626; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt626++;
			} while (true);
			}
			oid();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		_loop628:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop628;
			}
			
		} while (true);
		}
		match(CLOSE_CURLY);
	}
	
	public final void refinement() throws RecognitionException, TokenStreamException {
		
		
		switch ( LA(1)) {
		case ID_item:
		{
			item();
			break;
		}
		case ID_and:
		{
			and();
			break;
		}
		case ID_or:
		{
			or();
			break;
		}
		case ID_not:
		{
			not();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
	}
	
	public final void itemPermission() throws RecognitionException, TokenStreamException {
		
		
		match(OPEN_CURLY);
		{
		_loop659:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop659;
			}
			
		} while (true);
		}
		anyItemPermission();
		{
		_loop661:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop661;
			}
			
		} while (true);
		}
		{
		_loop667:
		do {
			if ((LA(1)==SEP)) {
				match(SEP);
				{
				_loop664:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop664;
					}
					
				} while (true);
				}
				anyItemPermission();
				{
				_loop666:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop666;
					}
					
				} while (true);
				}
			}
			else {
				break _loop667;
			}
			
		} while (true);
		}
		match(CLOSE_CURLY);
	}
	
	public final void anyItemPermission() throws RecognitionException, TokenStreamException {
		
		
		switch ( LA(1)) {
		case ID_precedence:
		{
			precedence();
			break;
		}
		case ID_userClasses:
		{
			userClasses();
			break;
		}
		case ID_grantsAndDenials:
		{
			grantsAndDenials();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
	}
	
	public final void grantsAndDenials() throws RecognitionException, TokenStreamException {
		
		
		match(ID_grantsAndDenials);
		{
		int _cnt671=0;
		_loop671:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt671>=1 ) { break _loop671; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt671++;
		} while (true);
		}
		match(OPEN_CURLY);
		{
		_loop673:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop673;
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
			grantAndDenial();
			{
			_loop676:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop676;
				}
				
			} while (true);
			}
			{
			_loop682:
			do {
				if ((LA(1)==SEP)) {
					match(SEP);
					{
					_loop679:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop679;
						}
						
					} while (true);
					}
					grantAndDenial();
					{
					_loop681:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop681;
						}
						
					} while (true);
					}
				}
				else {
					break _loop682;
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
	
	public final void grantAndDenial() throws RecognitionException, TokenStreamException {
		
		
		switch ( LA(1)) {
		case ID_grantAdd:
		{
			match(ID_grantAdd);
			break;
		}
		case ID_denyAdd:
		{
			match(ID_denyAdd);
			break;
		}
		case ID_grantDiscloseOnError:
		{
			match(ID_grantDiscloseOnError);
			break;
		}
		case ID_denyDiscloseOnError:
		{
			match(ID_denyDiscloseOnError);
			break;
		}
		case ID_grantRead:
		{
			match(ID_grantRead);
			break;
		}
		case ID_denyRead:
		{
			match(ID_denyRead);
			break;
		}
		case ID_grantRemove:
		{
			match(ID_grantRemove);
			break;
		}
		case ID_denyRemove:
		{
			match(ID_denyRemove);
			break;
		}
		case ID_grantBrowse:
		{
			match(ID_grantBrowse);
			break;
		}
		case ID_denyBrowse:
		{
			match(ID_denyBrowse);
			break;
		}
		case ID_grantExport:
		{
			match(ID_grantExport);
			break;
		}
		case ID_denyExport:
		{
			match(ID_denyExport);
			break;
		}
		case ID_grantImport:
		{
			match(ID_grantImport);
			break;
		}
		case ID_denyImport:
		{
			match(ID_denyImport);
			break;
		}
		case ID_grantModify:
		{
			match(ID_grantModify);
			break;
		}
		case ID_denyModify:
		{
			match(ID_denyModify);
			break;
		}
		case ID_grantRename:
		{
			match(ID_grantRename);
			break;
		}
		case ID_denyRename:
		{
			match(ID_denyRename);
			break;
		}
		case ID_grantReturnDN:
		{
			match(ID_grantReturnDN);
			break;
		}
		case ID_denyReturnDN:
		{
			match(ID_denyReturnDN);
			break;
		}
		case ID_grantCompare:
		{
			match(ID_grantCompare);
			break;
		}
		case ID_denyCompare:
		{
			match(ID_denyCompare);
			break;
		}
		case ID_grantFilterMatch:
		{
			match(ID_grantFilterMatch);
			break;
		}
		case ID_denyFilterMatch:
		{
			match(ID_denyFilterMatch);
			break;
		}
		case ID_grantInvoke:
		{
			match(ID_grantInvoke);
			break;
		}
		case ID_denyInvoke:
		{
			match(ID_denyInvoke);
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
	}
	
	public final void userClass() throws RecognitionException, TokenStreamException {
		
		
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
		
		
		match(ID_allUsers);
	}
	
	public final void thisEntry() throws RecognitionException, TokenStreamException {
		
		
		match(ID_thisEntry);
	}
	
	public final void parentOfEntry() throws RecognitionException, TokenStreamException {
		
		
		match(ID_parentOfEntry);
	}
	
	public final void name() throws RecognitionException, TokenStreamException {
		
		
		match(ID_name);
		{
		int _cnt704=0;
		_loop704:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt704>=1 ) { break _loop704; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt704++;
		} while (true);
		}
		match(OPEN_CURLY);
		{
		_loop706:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop706;
			}
			
		} while (true);
		}
		distinguishedName();
		{
		_loop708:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop708;
			}
			
		} while (true);
		}
		{
		_loop714:
		do {
			if ((LA(1)==SEP)) {
				match(SEP);
				{
				_loop711:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop711;
					}
					
				} while (true);
				}
				distinguishedName();
				{
				_loop713:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop713;
					}
					
				} while (true);
				}
			}
			else {
				break _loop714;
			}
			
		} while (true);
		}
		match(CLOSE_CURLY);
	}
	
	public final void userGroup() throws RecognitionException, TokenStreamException {
		
		
		match(ID_userGroup);
		{
		int _cnt717=0;
		_loop717:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt717>=1 ) { break _loop717; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt717++;
		} while (true);
		}
		match(OPEN_CURLY);
		{
		_loop719:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop719;
			}
			
		} while (true);
		}
		distinguishedName();
		{
		_loop721:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop721;
			}
			
		} while (true);
		}
		{
		_loop727:
		do {
			if ((LA(1)==SEP)) {
				match(SEP);
				{
				_loop724:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop724;
					}
					
				} while (true);
				}
				distinguishedName();
				{
				_loop726:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop726;
					}
					
				} while (true);
				}
			}
			else {
				break _loop727;
			}
			
		} while (true);
		}
		match(CLOSE_CURLY);
	}
	
	public final void subtree() throws RecognitionException, TokenStreamException {
		
		
		match(ID_subtree);
		{
		int _cnt730=0;
		_loop730:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt730>=1 ) { break _loop730; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt730++;
		} while (true);
		}
		match(OPEN_CURLY);
		{
		_loop732:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop732;
			}
			
		} while (true);
		}
		subtreeSpecification();
		{
		_loop734:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop734;
			}
			
		} while (true);
		}
		{
		_loop740:
		do {
			if ((LA(1)==SEP)) {
				match(SEP);
				{
				_loop737:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop737;
					}
					
				} while (true);
				}
				subtreeSpecification();
				{
				_loop739:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop739;
					}
					
				} while (true);
				}
			}
			else {
				break _loop740;
			}
			
		} while (true);
		}
		match(CLOSE_CURLY);
	}
	
	public final void distinguishedName() throws RecognitionException, TokenStreamException {
		
		
		match(SAFEUTF8STRING);
	}
	
	public final void subtreeSpecification() throws RecognitionException, TokenStreamException {
		
		
		match(OPEN_CURLY);
		{
		_loop769:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop769;
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
			_loop772:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop772;
				}
				
			} while (true);
			}
			{
			_loop778:
			do {
				if ((LA(1)==SEP)) {
					match(SEP);
					{
					_loop775:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop775;
						}
						
					} while (true);
					}
					subtreeSpecificationComponent();
					{
					_loop777:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop777;
						}
						
					} while (true);
					}
				}
				else {
					break _loop778;
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
	
	public final void userPermission() throws RecognitionException, TokenStreamException {
		
		
		match(OPEN_CURLY);
		{
		_loop757:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop757;
			}
			
		} while (true);
		}
		anyUserPermission();
		{
		_loop759:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop759;
			}
			
		} while (true);
		}
		{
		_loop765:
		do {
			if ((LA(1)==SEP)) {
				match(SEP);
				{
				_loop762:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop762;
					}
					
				} while (true);
				}
				anyUserPermission();
				{
				_loop764:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop764;
					}
					
				} while (true);
				}
			}
			else {
				break _loop765;
			}
			
		} while (true);
		}
		match(CLOSE_CURLY);
	}
	
	public final void anyUserPermission() throws RecognitionException, TokenStreamException {
		
		
		switch ( LA(1)) {
		case ID_precedence:
		{
			precedence();
			break;
		}
		case ID_protectedItems:
		{
			protectedItems();
			break;
		}
		case ID_grantsAndDenials:
		{
			grantsAndDenials();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
	}
	
	public final void subtreeSpecificationComponent() throws RecognitionException, TokenStreamException {
		
		
		switch ( LA(1)) {
		case ID_base:
		{
			ss_base();
			break;
		}
		case ID_specificExclusions:
		{
			ss_specificExclusions();
			break;
		}
		case ID_minimum:
		{
			ss_minimum();
			break;
		}
		case ID_maximum:
		{
			ss_maximum();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
	}
	
	public final void ss_base() throws RecognitionException, TokenStreamException {
		
		
		match(ID_base);
		{
		int _cnt782=0;
		_loop782:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt782>=1 ) { break _loop782; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt782++;
		} while (true);
		}
		distinguishedName();
	}
	
	public final void ss_specificExclusions() throws RecognitionException, TokenStreamException {
		
		
		match(ID_specificExclusions);
		{
		int _cnt785=0;
		_loop785:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt785>=1 ) { break _loop785; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt785++;
		} while (true);
		}
		specificExclusions();
	}
	
	public final void ss_minimum() throws RecognitionException, TokenStreamException {
		
		
		match(ID_minimum);
		{
		int _cnt811=0;
		_loop811:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt811>=1 ) { break _loop811; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt811++;
		} while (true);
		}
		baseDistance();
	}
	
	public final void ss_maximum() throws RecognitionException, TokenStreamException {
		
		
		match(ID_maximum);
		{
		int _cnt814=0;
		_loop814:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt814>=1 ) { break _loop814; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt814++;
		} while (true);
		}
		baseDistance();
	}
	
	public final void specificExclusions() throws RecognitionException, TokenStreamException {
		
		
		match(OPEN_CURLY);
		{
		_loop788:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop788;
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
			_loop791:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop791;
				}
				
			} while (true);
			}
			{
			_loop797:
			do {
				if ((LA(1)==SEP)) {
					match(SEP);
					{
					_loop794:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop794;
						}
						
					} while (true);
					}
					specificExclusion();
					{
					_loop796:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop796;
						}
						
					} while (true);
					}
				}
				else {
					break _loop797;
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
		
		
		match(ID_chopBefore);
		{
		_loop801:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop801;
			}
			
		} while (true);
		}
		match(COLON);
		{
		_loop803:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop803;
			}
			
		} while (true);
		}
		distinguishedName();
	}
	
	public final void chopAfter() throws RecognitionException, TokenStreamException {
		
		
		match(ID_chopAfter);
		{
		_loop806:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop806;
			}
			
		} while (true);
		}
		match(COLON);
		{
		_loop808:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop808;
			}
			
		} while (true);
		}
		distinguishedName();
	}
	
	public final void baseDistance() throws RecognitionException, TokenStreamException {
		
		
		match(INTEGER);
	}
	
	public final void item() throws RecognitionException, TokenStreamException {
		
		
		match(ID_item);
		{
		_loop822:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop822;
			}
			
		} while (true);
		}
		match(COLON);
		{
		_loop824:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop824;
			}
			
		} while (true);
		}
		oid();
	}
	
	public final void and() throws RecognitionException, TokenStreamException {
		
		
		match(ID_and);
		{
		_loop827:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop827;
			}
			
		} while (true);
		}
		match(COLON);
		{
		_loop829:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop829;
			}
			
		} while (true);
		}
		refinements();
	}
	
	public final void or() throws RecognitionException, TokenStreamException {
		
		
		match(ID_or);
		{
		_loop832:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop832;
			}
			
		} while (true);
		}
		match(COLON);
		{
		_loop834:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop834;
			}
			
		} while (true);
		}
		refinements();
	}
	
	public final void not() throws RecognitionException, TokenStreamException {
		
		
		match(ID_not);
		{
		_loop837:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop837;
			}
			
		} while (true);
		}
		match(COLON);
		{
		_loop839:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop839;
			}
			
		} while (true);
		}
		refinements();
	}
	
	public final void refinements() throws RecognitionException, TokenStreamException {
		
		
		match(OPEN_CURLY);
		{
		_loop842:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop842;
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
			refinement();
			{
			_loop845:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop845;
				}
				
			} while (true);
			}
			{
			_loop851:
			do {
				if ((LA(1)==SEP)) {
					match(SEP);
					{
					_loop848:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop848;
						}
						
					} while (true);
					}
					refinement();
					{
					_loop850:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop850;
						}
						
					} while (true);
					}
				}
				else {
					break _loop851;
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
