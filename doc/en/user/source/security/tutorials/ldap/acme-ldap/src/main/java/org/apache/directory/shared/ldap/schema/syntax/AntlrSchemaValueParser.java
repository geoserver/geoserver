// $ANTLR 2.7.4: "schema-value.g" -> "AntlrSchemaValueParser.java"$

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

import java.util.List;
import java.util.ArrayList;

import org.apache.directory.shared.ldap.schema.parsers.ParserMonitor;


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
 * An antlr generated schema parser. This is a sub-parser used to parse
 * numericoid, oid, oids, qdescr, qdescrs according to RFC4512.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AntlrSchemaValueParser extends antlr.LLkParser       implements AntlrSchemaValueTokenTypes
 {

    private ParserMonitor monitor = null;
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

protected AntlrSchemaValueParser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public AntlrSchemaValueParser(TokenBuffer tokenBuf) {
  this(tokenBuf,3);
}

protected AntlrSchemaValueParser(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public AntlrSchemaValueParser(TokenStream lexer) {
  this(lexer,3);
}

public AntlrSchemaValueParser(ParserSharedInputState state) {
  super(state,3);
  tokenNames = _tokenNames;
}

/**
     * noidlen = numericoid [ LCURLY len RCURLY ]
     * len = number
     */
	public final AntlrSchemaParser.NoidLen  noidlen() throws RecognitionException, TokenStreamException {
		AntlrSchemaParser.NoidLen noidlen = new AntlrSchemaParser.NoidLen();
		
		Token  d4 = null;
		Token  n2 = null;
		Token  l = null;
		
		matchedProduction( "AntlrSchemaValueParser.noidlen()" );
		
		
		{
		{
		switch ( LA(1)) {
		case LPAR:
		{
			match(LPAR);
			break;
		}
		case WHSP:
		case NUMERICOID:
		case DESCR:
		case QUOTE:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		switch ( LA(1)) {
		case WHSP:
		{
			match(WHSP);
			break;
		}
		case NUMERICOID:
		case DESCR:
		case QUOTE:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		switch ( LA(1)) {
		case QUOTE:
		{
			match(QUOTE);
			break;
		}
		case NUMERICOID:
		case DESCR:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		switch ( LA(1)) {
		case DESCR:
		{
			{
			d4 = LT(1);
			match(DESCR);
			noidlen.noid = d4.getText();
			}
			break;
		}
		case NUMERICOID:
		{
			{
			n2 = LT(1);
			match(NUMERICOID);
			noidlen.noid = n2.getText();
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		switch ( LA(1)) {
		case QUOTE:
		{
			match(QUOTE);
			break;
		}
		case EOF:
		case WHSP:
		case RPAR:
		case LEN:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		switch ( LA(1)) {
		case WHSP:
		{
			match(WHSP);
			break;
		}
		case EOF:
		case RPAR:
		case LEN:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		switch ( LA(1)) {
		case RPAR:
		{
			match(RPAR);
			break;
		}
		case EOF:
		case LEN:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		switch ( LA(1)) {
		case LEN:
		{
			l = LT(1);
			match(LEN);
			noidlen.len = Integer.parseInt(l.getText());
			{
			switch ( LA(1)) {
			case QUOTE:
			{
				match(QUOTE);
				break;
			}
			case EOF:
			case WHSP:
			case RPAR:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			{
			switch ( LA(1)) {
			case WHSP:
			{
				match(WHSP);
				break;
			}
			case EOF:
			case RPAR:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			{
			switch ( LA(1)) {
			case RPAR:
			{
				match(RPAR);
				break;
			}
			case EOF:
			{
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
		case EOF:
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
		return noidlen;
	}
	
/**
     * noidlen = numericoid [ LCURLY len RCURLY ]
     * len = number
     */
	public final AntlrSchemaParser.NoidLen  quirksNoidlen() throws RecognitionException, TokenStreamException {
		AntlrSchemaParser.NoidLen noidlen = new AntlrSchemaParser.NoidLen();
		
		Token  q2 = null;
		Token  d4 = null;
		Token  n2 = null;
		Token  l = null;
		
		matchedProduction( "AntlrSchemaValueParser.quirksNoidlen()" );
		
		
		{
		{
		switch ( LA(1)) {
		case LPAR:
		{
			match(LPAR);
			break;
		}
		case WHSP:
		case NUMERICOID:
		case DESCR:
		case QUIRKS_DESCR:
		case QUOTE:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		switch ( LA(1)) {
		case WHSP:
		{
			match(WHSP);
			break;
		}
		case NUMERICOID:
		case DESCR:
		case QUIRKS_DESCR:
		case QUOTE:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		switch ( LA(1)) {
		case QUOTE:
		{
			match(QUOTE);
			break;
		}
		case NUMERICOID:
		case DESCR:
		case QUIRKS_DESCR:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		switch ( LA(1)) {
		case QUIRKS_DESCR:
		{
			{
			q2 = LT(1);
			match(QUIRKS_DESCR);
			noidlen.noid = q2.getText();
			}
			break;
		}
		case DESCR:
		{
			{
			d4 = LT(1);
			match(DESCR);
			noidlen.noid = d4.getText();
			}
			break;
		}
		case NUMERICOID:
		{
			{
			n2 = LT(1);
			match(NUMERICOID);
			noidlen.noid = n2.getText();
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		switch ( LA(1)) {
		case QUOTE:
		{
			match(QUOTE);
			break;
		}
		case EOF:
		case WHSP:
		case RPAR:
		case LEN:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		switch ( LA(1)) {
		case WHSP:
		{
			match(WHSP);
			break;
		}
		case EOF:
		case RPAR:
		case LEN:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		switch ( LA(1)) {
		case RPAR:
		{
			match(RPAR);
			break;
		}
		case EOF:
		case LEN:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		switch ( LA(1)) {
		case LEN:
		{
			l = LT(1);
			match(LEN);
			noidlen.len = Integer.parseInt(l.getText());
			{
			switch ( LA(1)) {
			case QUOTE:
			{
				match(QUOTE);
				break;
			}
			case EOF:
			case WHSP:
			case RPAR:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			{
			switch ( LA(1)) {
			case WHSP:
			{
				match(WHSP);
				break;
			}
			case EOF:
			case RPAR:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			{
			switch ( LA(1)) {
			case RPAR:
			{
				match(RPAR);
				break;
			}
			case EOF:
			{
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
		case EOF:
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
		return noidlen;
	}
	
/**
     * numericoid = number 1*( DOT number )
     */
	public final String  numericoid() throws RecognitionException, TokenStreamException {
		String numericoid=null;
		
		Token  n1 = null;
		Token  n2 = null;
		
		matchedProduction( "AntlrSchemaValueParser.numericoid()" );
		
		
		{
		{
		switch ( LA(1)) {
		case WHSP:
		{
			match(WHSP);
			break;
		}
		case LPAR:
		case NUMERICOID:
		case QUOTE:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		switch ( LA(1)) {
		case LPAR:
		{
			match(LPAR);
			{
			switch ( LA(1)) {
			case WHSP:
			{
				match(WHSP);
				break;
			}
			case NUMERICOID:
			case QUOTE:
			{
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
		case NUMERICOID:
		case QUOTE:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		switch ( LA(1)) {
		case QUOTE:
		{
			{
			match(QUOTE);
			n1 = LT(1);
			match(NUMERICOID);
			numericoid = n1.getText();
			match(QUOTE);
			}
			break;
		}
		case NUMERICOID:
		{
			{
			n2 = LT(1);
			match(NUMERICOID);
			numericoid = n2.getText();
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		{
		switch ( LA(1)) {
		case WHSP:
		{
			match(WHSP);
			break;
		}
		case EOF:
		case RPAR:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		switch ( LA(1)) {
		case RPAR:
		{
			match(RPAR);
			break;
		}
		case EOF:
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
		}
		return numericoid;
	}
	
/**
     * oid = descr / numericoid
     * numericoid = number 1*( DOT number )
     * descr = keystring
     * keystring = leadkeychar *keychar
     * leadkeychar = ALPHA
     * keychar = ALPHA / DIGIT / HYPHEN
     * number  = DIGIT / ( LDIGIT 1*DIGIT )
     *
     */
	public final String  oid() throws RecognitionException, TokenStreamException {
		String oid=null;
		
		Token  n1 = null;
		Token  n2 = null;
		Token  d1 = null;
		Token  d2 = null;
		
		matchedProduction( "AntlrSchemaValueParser.oid()" );
		
		
		{
		{
		switch ( LA(1)) {
		case WHSP:
		{
			match(WHSP);
			break;
		}
		case NUMERICOID:
		case DESCR:
		case QUOTE:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		switch ( LA(1)) {
		case NUMERICOID:
		{
			{
			n2 = LT(1);
			match(NUMERICOID);
			oid = n2.getText();
			}
			break;
		}
		case DESCR:
		{
			{
			d2 = LT(1);
			match(DESCR);
			oid = d2.getText();
			}
			break;
		}
		default:
			if ((LA(1)==QUOTE) && (LA(2)==NUMERICOID)) {
				{
				match(QUOTE);
				n1 = LT(1);
				match(NUMERICOID);
				oid = n1.getText();
				match(QUOTE);
				}
			}
			else if ((LA(1)==QUOTE) && (LA(2)==DESCR)) {
				{
				match(QUOTE);
				d1 = LT(1);
				match(DESCR);
				oid = d1.getText();
				match(QUOTE);
				}
			}
		else {
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		if ((LA(1)==WHSP) && (_tokenSet_0.member(LA(2))) && (_tokenSet_0.member(LA(3)))) {
			match(WHSP);
		}
		else if ((_tokenSet_0.member(LA(1))) && (_tokenSet_0.member(LA(2))) && (_tokenSet_0.member(LA(3)))) {
		}
		else {
			throw new NoViableAltException(LT(1), getFilename());
		}
		
		}
		}
		return oid;
	}
	
/**
     * oids = oid / ( LPAREN WSP oidlist WSP RPAREN )
     * oidlist = oid *( WSP DOLLAR WSP oid )
     */
	public final List<String>  oids() throws RecognitionException, TokenStreamException {
		List<String> oids;
		
		
		matchedProduction( "AntlrSchemaValueParser.oids()" );
		oids = new ArrayList<String>();
		String oid = null;
		
		
		{
		switch ( LA(1)) {
		case WHSP:
		case NUMERICOID:
		case DESCR:
		case QUOTE:
		{
			{
			oid=oid();
			oids.add(oid);
			}
			break;
		}
		case LPAR:
		{
			{
			match(LPAR);
			oid=oid();
			oids.add(oid);
			{
			_loop1073:
			do {
				if ((_tokenSet_1.member(LA(1)))) {
					{
					switch ( LA(1)) {
					case DOLLAR:
					{
						match(DOLLAR);
						break;
					}
					case WHSP:
					case NUMERICOID:
					case DESCR:
					case QUOTE:
					{
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					oid=oid();
					oids.add(oid);
				}
				else {
					break _loop1073;
				}
				
			} while (true);
			}
			match(RPAR);
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		return oids;
	}
	
/**
     * qdescr = SQUOTE descr SQUOTE
     */
	public final String  qdescr() throws RecognitionException, TokenStreamException {
		String qdescr=null;
		
		Token  d1 = null;
		Token  d2 = null;
		
		matchedProduction( "AntlrSchemaValueParser.qdescr()" );
		
		
		{
		{
		switch ( LA(1)) {
		case WHSP:
		{
			match(WHSP);
			break;
		}
		case DESCR:
		case QUOTE:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		switch ( LA(1)) {
		case QUOTE:
		{
			{
			match(QUOTE);
			d1 = LT(1);
			match(DESCR);
			qdescr = d1.getText();
			match(QUOTE);
			}
			break;
		}
		case DESCR:
		{
			{
			d2 = LT(1);
			match(DESCR);
			qdescr = d2.getText();
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
		return qdescr;
	}
	
/**
     * qdescrs = qdescr / ( LPAREN WSP qdescrlist WSP RPAREN )
     * qdescrlist = [ qdescr *( SP qdescr ) ]
     */
	public final List<String>  qdescrs() throws RecognitionException, TokenStreamException {
		List<String> qdescrs;
		
		
		matchedProduction( "AntlrSchemaValueParser.qdescrs()" );
		qdescrs = new ArrayList<String>();
		String qdescr = null;
		
		
		{
		switch ( LA(1)) {
		case WHSP:
		case DESCR:
		case QUOTE:
		{
			{
			qdescr=qdescr();
			qdescrs.add(qdescr);
			}
			break;
		}
		case LPAR:
		{
			{
			match(LPAR);
			qdescr=qdescr();
			qdescrs.add(qdescr);
			{
			if ((LA(1)==WHSP) && (_tokenSet_2.member(LA(2))) && (_tokenSet_3.member(LA(3)))) {
				match(WHSP);
			}
			else if ((_tokenSet_2.member(LA(1))) && (_tokenSet_3.member(LA(2))) && (_tokenSet_3.member(LA(3)))) {
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			{
			switch ( LA(1)) {
			case DOLLAR:
			{
				match(DOLLAR);
				break;
			}
			case WHSP:
			case RPAR:
			case DESCR:
			case QUOTE:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			{
			if ((LA(1)==WHSP) && (_tokenSet_4.member(LA(2))) && (_tokenSet_3.member(LA(3)))) {
				match(WHSP);
			}
			else if ((_tokenSet_4.member(LA(1))) && (_tokenSet_3.member(LA(2))) && (_tokenSet_3.member(LA(3)))) {
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			{
			_loop1091:
			do {
				if ((LA(1)==WHSP||LA(1)==DESCR||LA(1)==QUOTE)) {
					qdescr=qdescr();
					qdescrs.add(qdescr);
					{
					if ((LA(1)==WHSP) && (_tokenSet_2.member(LA(2))) && (_tokenSet_3.member(LA(3)))) {
						match(WHSP);
					}
					else if ((_tokenSet_2.member(LA(1))) && (_tokenSet_3.member(LA(2))) && (_tokenSet_3.member(LA(3)))) {
					}
					else {
						throw new NoViableAltException(LT(1), getFilename());
					}
					
					}
					{
					switch ( LA(1)) {
					case DOLLAR:
					{
						match(DOLLAR);
						break;
					}
					case WHSP:
					case RPAR:
					case DESCR:
					case QUOTE:
					{
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					{
					if ((LA(1)==WHSP) && (_tokenSet_4.member(LA(2))) && (_tokenSet_3.member(LA(3)))) {
						match(WHSP);
					}
					else if ((_tokenSet_4.member(LA(1))) && (_tokenSet_3.member(LA(2))) && (_tokenSet_3.member(LA(3)))) {
					}
					else {
						throw new NoViableAltException(LT(1), getFilename());
					}
					
					}
				}
				else {
					break _loop1091;
				}
				
			} while (true);
			}
			match(RPAR);
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		return qdescrs;
	}
	
/**
     * qdescr = SQUOTE descr SQUOTE
     */
	public final String  quirksQdescr() throws RecognitionException, TokenStreamException {
		String qdescr=null;
		
		Token  d1 = null;
		Token  d2 = null;
		Token  d3 = null;
		Token  d4 = null;
		Token  n1 = null;
		Token  n2 = null;
		
		matchedProduction( "AntlrSchemaValueParser.qdescr()" );
		
		
		{
		{
		switch ( LA(1)) {
		case WHSP:
		{
			match(WHSP);
			break;
		}
		case NUMERICOID:
		case DESCR:
		case QUIRKS_DESCR:
		case QUOTE:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		switch ( LA(1)) {
		case QUIRKS_DESCR:
		{
			{
			d2 = LT(1);
			match(QUIRKS_DESCR);
			qdescr = d2.getText();
			}
			break;
		}
		case DESCR:
		{
			{
			d4 = LT(1);
			match(DESCR);
			qdescr = d4.getText();
			}
			break;
		}
		case NUMERICOID:
		{
			{
			n2 = LT(1);
			match(NUMERICOID);
			qdescr = n2.getText();
			}
			break;
		}
		default:
			if ((LA(1)==QUOTE) && (LA(2)==QUIRKS_DESCR)) {
				{
				match(QUOTE);
				d1 = LT(1);
				match(QUIRKS_DESCR);
				qdescr = d1.getText();
				match(QUOTE);
				}
			}
			else if ((LA(1)==QUOTE) && (LA(2)==DESCR)) {
				{
				match(QUOTE);
				d3 = LT(1);
				match(DESCR);
				qdescr = d3.getText();
				match(QUOTE);
				}
			}
			else if ((LA(1)==QUOTE) && (LA(2)==NUMERICOID)) {
				{
				match(QUOTE);
				n1 = LT(1);
				match(NUMERICOID);
				qdescr = n1.getText();
				match(QUOTE);
				}
			}
		else {
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		if ((LA(1)==WHSP) && (_tokenSet_5.member(LA(2))) && (_tokenSet_5.member(LA(3)))) {
			match(WHSP);
		}
		else if ((_tokenSet_5.member(LA(1))) && (_tokenSet_5.member(LA(2))) && (_tokenSet_5.member(LA(3)))) {
		}
		else {
			throw new NoViableAltException(LT(1), getFilename());
		}
		
		}
		}
		return qdescr;
	}
	
/**
     * qdescrs = qdescr / ( LPAREN WSP qdescrlist WSP RPAREN )
     * qdescrlist = [ qdescr *( SP qdescr ) ]
     */
	public final List<String>  quirksQdescrs() throws RecognitionException, TokenStreamException {
		List<String> qdescrs;
		
		
		matchedProduction( "AntlrSchemaValueParser.qdescrs()" );
		qdescrs = new ArrayList<String>();
		String qdescr = null;
		
		
		{
		switch ( LA(1)) {
		case WHSP:
		case NUMERICOID:
		case DESCR:
		case QUIRKS_DESCR:
		case QUOTE:
		{
			{
			qdescr=quirksQdescr();
			qdescrs.add(qdescr);
			}
			break;
		}
		case LPAR:
		{
			{
			match(LPAR);
			qdescr=quirksQdescr();
			qdescrs.add(qdescr);
			{
			if ((LA(1)==WHSP) && (_tokenSet_6.member(LA(2))) && (_tokenSet_5.member(LA(3)))) {
				match(WHSP);
			}
			else if ((_tokenSet_6.member(LA(1))) && (_tokenSet_5.member(LA(2))) && (_tokenSet_5.member(LA(3)))) {
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			{
			switch ( LA(1)) {
			case DOLLAR:
			{
				match(DOLLAR);
				break;
			}
			case WHSP:
			case RPAR:
			case NUMERICOID:
			case DESCR:
			case QUIRKS_DESCR:
			case QUOTE:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			{
			if ((LA(1)==WHSP) && (_tokenSet_7.member(LA(2))) && (_tokenSet_5.member(LA(3)))) {
				match(WHSP);
			}
			else if ((_tokenSet_7.member(LA(1))) && (_tokenSet_5.member(LA(2))) && (_tokenSet_5.member(LA(3)))) {
			}
			else {
				throw new NoViableAltException(LT(1), getFilename());
			}
			
			}
			{
			_loop1114:
			do {
				if ((_tokenSet_8.member(LA(1)))) {
					qdescr=quirksQdescr();
					qdescrs.add(qdescr);
					{
					if ((LA(1)==WHSP) && (_tokenSet_6.member(LA(2))) && (_tokenSet_5.member(LA(3)))) {
						match(WHSP);
					}
					else if ((_tokenSet_6.member(LA(1))) && (_tokenSet_5.member(LA(2))) && (_tokenSet_5.member(LA(3)))) {
					}
					else {
						throw new NoViableAltException(LT(1), getFilename());
					}
					
					}
					{
					switch ( LA(1)) {
					case DOLLAR:
					{
						match(DOLLAR);
						break;
					}
					case WHSP:
					case RPAR:
					case NUMERICOID:
					case DESCR:
					case QUIRKS_DESCR:
					case QUOTE:
					{
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					{
					if ((LA(1)==WHSP) && (_tokenSet_7.member(LA(2))) && (_tokenSet_5.member(LA(3)))) {
						match(WHSP);
					}
					else if ((_tokenSet_7.member(LA(1))) && (_tokenSet_5.member(LA(2))) && (_tokenSet_5.member(LA(3)))) {
					}
					else {
						throw new NoViableAltException(LT(1), getFilename());
					}
					
					}
				}
				else {
					break _loop1114;
				}
				
			} while (true);
			}
			match(RPAR);
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		return qdescrs;
	}
	
/**
     * ruleid = number
     * number  = DIGIT / ( LDIGIT 1*DIGIT )
     *
     */
	public final Integer  ruleid() throws RecognitionException, TokenStreamException {
		Integer ruleid=null;
		
		Token  n = null;
		
		matchedProduction( "AntlrSchemaValueParser.ruleid()" );
		
		
		{
		{
		switch ( LA(1)) {
		case WHSP:
		{
			match(WHSP);
			break;
		}
		case NUMBER:
		{
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		n = LT(1);
		match(NUMBER);
		ruleid = Integer.parseInt(n.getText());
		}
		return ruleid;
	}
	
/**
     * ruleids = ruleid / ( LPAREN WSP ruleidlist WSP RPAREN )
     * ruleidlist = ruleid *( SP ruleid )
     */
	public final List<Integer>  ruleids() throws RecognitionException, TokenStreamException {
		List<Integer> ruleids;
		
		
		matchedProduction( "AntlrSchemaValueParser.ruleids()" );
		ruleids = new ArrayList<Integer>();
		Integer ruleid = null;
		
		
		{
		switch ( LA(1)) {
		case WHSP:
		case NUMBER:
		{
			{
			ruleid=ruleid();
			ruleids.add(ruleid);
			}
			break;
		}
		case LPAR:
		{
			{
			match(LPAR);
			ruleid=ruleid();
			ruleids.add(ruleid);
			{
			_loop1123:
			do {
				if ((LA(1)==WHSP) && (LA(2)==WHSP||LA(2)==NUMBER)) {
					match(WHSP);
					ruleid=ruleid();
					ruleids.add(ruleid);
				}
				else {
					break _loop1123;
				}
				
			} while (true);
			}
			{
			switch ( LA(1)) {
			case WHSP:
			{
				match(WHSP);
				break;
			}
			case RPAR:
			{
				break;
			}
			default:
			{
				throw new NoViableAltException(LT(1), getFilename());
			}
			}
			}
			match(RPAR);
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
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
		"CHAR",
		"LDIGIT",
		"DIGIT",
		"NUMBER",
		"NUMBER2",
		"NUMERICOID",
		"HYPEN",
		"OTHER",
		"DESCR",
		"QUIRKS_DESCR",
		"QUOTE",
		"DOLLAR",
		"LCURLY",
		"RCURLY",
		"LEN",
		"DESCR_OR_QUIRKS_DESCR"
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { 430162L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = { 430096L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = { 426064L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = { 426066L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	private static final long[] mk_tokenSet_4() {
		long[] data = { 163920L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_4 = new BitSet(mk_tokenSet_4());
	private static final long[] mk_tokenSet_5() {
		long[] data = { 495698L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_5 = new BitSet(mk_tokenSet_5());
	private static final long[] mk_tokenSet_6() {
		long[] data = { 495696L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_6 = new BitSet(mk_tokenSet_6());
	private static final long[] mk_tokenSet_7() {
		long[] data = { 233552L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_7 = new BitSet(mk_tokenSet_7());
	private static final long[] mk_tokenSet_8() {
		long[] data = { 233488L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_8 = new BitSet(mk_tokenSet_8());
	
	}
