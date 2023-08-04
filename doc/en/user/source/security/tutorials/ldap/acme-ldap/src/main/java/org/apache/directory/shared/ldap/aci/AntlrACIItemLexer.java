// $ANTLR 2.7.4: "ACIItem.g" -> "AntlrACIItemLexer.java"$

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
  * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
  * @version $Rev$
  */
public class AntlrACIItemLexer extends antlr.CharScanner implements AntlrACIItemParserTokenTypes, TokenStream
 {

    private static final Logger log = LoggerFactory.getLogger( AntlrACIItemLexer.class );
public AntlrACIItemLexer(InputStream in) {
	this(new ByteBuffer(in));
}
public AntlrACIItemLexer(Reader in) {
	this(new CharBuffer(in));
}
public AntlrACIItemLexer(InputBuffer ib) {
	this(new LexerSharedInputState(ib));
}
public AntlrACIItemLexer(LexerSharedInputState state) {
	super(state);
	caseSensitiveLiterals = true;
	setCaseSensitive(true);
	literals = new Hashtable();
	literals.put(new ANTLRHashString("type", this), new Integer(30));
	literals.put(new ANTLRHashString("strong", this), new Integer(17));
	literals.put(new ANTLRHashString("name", this), new Integer(68));
	literals.put(new ANTLRHashString("specificExclusions", this), new Integer(73));
	literals.put(new ANTLRHashString("denyBrowse", this), new Integer(47));
	literals.put(new ANTLRHashString("denyModify", this), new Integer(53));
	literals.put(new ANTLRHashString("denyCompare", this), new Integer(59));
	literals.put(new ANTLRHashString("classes", this), new Integer(35));
	literals.put(new ANTLRHashString("denyAdd", this), new Integer(39));
	literals.put(new ANTLRHashString("maximum", this), new Integer(77));
	literals.put(new ANTLRHashString("grantInvoke", this), new Integer(62));
	literals.put(new ANTLRHashString("denyDiscloseOnError", this), new Integer(41));
	literals.put(new ANTLRHashString("rangeOfValues", this), new Integer(90));
	literals.put(new ANTLRHashString("maxCount", this), new Integer(31));
	literals.put(new ANTLRHashString("userClasses", this), new Integer(64));
	literals.put(new ANTLRHashString("denyInvoke", this), new Integer(63));
	literals.put(new ANTLRHashString("and", this), new Integer(81));
	literals.put(new ANTLRHashString("denyRead", this), new Integer(43));
	literals.put(new ANTLRHashString("not", this), new Integer(83));
	literals.put(new ANTLRHashString("grantReturnDN", this), new Integer(56));
	literals.put(new ANTLRHashString("maxImmSub", this), new Integer(32));
	literals.put(new ANTLRHashString("grantCompare", this), new Integer(58));
	literals.put(new ANTLRHashString("parentOfEntry", this), new Integer(67));
	literals.put(new ANTLRHashString("precedence", this), new Integer(12));
	literals.put(new ANTLRHashString("base", this), new Integer(72));
	literals.put(new ANTLRHashString("minimum", this), new Integer(76));
	literals.put(new ANTLRHashString("grantsAndDenials", this), new Integer(37));
	literals.put(new ANTLRHashString("itemOrUserFirst", this), new Integer(18));
	literals.put(new ANTLRHashString("entry", this), new Integer(23));
	literals.put(new ANTLRHashString("FALSE", this), new Integer(84));
	literals.put(new ANTLRHashString("selfValue", this), new Integer(28));
	literals.put(new ANTLRHashString("specificationFilter", this), new Integer(91));
	literals.put(new ANTLRHashString("itemPermissions", this), new Integer(36));
	literals.put(new ANTLRHashString("grantRemove", this), new Integer(44));
	literals.put(new ANTLRHashString("or", this), new Integer(82));
	literals.put(new ANTLRHashString("allAttributeValues", this), new Integer(26));
	literals.put(new ANTLRHashString("none", this), new Integer(15));
	literals.put(new ANTLRHashString("attributeType", this), new Integer(25));
	literals.put(new ANTLRHashString("chopAfter", this), new Integer(75));
	literals.put(new ANTLRHashString("subtree", this), new Integer(70));
	literals.put(new ANTLRHashString("denyRemove", this), new Integer(45));
	literals.put(new ANTLRHashString("userFirst", this), new Integer(21));
	literals.put(new ANTLRHashString("grantAdd", this), new Integer(38));
	literals.put(new ANTLRHashString("grantFilterMatch", this), new Integer(60));
	literals.put(new ANTLRHashString("allUserAttributeTypesAndValues", this), new Integer(27));
	literals.put(new ANTLRHashString("maxValueCount", this), new Integer(29));
	literals.put(new ANTLRHashString("grantExport", this), new Integer(48));
	literals.put(new ANTLRHashString("basicLevels", this), new Integer(87));
	literals.put(new ANTLRHashString("denyFilterMatch", this), new Integer(61));
	literals.put(new ANTLRHashString("protectedItems", this), new Integer(22));
	literals.put(new ANTLRHashString("identificationTag", this), new Integer(10));
	literals.put(new ANTLRHashString("grantRename", this), new Integer(54));
	literals.put(new ANTLRHashString("grantImport", this), new Integer(50));
	literals.put(new ANTLRHashString("localQualifier", this), new Integer(88));
	literals.put(new ANTLRHashString("userPermissions", this), new Integer(71));
	literals.put(new ANTLRHashString("grantRead", this), new Integer(42));
	literals.put(new ANTLRHashString("denyExport", this), new Integer(49));
	literals.put(new ANTLRHashString("denyRename", this), new Integer(55));
	literals.put(new ANTLRHashString("itemFirst", this), new Integer(19));
	literals.put(new ANTLRHashString("denyImport", this), new Integer(51));
	literals.put(new ANTLRHashString("restrictedBy", this), new Integer(33));
	literals.put(new ANTLRHashString("chopBefore", this), new Integer(74));
	literals.put(new ANTLRHashString("signed", this), new Integer(89));
	literals.put(new ANTLRHashString("grantDiscloseOnError", this), new Integer(40));
	literals.put(new ANTLRHashString("level", this), new Integer(86));
	literals.put(new ANTLRHashString("allUserAttributeTypes", this), new Integer(24));
	literals.put(new ANTLRHashString("TRUE", this), new Integer(85));
	literals.put(new ANTLRHashString("authenticationLevel", this), new Integer(14));
	literals.put(new ANTLRHashString("denyReturnDN", this), new Integer(57));
	literals.put(new ANTLRHashString("grantBrowse", this), new Integer(46));
	literals.put(new ANTLRHashString("thisEntry", this), new Integer(66));
	literals.put(new ANTLRHashString("grantModify", this), new Integer(52));
	literals.put(new ANTLRHashString("allUsers", this), new Integer(65));
	literals.put(new ANTLRHashString("item", this), new Integer(80));
	literals.put(new ANTLRHashString("userGroup", this), new Integer(69));
	literals.put(new ANTLRHashString("simple", this), new Integer(16));
	literals.put(new ANTLRHashString("valuesIn", this), new Integer(34));
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
				case '\t':  case '\n':  case '\r':  case ' ':
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
				case '0':  case '1':  case '2':  case '3':
				case '4':  case '5':  case '6':  case '7':
				case '8':  case '9':
				{
					mINTEGER_OR_NUMERICOID(true);
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
	
	public final void mOPEN_CURLY(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = OPEN_CURLY;
		int _saveIndex;
		
		match('{');
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
		if ( _createToken && _token==null && _ttype!=Token.SKIP ) {
			_token = makeToken(_ttype);
			_token.setText(new String(text.getBuffer(), _begin, text.length()-_begin));
		}
		_returnToken = _token;
	}
	
	public final void mSP(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = SP;
		int _saveIndex;
		
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
		case '\n':
		{
			match('\n');
			if ( inputState.guessing==0 ) {
				newline();
			}
			break;
		}
		case '\r':
		{
			match('\r');
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
	
	public final void mCOLON(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = COLON;
		int _saveIndex;
		
		match(':');
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
	
	protected final void mINTEGER(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = INTEGER;
		int _saveIndex;
		
		if (((LA(1) >= '1' && LA(1) <= '9')) && ((LA(2) >= '0' && LA(2) <= '9'))) {
			{
			mLDIGIT(false);
			{
			int _cnt406=0;
			_loop406:
			do {
				if (((LA(1) >= '0' && LA(1) <= '9'))) {
					mDIGIT(false);
				}
				else {
					if ( _cnt406>=1 ) { break _loop406; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
				}
				
				_cnt406++;
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
	
	protected final void mHYPHEN(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
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
	
	protected final void mNUMERICOID(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = NUMERICOID;
		int _saveIndex;
		
		mINTEGER(false);
		{
		int _cnt410=0;
		_loop410:
		do {
			if ((LA(1)=='.')) {
				mDOT(false);
				mINTEGER(false);
			}
			else {
				if ( _cnt410>=1 ) { break _loop410; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
			}
			
			_cnt410++;
		} while (true);
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
	
	public final void mINTEGER_OR_NUMERICOID(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = INTEGER_OR_NUMERICOID;
		int _saveIndex;
		
		boolean synPredMatched414 = false;
		if ((((LA(1) >= '0' && LA(1) <= '9')) && (_tokenSet_0.member(LA(2))))) {
			int _m414 = mark();
			synPredMatched414 = true;
			inputState.guessing++;
			try {
				{
				mINTEGER(false);
				mDOT(false);
				}
			}
			catch (RecognitionException pe) {
				synPredMatched414 = false;
			}
			rewind(_m414);
			inputState.guessing--;
		}
		if ( synPredMatched414 ) {
			mNUMERICOID(false);
			if ( inputState.guessing==0 ) {
				
				_ttype =  NUMERICOID;
				
			}
		}
		else if (((LA(1) >= '0' && LA(1) <= '9')) && (true)) {
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
	
	public final void mSAFEUTF8STRING(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
		int _ttype; Token _token=null; int _begin=text.length();
		_ttype = SAFEUTF8STRING;
		int _saveIndex;
		
		_saveIndex=text.length();
		match('"');
		text.setLength(_saveIndex);
		{
		_loop417:
		do {
			if ((_tokenSet_1.member(LA(1)))) {
				mSAFEUTF8CHAR(false);
			}
			else {
				break _loop417;
			}
			
		} while (true);
		}
		_saveIndex=text.length();
		match('"');
		text.setLength(_saveIndex);
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
		
		boolean synPredMatched422 = false;
		if (((LA(1)=='a') && (LA(2)=='t'))) {
			int _m422 = mark();
			synPredMatched422 = true;
			inputState.guessing++;
			try {
				{
				match("attributeValue");
				{
				int _cnt421=0;
				_loop421:
				do {
					if ((_tokenSet_2.member(LA(1)))) {
						_saveIndex=text.length();
						mSP(false);
						text.setLength(_saveIndex);
					}
					else {
						if ( _cnt421>=1 ) { break _loop421; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
					}
					
					_cnt421++;
				} while (true);
				}
				match('{');
				}
			}
			catch (RecognitionException pe) {
				synPredMatched422 = false;
			}
			rewind(_m422);
			inputState.guessing--;
		}
		if ( synPredMatched422 ) {
			_saveIndex=text.length();
			match("attributeValue");
			text.setLength(_saveIndex);
			{
			int _cnt424=0;
			_loop424:
			do {
				if ((_tokenSet_2.member(LA(1)))) {
					_saveIndex=text.length();
					mSP(false);
					text.setLength(_saveIndex);
				}
				else {
					if ( _cnt424>=1 ) { break _loop424; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
				}
				
				_cnt424++;
			} while (true);
			}
			_saveIndex=text.length();
			match('{');
			text.setLength(_saveIndex);
			{
			_loop426:
			do {
				// nongreedy exit test
				if ((LA(1)=='}') && (true)) break _loop426;
				if ((_tokenSet_3.member(LA(1))) && (_tokenSet_3.member(LA(2)))) {
					matchNot(EOF_CHAR);
				}
				else {
					break _loop426;
				}
				
			} while (true);
			}
			_saveIndex=text.length();
			match('}');
			text.setLength(_saveIndex);
			if ( inputState.guessing==0 ) {
				_ttype =  ATTRIBUTE_VALUE_CANDIDATE;
			}
		}
		else {
			boolean synPredMatched430 = false;
			if (((LA(1)=='r') && (LA(2)=='a'))) {
				int _m430 = mark();
				synPredMatched430 = true;
				inputState.guessing++;
				try {
					{
					match("rangeOfValues");
					{
					int _cnt429=0;
					_loop429:
					do {
						if ((_tokenSet_2.member(LA(1)))) {
							_saveIndex=text.length();
							mSP(false);
							text.setLength(_saveIndex);
						}
						else {
							if ( _cnt429>=1 ) { break _loop429; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
						}
						
						_cnt429++;
					} while (true);
					}
					match('(');
					}
				}
				catch (RecognitionException pe) {
					synPredMatched430 = false;
				}
				rewind(_m430);
				inputState.guessing--;
			}
			if ( synPredMatched430 ) {
				_saveIndex=text.length();
				match("rangeOfValues");
				text.setLength(_saveIndex);
				{
				int _cnt432=0;
				_loop432:
				do {
					if ((_tokenSet_2.member(LA(1)))) {
						_saveIndex=text.length();
						mSP(false);
						text.setLength(_saveIndex);
					}
					else {
						if ( _cnt432>=1 ) { break _loop432; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
					}
					
					_cnt432++;
				} while (true);
				}
				mFILTER(false);
				if ( inputState.guessing==0 ) {
					_ttype =  RANGE_OF_VALUES_CANDIDATE;
				}
			}
			else if ((_tokenSet_4.member(LA(1))) && (true)) {
				mALPHA(false);
				{
				_loop434:
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
						mHYPHEN(false);
						break;
					}
					default:
					{
						break _loop434;
					}
					}
				} while (true);
				}
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
		
	protected final void mFILTER(boolean _createToken) throws RecognitionException, CharStreamException, TokenStreamException {
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
			_loop439:
			do {
				if ((_tokenSet_2.member(LA(1)))) {
					mSP(false);
				}
				else {
					break _loop439;
				}
				
			} while (true);
			}
			{
			int _cnt441=0;
			_loop441:
			do {
				if ((LA(1)=='(')) {
					mFILTER(false);
				}
				else {
					if ( _cnt441>=1 ) { break _loop441; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
				}
				
				_cnt441++;
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
			_loop444:
			do {
				if ((_tokenSet_2.member(LA(1)))) {
					mSP(false);
				}
				else {
					break _loop444;
				}
				
			} while (true);
			}
			{
			int _cnt446=0;
			_loop446:
			do {
				if ((LA(1)=='(')) {
					mFILTER(false);
				}
				else {
					if ( _cnt446>=1 ) { break _loop446; } else {throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());}
				}
				
				_cnt446++;
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
			_loop449:
			do {
				if ((_tokenSet_2.member(LA(1)))) {
					mSP(false);
				}
				else {
					break _loop449;
				}
				
			} while (true);
			}
			mFILTER(false);
			}
			break;
		}
		default:
			if ((_tokenSet_5.member(LA(1)))) {
				mFILTER_VALUE(false);
			}
		else {
			throw new NoViableAltForCharException((char)LA(1), getFilename(), getLine(), getColumn());
		}
		}
		}
		match(')');
		{
		_loop451:
		do {
			if ((_tokenSet_2.member(LA(1)))) {
				mSP(false);
			}
			else {
				break _loop451;
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
		match(_tokenSet_5);
		}
		{
		_loop457:
		do {
			if ((_tokenSet_6.member(LA(1)))) {
				{
				match(_tokenSet_6);
				}
			}
			else {
				break _loop457;
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
		long[] data = new long[1025];
		data[0]=288019269919178752L;
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	private static final long[] mk_tokenSet_1() {
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
	public static final BitSet _tokenSet_1 = new BitSet(mk_tokenSet_1());
	private static final long[] mk_tokenSet_2() {
		long[] data = new long[1025];
		data[0]=4294977024L;
		return data;
	}
	public static final BitSet _tokenSet_2 = new BitSet(mk_tokenSet_2());
	private static final long[] mk_tokenSet_3() {
		long[] data = new long[2048];
		data[0]=-2L;
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
	private static final long[] mk_tokenSet_4() {
		long[] data = new long[1025];
		data[1]=576460743847706622L;
		return data;
	}
	public static final BitSet _tokenSet_4 = new BitSet(mk_tokenSet_4());
	private static final long[] mk_tokenSet_5() {
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
	public static final BitSet _tokenSet_5 = new BitSet(mk_tokenSet_5());
	private static final long[] mk_tokenSet_6() {
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
	public static final BitSet _tokenSet_6 = new BitSet(mk_tokenSet_6());
	
	}
