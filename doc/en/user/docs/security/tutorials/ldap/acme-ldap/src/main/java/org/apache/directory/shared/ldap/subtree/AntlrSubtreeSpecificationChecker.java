// $ANTLR 2.7.4: "SubtreeSpecificationChecker.g" -> "AntlrSubtreeSpecificationChecker.java"$

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


package org.apache.directory.shared.ldap.subtree;

import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.filter.FilterParser;
import org.apache.directory.shared.ldap.util.ComponentsMonitor;
import org.apache.directory.shared.ldap.util.OptionalComponentsMonitor;

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
 * The antlr generated subtree specification parser.
 *
 * @see <a href="http://www.faqs.org/rfcs/rfc3672.html">RFC 3672</a>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AntlrSubtreeSpecificationChecker extends antlr.LLkParser       implements AntlrSubtreeSpecificationCheckerTokenTypes
 {

    private static final Logger log = LoggerFactory.getLogger( AntlrSubtreeSpecificationChecker.class );
    
    private ComponentsMonitor subtreeSpecificationComponentsMonitor = null;
    
    /**
     * Does nothing.
     */
    public void init()
    {
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

protected AntlrSubtreeSpecificationChecker(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public AntlrSubtreeSpecificationChecker(TokenBuffer tokenBuf) {
  this(tokenBuf,1);
}

protected AntlrSubtreeSpecificationChecker(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public AntlrSubtreeSpecificationChecker(TokenStream lexer) {
  this(lexer,1);
}

public AntlrSubtreeSpecificationChecker(ParserSharedInputState state) {
  super(state,1);
  tokenNames = _tokenNames;
}

	public final void wrapperEntryPoint() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered wrapperEntryPoint()" );
		
		
		subtreeSpecification();
		match(LITERAL_end);
	}
	
	public final void subtreeSpecification() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered subtreeSpecification()" );
		subtreeSpecificationComponentsMonitor = new OptionalComponentsMonitor( 
		new String [] { "base", "specificExclusions", "minimum", "maximum", "specificationFilter" } );
		
		
		match(OPEN_CURLY);
		{
		_loop1561:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1561;
			}
			
		} while (true);
		}
		{
		switch ( LA(1)) {
		case ID_base:
		case ID_specificExclusions:
		case ID_minimum:
		case ID_maximum:
		case ID_specificationFilter:
		{
			subtreeSpecificationComponent();
			{
			_loop1564:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop1564;
				}
				
			} while (true);
			}
			{
			_loop1570:
			do {
				if ((LA(1)==SEP)) {
					match(SEP);
					{
					_loop1567:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop1567;
						}
						
					} while (true);
					}
					subtreeSpecificationComponent();
					{
					_loop1569:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop1569;
						}
						
					} while (true);
					}
				}
				else {
					break _loop1570;
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
			case ID_specificationFilter:
			{
				ss_specificationFilter();
				
				subtreeSpecificationComponentsMonitor.useComponent( "specificationFilter" );
				
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
		
		
		match(ID_base);
		{
		int _cnt1574=0;
		_loop1574:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt1574>=1 ) { break _loop1574; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt1574++;
		} while (true);
		}
		distinguishedName();
	}
	
	public final void ss_specificExclusions() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered ss_specificExclusions()" );
		
		
		match(ID_specificExclusions);
		{
		int _cnt1577=0;
		_loop1577:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt1577>=1 ) { break _loop1577; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt1577++;
		} while (true);
		}
		specificExclusions();
	}
	
	public final void ss_minimum() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered ss_minimum()" );
		
		
		match(ID_minimum);
		{
		int _cnt1603=0;
		_loop1603:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt1603>=1 ) { break _loop1603; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt1603++;
		} while (true);
		}
		baseDistance();
	}
	
	public final void ss_maximum() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered ss_maximum()" );
		
		
		match(ID_maximum);
		{
		int _cnt1606=0;
		_loop1606:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt1606>=1 ) { break _loop1606; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt1606++;
		} while (true);
		}
		baseDistance();
	}
	
	public final void ss_specificationFilter() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered ss_specificationFilter()" );
		
		
		match(ID_specificationFilter);
		{
		int _cnt1609=0;
		_loop1609:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt1609>=1 ) { break _loop1609; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt1609++;
		} while (true);
		}
		{
		switch ( LA(1)) {
		case ID_item:
		case ID_and:
		case ID_or:
		case ID_not:
		{
			{
			refinement();
			}
			break;
		}
		case FILTER:
		{
			{
			filter();
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
	}
	
	public final void distinguishedName() throws RecognitionException, TokenStreamException {
		
		Token  token = null;
		
		log.debug( "entered distinguishedName()" );
		
		
		try {      // for error handling
			token = LT(1);
			match(SAFEUTF8STRING);
			
			new DN( token.getText() );
			log.debug( "recognized a DistinguishedName: " + token.getText() );
			
		}
		catch (Exception e) {
			
			throw new RecognitionException( "dnParser failed for " + token.getText() + " " + e.getMessage() );
			
		}
	}
	
	public final void specificExclusions() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered specificExclusions()" );
		
		
		match(OPEN_CURLY);
		{
		_loop1580:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1580;
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
			_loop1583:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop1583;
				}
				
			} while (true);
			}
			{
			_loop1589:
			do {
				if ((LA(1)==SEP)) {
					match(SEP);
					{
					_loop1586:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop1586;
						}
						
					} while (true);
					}
					specificExclusion();
					{
					_loop1588:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop1588;
						}
						
					} while (true);
					}
				}
				else {
					break _loop1589;
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
		
		
		match(ID_chopBefore);
		{
		_loop1593:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1593;
			}
			
		} while (true);
		}
		match(COLON);
		{
		_loop1595:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1595;
			}
			
		} while (true);
		}
		distinguishedName();
	}
	
	public final void chopAfter() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered chopAfter()" );
		
		
		match(ID_chopAfter);
		{
		_loop1598:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1598;
			}
			
		} while (true);
		}
		match(COLON);
		{
		_loop1600:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1600;
			}
			
		} while (true);
		}
		distinguishedName();
	}
	
	public final void baseDistance() throws RecognitionException, TokenStreamException {
		
		Token  token = null;
		
		log.debug( "entered baseDistance()" );
		
		
		token = LT(1);
		match(INTEGER);
		
		token2Integer(token);
		
	}
	
	public final void refinement() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered refinement()" );
		
		
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
	
	public final void filter() throws RecognitionException, TokenStreamException {
		
		Token  filterToken = null;
		
		log.debug( "entered filter()" );
		
		
		try {      // for error handling
			{
			filterToken = LT(1);
			match(FILTER);
			FilterParser.parse( filterToken.getText() );
			}
		}
		catch (Exception e) {
			
			throw new RecognitionException( "filterParser failed. " + e.getMessage() );
			
		}
	}
	
	public final void oid() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered oid()" );
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
		
		log.debug( "recognized an oid: " + token.getText() );
		
	}
	
	public final void item() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered item()" );
		
		
		match(ID_item);
		{
		_loop1622:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1622;
			}
			
		} while (true);
		}
		match(COLON);
		{
		_loop1624:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1624;
			}
			
		} while (true);
		}
		oid();
	}
	
	public final void and() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered and()" );
		
		
		match(ID_and);
		{
		_loop1627:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1627;
			}
			
		} while (true);
		}
		match(COLON);
		{
		_loop1629:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1629;
			}
			
		} while (true);
		}
		refinements();
	}
	
	public final void or() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered or()" );
		
		
		match(ID_or);
		{
		_loop1632:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1632;
			}
			
		} while (true);
		}
		match(COLON);
		{
		_loop1634:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1634;
			}
			
		} while (true);
		}
		refinements();
	}
	
	public final void not() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered not()" );
		
		
		match(ID_not);
		{
		_loop1637:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1637;
			}
			
		} while (true);
		}
		match(COLON);
		{
		_loop1639:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1639;
			}
			
		} while (true);
		}
		refinements();
	}
	
	public final void refinements() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered refinements()" );
		
		
		match(OPEN_CURLY);
		{
		_loop1642:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1642;
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
			_loop1645:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop1645;
				}
				
			} while (true);
			}
			{
			_loop1651:
			do {
				if ((LA(1)==SEP)) {
					match(SEP);
					{
					_loop1648:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop1648;
						}
						
					} while (true);
					}
					refinement();
					{
					_loop1650:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop1650;
						}
						
					} while (true);
					}
				}
				else {
					break _loop1651;
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
		"\"end\"",
		"OPEN_CURLY",
		"SP",
		"SEP",
		"CLOSE_CURLY",
		"\"base\"",
		"\"specificExclusions\"",
		"\"chopBefore\"",
		"COLON",
		"\"chopAfter\"",
		"\"minimum\"",
		"\"maximum\"",
		"\"specificationFilter\"",
		"FILTER",
		"SAFEUTF8STRING",
		"INTEGER",
		"DESCR",
		"NUMERICOID",
		"\"item\"",
		"\"and\"",
		"\"or\"",
		"\"not\"",
		"INTEGER_OR_NUMERICOID",
		"DOT",
		"DIGIT",
		"LDIGIT",
		"ALPHA",
		"SAFEUTF8CHAR",
		"FILTER_VALUE"
	};
	
	
	}
