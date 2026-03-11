// $ANTLR 2.7.4: "schema-qdstring.g" -> "AntlrSchemaQdstringParser.java"$

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

import java.util.ArrayList;
import java.util.List;

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
 * qdstring and qdstrings according to RFC4512.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AntlrSchemaQdstringParser extends antlr.LLkParser       implements AntlrSchemaQdstringTokenTypes
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

protected AntlrSchemaQdstringParser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public AntlrSchemaQdstringParser(TokenBuffer tokenBuf) {
  this(tokenBuf,3);
}

protected AntlrSchemaQdstringParser(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public AntlrSchemaQdstringParser(TokenStream lexer) {
  this(lexer,3);
}

public AntlrSchemaQdstringParser(ParserSharedInputState state) {
  super(state,3);
  tokenNames = _tokenNames;
}

/**
     * qdstrings = qdstring / ( LPAREN WSP qdstringlist WSP RPAREN )
     * qdstringlist = [ qdstring *( SP qdstring ) ]
     */
	public final List<String>  qdstrings() throws RecognitionException, TokenStreamException {
		List<String> qdstrings;
		
		Token  q = null;
		
		matchedProduction( "AntlrSchemaQdstringParser.qdstrings()" );
		qdstrings = new ArrayList<String>();
		String qdstring = null;
		
		
		{
		switch ( LA(1)) {
		case QDSTRING:
		{
			{
			q = LT(1);
			match(QDSTRING);
			
			qdstring = q.getText(); 
			if(qdstring.startsWith("'")) {
			qdstring = qdstring.substring(1, qdstring.length());
			}
			if(qdstring.endsWith("'")) {
			qdstring = qdstring.substring(0, qdstring.length()-1);
			}
			qdstring = qdstring.replaceAll("\\\\5C", "\\\\");
			qdstring = qdstring.replaceAll("\\\\5c", "\\\\");
			qdstring = qdstring.replaceAll("\\\\27", "'");
			qdstrings.add(qdstring);
			
			}
			break;
		}
		case LPAR:
		{
			{
			match(LPAR);
			qdstring=qdstring();
			qdstrings.add(qdstring);
			{
			_loop963:
			do {
				if ((LA(1)==QDSTRING)) {
					qdstring=qdstring();
					qdstrings.add(qdstring);
				}
				else {
					break _loop963;
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
		return qdstrings;
	}
	
/**
     * qdstring = SQUOTE dstring SQUOTE
     * dstring = 1*( QS / QQ / QUTF8 )   ; escaped UTF-8 string
     *
     * QQ =  ESC %x32 %x37 ; "\27"
     * QS =  ESC %x35 ( %x43 / %x63 ) ; "\5C" / "\5c"
     *
     * ; Any UTF-8 encoded Unicode character
     * ; except %x27 ("\'") and %x5C ("\")
     * QUTF8    = QUTF1 / UTFMB
     *
     * ; Any ASCII character except %x27 ("\'") and %x5C ("\")
     * QUTF1    = %x00-26 / %x28-5B / %x5D-7F
     */
	public final String  qdstring() throws RecognitionException, TokenStreamException {
		String qdstring=null;
		
		Token  q = null;
		
		matchedProduction( "AntlrSchemaQdstringParser.qdstring()" );
		
		
		{
		q = LT(1);
		match(QDSTRING);
		
		qdstring = q.getText(); 
		if(qdstring.startsWith("'")) {
		qdstring = qdstring.substring(1, qdstring.length());
		}
		if(qdstring.endsWith("'")) {
		qdstring = qdstring.substring(0, qdstring.length()-1);
		}
		qdstring = qdstring.replaceAll("\\\\5C", "\\\\");
		qdstring = qdstring.replaceAll("\\\\5c", "\\\\");
		qdstring = qdstring.replaceAll("\\\\27", "'");
		
		}
		return qdstring;
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"WHSP",
		"LPAR",
		"RPAR",
		"QUOTE",
		"QDSTRING"
	};
	
	
	}
