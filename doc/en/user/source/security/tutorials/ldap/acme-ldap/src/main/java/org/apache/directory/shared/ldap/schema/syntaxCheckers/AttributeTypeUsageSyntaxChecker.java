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
 * A syntax checker which checks to see if an attributeType's type is either: 
 * userApplications
 * directoryOperation
 * distributedOperation
 * dSAOperation
.*  The case is NOT ignored.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AttributeTypeUsageSyntaxChecker extends SyntaxChecker
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( AttributeTypeUsageSyntaxChecker.class );

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /**
     * 
     * Creates a new instance of AttributeTypeUsageSyntaxChecker.
     *
     */
    public AttributeTypeUsageSyntaxChecker()
    {
        super( SchemaConstants.ATTRIBUTE_TYPE_USAGE_SYNTAX );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isValidSyntax( Object value )
    {
        String strValue = null;

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

        if ( ( strValue.length() < "userApplications".length() ) || 
             ( strValue.length() > "userApplications".length() ) )
        {
            LOG.debug( "Syntax invalid for '{}'", value );
            return false;
        }
        
        char ch = strValue.charAt( 0 );

        switch ( ch )
        {
            case( 'd' ):
                if ( "dSAOperation".equals( strValue ) ||
                     "directoryOperation".equals( strValue ) ||
                     "distributedOperation".equals( strValue ) )
                {
                    LOG.debug( "Syntax valid for '{}'", value );
                    return true;
                }

                LOG.debug( "Syntax invalid for '{}'", value );
                return false;
            
            case( 'u' ):
                boolean comp = "userApplications".equals( strValue );
            
                if ( comp )
                {
                    LOG.debug( "Syntax valid for '{}'", value );
                }
                else
                {
                    LOG.debug( "Syntax valid for '{}'", value );
                    
                }
                
                return comp;
            
            default:
                LOG.debug( "Syntax invalid for '{}'", value );
                return false;
        }
    }
}
