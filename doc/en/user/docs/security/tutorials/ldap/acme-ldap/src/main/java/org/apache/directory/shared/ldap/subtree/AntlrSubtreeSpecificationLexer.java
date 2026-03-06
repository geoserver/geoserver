// $ANTLR 2.7.4: "subtree-specification.g" -> "AntlrSubtreeSpecificationLexer.java"$

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

import java.io.InputStream;
import antlr.TokenStreamException;
import antlr.TokenStreamIOException;
import antlr.TokenStreamRecognitionException;
import antlr.CharStreamException;
import antlr.CharStreamIOException;
import antlr.ANTLRException;
import java.io.Reader;
import java.util.Hashtable;
import antlr.CharScanner;
import antlr.InputBuffer;
import antlr.ByteBuffer;
import antlr.CharBuffer;
import antlr.Token;
import antlr.CommonToken;
import antlr.RecognitionException;
import antlr.NoViableAltForCharException;
import antlr.MismatchedCharException;
import antlr.TokenStream;
import antlr.ANTLRHashString;
import antlr.LexerSharedInputState;
import antlr.collections.impl.BitSet;
import antlr.SemanticException;

/**
 * The parser's primary lexer.
 *
 * @see <a href="http://www.faqs.org/rfcs/rfc3672.html">RFC 3672</a>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AntlrSubtreeSpecificationLexer extends antlr.CharScanner implements AntlrSubtreeSpecificationParserTokenTypes, TokenStream
 {

    private static final Logger log = LoggerFactory.getLogger( AntlrSubtreeSpecificationLexer.class );
public AntlrSubtreeSpecificationLexer(InputStream in) {
	this(new ByteBuffer(in));
}
public AntlrSubtreeSpecificationLexer(Reader in) {
	this(new CharBuffer(in));
}
public AntlrSubtreeSpecificationLexer(InputBuffer ib) {
	this(new LexerSharedInputState(ib));
}
public AntlrSubtreeSpecificationLexer(LexerSharedInputState state) {
	super(state);
	caseSensitiveLiterals = true;
	setCaseSensitive(true);
	literals = new Hashtable();
	literals.put(new ANTLRHashString("end", this), new Integer(4));
	literals.put(new ANTLRHashString("specificExclusions", this), new Integer(10));
	literals.put(new ANTLRHashString("or", this), new Integer(24));
	literals.put(new ANTLRHashString("minimum", this), new Integer(14));
	literals.put(new ANTLRHashString("chopBefore", this), new Integer(11));
	literals.put(new ANTLRHashString("chopAfter", this), new Integer(13));
	literals.put(new ANTLRHashString("maximum", this), new Integer(15));
	literals.put(new ANTLRHashString("and", this), new Integer(23));
	literals.put(new ANTLRHashString("not", this), new Integer(25));
	literals.put(new ANTLRHashString("item", this), new Integer(22));
	literals.put(new ANTLRHashString("specificationFilter", this), new Integer(16));
	literals.put(new ANTLRHashString("base", this), new Integer(9));
}

public Token nextToken() throws TokenStreamException {
	Token theRetToken=null;
tryAgain:
	for (;;) {
		Token _token = null;
		int _ttype = Token.INVALID_TYPE;
		resetText();
		try {   // for char stream error handling
			try {   // for lexical error handling
				switch ( LA(1)) {
				case ' ':
				{
					mSP(true);
					theRetToken=_returnToken;
					break;
				}
				case ':':
				{
					mCOLON(true);
					theRetToken=_returnToken;
					break;
				}
				case '{':
				{
					mOPEN_CURLY(true);
					theRetToken=_returnToken;
					break;
				}
				case '}':
				{
					mCLOSE_CURLY(true);
					theRetToken=_returnToken;
					break;
				}
				case ',':
				{
					mSEP(true);
					theRetToken=_returnToken;
					break;
				}
				case '"':
				{
					mSAFEUTF8STRING(true);
					theRetToken=_returnToken;
					break;
				}
				case 'A':  case 'B':  case 'C':  case 'D':
				case 'E':  case 'F':  case 'G':  case 'H':
				case 'I':  case 'J':  case 'K':  case 'L':
				case 'M':  case 'N':  case 'O':  case 'P':
				case 'Q':  case 'R':  case 'S':  case 'T':
				case 'U':  case 'V':  case 'W':  case 'X':
				case 'Y':  case 'Z':  case 'a':  case 'b':
				case 'c':  case 'd':  case 'e':  case 'f':
				case 'g':  case 'h':  case 'i':  case 'j':
				case 'k':  case 'l':  case 'm':  case 'n':
				case 'o':  case 'p':  case 'q':  case 'r':
				case 's':  case 't':  case 'u':  case 'v':
				case 'w':  case 'x':  case 'y':  case 'z':
				{
					mDESCR(true);
					theRetToken=_returnToken;
					break;
				}
				case '0':  case '1':  case '2':  case '3':
				case '4':  case '5':  case '6':  case '7':
				case '8':  case '9':
				{
					mINTEGER_OR_NUMERICOID(true);
					theRetToken=_returnToken;
					break;
				}
				case '(':
				{
					mFILTER(true);
					theRetToken=_returnToken;
					break;
				}
				default:
				{
					if (LA(1)==EOF_CHAR) {uponEOF(); _returnToken = makeToken(Token.EOF_TYPE);}
				else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
				}
				}
				if ( _returnToken==null ) continue tryAgain; // found SKIP token
				_ttype = _returnToken.getType();
				_ttype = testLiteralsTable(_ttype);
				_returnToken.setType(_ttype);
				return _returnToken;
			}
			catch (RecognitionException e) {
				throw new TokenStreamRecognitionException(e);
			}
		}
		catch (CharStreamException cse) {
			if ( cse instanceof CharStreamIOException ) {
				throw new TokenStreamIOException(((CharStreamIOException)cse).io);
			}
			else {
				throw new TokenStreamException(cse.getMessage());
			}
		}
	}
}

	public final void mSP(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = SP;
		int _saveIndex;
		
		match(' ');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mCOLON(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = COLON;
		int _saveIndex;
		
		match(':');
		if ( inputState.guessing==0 ) {
			log.debug( "matched COLON(':')" );
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mOPEN_CURLY(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = OPEN_CURLY;
		int _saveIndex;
		
		match('{');
		if ( inputState.guessing==0 ) {
			log.debug( "matched LBRACKET('{')" );
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mCLOSE_CURLY(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = CLOSE_CURLY;
		int _saveIndex;
		
		match('}');
		if ( inputState.guessing==0 ) {
			log.debug( "matched RBRACKET('}')" );
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mSEP(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = SEP;
		int _saveIndex;
		
		match(',');
		if ( inputState.guessing==0 ) {
			log.debug( "matched SEP(',')" );
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mSAFEUTF8STRING(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = SAFEUTF8STRING;
		int _saveIndex;
		
		_saveIndex=text.length();
		match('"');
		text.setLength(_saveIndex);
		{
		_loop1515:
		do {
			if ((_tokenSet_0.member(LA(1)))) {
				mSAFEUTF8CHAR(false);
			}
			else {
				break _loop1515;
			}
			
		} while (true);
		}
		_saveIndex=text.length();
		match('"');
		text.setLength(_saveIndex);
		if ( inputState.guessing==0 ) {
			log.debug( "matched SAFEUTF8CHAR: \"" + getText() + "\"" );
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mSAFEUTF8CHAR(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = SAFEUTF8CHAR;
		int _saveIndex;
		
		switch ( LA(1)) {
		case '\u0001':  case '\u0002':  case '\u0003':  case '\u0004':
		case '\u0005':  case '\u0006':  case '\u0007':  case '\u0008':
		case '\t':  case '\n':  case '\u000b':  case '\u000c':
		case '\r':  case '\u000e':  case '\u000f':  case '\u0010':
		case '\u0011':  case '\u0012':  case '\u0013':  case '\u0014':
		case '\u0015':  case '\u0016':  case '\u0017':  case '\u0018':
		case '\u0019':  case '\u001a':  case '\u001b':  case '\u001c':
		case '\u001d':  case '\u001e':  case '\u001f':  case ' ':
		case '!':
		{
			matchRange('\u0001','\u0021');
			break;
		}
		case '#':  case '$':  case '%':  case '&':
		case '\'':  case '(':  case ')':  case '*':
		case '+':  case ',':  case '-':  case '.':
		case '/':  case '0':  case '1':  case '2':
		case '3':  case '4':  case '5':  case '6':
		case '7':  case '8':  case '9':  case ':':
		case ';':  case '<':  case '=':  case '>':
		case '?':  case '@':  case 'A':  case 'B':
		case 'C':  case 'D':  case 'E':  case 'F':
		case 'G':  case 'H':  case 'I':  case 'J':
		case 'K':  case 'L':  case 'M':  case 'N':
		case 'O':  case 'P':  case 'Q':  case 'R':
		case 'S':  case 'T':  case 'U':  case 'V':
		case 'W':  case 'X':  case 'Y':  case 'Z':
		case '[':  case '\\':  case ']':  case '^':
		case '_':  case '`':  case 'a':  case 'b':
		case 'c':  case 'd':  case 'e':  case 'f':
		case 'g':  case 'h':  case 'i':  case 'j':
		case 'k':  case 'l':  case 'm':  case 'n':
		case 'o':  case 'p':  case 'q':  case 'r':
		case 's':  case 't':  case 'u':  case 'v':
		case 'w':  case 'x':  case 'y':  case 'z':
		case '{':  case '|':  case '}':  case '~':
		case '\u007f':
		{
			matchRange('\u0023','\u007F');
			break;
		}
		case '\u00c0':  case '\u00c1':  case '\u00c2':  case '\u00c3':
		case '\u00c4':  case '\u00c5':  case '\u00c6':  case '\u00c7':
		case '\u00c8':  case '\u00c9':  case '\u00ca':  case '\u00cb':
		case '\u00cc':  case '\u00cd':  case '\u00ce':  case '\u00cf':
		case '\u00d0':  case '\u00d1':  case '\u00d2':  case '\u00d3':
		case '\u00d4':  case '\u00d5':  case '\u00d6':
		{
			matchRange('\u00c0','\u00d6');
			break;
		}
		case '\u00d8':  case '\u00d9':  case '\u00da':  case '\u00db':
		case '\u00dc':  case '\u00dd':  case '\u00de':  case '\u00df':
		case '\u00e0':  case '\u00e1':  case '\u00e2':  case '\u00e3':
		case '\u00e4':  case '\u00e5':  case '\u00e6':  case '\u00e7':
		case '\u00e8':  case '\u00e9':  case '\u00ea':  case '\u00eb':
		case '\u00ec':  case '\u00ed':  case '\u00ee':  case '\u00ef':
		case '\u00f0':  case '\u00f1':  case '\u00f2':  case '\u00f3':
		case '\u00f4':  case '\u00f5':  case '\u00f6':
		{
			matchRange('\u00d8','\u00f6');
			break;
		}
		case '\u00f8':  case '\u00f9':  case '\u00fa':  case '\u00fb':
		case '\u00fc':  case '\u00fd':  case '\u00fe':  case '\u00ff':
		{
			matchRange('\u00f8','\u00ff');
			break;
		}
		default:
			if (((LA(1) >= '\u0100' && LA(1) <= '\u1fff'))) {
				matchRange('\u0100','\u1fff');
			}
			else if (((LA(1) >= '\u3040' && LA(1) <= '\u318f'))) {
				matchRange('\u3040','\u318f');
			}
			else if (((LA(1) >= '\u3300' && LA(1) <= '\u337f'))) {
				matchRange('\u3300','\u337f');
			}
			else if (((LA(1) >= '\u3400' && LA(1) <= '\u3d2d'))) {
				matchRange('\u3400','\u3d2d');
			}
			else if (((LA(1) >= '\u4e00' && LA(1) <= '\u9fff'))) {
				matchRange('\u4e00','\u9fff');
			}
			else if (((LA(1) >= '\uf900' && LA(1) <= '\ufaff'))) {
				matchRange('\uf900','\ufaff');
			}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mDESCR(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = DESCR;
		int _saveIndex;
		
		mALPHA(false);
		{
		_loop1518:
		do {
			switch ( LA(1)) {
			case 'A':  case 'B':  case 'C':  case 'D':
			case 'E':  case 'F':  case 'G':  case 'H':
			case 'I':  case 'J':  case 'K':  case 'L':
			case 'M':  case 'N':  case 'O':  case 'P':
			case 'Q':  case 'R':  case 'S':  case 'T':
			case 'U':  case 'V':  case 'W':  case 'X':
			case 'Y':  case 'Z':  case 'a':  case 'b':
			case 'c':  case 'd':  case 'e':  case 'f':
			case 'g':  case 'h':  case 'i':  case 'j':
			case 'k':  case 'l':  case 'm':  case 'n':
			case 'o':  case 'p':  case 'q':  case 'r':
			case 's':  case 't':  case 'u':  case 'v':
			case 'w':  case 'x':  case 'y':  case 'z':
			{
				mALPHA(false);
				break;
			}
			case '0':  case '1':  case '2':  case '3':
			case '4':  case '5':  case '6':  case '7':
			case '8':  case '9':
			{
				mDIGIT(false);
				break;
			}
			case '-':
			{
				match('-');
				break;
			}
			default:
			{
				break _loop1518;
			}
			}
		} while (true);
		}
		if ( inputState.guessing==0 ) {
			log.debug( "matched DESCR" );
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mALPHA(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = ALPHA;
		int _saveIndex;
		
		switch ( LA(1)) {
		case 'A':  case 'B':  case 'C':  case 'D':
		case 'E':  case 'F':  case 'G':  case 'H':
		case 'I':  case 'J':  case 'K':  case 'L':
		case 'M':  case 'N':  case 'O':  case 'P':
		case 'Q':  case 'R':  case 'S':  case 'T':
		case 'U':  case 'V':  case 'W':  case 'X':
		case 'Y':  case 'Z':
		{
			matchRange('A','Z');
			break;
		}
		case 'a':  case 'b':  case 'c':  case 'd':
		case 'e':  case 'f':  case 'g':  case 'h':
		case 'i':  case 'j':  case 'k':  case 'l':
		case 'm':  case 'n':  case 'o':  case 'p':
		case 'q':  case 'r':  case 's':  case 't':
		case 'u':  case 'v':  case 'w':  case 'x':
		case 'y':  case 'z':
		{
			matchRange('a','z');
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mDIGIT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = DIGIT;
		int _saveIndex;
		
		switch ( LA(1)) {
		case '0':
		{
			match('0');
			break;
		}
		case '1':  case '2':  case '3':  case '4':
		case '5':  case '6':  case '7':  case '8':
		case '9':
		{
			mLDIGIT(false);
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mINTEGER_OR_NUMERICOID(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = INTEGER_OR_NUMERICOID;
		int _saveIndex;
		
		boolean synPredMatched1521 = false;
		if ((((LA(1) >= '0' && LA(1) <= '9')) && (_tokenSet_1.member(LA(2))) && (_tokenSet_1.member(LA(3))) && (true) && (true))) {
			int _m1521 = mark();
			synPredMatched1521 = true;
			inputState.guessing++;
			try {
				{
				mINTEGER(false);
				mDOT(false);
				}
			}
			catch (RecognitionException pe) {
				synPredMatched1521 = false;
			}
			rewind(_m1521);
			inputState.guessing--;
		}
		if ( synPredMatched1521 ) {
			mNUMERICOID(false);
			if ( inputState.guessing==0 ) {
				
				_ttype =  NUMERICOID;
				
			}
		}
		else if (((LA(1) >= '0' && LA(1) <= '9')) && (true) && (true) && (true) && (true)) {
			mINTEGER(false);
			if ( inputState.guessing==0 ) {
				
				_ttype =  INTEGER;
				
			}
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mINTEGER(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = INTEGER;
		int _saveIndex;
		
		if (((LA(1) >= '1' && LA(1) <= '9')) && ((LA(2) >= '0' && LA(2) <= '9'))) {
			{
			mLDIGIT(false);
			{
			int _cnt1525=0;
			_loop1525:
			do {
				if (((LA(1) >= '0' && LA(1) <= '9'))) {
					mDIGIT(false);
				}
				else {
					if ( _cnt1525>=1 ) { break _loop1525; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
				}
				
				_cnt1525++;
			} while (true);
			}
			}
			if ( inputState.guessing==0 ) {
				log.debug( "matched INTEGER: " + getText() );
			}
		}
		else if (((LA(1) >= '0' && LA(1) <= '9')) && (true)) {
			mDIGIT(false);
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mDOT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = DOT;
		int _saveIndex;
		
		match('.');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mNUMERICOID(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = NUMERICOID;
		int _saveIndex;
		
		mINTEGER(false);
		{
		int _cnt1528=0;
		_loop1528:
		do {
			if ((LA(1)=='.')) {
				mDOT(false);
				mINTEGER(false);
			}
			else {
				if ( _cnt1528>=1 ) { break _loop1528; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			
			_cnt1528++;
		} while (true);
		}
		if ( inputState.guessing==0 ) {
			log.debug( "matched NUMERICOID: " + getText() );
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mLDIGIT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LDIGIT;
		int _saveIndex;
		
		matchRange('1','9');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mFILTER(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = FILTER;
		int _saveIndex;
		
		match('(');
		{
		switch ( LA(1)) {
		case '&':
		{
			{
			match('&');
			{
			_loop1538:
			do {
				if ((LA(1)==' ')) {
					mSP(false);
				}
				else {
					break _loop1538;
				}
				
			} while (true);
			}
			{
			int _cnt1540=0;
			_loop1540:
			do {
				if ((LA(1)=='(')) {
					mFILTER(false);
				}
				else {
					if ( _cnt1540>=1 ) { break _loop1540; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
				}
				
				_cnt1540++;
			} while (true);
			}
			}
			break;
		}
		case '|':
		{
			{
			match('|');
			{
			_loop1543:
			do {
				if ((LA(1)==' ')) {
					mSP(false);
				}
				else {
					break _loop1543;
				}
				
			} while (true);
			}
			{
			int _cnt1545=0;
			_loop1545:
			do {
				if ((LA(1)=='(')) {
					mFILTER(false);
				}
				else {
					if ( _cnt1545>=1 ) { break _loop1545; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
				}
				
				_cnt1545++;
			} while (true);
			}
			}
			break;
		}
		case '!':
		{
			{
			match('!');
			{
			_loop1548:
			do {
				if ((LA(1)==' ')) {
					mSP(false);
				}
				else {
					break _loop1548;
				}
				
			} while (true);
			}
			mFILTER(false);
			}
			break;
		}
		default:
			if ((_tokenSet_2.member(LA(1)))) {
				mFILTER_VALUE(false);
			}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		}
		}
		match(')');
		{
		_loop1550:
		do {
			if ((LA(1)==' ')) {
				mSP(false);
			}
			else {
				break _loop1550;
			}
			
		} while (true);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mFILTER_VALUE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = FILTER_VALUE;
		int _saveIndex;
		
		{
		{
		match(_tokenSet_2);
		}
		{
		_loop1556:
		do {
			if ((_tokenSet_3.member(LA(1)))) {
				{
				match(_tokenSet_3);
				}
			}
			else {
				break _loop1556;
			}
			
		} while (true);
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	
	private static final long[] mk_tokenSet_0() {
		long[] data = new long[3988];
		data[0]=-17179869186L;
		data[1]=-1L;
		data[3]=-36028797027352577L;
		for (int i = 4; i<=127; i++) { data[i]=-1L; }
		for (int i = 193; i<=197; i++) { data[i]=-1L; }
		data[198]=65535L;
		for (int i = 204; i<=205; i++) { data[i]=-1L; }
		for (int i = 208; i<=243; i++) { data[i]=-1L; }
		data[244]=70368744177663L;
		for (int i = 312; i<=639; i++) { data[i]=-1L; }
		for (int i = 996; i<=1003; i++) { data[i]=-1L; }
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = new long[1025];
		data[0]=288019269919178752L;
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = new long[2048];
		data[0]=-3582002724866L;
		data[1]=-1152921504606846977L;
		for (int i = 2; i<=127; i++) { data[i]=-1L; }
		for (int i = 193; i<=197; i++) { data[i]=-1L; }
		data[198]=65535L;
		for (int i = 204; i<=205; i++) { data[i]=-1L; }
		for (int i = 208; i<=243; i++) { data[i]=-1L; }
		data[244]=70368744177663L;
		for (int i = 312; i<=639; i++) { data[i]=-1L; }
		for (int i = 996; i<=1003; i++) { data[i]=-1L; }
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = new long[2048];
		data[0]=-2199023255554L;
		for (int i = 1; i<=127; i++) { data[i]=-1L; }
		for (int i = 193; i<=197; i++) { data[i]=-1L; }
		data[198]=65535L;
		for (int i = 204; i<=205; i++) { data[i]=-1L; }
		for (int i = 208; i<=243; i++) { data[i]=-1L; }
		data[244]=70368744177663L;
		for (int i = 312; i<=639; i++) { data[i]=-1L; }
		for (int i = 996; i<=1003; i++) { data[i]=-1L; }
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	
	}
