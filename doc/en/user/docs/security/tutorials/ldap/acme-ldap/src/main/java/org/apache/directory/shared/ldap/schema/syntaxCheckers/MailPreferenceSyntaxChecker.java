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


import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.schema.SyntaxChecker;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A SyntaxChecker which verifies that a value is a Boolean according to RFC 1778.
 * 
 * From RFC 1778 :
 * 
 * <mail-preference> ::= "NO-LISTS" | "ANY-LIST" | "PROFESSIONAL-LISTS"
 * 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class MailPreferenceSyntaxChecker extends SyntaxChecker
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( MailPreferenceSyntaxChecker.class );

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance of MailPreferenceSyntaxChecker.
     */
    public MailPreferenceSyntaxChecker()
    {
        super( SchemaConstants.MAIL_PREFERENCE_SYNTAX );
    }
    
    /**
     * 
     * Creates a new instance of MailPreferenceSyntaxChecker.
     * 
     * @param oid the oid to associate with this new SyntaxChecker
     *
     */
    protected MailPreferenceSyntaxChecker( String oid )
    {
        super( oid );
    }
    
    
    /**
     * {@inheritDoc}
     */
    public boolean isValidSyntax( Object value )
    {
        String strValue =null;

        if ( value == null )
        {
            LOG.debug( "Syntax invalid for '{}'", value );
            return false;
        }
        
        if ( value instanceof String )
        {
            strValue = ( String ) value;
        }
        else if ( value instanceof byte[] )
        {
            strValue = StringTools.utf8ToString( ( byte[] ) value ); 
        }
        else
        {
            strValue = value.toString();
        }

        if ( ( strValue.length() < 8 ) || ( strValue.length() > 18 ) )
        {
            LOG.debug( "Syntax invalid for '{}'", value );
            return false;
        }
        
        boolean result = ( ( "NO-LISTS".equals( strValue ) ) || ( "ANY-LIST".equals( strValue ) ) || 
            ( "PROFESSIONAL-LISTS".equals( strValue ) ) );
        
        if ( result )
        {
            LOG.debug( "Syntax valid for '{}'", value );
        }
        else
        {
            LOG.debug( "Syntax invalid for '{}'", value );
        }
        
        return result;
    }
}
