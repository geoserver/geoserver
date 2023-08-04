// $ANTLR 2.7.4: "distinguishedName.g" -> "AntlrDnParser.java"$

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
 * An antlr generated DN parser.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AntlrDnParser extends antlr.LLkParser       implements AntlrDnTokenTypes
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
    static class UpAndNormValue
    {
        Object value = "";
        String rawValue = "";
    }

protected AntlrDnParser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public AntlrDnParser(TokenBuffer tokenBuf) {
  this(tokenBuf,3);
}

protected AntlrDnParser(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public AntlrDnParser(TokenStream lexer) {
  this(lexer,3);
}

public AntlrDnParser(ParserSharedInputState state) {
  super(state,3);
  tokenNames = _tokenNames;
}

/**
     * Parses an DN string.
     *
     * RFC 4514, Section 3
     * distinguishedName = [ relativeDistinguishedName
     *     *( COMMA relativeDistinguishedName ) ]
     *
     * RFC 2253, Section 3
     * distinguishedName = [name] 
     * name       = name-component *("," name-component)
     *
     * RFC 1779, Section 2.3
     * <name> ::= <name-component> ( <spaced-separator> )
     *        | <name-component> <spaced-separator> <name>
     * <spaced-separator> ::= <optional-space>
     *             <separator>
     *             <optional-space>
     * <separator> ::=  "," | ";"
     * <optional-space> ::= ( <CR> ) *( " " )
     *
     */
	public final void distinguishedName(
		DN dn
	) throws RecognitionException, TokenStreamException {
		
		
		matchedProduction( "distinguishedName()" );
		RDN rdn = null;
		
		
		{
		switch ( LA(1)) {
		case SPACE:
		case NUMERICOID:
		case ALPHA:
		{
			rdn=relativeDistinguishedName(new RDN());
			dn.add( rdn ); rdn=null;
			{
			_loop1922:
			do {
				if ((LA(1)==COMMA||LA(1)==SEMI)) {
					{
					switch ( LA(1)) {
					case COMMA:
					{
						match(COMMA);
						break;
					}
					case SEMI:
					{
						match(SEMI);
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					rdn=relativeDistinguishedName(new RDN());
					dn.add( rdn ); rdn=null;
				}
				else {
					break _loop1922;
				}
				
			} while (true);
			}
			match(Token.EOF_TYPE);
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
	
/**
     * Parses an RDN string.
     *
     * RFC 4514, Section 3
     * relativeDistinguishedName = attributeTypeAndValue
     *     *( PLUS attributeTypeAndValue )
     *
     * RFC 2253, Section 3
     * name-component = attributeTypeAndValue *("+" attributeTypeAndValue)
     *
     * RFC 1779, Section 2.3
     * <name-component> ::= <attribute>
     *     | <attribute> <optional-space> "+"
     *       <optional-space> <name-component>
     *
     */
	public final RDN  relativeDistinguishedName(
		RDN initialRdn
	) throws RecognitionException, TokenStreamException {
		RDN rdn;
		
		
		matchedProduction( "relativeDistinguishedName()" );
		rdn = initialRdn;
		String tmp;
		String upName = "";
		
		
		{
		tmp=attributeTypeAndValue(rdn);
		
		upName += tmp;
		
		{
		_loop1931:
		do {
			if ((LA(1)==PLUS)) {
				match(PLUS);
				upName += "+";
				tmp=attributeTypeAndValue(rdn);
				
				upName += tmp;
				
			}
			else {
				break _loop1931;
			}
			
		} while (true);
		}
		}
		
		rdn.normalize();
		rdn.setUpName( upName );
		
		return rdn;
	}
	
/**
     * Parses an DN string.
     *
     * RFC 4514, Section 3
     * distinguishedName = [ relativeDistinguishedName
     *     *( COMMA relativeDistinguishedName ) ]
     *
     * RFC 2253, Section 3
     * distinguishedName = [name] 
     * name       = name-component *("," name-component)
     *
     * RFC 1779, Section 2.3
     * <name> ::= <name-component> ( <spaced-separator> )
     *        | <name-component> <spaced-separator> <name>
     * <spaced-separator> ::= <optional-space>
     *             <separator>
     *             <optional-space>
     * <separator> ::=  "," | ";"
     * <optional-space> ::= ( <CR> ) *( " " )
     *
     */
	public final void relativeDistinguishedNames(
		List<RDN> rdns
	) throws RecognitionException, TokenStreamException {
		
		
		matchedProduction( "relativeDistinguishedNames()" );
		RDN rdn = null;
		
		
		{
		switch ( LA(1)) {
		case SPACE:
		case NUMERICOID:
		case ALPHA:
		{
			rdn=relativeDistinguishedName(new RDN());
			rdns.add( rdn );
			{
			_loop1927:
			do {
				if ((LA(1)==COMMA||LA(1)==SEMI)) {
					{
					switch ( LA(1)) {
					case COMMA:
					{
						match(COMMA);
						break;
					}
					case SEMI:
					{
						match(SEMI);
						break;
					}
					default:
					{
						throw new NoViableAltException(LT(1), getFilename());
					}
					}
					}
					rdn=relativeDistinguishedName(new RDN());
					rdns.add( rdn );
				}
				else {
					break _loop1927;
				}
				
			} while (true);
			}
			match(Token.EOF_TYPE);
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
	
/**
     * RFC 4514, Section 3
     * attributeTypeAndValue = attributeType EQUALS attributeValue
     *
     * RFC 2253, Section 3
     * attributeTypeAndValue = attributeType "=" attributeValue
     *
     */
	public final String  attributeTypeAndValue(
		RDN rdn
	) throws RecognitionException, TokenStreamException {
		String upName = "";
		
		
		matchedProduction( "attributeTypeAndValue()" );
		String type = null;
		UpAndNormValue value = new UpAndNormValue();
		
		
		{
		{
		_loop1935:
		do {
			if ((LA(1)==SPACE)) {
				match(SPACE);
				upName += " ";
			}
			else {
				break _loop1935;
			}
			
		} while (true);
		}
		type=attributeType();
		upName += type;
		{
		_loop1937:
		do {
			if ((LA(1)==SPACE)) {
				match(SPACE);
				upName += " ";
			}
			else {
				break _loop1937;
			}
			
		} while (true);
		}
		match(EQUALS);
		upName += "=";
		{
		_loop1939:
		do {
			if ((LA(1)==SPACE)) {
				match(SPACE);
				upName += " ";
			}
			else {
				break _loop1939;
			}
			
		} while (true);
		}
		attributeValue(value);
		
		try
		{
		upName += value.rawValue;
		AVA ava = null;
		
		if ( value.value instanceof String )
		{
		ava = new AVA(
		type,
		type,
		new StringValue( (String)value.value ), 
		new StringValue( (String)value.value ),
		upName
		);
		}
		else
		{
		ava = new AVA(
		type,
		type,
		new BinaryValue( (byte[])value.value ), 
		new BinaryValue( (byte[])value.value ),
		upName
		);
		}
		
		rdn.addAttributeTypeAndValue( ava );
		}
		catch ( LdapInvalidDnException e )
		{
		throw new SemanticException( e.getMessage() );
		} 
		
		}
		return upName;
	}
	
/**
     * RFC 4514 Section 3
     *
     * attributeType = descr / numericoid
     *
     */
	public final String  attributeType() throws RecognitionException, TokenStreamException {
		String attributeType;
		
		
		matchedProduction( "attributeType()" );
		
		
		{
		switch ( LA(1)) {
		case ALPHA:
		{
			attributeType=descr();
			break;
		}
		case NUMERICOID:
		{
			attributeType=numericoid();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		return attributeType;
	}
	
/**
     * RFC 4514, Section 3
     * attributeValue = string / hexstring
     *
     * RFC 2253, Section 3
     * attributeValue = string
     * string     = *( stringchar / pair )
     *              / "#" hexstring
     *              / QUOTATION *( quotechar / pair ) QUOTATION ; only from v2
     * 
     */
	public final void attributeValue(
		UpAndNormValue value
	) throws RecognitionException, TokenStreamException {
		
		
		matchedProduction( "attributeValue()" );
		
		
		{
		switch ( LA(1)) {
		case DQUOTE:
		{
			{
			quotestring(value);
			{
			_loop1950:
			do {
				if ((LA(1)==SPACE)) {
					match(SPACE);
					value.rawValue += " ";
				}
				else {
					break _loop1950;
				}
				
			} while (true);
			}
			}
			break;
		}
		case EQUALS:
		case HYPHEN:
		case NUMERICOID:
		case DIGIT:
		case ALPHA:
		case HEXPAIR:
		case ESC:
		case ESCESC:
		case ESCSHARP:
		case UTFMB:
		case LUTF1_REST:
		{
			string(value);
			break;
		}
		case HEXVALUE:
		{
			{
			hexstring(value);
			{
			_loop1953:
			do {
				if ((LA(1)==SPACE)) {
					match(SPACE);
					value.rawValue += " ";
				}
				else {
					break _loop1953;
				}
				
			} while (true);
			}
			}
			break;
		}
		case EOF:
		case COMMA:
		case PLUS:
		case SEMI:
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
	
/**
     * RFC 4512 Section 1.4
     *
     * descr = keystring
     * keystring = leadkeychar *keychar
     * leadkeychar = ALPHA
     * keychar = ALPHA / DIGIT / HYPHEN
     *
     */
	public final String  descr() throws RecognitionException, TokenStreamException {
		String descr;
		
		Token  leadkeychar = null;
		Token  alpha = null;
		Token  digit = null;
		Token  hyphen = null;
		
		matchedProduction( "descr()" );
		
		
		leadkeychar = LT(1);
		match(ALPHA);
		descr = leadkeychar.getText();
		{
		_loop1944:
		do {
			switch ( LA(1)) {
			case ALPHA:
			{
				alpha = LT(1);
				match(ALPHA);
				descr += alpha.getText();
				break;
			}
			case DIGIT:
			{
				digit = LT(1);
				match(DIGIT);
				descr += digit.getText();
				break;
			}
			case HYPHEN:
			{
				hyphen = LT(1);
				match(HYPHEN);
				descr += hyphen.getText();
				break;
			}
			default:
			{
				break _loop1944;
			}
			}
		} while (true);
		}
		return descr;
	}
	
/**
     * RFC 4512 Section 1.4
     *
     * numericoid = number 1*( DOT number )
     * number  = DIGIT / ( LDIGIT 1*DIGIT )
     * DIGIT   = %x30 / LDIGIT       ; "0"-"9"
     * LDIGIT  = %x31-39             ; "1"-"9"
     *
     */
	public final String  numericoid() throws RecognitionException, TokenStreamException {
		String numericoid = "";
		
		Token  noid = null;
		
		matchedProduction( "numericoid()" );
		
		
		noid = LT(1);
		match(NUMERICOID);
		numericoid += noid.getText();
		return numericoid;
	}
	
/**
     * RFC 2253, Section 3
     *              / QUOTATION *( quotechar / pair ) QUOTATION ; only from v2
     * quotechar     = <any character except "\" or QUOTATION >
     *
     */
	public final void quotestring(
		UpAndNormValue value
	) throws RecognitionException, TokenStreamException {
		
		Token  dq1 = null;
		Token  s = null;
		Token  dq2 = null;
		
		matchedProduction( "quotestring()" );
		org.apache.directory.shared.ldap.util.ByteBuffer bb = new org.apache.directory.shared.ldap.util.ByteBuffer();
		byte[] bytes;
		
		
		{
		dq1 = LT(1);
		match(DQUOTE);
		value.rawValue += dq1.getText();
		{
		_loop1959:
		do {
			switch ( LA(1)) {
			case COMMA:
			case EQUALS:
			case PLUS:
			case HYPHEN:
			case SEMI:
			case LANGLE:
			case RANGLE:
			case SPACE:
			case NUMERICOID_OR_ALPHA_OR_DIGIT:
			case NUMERICOID:
			case DOT:
			case NUMBER:
			case LDIGIT:
			case DIGIT:
			case ALPHA:
			case HEXPAIR_OR_ESCESC_ESCSHARP_OR_ESC:
			case HEX:
			case HEXVALUE_OR_SHARP:
			case HEXVALUE:
			case SHARP:
			case UTFMB:
			case LUTF1_REST:
			{
				{
				{
				s = LT(1);
				match(_tokenSet_0);
				}
				
				value.rawValue += s.getText();
				bb.append( StringTools.getBytesUtf8( s.getText() ) ); 
				
				}
				break;
			}
			case HEXPAIR:
			case ESC:
			case ESCESC:
			case ESCSHARP:
			{
				bytes=pair(value);
				bb.append( bytes );
				break;
			}
			default:
			{
				break _loop1959;
			}
			}
		} while (true);
		}
		dq2 = LT(1);
		match(DQUOTE);
		value.rawValue += dq2.getText();
		}
		
		String string = StringTools.utf8ToString( bb.copyOfUsedBytes() );
		value.value = string;
		
	}
	
/**
     * RFC 4514 Section 3
     *
     * ; The following characters are to be escaped when they appear
     * ; in the value to be encoded: ESC, one of <escaped>, leading
     * ; SHARP or SPACE, trailing SPACE, and NULL.
     * string =   [ ( leadchar / pair ) [ *( stringchar / pair )
     *    ( trailchar / pair ) ] ]
     *
     */
	public final void string(
		UpAndNormValue value
	) throws RecognitionException, TokenStreamException {
		
		
		matchedProduction( "string()" );
		org.apache.directory.shared.ldap.util.ByteBuffer bb = new org.apache.directory.shared.ldap.util.ByteBuffer();
		String tmp;
		byte[] bytes;
		
		
		{
		{
		switch ( LA(1)) {
		case EQUALS:
		case HYPHEN:
		case NUMERICOID:
		case DIGIT:
		case ALPHA:
		case LUTF1_REST:
		{
			tmp=lutf1();
			
			value.rawValue += tmp;
			bb.append( StringTools.getBytesUtf8( tmp ) ); 
			
			break;
		}
		case UTFMB:
		{
			tmp=utfmb();
			
			value.rawValue += tmp;
			bb.append( StringTools.getBytesUtf8( tmp ) );
			
			break;
		}
		case HEXPAIR:
		case ESC:
		case ESCESC:
		case ESCSHARP:
		{
			bytes=pair(value);
			bb.append( bytes );
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		{
		_loop1965:
		do {
			switch ( LA(1)) {
			case EQUALS:
			case HYPHEN:
			case SPACE:
			case NUMERICOID:
			case DIGIT:
			case ALPHA:
			case SHARP:
			case LUTF1_REST:
			{
				tmp=sutf1();
				
				value.rawValue += tmp;
				bb.append( StringTools.getBytesUtf8( tmp ) ); 
				
				break;
			}
			case UTFMB:
			{
				tmp=utfmb();
				
				value.rawValue += tmp;
				bb.append( StringTools.getBytesUtf8( tmp ) ); 
				
				break;
			}
			case HEXPAIR:
			case ESC:
			case ESCESC:
			case ESCSHARP:
			{
				bytes=pair(value);
				bb.append( bytes );
				break;
			}
			default:
			{
				break _loop1965;
			}
			}
		} while (true);
		}
		}
		
		String string = StringTools.utf8ToString( bb.copyOfUsedBytes() );
		
		// trim trailing space characters manually
		// don't know how to tell antlr that the last char mustn't be a space.
		int rawIndex = value.rawValue.length();
		while ( string.length() > 0 && rawIndex > 1 
		&& value.rawValue.charAt( rawIndex - 1 ) == ' ' 
		&& value.rawValue.charAt( rawIndex - 2 ) != '\\' )
		{
		string = string.substring( 0, string.length() - 1 );
		rawIndex--;
		}
		
		value.value = string;
		
	}
	
/**
     * RFC 4514 Section 3
     *
     * hexstring = SHARP 1*hexpair
     *
     * If in <hexstring> form, a BER representation can be obtained from
     * converting each <hexpair> of the <hexstring> to the octet indicated
     * by the <hexpair>.
     *
     */
	public final void hexstring(
		UpAndNormValue value
	) throws RecognitionException, TokenStreamException {
		
		Token  hexValue = null;
		
		matchedProduction( "hexstring()" );
		
		
		hexValue = LT(1);
		match(HEXVALUE);
		
		// convert to byte[]
		value.rawValue = "#" + hexValue.getText();
		value.value = StringTools.toByteArray( hexValue.getText() ); 
		
	}
	
/**
     * RFC 4514, Section 3
     * pair = ESC ( ESC / special / hexpair )
     * special = escaped / SPACE / SHARP / EQUALS
     * escaped = DQUOTE / PLUS / COMMA / SEMI / LANGLE / RANGLE
     * hexpair = HEX HEX
     *
     * If in <string> form, a LDAP string representation asserted value can
     * be obtained by replacing (left to right, non-recursively) each <pair>
     * appearing in the <string> as follows:
     *   replace <ESC><ESC> with <ESC>;
     *   replace <ESC><special> with <special>;
     *   replace <ESC><hexpair> with the octet indicated by the <hexpair>.
     * 
     * RFC 2253, Section 3
     * pair       = "\" ( special / "\" / QUOTATION / hexpair )
     * special    = "," / "=" / "+" / "<" /  ">" / "#" / ";"
     * 
     * RFC 1779, Section 2.3
     * <pair> ::= "\" ( <special> | "\" | '"')
     * <special> ::= "," | "=" | <CR> | "+" | "<" |  ">"
     *           | "#" | ";"
     * 
     */
	public final byte[]  pair(
		UpAndNormValue value
	) throws RecognitionException, TokenStreamException {
		byte[] pair;
		
		Token  hexpair = null;
		
		matchedProduction( "pair()" );
		String tmp;
		
		
		switch ( LA(1)) {
		case ESCESC:
		{
			{
			match(ESCESC);
			
			value.rawValue += "\\\\";
			pair = StringTools.getBytesUtf8( "\\" );
			
			}
			break;
		}
		case ESCSHARP:
		{
			{
			match(ESCSHARP);
			
			value.rawValue += "\\#";
			pair = StringTools.getBytesUtf8( "#" );
			
			}
			break;
		}
		case ESC:
		{
			{
			match(ESC);
			tmp=special();
			
			value.rawValue += "\\" + tmp;
			pair = StringTools.getBytesUtf8( tmp ); 
			
			}
			break;
		}
		case HEXPAIR:
		{
			{
			hexpair = LT(1);
			match(HEXPAIR);
			
			value.rawValue += "\\" + hexpair.getText();
			pair = StringTools.toByteArray( hexpair.getText() ); 
			
			}
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return pair;
	}
	
/**
 * RFC 4514, Section 3:
 * LUTF1 = %x01-1F / %x21 / %x24-2A / %x2D-3A /
 *    %x3D / %x3F-5B / %x5D-7F
 *
 * The rule LUTF1_REST doesn't contain the following charcters,
 * so we must check them additionally
 *   EQUALS (0x3D) 
 *   HYPHEN (0x2D)  
 *   DIGIT (0x30-0x39)
 *   ALPHA (0x41-0x5A and 0x61-0x7A)
 */
	public final String  lutf1() throws RecognitionException, TokenStreamException {
		String lutf1="";
		
		Token  rest = null;
		Token  equals = null;
		Token  hyphen = null;
		Token  digit = null;
		Token  alpha = null;
		Token  numericoid = null;
		
		matchedProduction( "lutf1()" );
		
		
		switch ( LA(1)) {
		case LUTF1_REST:
		{
			rest = LT(1);
			match(LUTF1_REST);
			lutf1 = rest.getText();
			break;
		}
		case EQUALS:
		{
			equals = LT(1);
			match(EQUALS);
			lutf1 = equals.getText();
			break;
		}
		case HYPHEN:
		{
			hyphen = LT(1);
			match(HYPHEN);
			lutf1 = hyphen.getText();
			break;
		}
		case DIGIT:
		{
			digit = LT(1);
			match(DIGIT);
			lutf1 = digit.getText();
			break;
		}
		case ALPHA:
		{
			alpha = LT(1);
			match(ALPHA);
			lutf1 = alpha.getText();
			break;
		}
		case NUMERICOID:
		{
			numericoid = LT(1);
			match(NUMERICOID);
			lutf1 = numericoid.getText();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return lutf1;
	}
	
	public final String  utfmb() throws RecognitionException, TokenStreamException {
		String utfmb;
		
		Token  s = null;
		
		matchedProduction( "utfmb()" );
		
		
		s = LT(1);
		match(UTFMB);
		utfmb = s.getText();
		return utfmb;
	}
	
/**
 * RFC 4514, Section 3:
 * SUTF1 = %x01-21 / %x23-2A / %x2D-3A /
 *    %x3D / %x3F-5B / %x5D-7F
 *
 * The rule LUTF1_REST doesn't contain the following charcters,
 * so we must check them additionally
 *   EQUALS (0x3D) 
 *   HYPHEN (0x2D)  
 *   DIGIT (0x30-0x39)
 *   ALPHA (0x41-0x5A and 0x61-0x7A)
 *   SHARP
 *   SPACE
 */
	public final String  sutf1() throws RecognitionException, TokenStreamException {
		String sutf1="";
		
		Token  rest = null;
		Token  equals = null;
		Token  hyphen = null;
		Token  digit = null;
		Token  alpha = null;
		Token  sharp = null;
		Token  space = null;
		Token  numericoid = null;
		
		matchedProduction( "sutf1()" );
		
		
		switch ( LA(1)) {
		case LUTF1_REST:
		{
			rest = LT(1);
			match(LUTF1_REST);
			sutf1 = rest.getText();
			break;
		}
		case EQUALS:
		{
			equals = LT(1);
			match(EQUALS);
			sutf1 = equals.getText();
			break;
		}
		case HYPHEN:
		{
			hyphen = LT(1);
			match(HYPHEN);
			sutf1 = hyphen.getText();
			break;
		}
		case DIGIT:
		{
			digit = LT(1);
			match(DIGIT);
			sutf1 = digit.getText();
			break;
		}
		case ALPHA:
		{
			alpha = LT(1);
			match(ALPHA);
			sutf1 = alpha.getText();
			break;
		}
		case SHARP:
		{
			sharp = LT(1);
			match(SHARP);
			sutf1 = sharp.getText();
			break;
		}
		case SPACE:
		{
			space = LT(1);
			match(SPACE);
			sutf1 = space.getText();
			break;
		}
		case NUMERICOID:
		{
			numericoid = LT(1);
			match(NUMERICOID);
			sutf1 = numericoid.getText();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		return sutf1;
	}
	
/**
     * RFC 4514 Section 3
     * 
     * special = escaped / SPACE / SHARP / EQUALS
     * escaped = DQUOTE / PLUS / COMMA / SEMI / LANGLE / RANGLE
     *
     */
	public final String  special() throws RecognitionException, TokenStreamException {
		String special;
		
		Token  dquote = null;
		Token  plus = null;
		Token  comma = null;
		Token  semi = null;
		Token  langle = null;
		Token  rangle = null;
		Token  space = null;
		Token  sharp = null;
		Token  equals = null;
		
		matchedProduction( "special()" );
		
		
		{
		switch ( LA(1)) {
		case DQUOTE:
		{
			dquote = LT(1);
			match(DQUOTE);
			special = dquote.getText();
			break;
		}
		case PLUS:
		{
			plus = LT(1);
			match(PLUS);
			special = plus.getText();
			break;
		}
		case COMMA:
		{
			comma = LT(1);
			match(COMMA);
			special = comma.getText();
			break;
		}
		case SEMI:
		{
			semi = LT(1);
			match(SEMI);
			special = semi.getText();
			break;
		}
		case LANGLE:
		{
			langle = LT(1);
			match(LANGLE);
			special = langle.getText();
			break;
		}
		case RANGLE:
		{
			rangle = LT(1);
			match(RANGLE);
			special = rangle.getText();
			break;
		}
		case SPACE:
		{
			space = LT(1);
			match(SPACE);
			special = space.getText();
			break;
		}
		case SHARP:
		{
			sharp = LT(1);
			match(SHARP);
			special = sharp.getText();
			break;
		}
		case EQUALS:
		{
			equals = LT(1);
			match(EQUALS);
			special = equals.getText();
			break;
		}
		default:
		{
			throw new NoViableAltException(LT(1), getFilename());
		}
		}
		}
		return special;
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"COMMA",
		"EQUALS",
		"PLUS",
		"HYPHEN",
		"DQUOTE",
		"SEMI",
		"LANGLE",
		"RANGLE",
		"SPACE",
		"NUMERICOID_OR_ALPHA_OR_DIGIT",
		"NUMERICOID",
		"DOT",
		"NUMBER",
		"LDIGIT",
		"DIGIT",
		"ALPHA",
		"HEXPAIR_OR_ESCESC_ESCSHARP_OR_ESC",
		"HEXPAIR",
		"ESC",
		"ESCESC",
		"ESCSHARP",
		"HEX",
		"HEXVALUE_OR_SHARP",
		"HEXVALUE",
		"SHARP",
		"UTFMB",
		"LUTF1_REST"
	};
	
	private static final long[] mk_tokenSet_0() {
		long[] data = { 2116026096L, 0L, 0L, 0L};
		return data;
	}
	public static final BitSet _tokenSet_0 = new BitSet(mk_tokenSet_0());
	
	}
