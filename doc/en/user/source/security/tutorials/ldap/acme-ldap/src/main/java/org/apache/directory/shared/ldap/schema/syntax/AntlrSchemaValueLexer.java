// $ANTLR 2.7.4: "schema-value.g" -> "AntlrSchemaValueLexer.java"$

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
 * An antlr generated schema lexer. This is a sub-lexer.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AntlrSchemaValueLexer extends antlr.CharScanner implements AntlrSchemaValueTokenTypes, TokenStream
 {
public AntlrSchemaValueLexer(InputStream in) {
	this(new ByteBuffer(in));
}
public AntlrSchemaValueLexer(Reader in) {
	this(new CharBuffer(in));
}
public AntlrSchemaValueLexer(InputBuffer ib) {
	this(new LexerSharedInputState(ib));
}
public AntlrSchemaValueLexer(LexerSharedInputState state) {
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
				case '\t':  case '\n':  case '\r':  case ' ':
				case '#':
				{
					mWHSP(true);
					theRetToken=_returnToken;
					break;
				}
				case '(':
				{
					mLPAR(true);
					theRetToken=_returnToken;
					break;
				}
				case ')':
				{
					mRPAR(true);
					theRetToken=_returnToken;
					break;
				}
				case '\'':
				{
					mQUOTE(true);
					theRetToken=_returnToken;
					break;
				}
				case '$':
				{
					mDOLLAR(true);
					theRetToken=_returnToken;
					break;
				}
				case '}':
				{
					mRCURLY(true);
					theRetToken=_returnToken;
					break;
				}
				case '-':  case '.':  case '0':  case '1':
				case '2':  case '3':  case '4':  case '5':
				case '6':  case '7':  case '8':  case '9':
				case ':':  case ';':  case '_':  case 'a':
				case 'b':  case 'c':  case 'd':  case 'e':
				case 'f':  case 'g':  case 'h':  case 'i':
				case 'j':  case 'k':  case 'l':  case 'm':
				case 'n':  case 'o':  case 'p':  case 'q':
				case 'r':  case 's':  case 't':  case 'u':
				case 'v':  case 'w':  case 'x':  case 'y':
				case 'z':
				{
					mDESCR_OR_QUIRKS_DESCR(true);
					theRetToken=_returnToken;
					break;
				}
				default:
					if ((LA(1)=='{') && ((LA(2) >= '0' && LA(2) <= '9'))) {
						mLEN(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='{') && (true)) {
						mLCURLY(true);
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

	public final void mWHSP(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = WHSP;
		int _saveIndex;
		
		{
		int _cnt972=0;
		_loop972:
		do {
			switch ( LA(1)) {
			case ' ':
			{
				match(' ');
				break;
			}
			case '\t':
			{
				match('\t');
				break;
			}
			case '\r':
			{
				match('\r');
				{
				if ((LA(1)=='\n') && (true) && (true)) {
					match('\n');
				}
				else {
				}
				
				}
				if ( inputState.guessing==0 ) {
					newline();
				}
				break;
			}
			case '\n':
			{
				match('\n');
				if ( inputState.guessing==0 ) {
					newline();
				}
				break;
			}
			case '#':
			{
				match('#');
				{
				_loop971:
				do {
					if ((_tokenSet_0.member(LA(1)))) {
						matchNot('\n');
					}
					else {
						break _loop971;
					}
					
				} while (true);
				}
				match('\n');
				if ( inputState.guessing==0 ) {
					newline();
				}
				break;
			}
			default:
			{
				if ( _cnt972>=1 ) { break _loop972; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			}
			_cnt972++;
		} while (true);
		}
		if ( inputState.guessing==0 ) {
			setText(" ");
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mLPAR(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LPAR;
		int _saveIndex;
		
		match('(');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mRPAR(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = RPAR;
		int _saveIndex;
		
		match(')');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mCHAR(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = CHAR;
		int _saveIndex;
		
		matchRange('a','z');
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
	
	protected final void mNUMBER(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = NUMBER;
		int _saveIndex;
		
		if (((LA(1) >= '1' && LA(1) <= '9')) && ((LA(2) >= '0' && LA(2) <= '9'))) {
			{
			mLDIGIT(false);
			{
			int _cnt981=0;
			_loop981:
			do {
				if (((LA(1) >= '0' && LA(1) <= '9'))) {
					mDIGIT(false);
				}
				else {
					if ( _cnt981>=1 ) { break _loop981; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
				}
				
				_cnt981++;
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
	
	protected final void mNUMBER2(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = NUMBER2;
		int _saveIndex;
		
		{
		int _cnt984=0;
		_loop984:
		do {
			if (((LA(1) >= '0' && LA(1) <= '9'))) {
				mDIGIT(false);
			}
			else {
				if ( _cnt984>=1 ) { break _loop984; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			
			_cnt984++;
		} while (true);
		}
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
		
		mNUMBER(false);
		{
		int _cnt987=0;
		_loop987:
		do {
			if ((LA(1)=='.')) {
				match('.');
				mNUMBER(false);
			}
			else {
				if ( _cnt987>=1 ) { break _loop987; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			
			_cnt987++;
		} while (true);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mHYPEN(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = HYPEN;
		int _saveIndex;
		
		match('-');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mOTHER(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = OTHER;
		int _saveIndex;
		
		switch ( LA(1)) {
		case '_':
		{
			match('_');
			break;
		}
		case ';':
		{
			match(';');
			break;
		}
		case '.':
		{
			match('.');
			break;
		}
		case ':':
		{
			match(':');
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
	
	protected final void mDESCR(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = DESCR;
		int _saveIndex;
		
		mCHAR(false);
		{
		_loop992:
		do {
			switch ( LA(1)) {
			case 'a':  case 'b':  case 'c':  case 'd':
			case 'e':  case 'f':  case 'g':  case 'h':
			case 'i':  case 'j':  case 'k':  case 'l':
			case 'm':  case 'n':  case 'o':  case 'p':
			case 'q':  case 'r':  case 's':  case 't':
			case 'u':  case 'v':  case 'w':  case 'x':
			case 'y':  case 'z':
			{
				mCHAR(false);
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
				mHYPEN(false);
				break;
			}
			default:
			{
				break _loop992;
			}
			}
		} while (true);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mQUIRKS_DESCR(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = QUIRKS_DESCR;
		int _saveIndex;
		
		{
		int _cnt995=0;
		_loop995:
		do {
			switch ( LA(1)) {
			case 'a':  case 'b':  case 'c':  case 'd':
			case 'e':  case 'f':  case 'g':  case 'h':
			case 'i':  case 'j':  case 'k':  case 'l':
			case 'm':  case 'n':  case 'o':  case 'p':
			case 'q':  case 'r':  case 's':  case 't':
			case 'u':  case 'v':  case 'w':  case 'x':
			case 'y':  case 'z':
			{
				mCHAR(false);
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
				mHYPEN(false);
				break;
			}
			case '.':  case ':':  case ';':  case '_':
			{
				mOTHER(false);
				break;
			}
			default:
			{
				if ( _cnt995>=1 ) { break _loop995; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			}
			_cnt995++;
		} while (true);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mQUOTE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = QUOTE;
		int _saveIndex;
		
		match('\'');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mDOLLAR(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = DOLLAR;
		int _saveIndex;
		
		match('$');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mLCURLY(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LCURLY;
		int _saveIndex;
		
		match('{');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mRCURLY(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = RCURLY;
		int _saveIndex;
		
		match('}');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mLEN(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LEN;
		int _saveIndex;
		Token n=null;
		
		mLCURLY(false);
		mNUMBER2(true);
		n=_returnToken;
		mRCURLY(false);
		if ( inputState.guessing==0 ) {
			setText(n.getText());
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mDESCR_OR_QUIRKS_DESCR(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = DESCR_OR_QUIRKS_DESCR;
		int _saveIndex;
		
		boolean synPredMatched1014 = false;
		if ((((LA(1) >= '0' && LA(1) <= '9')) && (_tokenSet_1.member(LA(2))) && (_tokenSet_1.member(LA(3))))) {
			int _m1014 = mark();
			synPredMatched1014 = true;
			inputState.guessing++;
			try {
				{
				mNUMBER(false);
				match('.');
				}
			}
			catch (RecognitionException pe) {
				synPredMatched1014 = false;
			}
			rewind(_m1014);
			inputState.guessing--;
		}
		if ( synPredMatched1014 ) {
			mNUMERICOID(false);
			if ( inputState.guessing==0 ) {
				_ttype =  NUMERICOID;
			}
		}
		else {
			boolean synPredMatched1003 = false;
			if (((_tokenSet_2.member(LA(1))) && (true) && (true))) {
				int _m1003 = mark();
				synPredMatched1003 = true;
				inputState.guessing++;
				try {
					{
					mNUMERICOID(false);
					mQUIRKS_DESCR(false);
					}
				}
				catch (RecognitionException pe) {
					synPredMatched1003 = false;
				}
				rewind(_m1003);
				inputState.guessing--;
			}
			if ( synPredMatched1003 ) {
				mQUIRKS_DESCR(false);
				if ( inputState.guessing==0 ) {
					_ttype =  QUIRKS_DESCR;
				}
			}
			else {
				boolean synPredMatched1005 = false;
				if (((_tokenSet_2.member(LA(1))) && (true) && (true))) {
					int _m1005 = mark();
					synPredMatched1005 = true;
					inputState.guessing++;
					try {
						{
						mNUMBER(false);
						mQUIRKS_DESCR(false);
						}
					}
					catch (RecognitionException pe) {
						synPredMatched1005 = false;
					}
					rewind(_m1005);
					inputState.guessing--;
				}
				if ( synPredMatched1005 ) {
					mQUIRKS_DESCR(false);
					if ( inputState.guessing==0 ) {
						_ttype =  QUIRKS_DESCR;
					}
				}
				else {
					boolean synPredMatched1007 = false;
					if (((_tokenSet_2.member(LA(1))) && (true) && (true))) {
						int _m1007 = mark();
						synPredMatched1007 = true;
						inputState.guessing++;
						try {
							{
							mHYPEN(false);
							mQUIRKS_DESCR(false);
							}
						}
						catch (RecognitionException pe) {
							synPredMatched1007 = false;
						}
						rewind(_m1007);
						inputState.guessing--;
					}
					if ( synPredMatched1007 ) {
						mQUIRKS_DESCR(false);
						if ( inputState.guessing==0 ) {
							_ttype =  QUIRKS_DESCR;
						}
					}
					else {
						boolean synPredMatched1009 = false;
						if (((_tokenSet_2.member(LA(1))) && (true) && (true))) {
							int _m1009 = mark();
							synPredMatched1009 = true;
							inputState.guessing++;
							try {
								{
								mOTHER(false);
								mQUIRKS_DESCR(false);
								}
							}
							catch (RecognitionException pe) {
								synPredMatched1009 = false;
							}
							rewind(_m1009);
							inputState.guessing--;
						}
						if ( synPredMatched1009 ) {
							mQUIRKS_DESCR(false);
							if ( inputState.guessing==0 ) {
								_ttype =  QUIRKS_DESCR;
							}
						}
						else {
							boolean synPredMatched1011 = false;
							if (((_tokenSet_2.member(LA(1))) && (true) && (true))) {
								int _m1011 = mark();
								synPredMatched1011 = true;
								inputState.guessing++;
								try {
									{
									mDESCR(false);
									mQUIRKS_DESCR(false);
									}
								}
								catch (RecognitionException pe) {
									synPredMatched1011 = false;
								}
								rewind(_m1011);
								inputState.guessing--;
							}
							if ( synPredMatched1011 ) {
								mQUIRKS_DESCR(false);
								if ( inputState.guessing==0 ) {
									_ttype =  QUIRKS_DESCR;
								}
							}
							else if (((LA(1) >= 'a' && LA(1) <= 'z')) && (true) && (true)) {
								{
								mDESCR(false);
								}
								if ( inputState.guessing==0 ) {
									_ttype =  DESCR;
								}
							}
							else if (((LA(1) >= '0' && LA(1) <= '9')) && (true) && (true)) {
								{
								mNUMBER(false);
								}
								if ( inputState.guessing==0 ) {
									_ttype =  NUMBER;
								}
							}
							else {
								throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
							}
							}}}}}
							if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
								_token = makeToken(_ttype);
								_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
							}
							_returnToken = _token;
						}
						
						
						private static final long[] mk_tokenSet_0() {
							long[] data = new long[8];
							data[0]=-1032L;
							for (int i = 1; i<=3; i++) { data[i]=-1L; }
							return data;
						}
						public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
						private static final long[] mk_tokenSet_1() {
							long[] data = { 288019269919178752L, 0L, 0L, 0L, 0L};
							return data;
						}
						public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
						private static final long[] mk_tokenSet_2() {
							long[] data = { 1152745582746402816L, 576460745860972544L, 0L, 0L, 0L};
							return data;
						}
						public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
						
						}
