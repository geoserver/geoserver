// $ANTLR 2.7.4: "subtree-specification.g" -> "AntlrSubtreeSpecificationParser.java"$

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

import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.LeafNode;
import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.AndNode;
import org.apache.directory.shared.ldap.filter.OrNode;
import org.apache.directory.shared.ldap.filter.NotNode;
import org.apache.directory.shared.ldap.filter.EqualityNode;
import org.apache.directory.shared.ldap.filter.FilterParser;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecification;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecificationModifier;
import org.apache.directory.shared.ldap.schema.NormalizerMappingResolver;
import org.apache.directory.shared.ldap.schema.normalizers.OidNormalizer;
import org.apache.directory.shared.ldap.util.ComponentsMonitor;
import org.apache.directory.shared.ldap.util.OptionalComponentsMonitor;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
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
 * The antlr generated subtree specification parser.
 *
 * @see <a href="http://www.faqs.org/rfcs/rfc3672.html">RFC 3672</a>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AntlrSubtreeSpecificationParser extends antlr.LLkParser       implements AntlrSubtreeSpecificationParserTokenTypes
 {

    private static final Logger log = LoggerFactory.getLogger( AntlrSubtreeSpecificationParser.class );
    
    private NormalizerMappingResolver resolver;
    
    private Set<DN> chopBeforeExclusions = null;
    private Set<DN> chopAfterExclusions = null;

    private SubtreeSpecificationModifier ssModifier = null;
    
    private Map<String, OidNormalizer> oidsMap;
    
    private ComponentsMonitor subtreeSpecificationComponentsMonitor = null;

    /**
     * Does nothing.
     */
    public void init( Map<String, OidNormalizer> oidsMap )
    {
        this.oidsMap = oidsMap;
    }
    
    
    public void setNormalizerMappingResolver( NormalizerMappingResolver resolver )
    {
        this.resolver = resolver;
    }
    
    
    public boolean isNormalizing()
    {
        return this.resolver != null;
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

protected AntlrSubtreeSpecificationParser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public AntlrSubtreeSpecificationParser(TokenBuffer tokenBuf) {
  this(tokenBuf,1);
}

protected AntlrSubtreeSpecificationParser(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public AntlrSubtreeSpecificationParser(TokenStream lexer) {
  this(lexer,1);
}

public AntlrSubtreeSpecificationParser(ParserSharedInputState state) {
  super(state,1);
  tokenNames = _tokenNames;
}

	public final SubtreeSpecification  wrapperEntryPoint() throws RecognitionException, TokenStreamException {
		SubtreeSpecification ss;
		
		
		log.debug( "entered wrapperEntryPoint()" );
		ss = null;
		SubtreeSpecification tempSs = null;
		
		
		tempSs=subtreeSpecification();
		match(LITERAL_end);
		
		ss = tempSs;
		
		return ss;
	}
	
	public final SubtreeSpecification  subtreeSpecification() throws RecognitionException, TokenStreamException {
		SubtreeSpecification ss;
		
		
		log.debug( "entered subtreeSpecification()" );
		// clear out ss, ssModifier, subtreeSpecificationComponentsMonitor,
		// chopBeforeExclusions and chopAfterExclusions
		// in case something is left from the last parse
		ss = null;
		ssModifier = new SubtreeSpecificationModifier();
		subtreeSpecificationComponentsMonitor = new OptionalComponentsMonitor( 
		new String [] { "base", "specificExclusions", "minimum", "maximum", "specificationFilter" } );
		chopBeforeExclusions = new HashSet<DN>();
		chopAfterExclusions = new HashSet<DN>();
		// always create a new filter parser in case we may have some statefulness problems with it
		
		
		match(OPEN_CURLY);
		{
		_loop1417:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1417;
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
			_loop1420:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop1420;
				}
				
			} while (true);
			}
			{
			_loop1426:
			do {
				if ((LA(1)==SEP)) {
					match(SEP);
					{
					_loop1423:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop1423;
						}
						
					} while (true);
					}
					subtreeSpecificationComponent();
					{
					_loop1425:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop1425;
						}
						
					} while (true);
					}
				}
				else {
					break _loop1426;
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
		DN base = null;
		
		
		match(ID_base);
		{
		int _cnt1430=0;
		_loop1430:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt1430>=1 ) { break _loop1430; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt1430++;
		} while (true);
		}
		base=distinguishedName();
		
		ssModifier.setBase( base );
		
	}
	
	public final void ss_specificExclusions() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered ss_specificExclusions()" );
		
		
		match(ID_specificExclusions);
		{
		int _cnt1433=0;
		_loop1433:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt1433>=1 ) { break _loop1433; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt1433++;
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
		int _cnt1459=0;
		_loop1459:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt1459>=1 ) { break _loop1459; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt1459++;
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
		int _cnt1462=0;
		_loop1462:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt1462>=1 ) { break _loop1462; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt1462++;
		} while (true);
		}
		maximum=baseDistance();
		
		ssModifier.setMaxBaseDistance( maximum );
		
	}
	
	public final void ss_specificationFilter() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered ss_specificationFilter()" );
		ExprNode filterExpr = null;
		
		
		match(ID_specificationFilter);
		{
		int _cnt1465=0;
		_loop1465:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				if ( _cnt1465>=1 ) { break _loop1465; } else {throw new NoViableAltException(LT(1), getFilename());}
			}
			
			_cnt1465++;
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
			filterExpr=refinement();
			}
			break;
		}
		case FILTER:
		{
			{
			filterExpr=filter();
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		ssModifier.setRefinement( filterExpr );
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
			
			if ( isNormalizing() )
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
	
	public final void specificExclusions() throws RecognitionException, TokenStreamException {
		
		
		log.debug( "entered specificExclusions()" );
		
		
		match(OPEN_CURLY);
		{
		_loop1436:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1436;
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
			_loop1439:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop1439;
				}
				
			} while (true);
			}
			{
			_loop1445:
			do {
				if ((LA(1)==SEP)) {
					match(SEP);
					{
					_loop1442:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop1442;
						}
						
					} while (true);
					}
					specificExclusion();
					{
					_loop1444:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop1444;
						}
						
					} while (true);
					}
				}
				else {
					break _loop1445;
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
		_loop1449:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1449;
			}
			
		} while (true);
		}
		match(COLON);
		{
		_loop1451:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1451;
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
		_loop1454:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1454;
			}
			
		} while (true);
		}
		match(COLON);
		{
		_loop1456:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1456;
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
	
	public final ExprNode  filter() throws RecognitionException, TokenStreamException {
		 ExprNode filterExpr = null ;
		
		Token  filterToken = null;
		
		log.debug( "entered filter()" );
		
		
		try {      // for error handling
			{
			filterToken = LT(1);
			match(FILTER);
			filterExpr=FilterParser.parse( filterToken.getText() );
			}
		}
		catch (Exception e) {
			
			throw new RecognitionException( "filterParser failed. " + e.getMessage() );
			
		}
		return filterExpr;
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
	
	public final  LeafNode  item() throws RecognitionException, TokenStreamException {
		 LeafNode node ;
		
		
		log.debug( "entered item()" );
		node = null;
		String oid = null;
		
		
		match(ID_item);
		{
		_loop1478:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1478;
			}
			
		} while (true);
		}
		match(COLON);
		{
		_loop1480:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1480;
			}
			
		} while (true);
		}
		oid=oid();
		
		node = new EqualityNode( SchemaConstants.OBJECT_CLASS_AT, new StringValue( oid ) );
		
		return node ;
	}
	
	public final  BranchNode  and() throws RecognitionException, TokenStreamException {
		 BranchNode node ;
		
		
		log.debug( "entered and()" );
		node = null;
		List<ExprNode> children = null; 
		
		
		match(ID_and);
		{
		_loop1483:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1483;
			}
			
		} while (true);
		}
		match(COLON);
		{
		_loop1485:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1485;
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
		_loop1488:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1488;
			}
			
		} while (true);
		}
		match(COLON);
		{
		_loop1490:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1490;
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
		_loop1493:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1493;
			}
			
		} while (true);
		}
		match(COLON);
		{
		_loop1495:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1495;
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
		_loop1498:
		do {
			if ((LA(1)==SP)) {
				match(SP);
			}
			else {
				break _loop1498;
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
			_loop1501:
			do {
				if ((LA(1)==SP)) {
					match(SP);
				}
				else {
					break _loop1501;
				}
				
			} while (true);
			}
			
			tempChildren.add( child );
			
			{
			_loop1507:
			do {
				if ((LA(1)==SEP)) {
					match(SEP);
					{
					_loop1504:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop1504;
						}
						
					} while (true);
					}
					child=refinement();
					{
					_loop1506:
					do {
						if ((LA(1)==SP)) {
							match(SP);
						}
						else {
							break _loop1506;
						}
						
					} while (true);
					}
					
					tempChildren.add( child );
					
				}
				else {
					break _loop1507;
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
