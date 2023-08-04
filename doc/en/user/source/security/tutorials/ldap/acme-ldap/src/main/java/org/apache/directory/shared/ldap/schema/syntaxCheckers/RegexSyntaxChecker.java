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
package org.apache.directory.shared.ldap.schema.syntaxCheckers;

import java.util.ArrayList;
import java.util.List;

import org.apache.directory.shared.ldap.schema.SyntaxChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A SyntaxChecker implemented using Perl5 regular expressions to constrain
 * values.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 736240 $
 */
public class RegexSyntaxChecker extends SyntaxChecker
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( RegexSyntaxChecker.class );

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** the set of regular expressions */
    private List<String> expressions;

    /**
     * Creates a Syntax validator for a specific Syntax using Perl5 matching
     * rules for validation.
     * 
     * @param oid
     *            the oid of the Syntax values checked
     * @param matchExprArray
     *            the array of matching expressions
     */
    public RegexSyntaxChecker( String oid, String[] matchExprArray )
    {
        super( oid );
        
        if ( ( matchExprArray != null ) && ( matchExprArray.length != 0 ) )
        {
            expressions = new ArrayList<String>( matchExprArray.length );
            
            for ( String regexp:matchExprArray )
            {
                expressions.add( regexp );
            }
        }
        else
        {
            expressions = new ArrayList<String>();
        }
    }


    /**
     * 
     * Creates a new instance of RegexSyntaxChecker.
     * 
     * @param oid the oid to associate with this new SyntaxChecker
     *
     */
    public RegexSyntaxChecker( String oid )
    {
        super( oid );
        expressions = new ArrayList<String>();
    }
    

    /**
     * {@inheritDoc}
     */
    public boolean isValidSyntax( Object value )
    {
        String str = null;
        boolean match = true;

        if ( value instanceof String )
        {
            str = ( String ) value;

            for ( String regexp:expressions )
            {
                match = match && str.matches( regexp );

                if ( !match )
                {
                    break;
                }
            }
        }

         if ( match )
         {
             LOG.debug( "Syntax valid for '{}'", value );
         }
         else
         {
             LOG.debug( "Syntax invalid for '{}'", value );
         }
         
         return match;
    }

    /**
     * Get the list of regexp stored into this SyntaxChecker
     * 
     * @return AN array containing all the stored regexp
     */
    public String[] getExpressions()
    {
        String[] exprs = new String[ expressions.size() ];
        return expressions.toArray( exprs );
    }

    /**
     * Add a list of regexp to be applied by this SyntaxChecker
     * 
     * @param expressions The regexp list to add
     */
    public void setExpressions( String[] expressions )
    {
        for ( String regexp:expressions )
        {
            this.expressions.add( regexp );
        }
    }
}
