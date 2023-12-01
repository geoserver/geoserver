// $ANTLR 2.7.4: "distinguishedName.g" -> "AntlrDnLexer.java"$

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
package org.apache.directory.shared.ldap.name;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import javax.naming.NameParser;
import org.apache.directory.shared.ldap.entry.StringValue;
import org.apache.directory.shared.ldap.entry.BinaryValue;
import org.apache.directory.shared.ldap.schema.parsers.ParserMonitor;
import org.apache.directory.shared.ldap.util.StringTools;


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
 * An antlr generated DN lexer.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AntlrDnLexer extends antlr.CharScanner implements AntlrDnTokenTypes, TokenStream
 {
public AntlrDnLexer(InputStream in) {
	this(new ByteBuffer(in));
}
public AntlrDnLexer(Reader in) {
	this(new CharBuffer(in));
}
public AntlrDnLexer(InputBuffer ib) {
	this(new LexerSharedInputState(ib));
}
public AntlrDnLexer(LexerSharedInputState state) {
	super(state);
	caseSensitiveLiterals = true;
	setCaseSensitive(false);
	literals = new Hashtable();
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
				case ',':
				{
					mCOMMA(true);
					theRetToken=_returnToken;
					break;
				}
				case '=':
				{
					mEQUALS(true);
					theRetToken=_returnToken;
					break;
				}
				case '+':
				{
					mPLUS(true);
					theRetToken=_returnToken;
					break;
				}
				case '-':
				{
					mHYPHEN(true);
					theRetToken=_returnToken;
					break;
				}
				case '"':
				{
					mDQUOTE(true);
					theRetToken=_returnToken;
					break;
				}
				case ';':
				{
					mSEMI(true);
					theRetToken=_returnToken;
					break;
				}
				case '<':
				{
					mLANGLE(true);
					theRetToken=_returnToken;
					break;
				}
				case '>':
				{
					mRANGLE(true);
					theRetToken=_returnToken;
					break;
				}
				case ' ':
				{
					mSPACE(true);
					theRetToken=_returnToken;
					break;
				}
				case '0':  case '1':  case '2':  case '3':
				case '4':  case '5':  case '6':  case '7':
				case '8':  case '9':  case 'a':  case 'b':
				case 'c':  case 'd':  case 'e':  case 'f':
				case 'g':  case 'h':  case 'i':  case 'j':
				case 'k':  case 'l':  case 'm':  case 'n':
				case 'o':  case 'p':  case 'q':  case 'r':
				case 's':  case 't':  case 'u':  case 'v':
				case 'w':  case 'x':  case 'y':  case 'z':
				{
					mNUMERICOID_OR_ALPHA_OR_DIGIT(true);
					theRetToken=_returnToken;
					break;
				}
				case '\\':
				{
					mHEXPAIR_OR_ESCESC_ESCSHARP_OR_ESC(true);
					theRetToken=_returnToken;
					break;
				}
				case '#':
				{
					mHEXVALUE_OR_SHARP(true);
					theRetToken=_returnToken;
					break;
				}
				case '\u0001':  case '\u0002':  case '\u0003':  case '\u0004':
				case '\u0005':  case '\u0006':  case '\u0007':  case '\u0008':
				case '\t':  case '\n':  case '\u000b':  case '\u000c':
				case '\r':  case '\u000e':  case '\u000f':  case '\u0010':
				case '\u0011':  case '\u0012':  case '\u0013':  case '\u0014':
				case '\u0015':  case '\u0016':  case '\u0017':  case '\u0018':
				case '\u0019':  case '\u001a':  case '\u001b':  case '\u001c':
				case '\u001d':  case '\u001e':  case '\u001f':  case '!':
				case '$':  case '%':  case '&':  case '\'':
				case '(':  case ')':  case '*':  case '.':
				case '/':  case ':':  case '?':  case '@':
				case '[':  case ']':  case '^':  case '_':
				case '`':  case '{':  case '|':  case '}':
				case '~':  case '\u007f':
				{
					mLUTF1_REST(true);
					theRetToken=_returnToken;
					break;
				}
				default:
					if (((LA(1) >= '\u0080' && LA(1) <= '\ufffe'))) {
						mUTFMB(true);
						theRetToken=_returnToken;
					}
				else {
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

	public final void mCOMMA(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = COMMA;
		int _saveIndex;
		
		match(',');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mEQUALS(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = EQUALS;
		int _saveIndex;
		
		match('=');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mPLUS(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = PLUS;
		int _saveIndex;
		
		match('+');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mHYPHEN(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = HYPHEN;
		int _saveIndex;
		
		match('-');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mDQUOTE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = DQUOTE;
		int _saveIndex;
		
		match('"');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mSEMI(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = SEMI;
		int _saveIndex;
		
		match(';');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mLANGLE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LANGLE;
		int _saveIndex;
		
		match('<');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mRANGLE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = RANGLE;
		int _saveIndex;
		
		match('>');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mSPACE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = SPACE;
		int _saveIndex;
		
		match(' ');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mNUMERICOID_OR_ALPHA_OR_DIGIT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = NUMERICOID_OR_ALPHA_OR_DIGIT;
		int _saveIndex;
		
		boolean synPredMatched1882 = false;
		if (((_tokenSet_0.member(LA(1))) && (_tokenSet_1.member(LA(2))))) {
			int _m1882 = mark();
			synPredMatched1882 = true;
			inputState.guessing++;
			try {
				{
				mNUMERICOID(false);
				}
			}
			catch (RecognitionException pe) {
				synPredMatched1882 = false;
			}
			rewind(_m1882);
			inputState.guessing--;
		}
		if ( synPredMatched1882 ) {
			mNUMERICOID(false);
			if ( inputState.guessing==0 ) {
				_ttype = NUMERICOID;
			}
		}
		else {
			boolean synPredMatched1884 = false;
			if ((((LA(1) >= '0' && LA(1) <= '9')) && (true))) {
				int _m1884 = mark();
				synPredMatched1884 = true;
				inputState.guessing++;
				try {
					{
					mDIGIT(false);
					}
				}
				catch (RecognitionException pe) {
					synPredMatched1884 = false;
				}
				rewind(_m1884);
				inputState.guessing--;
			}
			if ( synPredMatched1884 ) {
				mDIGIT(false);
				if ( inputState.guessing==0 ) {
					_ttype = DIGIT;
				}
			}
			else {
				boolean synPredMatched1886 = false;
				if ((((LA(1) >= 'a' && LA(1) <= 'z')) && (true))) {
					int _m1886 = mark();
					synPredMatched1886 = true;
					inputState.guessing++;
					try {
						{
						mALPHA(false);
						}
					}
					catch (RecognitionException pe) {
						synPredMatched1886 = false;
					}
					rewind(_m1886);
					inputState.guessing--;
				}
				if ( synPredMatched1886 ) {
					mALPHA(false);
					if ( inputState.guessing==0 ) {
						_ttype = ALPHA;
					}
				}
				else {
					throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
				}
				}}
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
		
		{
		switch ( LA(1)) {
		case 'o':
		{
			match("oid.");
			break;
		}
		case '0':  case '1':  case '2':  case '3':
		case '4':  case '5':  case '6':  case '7':
		case '8':  case '9':
		{
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		}
		}
		mNUMBER(false);
		{
		int _cnt1890=0;
		_loop1890:
		do {
			if ((LA(1)=='.')) {
				mDOT(false);
				mNUMBER(false);
			}
			else {
				if ( _cnt1890>=1 ) { break _loop1890; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			
			_cnt1890++;
		} while (true);
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
		
		matchRange('0','9');
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
		
		matchRange('a','z');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mNUMBER(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = NUMBER;
		int _saveIndex;
		
		if (((LA(1) >= '1' && LA(1) <= '9')) && ((LA(2) >= '0' && LA(2) <= '9'))) {
			{
			mLDIGIT(false);
			{
			int _cnt1895=0;
			_loop1895:
			do {
				if (((LA(1) >= '0' && LA(1) <= '9'))) {
					mDIGIT(false);
				}
				else {
					if ( _cnt1895>=1 ) { break _loop1895; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
				}
				
				_cnt1895++;
			} while (true);
			}
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
	
	public final void mHEXPAIR_OR_ESCESC_ESCSHARP_OR_ESC(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = HEXPAIR_OR_ESCESC_ESCSHARP_OR_ESC;
		int _saveIndex;
		
		boolean synPredMatched1901 = false;
		if (((LA(1)=='\\') && (_tokenSet_2.member(LA(2))))) {
			int _m1901 = mark();
			synPredMatched1901 = true;
			inputState.guessing++;
			try {
				{
				mESC(false);
				mHEX(false);
				mHEX(false);
				}
			}
			catch (RecognitionException pe) {
				synPredMatched1901 = false;
			}
			rewind(_m1901);
			inputState.guessing--;
		}
		if ( synPredMatched1901 ) {
			mHEXPAIR(false);
			if ( inputState.guessing==0 ) {
				_ttype = HEXPAIR;
			}
		}
		else if ((LA(1)=='\\') && (LA(2)=='\\')) {
			mESCESC(false);
			if ( inputState.guessing==0 ) {
				_ttype = ESCESC;
			}
		}
		else if ((LA(1)=='\\') && (LA(2)=='#')) {
			mESCSHARP(false);
			if ( inputState.guessing==0 ) {
				_ttype = ESCSHARP;
			}
		}
		else if ((LA(1)=='\\') && (true)) {
			mESC(false);
			if ( inputState.guessing==0 ) {
				_ttype = ESC;
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
	
	protected final void mESC(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = ESC;
		int _saveIndex;
		
		match('\\');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mHEX(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = HEX;
		int _saveIndex;
		
		switch ( LA(1)) {
		case '0':  case '1':  case '2':  case '3':
		case '4':  case '5':  case '6':  case '7':
		case '8':  case '9':
		{
			mDIGIT(false);
			break;
		}
		case 'a':  case 'b':  case 'c':  case 'd':
		case 'e':  case 'f':
		{
			matchRange('a','f');
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
	
	protected final void mHEXPAIR(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = HEXPAIR;
		int _saveIndex;
		
		_saveIndex=text.length();
		mESC(false);
		text.setLength(_saveIndex);
		mHEX(false);
		mHEX(false);
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mESCESC(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = ESCESC;
		int _saveIndex;
		
		mESC(false);
		mESC(false);
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mESCSHARP(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = ESCSHARP;
		int _saveIndex;
		
		mESC(false);
		mSHARP(false);
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mSHARP(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = SHARP;
		int _saveIndex;
		
		match('#');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mHEXVALUE_OR_SHARP(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = HEXVALUE_OR_SHARP;
		int _saveIndex;
		
		boolean synPredMatched1911 = false;
		if (((LA(1)=='#') && (_tokenSet_2.member(LA(2))))) {
			int _m1911 = mark();
			synPredMatched1911 = true;
			inputState.guessing++;
			try {
				{
				mSHARP(false);
				{
				int _cnt1910=0;
				_loop1910:
				do {
					if ((_tokenSet_2.member(LA(1)))) {
						mHEX(false);
						mHEX(false);
					}
					else {
						if ( _cnt1910>=1 ) { break _loop1910; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
					}
					
					_cnt1910++;
				} while (true);
				}
				}
			}
			catch (RecognitionException pe) {
				synPredMatched1911 = false;
			}
			rewind(_m1911);
			inputState.guessing--;
		}
		if ( synPredMatched1911 ) {
			mHEXVALUE(false);
			if ( inputState.guessing==0 ) {
				_ttype = HEXVALUE;
			}
		}
		else if ((LA(1)=='#') && (true)) {
			mSHARP(false);
			if ( inputState.guessing==0 ) {
				_ttype = SHARP;
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
	
	protected final void mHEXVALUE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = HEXVALUE;
		int _saveIndex;
		
		_saveIndex=text.length();
		mSHARP(false);
		text.setLength(_saveIndex);
		{
		int _cnt1914=0;
		_loop1914:
		do {
			if ((_tokenSet_2.member(LA(1)))) {
				mHEX(false);
				mHEX(false);
			}
			else {
				if ( _cnt1914>=1 ) { break _loop1914; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			
			_cnt1914++;
		} while (true);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mUTFMB(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = UTFMB;
		int _saveIndex;
		
		matchRange('\u0080','\uFFFE');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
/**
 * RFC 4514, Section 3:
 * LUTF1 = %x01-1F / %x21 / %x24-2A / %x2D-3A /
 *    %x3D / %x3F-5B / %x5D-7F
 *
 * To avoid nondeterminism the following 
 * rules are excluded. These rules are 
 * explicitly added in the productions.
 *   EQUALS (0x3D) 
 *   HYPHEN (0x2D)  
 *   DIGIT (0x30-0x39)
 *   ALPHA (0x41-0x5A and 0x61-0x7A)
 */
	public final void mLUTF1_REST(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LUTF1_REST;
		int _saveIndex;
		
		switch ( LA(1)) {
		case '\u0001':  case '\u0002':  case '\u0003':  case '\u0004':
		case '\u0005':  case '\u0006':  case '\u0007':  case '\u0008':
		case '\t':  case '\n':  case '\u000b':  case '\u000c':
		case '\r':  case '\u000e':  case '\u000f':  case '\u0010':
		case '\u0011':  case '\u0012':  case '\u0013':  case '\u0014':
		case '\u0015':  case '\u0016':  case '\u0017':  case '\u0018':
		case '\u0019':  case '\u001a':  case '\u001b':  case '\u001c':
		case '\u001d':  case '\u001e':  case '\u001f':
		{
			matchRange('\u0001','\u001F');
			break;
		}
		case '!':
		{
			match('\u0021');
			break;
		}
		case '$':  case '%':  case '&':  case '\'':
		case '(':  case ')':  case '*':
		{
			matchRange('\u0024','\u002A');
			break;
		}
		case '.':  case '/':
		{
			matchRange('\u002E','\u002F');
			break;
		}
		case ':':
		{
			match('\u003A');
			break;
		}
		case '?':  case '@':
		{
			matchRange('\u003F','\u0040');
			break;
		}
		case '[':
		{
			match('\u005B');
			break;
		}
		case ']':  case '^':  case '_':  case '`':
		{
			matchRange('\u005D','\u0060');
			break;
		}
		case '{':  case '|':  case '}':  case '~':
		case '\u007f':
		{
			matchRange('\u007B','\u007F');
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
	
	
	private static final long[] mk_tokenSet_0() {
		long[] data = new long[1025];
		data[0]=287948901175001088L;
		data[1]=140737488355328L;
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = new long[1025];
		data[0]=288019269919178752L;
		data[1]=2199023255552L;
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = new long[1025];
		data[0]=287948901175001088L;
		data[1]=541165879296L;
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	
	}
