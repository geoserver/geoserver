// $ANTLR 2.7.4: "schema.g" -> "AntlrSchemaLexer.java"$

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
 * An antlr generated schema main lexer.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AntlrSchemaLexer extends antlr.CharScanner implements AntlrSchemaTokenTypes, TokenStream
 {
public AntlrSchemaLexer(InputStream in) {
	this(new ByteBuffer(in));
}
public AntlrSchemaLexer(Reader in) {
	this(new CharBuffer(in));
}
public AntlrSchemaLexer(InputBuffer ib) {
	this(new LexerSharedInputState(ib));
}
public AntlrSchemaLexer(LexerSharedInputState state) {
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
					mRBRACKET(true);
					theRetToken=_returnToken;
					break;
				}
				case 'c':
				{
					mCOLLECTIVE(true);
					theRetToken=_returnToken;
					break;
				}
				case 'e':
				{
					mEQUALITY(true);
					theRetToken=_returnToken;
					break;
				}
				case 'x':
				{
					mEXTENSION(true);
					theRetToken=_returnToken;
					break;
				}
				case 'b':
				{
					mBYTECODE(true);
					theRetToken=_returnToken;
					break;
				}
				default:
					if ((LA(1)=='o') && (LA(2)=='b') && (LA(3)=='j') && (LA(4)=='e') && (LA(5)=='c') && (LA(6)=='t') && (LA(7)=='i')) {
						mOBJECTIDENTIFIER(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='o') && (LA(2)=='b') && (LA(3)=='j') && (LA(4)=='e') && (LA(5)=='c') && (LA(6)=='t') && (LA(7)=='c')) {
						mOBJECTCLASS(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='n') && (LA(2)=='o') && (LA(3)=='-')) {
						mNO_USER_MODIFICATION(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='o') && (LA(2)=='b') && (LA(3)=='s')) {
						mOBSOLETE(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='s') && (LA(2)=='u') && (LA(3)=='p')) {
						mSUP(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='n') && (LA(2)=='o') && (LA(3)=='t')) {
						mNOT(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='s') && (LA(2)=='u') && (LA(3)=='b')) {
						mSUBSTR(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='u') && (LA(2)=='s') && (LA(3)=='a')) {
						mUSAGE(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='u') && (LA(2)=='s') && (LA(3)=='e')) {
						mUSER_APPLICATIONS(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='d') && (LA(2)=='i') && (LA(3)=='r')) {
						mDIRECTORY_OPERATION(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='d') && (LA(2)=='i') && (LA(3)=='s')) {
						mDISTRIBUTED_OPERATION(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='{') && ((LA(2) >= '0' && LA(2) <= '9'))) {
						mLEN(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='s') && (LA(2)=='i')) {
						mSINGLE_VALUE(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='a') && (LA(2)=='b')) {
						mABSTRACT(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='s') && (LA(2)=='t')) {
						mSTRUCTURAL(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='a') && (LA(2)=='t')) {
						mATTRIBUTETYPE(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='(') && (_tokenSet_0.member(LA(2)))) {
						mSTARTNUMERICOID(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='n') && (LA(2)=='a')) {
						mNAME(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='d') && (LA(2)=='e')) {
						mDESC(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='m') && (LA(2)=='u')) {
						mMUST(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='m') && (LA(2)=='a')) {
						mMAY(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='f') && (LA(2)=='o')) {
						mFORM(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='o') && (LA(2)=='c')) {
						mOC(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='o') && (LA(2)=='r')) {
						mORDERING(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='s') && (LA(2)=='y')) {
						mSYNTAX(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='a') && (LA(2)=='p')) {
						mAPPLIES(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='f') && (LA(2)=='q')) {
						mFQCN(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='a') && (LA(2)=='u')) {
						mAUX_OR_AUXILIARY(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='d') && (LA(2)=='s')) {
						mDSA_OPERATION(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='(') && (true)) {
						mLPAR(true);
						theRetToken=_returnToken;
					}
					else if ((LA(1)=='{') && (true)) {
						mLBRACKET(true);
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
		int _cnt1131=0;
		_loop1131:
		do {
			if ((LA(1)=='#') && ((LA(2) >= '\u0000' && LA(2) <= '\ufffe')) && (true) && (true) && (true) && (true) && (true) && (true)) {
				match('#');
				{
				_loop1130:
				do {
					if ((_tokenSet_1.member(LA(1)))) {
						matchNot('\n');
					}
					else {
						break _loop1130;
					}
					
				} while (true);
				}
				match('\n');
				if ( inputState.guessing==0 ) {
					newline();
				}
			}
			else if ((LA(1)==' ') && (true) && (true) && (true) && (true) && (true) && (true) && (true)) {
				match(' ');
			}
			else if ((LA(1)=='\t') && (true) && (true) && (true) && (true) && (true) && (true) && (true)) {
				match('\t');
			}
			else if ((LA(1)=='\r') && (true) && (true) && (true) && (true) && (true) && (true) && (true)) {
				match('\r');
				{
				if ((LA(1)=='\n') && (true) && (true) && (true) && (true) && (true) && (true) && (true)) {
					match('\n');
				}
				else {
				}
				
				}
				if ( inputState.guessing==0 ) {
					newline();
				}
			}
			else if ((LA(1)=='\n') && (true) && (true) && (true) && (true) && (true) && (true) && (true)) {
				match('\n');
				if ( inputState.guessing==0 ) {
					newline();
				}
			}
			else {
				if ( _cnt1131>=1 ) { break _loop1131; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			
			_cnt1131++;
		} while (true);
		}
		if ( inputState.guessing==0 ) {
			_ttype = Token.SKIP;
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
	
	public final void mLBRACKET(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = LBRACKET;
		int _saveIndex;
		
		match('{');
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mRBRACKET(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = RBRACKET;
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
		
		mLBRACKET(false);
		{
		int _cnt1140=0;
		_loop1140:
		do {
			if (((LA(1) >= '0' && LA(1) <= '9'))) {
				matchRange('0','9');
			}
			else {
				if ( _cnt1140>=1 ) { break _loop1140; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			
			_cnt1140++;
		} while (true);
		}
		mRBRACKET(false);
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mSINGLE_VALUE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = SINGLE_VALUE;
		int _saveIndex;
		
		{
		match("single-value");
		{
		if ((_tokenSet_2.member(LA(1)))) {
			mWHSP(false);
		}
		else {
		}
		
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mCOLLECTIVE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = COLLECTIVE;
		int _saveIndex;
		
		{
		match("collective");
		{
		if ((_tokenSet_2.member(LA(1)))) {
			mWHSP(false);
		}
		else {
		}
		
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mNO_USER_MODIFICATION(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = NO_USER_MODIFICATION;
		int _saveIndex;
		
		{
		match("no-user-modification");
		{
		if ((_tokenSet_2.member(LA(1)))) {
			mWHSP(false);
		}
		else {
		}
		
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mOBSOLETE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = OBSOLETE;
		int _saveIndex;
		
		{
		match("obsolete");
		{
		if ((_tokenSet_2.member(LA(1)))) {
			mWHSP(false);
		}
		else {
		}
		
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mABSTRACT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = ABSTRACT;
		int _saveIndex;
		
		{
		match("abstract");
		{
		if ((_tokenSet_2.member(LA(1)))) {
			mWHSP(false);
		}
		else {
		}
		
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mSTRUCTURAL(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = STRUCTURAL;
		int _saveIndex;
		
		{
		match("structural");
		{
		if ((_tokenSet_2.member(LA(1)))) {
			mWHSP(false);
		}
		else {
		}
		
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mAUXILIARY(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = AUXILIARY;
		int _saveIndex;
		
		{
		match("auxiliary");
		{
		if ((_tokenSet_2.member(LA(1)))) {
			mWHSP(false);
		}
		else {
		}
		
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mOBJECTIDENTIFIER(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = OBJECTIDENTIFIER;
		int _saveIndex;
		Token oiName=null;
		Token oiValue=null;
		
		{
		match("objectidentifier");
		mWHSP(false);
		{
		mUNQUOTED_STRING(true);
		oiName=_returnToken;
		}
		mWHSP(false);
		{
		mUNQUOTED_STRING(true);
		oiValue=_returnToken;
		}
		}
		if ( inputState.guessing==0 ) {
			setText( oiName.getText() + " " + oiValue.getText() );
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mUNQUOTED_STRING(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = UNQUOTED_STRING;
		int _saveIndex;
		
		{
		int _cnt1245=0;
		_loop1245:
		do {
			if (((LA(1) >= 'a' && LA(1) <= 'z')) && (true) && (true) && (true) && (true) && (true) && (true) && (true)) {
				matchRange('a','z');
			}
			else if (((LA(1) >= '0' && LA(1) <= '9')) && (true) && (true) && (true) && (true) && (true) && (true) && (true)) {
				matchRange('0','9');
			}
			else if ((LA(1)=='-') && (true) && (true) && (true) && (true) && (true) && (true) && (true)) {
				match('-');
			}
			else if ((LA(1)=='_') && (true) && (true) && (true) && (true) && (true) && (true) && (true)) {
				match('_');
			}
			else if ((LA(1)==';') && (true) && (true) && (true) && (true) && (true) && (true) && (true)) {
				match(';');
			}
			else if ((LA(1)=='.') && (true) && (true) && (true) && (true) && (true) && (true) && (true)) {
				match('.');
			}
			else if ((LA(1)==':') && (true) && (true) && (true) && (true) && (true) && (true) && (true)) {
				match(':');
			}
			else {
				if ( _cnt1245>=1 ) { break _loop1245; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			
			_cnt1245++;
		} while (true);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mOBJECTCLASS(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = OBJECTCLASS;
		int _saveIndex;
		
		{
		match("objectclass");
		{
		if ((_tokenSet_2.member(LA(1)))) {
			mWHSP(false);
		}
		else {
		}
		
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mATTRIBUTETYPE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = ATTRIBUTETYPE;
		int _saveIndex;
		
		{
		match("attributetype");
		{
		if ((_tokenSet_2.member(LA(1)))) {
			mWHSP(false);
		}
		else {
		}
		
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mSTARTNUMERICOID(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = STARTNUMERICOID;
		int _saveIndex;
		Token numericoid=null;
		
		{
		mLPAR(false);
		{
		if ((_tokenSet_2.member(LA(1))) && ((LA(2) >= '\u0000' && LA(2) <= '\ufffe')) && (true) && (true) && (true) && (true) && (true) && (true)) {
			mWHSP(false);
		}
		else if ((_tokenSet_0.member(LA(1))) && (true) && (true) && (true) && (true) && (true) && (true) && (true)) {
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		}
		{
		mVALUES(true);
		numericoid=_returnToken;
		}
		}
		if ( inputState.guessing==0 ) {
			setText(numericoid.getText());
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mVALUES(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = VALUES;
		int _saveIndex;
		
		{
		switch ( LA(1)) {
		case '\t':  case '\n':  case '\r':  case ' ':
		case '#':  case '\'':  case '-':  case '.':
		case '0':  case '1':  case '2':  case '3':
		case '4':  case '5':  case '6':  case '7':
		case '8':  case '9':  case ':':  case ';':
		case '_':  case 'a':  case 'b':  case 'c':
		case 'd':  case 'e':  case 'f':  case 'g':
		case 'h':  case 'i':  case 'j':  case 'k':
		case 'l':  case 'm':  case 'n':  case 'o':
		case 'p':  case 'q':  case 'r':  case 's':
		case 't':  case 'u':  case 'v':  case 'w':
		case 'x':  case 'y':  case 'z':
		{
			mVALUE(false);
			break;
		}
		case '(':
		{
			mLPAR(false);
			mVALUE(false);
			{
			_loop1238:
			do {
				if ((_tokenSet_3.member(LA(1)))) {
					{
					switch ( LA(1)) {
					case '$':
					{
						mDOLLAR(false);
						break;
					}
					case '\t':  case '\n':  case '\r':  case ' ':
					case '#':  case '\'':  case '-':  case '.':
					case '0':  case '1':  case '2':  case '3':
					case '4':  case '5':  case '6':  case '7':
					case '8':  case '9':  case ':':  case ';':
					case '_':  case 'a':  case 'b':  case 'c':
					case 'd':  case 'e':  case 'f':  case 'g':
					case 'h':  case 'i':  case 'j':  case 'k':
					case 'l':  case 'm':  case 'n':  case 'o':
					case 'p':  case 'q':  case 'r':  case 's':
					case 't':  case 'u':  case 'v':  case 'w':
					case 'x':  case 'y':  case 'z':
					{
						break;
					}
					default:
					{
						throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
					}
					}
					}
					mVALUE(false);
				}
				else {
					break _loop1238;
				}
				
			} while (true);
			}
			mRPAR(false);
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mNAME(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = NAME;
		int _saveIndex;
		Token qdstrings=null;
		
		{
		match("name");
		{
		if ((_tokenSet_2.member(LA(1))) && ((LA(2) >= '\u0000' && LA(2) <= '\ufffe')) && (true) && (true) && (true) && (true) && (true) && (true)) {
			mWHSP(false);
		}
		else if ((_tokenSet_0.member(LA(1))) && (true) && (true) && (true) && (true) && (true) && (true) && (true)) {
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		}
		mVALUES(true);
		qdstrings=_returnToken;
		}
		if ( inputState.guessing==0 ) {
			setText(qdstrings.getText().trim());
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mDESC(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = DESC;
		int _saveIndex;
		Token qdstring=null;
		
		{
		match("desc");
		{
		if ((_tokenSet_2.member(LA(1))) && ((LA(2) >= '\u0000' && LA(2) <= '\ufffe')) && (true) && (true) && (true) && (true) && (true) && (true)) {
			mWHSP(false);
		}
		else if ((_tokenSet_0.member(LA(1))) && (true) && (true) && (true) && (true) && (true) && (true) && (true)) {
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		}
		mVALUES(true);
		qdstring=_returnToken;
		}
		if ( inputState.guessing==0 ) {
			setText(qdstring.getText().trim());
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mSUP(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = SUP;
		int _saveIndex;
		Token sup=null;
		
		{
		match("sup");
		{
		if ((_tokenSet_2.member(LA(1))) && ((LA(2) >= '\u0000' && LA(2) <= '\ufffe')) && (true) && (true) && (true) && (true) && (true) && (true)) {
			mWHSP(false);
		}
		else if ((_tokenSet_0.member(LA(1))) && (true) && (true) && (true) && (true) && (true) && (true) && (true)) {
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		}
		mVALUES(true);
		sup=_returnToken;
		}
		if ( inputState.guessing==0 ) {
			setText(sup.getText().trim());
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mMUST(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = MUST;
		int _saveIndex;
		Token must=null;
		
		{
		match("must");
		{
		if ((_tokenSet_2.member(LA(1))) && ((LA(2) >= '\u0000' && LA(2) <= '\ufffe')) && (true) && (true) && (true) && (true) && (true) && (true)) {
			mWHSP(false);
		}
		else if ((_tokenSet_0.member(LA(1))) && (true) && (true) && (true) && (true) && (true) && (true) && (true)) {
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		}
		mVALUES(true);
		must=_returnToken;
		}
		if ( inputState.guessing==0 ) {
			setText(must.getText().trim());
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mMAY(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = MAY;
		int _saveIndex;
		Token may=null;
		
		{
		match("may");
		{
		if ((_tokenSet_2.member(LA(1))) && ((LA(2) >= '\u0000' && LA(2) <= '\ufffe')) && (true) && (true) && (true) && (true) && (true) && (true)) {
			mWHSP(false);
		}
		else if ((_tokenSet_0.member(LA(1))) && (true) && (true) && (true) && (true) && (true) && (true) && (true)) {
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		}
		mVALUES(true);
		may=_returnToken;
		}
		if ( inputState.guessing==0 ) {
			setText(may.getText());
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mAUX(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = AUX;
		int _saveIndex;
		Token aux=null;
		
		{
		match("aux");
		{
		if ((_tokenSet_2.member(LA(1))) && ((LA(2) >= '\u0000' && LA(2) <= '\ufffe')) && (true) && (true) && (true) && (true) && (true) && (true)) {
			mWHSP(false);
		}
		else if ((_tokenSet_0.member(LA(1))) && (true) && (true) && (true) && (true) && (true) && (true) && (true)) {
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		}
		mVALUES(true);
		aux=_returnToken;
		}
		if ( inputState.guessing==0 ) {
			setText(aux.getText());
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mNOT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = NOT;
		int _saveIndex;
		Token not=null;
		
		{
		match("not");
		{
		if ((_tokenSet_2.member(LA(1))) && ((LA(2) >= '\u0000' && LA(2) <= '\ufffe')) && (true) && (true) && (true) && (true) && (true) && (true)) {
			mWHSP(false);
		}
		else if ((_tokenSet_0.member(LA(1))) && (true) && (true) && (true) && (true) && (true) && (true) && (true)) {
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		}
		mVALUES(true);
		not=_returnToken;
		}
		if ( inputState.guessing==0 ) {
			setText(not.getText());
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mFORM(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = FORM;
		int _saveIndex;
		Token form=null;
		
		{
		match("form");
		{
		if ((_tokenSet_2.member(LA(1))) && ((LA(2) >= '\u0000' && LA(2) <= '\ufffe')) && (true) && (true) && (true) && (true) && (true) && (true)) {
			mWHSP(false);
		}
		else if ((_tokenSet_0.member(LA(1))) && (true) && (true) && (true) && (true) && (true) && (true) && (true)) {
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		}
		mVALUES(true);
		form=_returnToken;
		}
		if ( inputState.guessing==0 ) {
			setText(form.getText());
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mOC(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = OC;
		int _saveIndex;
		Token oc=null;
		
		{
		match("oc");
		{
		if ((_tokenSet_2.member(LA(1))) && ((LA(2) >= '\u0000' && LA(2) <= '\ufffe')) && (true) && (true) && (true) && (true) && (true) && (true)) {
			mWHSP(false);
		}
		else if ((_tokenSet_0.member(LA(1))) && (true) && (true) && (true) && (true) && (true) && (true) && (true)) {
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		}
		mVALUES(true);
		oc=_returnToken;
		}
		if ( inputState.guessing==0 ) {
			setText(oc.getText());
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mEQUALITY(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = EQUALITY;
		int _saveIndex;
		Token equality=null;
		
		{
		match("equality");
		{
		if ((_tokenSet_2.member(LA(1))) && ((LA(2) >= '\u0000' && LA(2) <= '\ufffe')) && (true) && (true) && (true) && (true) && (true) && (true)) {
			mWHSP(false);
		}
		else if ((_tokenSet_0.member(LA(1))) && (true) && (true) && (true) && (true) && (true) && (true) && (true)) {
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		}
		mVALUES(true);
		equality=_returnToken;
		}
		if ( inputState.guessing==0 ) {
			setText(equality.getText().trim());
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mORDERING(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = ORDERING;
		int _saveIndex;
		Token ordering=null;
		
		{
		match("ordering");
		{
		if ((_tokenSet_2.member(LA(1))) && ((LA(2) >= '\u0000' && LA(2) <= '\ufffe')) && (true) && (true) && (true) && (true) && (true) && (true)) {
			mWHSP(false);
		}
		else if ((_tokenSet_0.member(LA(1))) && (true) && (true) && (true) && (true) && (true) && (true) && (true)) {
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		}
		mVALUES(true);
		ordering=_returnToken;
		}
		if ( inputState.guessing==0 ) {
			setText(ordering.getText().trim());
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mSUBSTR(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = SUBSTR;
		int _saveIndex;
		Token substring=null;
		
		{
		match("substr");
		{
		if ((_tokenSet_2.member(LA(1))) && ((LA(2) >= '\u0000' && LA(2) <= '\ufffe')) && (true) && (true) && (true) && (true) && (true) && (true)) {
			mWHSP(false);
		}
		else if ((_tokenSet_0.member(LA(1))) && (true) && (true) && (true) && (true) && (true) && (true) && (true)) {
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		}
		mVALUES(true);
		substring=_returnToken;
		}
		if ( inputState.guessing==0 ) {
			setText(substring.getText().trim());
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mSYNTAX(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = SYNTAX;
		int _saveIndex;
		Token syntax=null;
		Token len=null;
		
		{
		match("syntax");
		{
		if ((_tokenSet_2.member(LA(1))) && ((LA(2) >= '\u0000' && LA(2) <= '\ufffe')) && (true) && (true) && (true) && (true) && (true) && (true)) {
			mWHSP(false);
		}
		else if ((_tokenSet_0.member(LA(1))) && (true) && (true) && (true) && (true) && (true) && (true) && (true)) {
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		}
		mVALUES(true);
		syntax=_returnToken;
		{
		if ((LA(1)=='{')) {
			mLEN(true);
			len=_returnToken;
		}
		else {
		}
		
		}
		}
		if ( inputState.guessing==0 ) {
			setText(syntax.getText().trim() + (len!=null?len.getText().trim():""));
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mAPPLIES(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = APPLIES;
		int _saveIndex;
		Token applies=null;
		
		{
		match("applies");
		{
		if ((_tokenSet_2.member(LA(1))) && ((LA(2) >= '\u0000' && LA(2) <= '\ufffe')) && (true) && (true) && (true) && (true) && (true) && (true)) {
			mWHSP(false);
		}
		else if ((_tokenSet_0.member(LA(1))) && (true) && (true) && (true) && (true) && (true) && (true) && (true)) {
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		}
		mVALUES(true);
		applies=_returnToken;
		}
		if ( inputState.guessing==0 ) {
			setText(applies.getText().trim());
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mEXTENSION(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = EXTENSION;
		int _saveIndex;
		Token x=null;
		
		{
		match("x-");
		{
		int _cnt1222=0;
		_loop1222:
		do {
			if (((LA(1) >= 'a' && LA(1) <= 'z')) && (_tokenSet_0.member(LA(2))) && (true) && (true) && (true) && (true) && (true) && (true)) {
				matchRange('a','z');
			}
			else if ((LA(1)=='-') && (_tokenSet_0.member(LA(2))) && (true) && (true) && (true) && (true) && (true) && (true)) {
				match('-');
			}
			else if ((LA(1)=='_') && (_tokenSet_0.member(LA(2))) && (true) && (true) && (true) && (true) && (true) && (true)) {
				match('_');
			}
			else {
				if ( _cnt1222>=1 ) { break _loop1222; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			
			_cnt1222++;
		} while (true);
		}
		{
		if ((_tokenSet_2.member(LA(1))) && ((LA(2) >= '\u0000' && LA(2) <= '\ufffe')) && (true) && (true) && (true) && (true) && (true) && (true)) {
			mWHSP(false);
		}
		else if ((_tokenSet_0.member(LA(1))) && (true) && (true) && (true) && (true) && (true) && (true) && (true)) {
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		}
		mVALUES(false);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mFQCN(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = FQCN;
		int _saveIndex;
		Token fqcn=null;
		
		{
		match("fqcn");
		{
		if ((_tokenSet_2.member(LA(1)))) {
			mWHSP(false);
		}
		else if ((_tokenSet_4.member(LA(1)))) {
		}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		
		}
		mFQCN_VALUE(true);
		fqcn=_returnToken;
		}
		if ( inputState.guessing==0 ) {
			setText(fqcn.getText().trim());
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mFQCN_VALUE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = FQCN_VALUE;
		int _saveIndex;
		
		{
		mFQCN_IDENTIFIER(false);
		{
		_loop1253:
		do {
			if ((LA(1)=='.')) {
				match('.');
				mFQCN_IDENTIFIER(false);
			}
			else {
				break _loop1253;
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
	
	public final void mBYTECODE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = BYTECODE;
		int _saveIndex;
		Token bytecode=null;
		
		{
		match("bytecode");
		{
		switch ( LA(1)) {
		case '\t':  case '\n':  case '\r':  case ' ':
		case '#':
		{
			mWHSP(false);
			break;
		}
		case '+':  case '/':  case '0':  case '1':
		case '2':  case '3':  case '4':  case '5':
		case '6':  case '7':  case '8':  case '9':
		case '=':  case 'a':  case 'b':  case 'c':
		case 'd':  case 'e':  case 'f':  case 'g':
		case 'h':  case 'i':  case 'j':  case 'k':
		case 'l':  case 'm':  case 'n':  case 'o':
		case 'p':  case 'q':  case 'r':  case 's':
		case 't':  case 'u':  case 'v':  case 'w':
		case 'x':  case 'y':  case 'z':
		{
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		}
		}
		mBYTECODE_VALUE(true);
		bytecode=_returnToken;
		}
		if ( inputState.guessing==0 ) {
			setText(bytecode.getText().trim());
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mBYTECODE_VALUE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = BYTECODE_VALUE;
		int _saveIndex;
		
		{
		int _cnt1262=0;
		_loop1262:
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
				matchRange('a','z');
				break;
			}
			case '0':  case '1':  case '2':  case '3':
			case '4':  case '5':  case '6':  case '7':
			case '8':  case '9':
			{
				matchRange('0','9');
				break;
			}
			case '+':
			{
				match('+');
				break;
			}
			case '/':
			{
				match('/');
				break;
			}
			case '=':
			{
				match('=');
				break;
			}
			default:
			{
				if ( _cnt1262>=1 ) { break _loop1262; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			}
			_cnt1262++;
		} while (true);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mAUX_OR_AUXILIARY(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = AUX_OR_AUXILIARY;
		int _saveIndex;
		
		boolean synPredMatched1232 = false;
		if (((LA(1)=='a') && (LA(2)=='u') && (LA(3)=='x') && (LA(4)=='i') && (LA(5)=='l') && (LA(6)=='i') && (LA(7)=='a') && (LA(8)=='r'))) {
			int _m1232 = mark();
			synPredMatched1232 = true;
			inputState.guessing++;
			try {
				{
				mAUXILIARY(false);
				}
			}
			catch (RecognitionException pe) {
				synPredMatched1232 = false;
			}
			rewind(_m1232);
			inputState.guessing--;
		}
		if ( synPredMatched1232 ) {
			mAUXILIARY(false);
			if ( inputState.guessing==0 ) {
				_ttype =  AUXILIARY;
			}
		}
		else if ((LA(1)=='a') && (LA(2)=='u') && (LA(3)=='x') && (_tokenSet_0.member(LA(4))) && (true) && (true) && (true) && (true)) {
			{
			mAUX(false);
			}
			if ( inputState.guessing==0 ) {
				_ttype =  AUX;
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
	
	protected final void mVALUE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = VALUE;
		int _saveIndex;
		
		{
		switch ( LA(1)) {
		case '\t':  case '\n':  case '\r':  case ' ':
		case '#':
		{
			mWHSP(false);
			break;
		}
		case '\'':  case '-':  case '.':  case '0':
		case '1':  case '2':  case '3':  case '4':
		case '5':  case '6':  case '7':  case '8':
		case '9':  case ':':  case ';':  case '_':
		case 'a':  case 'b':  case 'c':  case 'd':
		case 'e':  case 'f':  case 'g':  case 'h':
		case 'i':  case 'j':  case 'k':  case 'l':
		case 'm':  case 'n':  case 'o':  case 'p':
		case 'q':  case 'r':  case 's':  case 't':
		case 'u':  case 'v':  case 'w':  case 'x':
		case 'y':  case 'z':
		{
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		}
		}
		{
		switch ( LA(1)) {
		case '\'':
		{
			mQUOTED_STRING(false);
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
			mUNQUOTED_STRING(false);
			break;
		}
		default:
		{
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		}
		}
		{
		if ((_tokenSet_2.member(LA(1))) && (true) && (true) && (true) && (true) && (true) && (true) && (true)) {
			mWHSP(false);
		}
		else {
		}
		
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mQUOTED_STRING(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = QUOTED_STRING;
		int _saveIndex;
		
		{
		mQUOTE(false);
		{
		_loop1249:
		do {
			if ((_tokenSet_5.member(LA(1)))) {
				matchNot('\'');
			}
			else {
				break _loop1249;
			}
			
		} while (true);
		}
		mQUOTE(false);
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	protected final void mFQCN_IDENTIFIER(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = FQCN_IDENTIFIER;
		int _saveIndex;
		
		{
		mFQCN_LETTER(false);
		{
		_loop1257:
		do {
			if ((_tokenSet_6.member(LA(1)))) {
				mFQCN_LETTERORDIGIT(false);
			}
			else {
				break _loop1257;
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
	
	protected final void mFQCN_LETTER(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = FQCN_LETTER;
		int _saveIndex;
		
		switch ( LA(1)) {
		case '$':
		{
			match('\u0024');
			break;
		}
		case '_':
		{
			match('\u005f');
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
			matchRange('\u0061','\u007a');
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
	
	protected final void mFQCN_LETTERORDIGIT(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = FQCN_LETTERORDIGIT;
		int _saveIndex;
		
		switch ( LA(1)) {
		case '$':
		{
			match('\u0024');
			break;
		}
		case '_':
		{
			match('\u005f');
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
			matchRange('\u0061','\u007a');
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
		case '0':  case '1':  case '2':  case '3':
		case '4':  case '5':  case '6':  case '7':
		case '8':  case '9':
		{
			matchRange('\u0030','\u0039');
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
	
	public final void mUSAGE(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = USAGE;
		int _saveIndex;
		
		{
		match("usage");
		{
		if ((_tokenSet_2.member(LA(1)))) {
			mWHSP(false);
		}
		else {
		}
		
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mUSER_APPLICATIONS(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = USER_APPLICATIONS;
		int _saveIndex;
		
		{
		match("userapplications");
		{
		if ((_tokenSet_2.member(LA(1)))) {
			mWHSP(false);
		}
		else {
		}
		
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mDIRECTORY_OPERATION(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = DIRECTORY_OPERATION;
		int _saveIndex;
		
		{
		match("directoryoperation");
		{
		if ((_tokenSet_2.member(LA(1)))) {
			mWHSP(false);
		}
		else {
		}
		
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mDISTRIBUTED_OPERATION(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = DISTRIBUTED_OPERATION;
		int _saveIndex;
		
		{
		match("distributedoperation");
		{
		if ((_tokenSet_2.member(LA(1)))) {
			mWHSP(false);
		}
		else {
		}
		
		}
		}
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mDSA_OPERATION(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = DSA_OPERATION;
		int _saveIndex;
		
		{
		match("dsaoperation");
		{
		if ((_tokenSet_2.member(LA(1)))) {
			mWHSP(false);
		}
		else {
		}
		
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
		data[0]=1152747270668559872L;
		data[1]=576460745860972544L;
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
		long[] data = new long[2048];
		data[0]=-1025L;
		for (int i = 1; i<=1022; i++) { data[i]=-1L; }
		data[1023]=9223372036854775807L;
		return data;
	}
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = new long[1025];
		data[0]=38654715392L;
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = new long[1025];
		data[0]=1152746239876408832L;
		data[1]=576460745860972544L;
		return data;
	}
	public static final BitSet _tokenSet_3 = new BitSet(mk_tokenSet_3());
	private static final long[] mk_tokenSet_4() {
		long[] data = new long[3988];
		data[0]=68719476736L;
		data[1]=576460745860972544L;
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
	public static final BitSet _tokenSet_4 = new BitSet(mk_tokenSet_4());
	private static final long[] mk_tokenSet_5() {
		long[] data = new long[2048];
		data[0]=-549755813889L;
		for (int i = 1; i<=1022; i++) { data[i]=-1L; }
		data[1023]=9223372036854775807L;
		return data;
	}
	public static final BitSet _tokenSet_5 = new BitSet(mk_tokenSet_5());
	private static final long[] mk_tokenSet_6() {
		long[] data = new long[3988];
		data[0]=287948969894477824L;
		data[1]=576460745860972544L;
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
	public static final BitSet _tokenSet_6 = new BitSet(mk_tokenSet_6());
	
	}
