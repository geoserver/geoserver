// $ANTLR 2.7.4: "TriggerSpecification.g" -> "AntlrTriggerSpecificationParser.java"$

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


package org.apache.directory.shared.ldap.trigger;

import java.util.ArrayList;
import java.util.List;

import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.NormalizerMappingResolver;
import org.apache.directory.shared.ldap.trigger.StoredProcedureOption;
import org.apache.directory.shared.ldap.trigger.StoredProcedureParameter;
import org.apache.directory.shared.ldap.trigger.TriggerSpecification.SPSpec;
import org.apache.directory.shared.ldap.filter.SearchScope;

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
 * The ANTLR generated TriggerSpecification parser.
 * 
 * @see http://docs.safehaus.org/display/APACHEDS/Grammar+for+Triggers
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$, $Date:$
 */
public class AntlrTriggerSpecificationParser extends antlr.LLkParser       implements AntlrTriggerSpecificationParserTokenTypes
 {

    private static final Logger log = LoggerFactory.getLogger( AntlrTriggerSpecificationParser.class );
    
    private NormalizerMappingResolver resolver;
    
    private ActionTime triggerActionTime;
    
    private LdapOperation triggerLdapOperation;
    
    private String triggerStoredProcedureName;
    
    private List<StoredProcedureParameter> triggerStoredProcedureParameters;
    
    private List<StoredProcedureOption> triggerStoredProcedureOptions;
    
    private List<SPSpec> spSpecs;   
    
    public void init()
    {
    }


    /**
     * Sets the NameComponentNormalizer for this parser's dnParser.
     */
    public void setNormalizerMappingResolver( NormalizerMappingResolver resolver )
    {
        this.resolver = resolver;
    }

protected AntlrTriggerSpecificationParser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public AntlrTriggerSpecificationParser(TokenBuffer tokenBuf) {
  this(tokenBuf,1);
}

protected AntlrTriggerSpecificationParser(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public AntlrTriggerSpecificationParser(TokenStream lexer) {
  this(lexer,1);
}

public AntlrTriggerSpecificationParser(ParserSharedInputState state) {
  super(state,1);
  tokenNames = _tokenNames;
}

	public final  TriggerSpecification  wrapperEntryPoint() throws RecognitionException, TokenStreamException {
		 TriggerSpecification triggerSpec ;
		
		
		log.debug( "entered wrapperEntryPoint()" );
		triggerSpec = null;
		spSpecs = new ArrayList<SPSpec>(); 
		
		
		{
		_loop1704:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1704;
			}
			
		} while (true);
		}
		triggerSpec=triggerSpecification();
		match(Token.EOF_TYPE);
		return triggerSpec ;
	}
	
	public final  TriggerSpecification  triggerSpecification() throws RecognitionException, TokenStreamException {
		 TriggerSpecification triggerSpec ;
		
		
		log.debug( "entered triggerSpecification()" );
		triggerSpec = null;
		
		
		actionTime();
		{
		int _cnt1707=0;
		_loop1707:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt1707>=1 ) { break _loop1707; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt1707++;
		} while (true);
		}
		ldapOperationAndStoredProcedureCalls();
		triggerSpec = new TriggerSpecification( triggerLdapOperation,
		triggerActionTime,
		spSpecs
		);
		
		return triggerSpec ;
	}
	
	public final void actionTime() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered actionTime()" );
		
		
		match(ID_AFTER);
		triggerActionTime = ActionTime.AFTER;
	}
	
	public final void ldapOperationAndStoredProcedureCalls() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered ldapOperationAndStoredProcedureCall()" );
		
		
		switch ( LA(1)) {
		case ID_modify:
		{
			modifyOperationAndStoredProcedureCalls();
			triggerLdapOperation = LdapOperation.MODIFY;
			break;
		}
		case ID_add:
		{
			addOperationAndStoredProcedureCalls();
			triggerLdapOperation = LdapOperation.ADD;
			break;
		}
		case ID_delete:
		{
			deleteOperationAndStoredProcedureCalls();
			triggerLdapOperation = LdapOperation.DELETE;
			break;
		}
		case ID_modifyDN:
		{
			modifyDNOperationAndStoredProcedureCalls();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
	}
	
	public final void modifyOperationAndStoredProcedureCalls() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered modifyOperationAndStoredProcedureCalls()" );
		
		
		match(ID_modify);
		{
		int _cnt1712=0;
		_loop1712:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt1712>=1 ) { break _loop1712; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt1712++;
		} while (true);
		}
		{
		int _cnt1721=0;
		_loop1721:
		do {
			if ((LA(1)==ID_CALL)) {
				theCompositeRuleForCallAndSPNameAndSPOptionList();
				match(OPEN_PARAN);
				{
				_loop1715:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop1715;
					}
					
				} while (true);
				}
				{
				switch ( LA(1)) {
				case ID_object:
				case ID_modification:
				case ID_oldEntry:
				case ID_newEntry:
				case ID_operationPrincipal:
				case ID_ldapContext:
				{
					modifyStoredProcedureParameterList();
					break;
				}
				case CLOSE_PARAN:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				match(CLOSE_PARAN);
				{
				_loop1718:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop1718;
					}
					
				} while (true);
				}
				match(SEMI);
				{
				_loop1720:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop1720;
					}
					
				} while (true);
				}
				
				spSpecs.add( new SPSpec(triggerStoredProcedureName, triggerStoredProcedureOptions, triggerStoredProcedureParameters ) );
				
			}
			else {
				if ( _cnt1721>=1 ) { break _loop1721; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt1721++;
		} while (true);
		}
	}
	
	public final void addOperationAndStoredProcedureCalls() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered addOperationAndStoredProcedureCalls()" );
		
		
		match(ID_add);
		{
		int _cnt1724=0;
		_loop1724:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt1724>=1 ) { break _loop1724; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt1724++;
		} while (true);
		}
		{
		int _cnt1733=0;
		_loop1733:
		do {
			if ((LA(1)==ID_CALL)) {
				theCompositeRuleForCallAndSPNameAndSPOptionList();
				match(OPEN_PARAN);
				{
				_loop1727:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop1727;
					}
					
				} while (true);
				}
				{
				switch ( LA(1)) {
				case ID_entry:
				case ID_attributes:
				case ID_operationPrincipal:
				case ID_ldapContext:
				{
					addStoredProcedureParameterList();
					break;
				}
				case CLOSE_PARAN:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				match(CLOSE_PARAN);
				{
				_loop1730:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop1730;
					}
					
				} while (true);
				}
				match(SEMI);
				{
				_loop1732:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop1732;
					}
					
				} while (true);
				}
				
				spSpecs.add( new SPSpec(triggerStoredProcedureName, triggerStoredProcedureOptions, triggerStoredProcedureParameters ) );
				
			}
			else {
				if ( _cnt1733>=1 ) { break _loop1733; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt1733++;
		} while (true);
		}
	}
	
	public final void deleteOperationAndStoredProcedureCalls() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered deleteOperationAndStoredProcedureCalls()" );
		
		
		match(ID_delete);
		{
		int _cnt1736=0;
		_loop1736:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt1736>=1 ) { break _loop1736; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt1736++;
		} while (true);
		}
		{
		int _cnt1745=0;
		_loop1745:
		do {
			if ((LA(1)==ID_CALL)) {
				theCompositeRuleForCallAndSPNameAndSPOptionList();
				match(OPEN_PARAN);
				{
				_loop1739:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop1739;
					}
					
				} while (true);
				}
				{
				switch ( LA(1)) {
				case ID_name:
				case ID_deletedEntry:
				case ID_operationPrincipal:
				case ID_ldapContext:
				{
					deleteStoredProcedureParameterList();
					break;
				}
				case CLOSE_PARAN:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				match(CLOSE_PARAN);
				{
				_loop1742:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop1742;
					}
					
				} while (true);
				}
				match(SEMI);
				{
				_loop1744:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop1744;
					}
					
				} while (true);
				}
				
				spSpecs.add( new SPSpec(triggerStoredProcedureName, triggerStoredProcedureOptions, triggerStoredProcedureParameters ) );
				
			}
			else {
				if ( _cnt1745>=1 ) { break _loop1745; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt1745++;
		} while (true);
		}
	}
	
	public final void modifyDNOperationAndStoredProcedureCalls() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered modifyDNOperationAndStoredProcedureCalls()" );
		
		
		match(ID_modifyDN);
		match(DOT);
		{
		int _cnt1758=0;
		_loop1758:
		do {
			if (((LA(1) >= ID_modifyDNRename && LA(1) <= ID_modifyDNImport))) {
				{
				switch ( LA(1)) {
				case ID_modifyDNRename:
				{
					match(ID_modifyDNRename);
					triggerLdapOperation = LdapOperation.MODIFYDN_RENAME;
					break;
				}
				case ID_modifyDNExport:
				{
					match(ID_modifyDNExport);
					triggerLdapOperation = LdapOperation.MODIFYDN_EXPORT;
					break;
				}
				case ID_modifyDNImport:
				{
					match(ID_modifyDNImport);
					triggerLdapOperation = LdapOperation.MODIFYDN_IMPORT;
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				{
				int _cnt1750=0;
				_loop1750:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						if ( _cnt1750>=1 ) { break _loop1750; } else {throw new NoViableAltException(LT(1), getFilename());}
					}
					
					_cnt1750++;
				} while (true);
				}
				theCompositeRuleForCallAndSPNameAndSPOptionList();
				match(OPEN_PARAN);
				{
				_loop1752:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop1752;
					}
					
				} while (true);
				}
				{
				switch ( LA(1)) {
				case ID_entry:
				case ID_newrdn:
				case ID_deleteoldrdn:
				case ID_newSuperior:
				case ID_oldRDN:
				case ID_oldSuperiorDN:
				case ID_newDN:
				case ID_operationPrincipal:
				case ID_ldapContext:
				{
					modifyDNStoredProcedureParameterList();
					break;
				}
				case CLOSE_PARAN:
				{
					break;
				}
				default:
				{
					throw new NoViableAltException(LT(1), getFilename());
				}
				}
				}
				match(CLOSE_PARAN);
				{
				_loop1755:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop1755;
					}
					
				} while (true);
				}
				match(SEMI);
				{
				_loop1757:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop1757;
					}
					
				} while (true);
				}
				
				spSpecs.add( new SPSpec(triggerStoredProcedureName, triggerStoredProcedureOptions, triggerStoredProcedureParameters ) );
				
			}
			else {
				if ( _cnt1758>=1 ) { break _loop1758; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt1758++;
		} while (true);
		}
	}
	
	public final void theCompositeRuleForCallAndSPNameAndSPOptionList() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered theCompositeRuleForCallAndSPNameAndSPOptionList()" );
		
		
		match(ID_CALL);
		
		triggerStoredProcedureName = null;
		triggerStoredProcedureParameters = new ArrayList<StoredProcedureParameter>();
		triggerStoredProcedureOptions = new ArrayList<StoredProcedureOption>();
		
		{
		int _cnt1761=0;
		_loop1761:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt1761>=1 ) { break _loop1761; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt1761++;
		} while (true);
		}
		triggerStoredProcedureName=fullyQualifiedStoredProcedureName();
		{
		_loop1763:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1763;
			}
			
		} while (true);
		}
		{
		switch ( LA(1)) {
		case OPEN_CURLY:
		{
			genericStoredProcedureOptionList();
			{
			_loop1766:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop1766;
				}
				
			} while (true);
			}
			break;
		}
		case OPEN_PARAN:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		
	}
	
	public final void modifyStoredProcedureParameterList() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered modifyStoredProcedureParameterList()" );
		
		
		modifyStoredProcedureParameter();
		{
		_loop1769:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1769;
			}
			
		} while (true);
		}
		{
		_loop1775:
		do {
			if ((LA(1)==SEP)) {
				match(SEP);
				{
				_loop1772:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop1772;
					}
					
				} while (true);
				}
				modifyStoredProcedureParameter();
				{
				_loop1774:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop1774;
					}
					
				} while (true);
				}
			}
			else {
				break _loop1775;
			}
			
		} while (true);
		}
	}
	
	public final void addStoredProcedureParameterList() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered addStoredProcedureParameterList()" );
		
		
		addStoredProcedureParameter();
		{
		_loop1778:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1778;
			}
			
		} while (true);
		}
		{
		_loop1784:
		do {
			if ((LA(1)==SEP)) {
				match(SEP);
				{
				_loop1781:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop1781;
					}
					
				} while (true);
				}
				addStoredProcedureParameter();
				{
				_loop1783:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop1783;
					}
					
				} while (true);
				}
			}
			else {
				break _loop1784;
			}
			
		} while (true);
		}
	}
	
	public final void deleteStoredProcedureParameterList() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered deleteStoredProcedureParameterList()" );
		
		
		deleteStoredProcedureParameter();
		{
		_loop1787:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1787;
			}
			
		} while (true);
		}
		{
		_loop1793:
		do {
			if ((LA(1)==SEP)) {
				match(SEP);
				{
				_loop1790:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop1790;
					}
					
				} while (true);
				}
				deleteStoredProcedureParameter();
				{
				_loop1792:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop1792;
					}
					
				} while (true);
				}
			}
			else {
				break _loop1793;
			}
			
		} while (true);
		}
	}
	
	public final void modifyDNStoredProcedureParameterList() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered modifyDNStoredProcedureParameterList()" );
		
		
		modifyDNStoredProcedureParameter();
		{
		_loop1796:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1796;
			}
			
		} while (true);
		}
		{
		_loop1802:
		do {
			if ((LA(1)==SEP)) {
				match(SEP);
				{
				_loop1799:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop1799;
					}
					
				} while (true);
				}
				modifyDNStoredProcedureParameter();
				{
				_loop1801:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop1801;
					}
					
				} while (true);
				}
			}
			else {
				break _loop1802;
			}
			
		} while (true);
		}
	}
	
	public final  String  fullyQualifiedStoredProcedureName() throws RecognitionException, TokenStreamException {
		 String spName ;
		
		Token  spNameToken = null;
		
		log.debug( "entered fullyQualifiedStoredProcedureName()" );
		spName = null;
		
		
		spNameToken = LT(1);
		match(UTF8String);
		spName = spNameToken.getText();
		return spName ;
	}
	
	public final void genericStoredProcedureOptionList() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered genericStoredProcedureOptionList()" );
		
		
		match(OPEN_CURLY);
		{
		_loop1813:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1813;
			}
			
		} while (true);
		}
		{
		_loop1823:
		do {
			if ((LA(1)==ID_languageScheme||LA(1)==ID_searchContext)) {
				genericStoredProcedureOption();
				{
				_loop1816:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop1816;
					}
					
				} while (true);
				}
				{
				_loop1822:
				do {
					if ((LA(1)==SEP)) {
						match(SEP);
						{
						_loop1819:
						do {
							if ((LA(1)==SP)) {
								match(SP);
							}
							else {
								break _loop1819;
							}
							
						} while (true);
						}
						genericStoredProcedureOption();
						{
						_loop1821:
						do {
							if ((LA(1)==SP)) {
								match(SP);
							}
							else {
								break _loop1821;
							}
							
						} while (true);
						}
					}
					else {
						break _loop1822;
					}
					
				} while (true);
				}
			}
			else {
				break _loop1823;
			}
			
		} while (true);
		}
		match(CLOSE_CURLY);
	}
	
	public final void modifyStoredProcedureParameter() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered modifyStoredProcedureParameter()" );
		
		
		switch ( LA(1)) {
		case ID_object:
		{
			match(ID_object);
			triggerStoredProcedureParameters.add( StoredProcedureParameter.Modify_OBJECT.instance() );
			break;
		}
		case ID_modification:
		{
			match(ID_modification);
			triggerStoredProcedureParameters.add( StoredProcedureParameter.Modify_MODIFICATION.instance() );
			break;
		}
		case ID_oldEntry:
		{
			match(ID_oldEntry);
			triggerStoredProcedureParameters.add( StoredProcedureParameter.Modify_OLD_ENTRY.instance() );
			break;
		}
		case ID_newEntry:
		{
			match(ID_newEntry);
			triggerStoredProcedureParameters.add( StoredProcedureParameter.Modify_NEW_ENTRY.instance() );
			break;
		}
		case ID_operationPrincipal:
		case ID_ldapContext:
		{
			genericStoredProcedureParameter();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
	}
	
	public final void addStoredProcedureParameter() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered addStoredProcedureParameter()" );
		
		
		switch ( LA(1)) {
		case ID_entry:
		{
			match(ID_entry);
			triggerStoredProcedureParameters.add( StoredProcedureParameter.Add_ENTRY.instance() );
			break;
		}
		case ID_attributes:
		{
			match(ID_attributes);
			triggerStoredProcedureParameters.add( StoredProcedureParameter.Add_ATTRIBUTES.instance() );
			break;
		}
		case ID_operationPrincipal:
		case ID_ldapContext:
		{
			genericStoredProcedureParameter();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
	}
	
	public final void deleteStoredProcedureParameter() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered deleteStoredProcedureParameter()" );
		
		
		switch ( LA(1)) {
		case ID_name:
		{
			match(ID_name);
			triggerStoredProcedureParameters.add( StoredProcedureParameter.Delete_NAME.instance() );
			break;
		}
		case ID_deletedEntry:
		{
			match(ID_deletedEntry);
			triggerStoredProcedureParameters.add( StoredProcedureParameter.Delete_DELETED_ENTRY.instance() );
			break;
		}
		case ID_operationPrincipal:
		case ID_ldapContext:
		{
			genericStoredProcedureParameter();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
	}
	
	public final void modifyDNStoredProcedureParameter() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered modifyDNStoredProcedureParameter()" );
		
		
		switch ( LA(1)) {
		case ID_entry:
		{
			match(ID_entry);
			triggerStoredProcedureParameters.add( StoredProcedureParameter.ModifyDN_ENTRY.instance() );
			break;
		}
		case ID_newrdn:
		{
			match(ID_newrdn);
			triggerStoredProcedureParameters.add( StoredProcedureParameter.ModifyDN_NEW_RDN.instance() );
			break;
		}
		case ID_deleteoldrdn:
		{
			match(ID_deleteoldrdn);
			triggerStoredProcedureParameters.add( StoredProcedureParameter.ModifyDN_DELETE_OLD_RDN.instance() );
			break;
		}
		case ID_newSuperior:
		{
			match(ID_newSuperior);
			triggerStoredProcedureParameters.add( StoredProcedureParameter.ModifyDN_NEW_SUPERIOR.instance() );
			break;
		}
		case ID_oldRDN:
		{
			match(ID_oldRDN);
			triggerStoredProcedureParameters.add( StoredProcedureParameter.ModifyDN_OLD_RDN.instance() );
			break;
		}
		case ID_oldSuperiorDN:
		{
			match(ID_oldSuperiorDN);
			triggerStoredProcedureParameters.add( StoredProcedureParameter.ModifyDN_OLD_SUPERIOR_DN.instance() );
			break;
		}
		case ID_newDN:
		{
			match(ID_newDN);
			triggerStoredProcedureParameters.add( StoredProcedureParameter.ModifyDN_NEW_DN.instance() );
			break;
		}
		case ID_operationPrincipal:
		case ID_ldapContext:
		{
			genericStoredProcedureParameter();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
	}
	
	public final void genericStoredProcedureParameter() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered genericStoredProcedureParameter()" );
		
		
		switch ( LA(1)) {
		case ID_ldapContext:
		{
			ldapContextStoredProcedureParameter();
			break;
		}
		case ID_operationPrincipal:
		{
			match(ID_operationPrincipal);
			triggerStoredProcedureParameters.add( StoredProcedureParameter.Generic_OPERATION_PRINCIPAL.instance() );
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
	}
	
	public final void ldapContextStoredProcedureParameter() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered ldapContextStoredProcedureParameter()" );
		DN ldapContext = null;
		
		
		match(ID_ldapContext);
		{
		int _cnt1810=0;
		_loop1810:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt1810>=1 ) { break _loop1810; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt1810++;
		} while (true);
		}
		ldapContext=distinguishedName();
		triggerStoredProcedureParameters.add( StoredProcedureParameter.Generic_LDAP_CONTEXT.instance( ldapContext ) );
	}
	
	public final  DN  distinguishedName() throws RecognitionException, TokenStreamException {
		 DN name ;
		
		Token  nameToken = null;
		
		log.debug( "entered distinguishedName()" );
		name = null;
		
		
		try {      // for error handling
			nameToken = LT(1);
			match(UTF8String);
			
			name = new DN( nameToken.getText() );
			
		}
		catch (Exception e) {
			
			throw new RecognitionException( "name parse failed for " + nameToken.getText() + " " + e.getMessage() );
			
		}
		return name ;
	}
	
	public final void genericStoredProcedureOption() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered genericStoredProcedureOption()" );
		StoredProcedureOption spOption = null;
		
		
		{
		switch ( LA(1)) {
		case ID_languageScheme:
		{
			spOption=storedProcedureLanguageSchemeOption();
			break;
		}
		case ID_searchContext:
		{
			spOption=storedProcedureSearchContextOption();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		triggerStoredProcedureOptions.add( spOption );
	}
	
	public final  StoredProcedureLanguageSchemeOption  storedProcedureLanguageSchemeOption() throws RecognitionException, TokenStreamException {
		 StoredProcedureLanguageSchemeOption spLanguageSchemeOption ;
		
		Token  languageToken = null;
		
		log.debug( "entered storedProcedureLanguageSchemeOption()" );
		spLanguageSchemeOption = null;
		
		
		match(ID_languageScheme);
		{
		int _cnt1828=0;
		_loop1828:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt1828>=1 ) { break _loop1828; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt1828++;
		} while (true);
		}
		languageToken = LT(1);
		match(UTF8String);
		spLanguageSchemeOption = new StoredProcedureLanguageSchemeOption( languageToken.getText() );
		return spLanguageSchemeOption ;
	}
	
	public final  StoredProcedureSearchContextOption  storedProcedureSearchContextOption() throws RecognitionException, TokenStreamException {
		 StoredProcedureSearchContextOption spSearchContextOption ;
		
		
		log.debug( "entered storedProcedureSearchContextOption()" );
		spSearchContextOption = null;
		SearchScope searchScope = SearchScope.OBJECT; // default scope
		DN spSearchContext = null;
		
		
		match(ID_searchContext);
		{
		int _cnt1831=0;
		_loop1831:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt1831>=1 ) { break _loop1831; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt1831++;
		} while (true);
		}
		{
		switch ( LA(1)) {
		case OPEN_CURLY:
		{
			match(OPEN_CURLY);
			{
			_loop1834:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop1834;
				}
				
			} while (true);
			}
			{
			switch ( LA(1)) {
			case ID_search_scope:
			{
				match(ID_search_scope);
				{
				int _cnt1837=0;
				_loop1837:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						if ( _cnt1837>=1 ) { break _loop1837; } else {throw new NoViableAltException(LT(1), getFilename());}
					}
					
					_cnt1837++;
				} while (true);
				}
				searchScope=storedProcedureSearchScope();
				{
				_loop1839:
				do {
					if ((LA(1)==SP)) {
						match(SP);
					}
					else {
						break _loop1839;
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
			{
			int _cnt1841=0;
			_loop1841:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					if ( _cnt1841>=1 ) { break _loop1841; } else {throw new NoViableAltException(LT(1), getFilename());}
				}
				
				_cnt1841++;
			} while (true);
			}
			break;
		}
		case UTF8String:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		spSearchContext=storedProcedureSearchContext();
		spSearchContextOption = new StoredProcedureSearchContextOption( spSearchContext, searchScope );
		return spSearchContextOption ;
	}
	
	public final  SearchScope  storedProcedureSearchScope() throws RecognitionException, TokenStreamException {
		 SearchScope scope ;
		
		
		log.debug( "entered storedProcedureSearchScope()" );
		scope = null;
		
		
		switch ( LA(1)) {
		case ID_scope_base:
		{
			match(ID_scope_base);
			scope = SearchScope.OBJECT;
			break;
		}
		case ID_scope_one:
		{
			match(ID_scope_one);
			scope = SearchScope.ONELEVEL;
			break;
		}
		case ID_scope_subtree:
		{
			match(ID_scope_subtree);
			scope = SearchScope.SUBTREE;
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return scope ;
	}
	
	public final  DN  storedProcedureSearchContext() throws RecognitionException, TokenStreamException {
		 DN spSearchContext ;
		
		
		log.debug( "entered storedProcedureSearchContext()" );
		spSearchContext = null;
		
		
		spSearchContext=distinguishedName();
		return spSearchContext ;
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"SP",
		"\"after\"",
		"\"modify\"",
		"OPEN_PARAN",
		"CLOSE_PARAN",
		"SEMI",
		"\"add\"",
		"\"delete\"",
		"\"modifydn\"",
		"DOT",
		"\"rename\"",
		"\"export\"",
		"\"import\"",
		"\"call\"",
		"SEP",
		"\"$object\"",
		"\"$modification\"",
		"\"$oldentry\"",
		"\"$newentry\"",
		"\"$entry\"",
		"\"$attributes\"",
		"\"$name\"",
		"\"$deletedentry\"",
		"\"$newrdn\"",
		"\"$deleteoldrdn\"",
		"\"$newSuperior\"",
		"\"$oldRDN\"",
		"\"$oldSuperiorDN\"",
		"\"$newDN\"",
		"\"$operationprincipal\"",
		"\"$ldapcontext\"",
		"OPEN_CURLY",
		"CLOSE_CURLY",
		"\"languagescheme\"",
		"UTF8String",
		"\"searchcontext\"",
		"\"scope\"",
		"\"base\"",
		"\"one\"",
		"\"subtree\"",
		"SAFEUTF8CHAR",
		"COMMENT",
		"IDENTIFIER",
		"ALPHA"
	};
	
	
	}
