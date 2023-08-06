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


import java.io.StringReader;
import java.text.ParseException;
import java.util.Map;

import org.apache.directory.shared.i18n.I18n;

import antlr.RecognitionException;
import antlr.TokenStreamException;


/**
 * A reusable wrapper around the antlr generated parser for an LDAP subtree
 * specification as defined by <a href="http://www.faqs.org/rfcs/rfc3672.html">
 * RFC 3672</a>. This class enables the reuse of the antlr parser/lexer pair
 * without having to recreate the pair every time.
 * 
 * @see <a href="http://www.faqs.org/rfcs/rfc3672.html">RFC 3672</a>
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 437007 $
 */
public class SubtreeSpecificationChecker
{
    /** the antlr generated parser being wrapped */
    private ReusableAntlrSubtreeSpecificationChecker parser;

    /** the antlr generated lexer being wrapped */
    private ReusableAntlrSubtreeSpecificationCheckerLexer lexer;


    /**
     * Creates a subtree specification parser.
     */
    public SubtreeSpecificationChecker( Map oidsMap )
    {
        StringReader in = new StringReader( "" ); // place holder for the
                                                    // first input
        this.lexer = new ReusableAntlrSubtreeSpecificationCheckerLexer( in );
        this.parser = new ReusableAntlrSubtreeSpecificationChecker( lexer );
        this.parser.init(); // this method MUST be called while we cannot do
                            // constructor overloading for antlr generated parser
    }


    /**
     * Creates a normalizing subtree specification parser.
     */
    public SubtreeSpecificationChecker()
    {
        StringReader in = new StringReader( "" ); // place holder for the
                                                  // first input
        this.lexer = new ReusableAntlrSubtreeSpecificationCheckerLexer( in );
        this.parser = new ReusableAntlrSubtreeSpecificationChecker( lexer );
        this.parser.init(); // this method MUST be called while we cannot do
                            // constructor overloading for antlr generated parser
    }


    /**
     * Initializes the plumbing by creating a pipe and coupling the parser/lexer
     * pair with it. param spec the specification to be parsed
     */
    private synchronized void reset( String spec )
    {
        StringReader in = new StringReader( spec + "end" ); // append end of
                                                            // input token
        this.lexer.prepareNextInput( in );
        this.parser.resetState();
    }


    /**
     * Parses a subtree specification without exhausting the parser.
     * 
     * @param spec
     *            the specification to be parsed
     * @throws ParseException
     *             if there are any recognition errors (bad syntax)
     */
    public synchronized void parse( String spec ) throws ParseException
    {
        if ( spec == null || spec.trim().equals( "" ) )
        {
            return;
        }

        reset( spec ); // reset and initialize the parser / lexer pair

        try
        {
            this.parser.wrapperEntryPoint();
        }
        catch ( TokenStreamException e )
        {
            String msg = I18n.err( I18n.ERR_04329, spec, e.getLocalizedMessage() );
            throw new ParseException( msg, 0 );
        }
        catch ( RecognitionException e )
        {
            String msg = I18n.err( I18n.ERR_04329, spec, e.getLocalizedMessage() );
            throw new ParseException( msg, e.getColumn() );
        }
        catch ( Exception e )
        {
            String msg = I18n.err( I18n.ERR_04329, spec, e.getLocalizedMessage() );
            throw new ParseException( msg, 0 );
        }
    }

}
