// $ANTLR 2.7.4: "schema-extension.g" -> "AntlrSchemaExtensionParser.java"$

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
import java.util.List;


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
 * extensions according to RFC4512.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AntlrSchemaExtensionParser extends antlr.LLkParser       implements AntlrSchemaExtensionTokenTypes
 {

protected AntlrSchemaExtensionParser(TokenBuffer tokenBuf, int k) {
  super(tokenBuf,k);
  tokenNames = _tokenNames;
}

public AntlrSchemaExtensionParser(TokenBuffer tokenBuf) {
  this(tokenBuf,3);
}

protected AntlrSchemaExtensionParser(TokenStream lexer, int k) {
  super(lexer,k);
  tokenNames = _tokenNames;
}

public AntlrSchemaExtensionParser(TokenStream lexer) {
  this(lexer,3);
}

public AntlrSchemaExtensionParser(ParserSharedInputState state) {
  super(state,3);
  tokenNames = _tokenNames;
}

/**
     * extensions = *( SP xstring SP qdstrings )
     * xstring = "X" HYPHEN 1*( ALPHA / HYPHEN / USCORE )
     */
	public final AntlrSchemaParser.Extension  extension() throws RecognitionException, TokenStreamException {
		AntlrSchemaParser.Extension extension = new AntlrSchemaParser.Extension();
		
		Token  xkey = null;
		Token  xvalues = null;
		
		{
		xkey = LT(1);
		match(XKEY);
		extension.key = xkey.getText();
		}
		{
		xvalues = LT(1);
		match(XVALUES);
		extension.values = qdstrings(xvalues.getText());
		}
		return extension;
	}
	
	public final List<String>  qdstrings(
		String s
	) throws RecognitionException, TokenStreamException {
		List<String> qdstrings;
		
		
		try 
		{
		AntlrSchemaQdstringLexer lexer = new AntlrSchemaQdstringLexer(new StringReader(s));
		AntlrSchemaQdstringParser parser = new AntlrSchemaQdstringParser(lexer);
		qdstrings = parser.qdstrings();
		}
		catch (RecognitionException re) {
		re.printStackTrace();
		throw re;
		}
		catch (TokenStreamException tse) {
		tse.printStackTrace();
		throw tse;
		}
		
		
		return qdstrings;
	}
	
	
	public static final String[] _tokenNames = {
		"<0>",
		"EOF",
		"<2>",
		"NULL_TREE_LOOKAHEAD",
		"WHSP",
		"QUOTE",
		"XKEY",
		"XVALUES",
		"XSTRING",
		"VALUES",
		"VALUE",
		"QUOTED_STRING"
	};
	
	
	}
