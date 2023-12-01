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
package org.apache.directory.shared.ldap.schema.parsers;


import java.io.StringReader;
import java.text.ParseException;
import java.util.List;

import org.apache.directory.shared.ldap.constants.MetaSchemaConstants;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.util.StringTools;



/**
 * 
 * TODO AbstractSchemaParser.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class AbstractSchemaParser
{

    /** the monitor to use for this parser */
    protected ParserMonitor monitor = new ParserMonitorAdapter();

    /** the antlr generated parser being wrapped */
    protected ReusableAntlrSchemaParser parser;

    /** the antlr generated lexer being wrapped */
    protected ReusableAntlrSchemaLexer lexer;


    protected AbstractSchemaParser()
    {
        lexer = new ReusableAntlrSchemaLexer( new StringReader( "" ) );
        parser = new ReusableAntlrSchemaParser( lexer );
    }


    /**
     * Initializes the plumbing by creating a pipe and coupling the parser/lexer
     * pair with it. param spec the specification to be parsed
     */
    protected void reset( String spec )
    {
        StringReader in = new StringReader( spec );
        lexer.prepareNextInput( in );
        parser.resetState();
    }


    /**
     * Sets the parser monitor.
     * 
     * @param monitor the new parser monitor
     */
    public void setParserMonitor( ParserMonitor monitor )
    {
        this.monitor = monitor;
        parser.setParserMonitor( monitor );
    }


    /**
     * Sets the quirks mode. 
     * 
     * If enabled the parser accepts non-numeric OIDs and some 
     * special characters in descriptions.
     * 
     * @param enabled the new quirks mode
     */
    public void setQuirksMode( boolean enabled )
    {
        parser.setQuirksMode( enabled );
    }


    /**
     * Checks if quirks mode is enabled.
     * 
     * @return true, if is quirks mode is enabled
     */
    public boolean isQuirksMode()
    {
        return parser.isQuirksMode();
    }


    /**
     * Parse a SchemaObject description and returns back an instance of SchemaObject.
     * 
     * @param schemaDescription The SchemaObject description
     * @return A SchemaObject instance
     * @throws ParseException If the parsing failed
     */
    public abstract SchemaObject parse( String schemaDescription ) throws ParseException;
    
    
    /**
     * Update the schemaName for this SchemaObject, accordingly to the X-SCHEMA parameter. If
     * not present, default to 'other'
     */
    protected void setSchemaName( SchemaObject schemaObject )
    {
        
        // Update the Schema if we have the X-SCHEMA extension
        List<String> schemaExtension = schemaObject.getExtensions().get( MetaSchemaConstants.X_SCHEMA );
        
        if ( schemaExtension != null )
        {
            String schemaName = schemaExtension.get( 0 );
            
            if ( StringTools.isEmpty( schemaName ) )
            {
                schemaObject.setSchemaName( MetaSchemaConstants.SCHEMA_OTHER );
            }
            else
            {
                schemaObject.setSchemaName( schemaName );
            }
        }
        else
        {
            schemaObject.setSchemaName( MetaSchemaConstants.SCHEMA_OTHER );
        }
    }
}
