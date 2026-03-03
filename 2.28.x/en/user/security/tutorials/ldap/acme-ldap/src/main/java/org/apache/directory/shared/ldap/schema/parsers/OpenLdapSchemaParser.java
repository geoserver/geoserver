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


import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.ObjectClass;
import org.apache.directory.shared.ldap.schema.SchemaObject;
import org.apache.directory.shared.ldap.schema.syntaxCheckers.OpenLdapObjectIdentifierMacro;
import org.apache.directory.shared.ldap.util.ExceptionUtils;

import antlr.RecognitionException;
import antlr.TokenStreamException;


/**
 * A reusable wrapper for antlr generated OpenLDAP schema parsers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 494164 $
 */
public class OpenLdapSchemaParser extends AbstractSchemaParser
{

    /** The list of parsed schema descriptions */
    private List<Object> schemaDescriptions;

    /** The list of attribute type, initialized by splitParsedSchemaDescriptions() */
    private List<AttributeType> attributeTypes;

    /** The list of object classes, initialized by splitParsedSchemaDescriptions()*/
    private List<ObjectClass> objectClasses;

    /** The map of object identifier macros, initialized by splitParsedSchemaDescriptions()*/
    private Map<String, OpenLdapObjectIdentifierMacro> objectIdentifierMacros;

    /** Flag whether object identifier macros should be resolved. */
    private boolean isResolveObjectIdentifierMacros;


    /**
     * Creates a reusable instance of an OpenLdapSchemaParser.
     *
     * @throws IOException if the pipe cannot be formed
     */
    public OpenLdapSchemaParser() throws IOException
    {
        isResolveObjectIdentifierMacros = true;
        super.setQuirksMode( true );
    }


    /**
     * Reset the parser 
     */
    public void clear()
    {
    }


    /**
     * Gets the attribute types.
     * 
     * @return the attribute types
     */
    public List<AttributeType> getAttributeTypes()
    {
        return attributeTypes;
    }


    /**
     * Gets the object class types.
     * 
     * @return the object class types
     */
    public List<ObjectClass> getObjectClassTypes()
    {
        return objectClasses;
    }


    /**
     * Gets the object identifier macros.
     * 
     * @return the object identifier macros
     */
    public Map<String, OpenLdapObjectIdentifierMacro> getObjectIdentifierMacros()
    {
        return objectIdentifierMacros;
    }


    /**
     * Splits parsed schema descriptions and resolved
     * object identifier macros.
     * 
     * @throws ParseException the parse exception
     */
    private void afterParse() throws ParseException
    {
        objectClasses = new ArrayList<ObjectClass>();
        attributeTypes = new ArrayList<AttributeType>();
        objectIdentifierMacros = new HashMap<String, OpenLdapObjectIdentifierMacro>();

        // split parsed schema descriptions
        for ( Object obj : schemaDescriptions )
        {
            if ( obj instanceof OpenLdapObjectIdentifierMacro )
            {
                OpenLdapObjectIdentifierMacro oid = ( OpenLdapObjectIdentifierMacro ) obj;
                objectIdentifierMacros.put( oid.getName(), oid );
            }
            else if ( obj instanceof AttributeType )
            {
                AttributeType attributeType = ( AttributeType ) obj;

                attributeTypes.add( attributeType );
            }
            else if ( obj instanceof ObjectClass )
            {
                ObjectClass objectClass = ( ObjectClass ) obj;

                objectClasses.add( objectClass );
            }
        }

        if ( isResolveObjectIdentifierMacros() )
        {
            // resolve object identifier macros
            for ( OpenLdapObjectIdentifierMacro oid : objectIdentifierMacros.values() )
            {
                resolveObjectIdentifierMacro( oid );
            }

            // apply object identifier macros to object classes
            for ( ObjectClass objectClass : objectClasses )
            {
                objectClass.setOid( getResolveOid( objectClass.getOid() ) );
            }
            
            // apply object identifier macros to attribute types
            for ( AttributeType attributeType : attributeTypes )
            {
                attributeType.setOid( getResolveOid( attributeType.getOid() ) );
                attributeType.setSyntaxOid( getResolveOid( attributeType.getSyntaxOid() ) );
            }
            
        }
    }


    private String getResolveOid( String oid )
    {
        if ( oid != null && oid.indexOf( ':' ) != -1 )
        {
            // resolve OID
            String[] nameAndSuffix = oid.split( ":" );
            if ( objectIdentifierMacros.containsKey( nameAndSuffix[0] ) )
            {
                OpenLdapObjectIdentifierMacro macro = objectIdentifierMacros.get( nameAndSuffix[0] );
                return macro.getResolvedOid() + "." + nameAndSuffix[1];
            }
        }
        return oid;
    }


    private void resolveObjectIdentifierMacro( OpenLdapObjectIdentifierMacro macro ) throws ParseException
    {
        String rawOidOrNameSuffix = macro.getRawOidOrNameSuffix();

        if ( macro.isResolved() )
        {
            // finished
        }
        else if ( rawOidOrNameSuffix.indexOf( ':' ) != -1 )
        {
            // resolve OID
            String[] nameAndSuffix = rawOidOrNameSuffix.split( ":" );
            if ( objectIdentifierMacros.containsKey( nameAndSuffix[0] ) )
            {
                OpenLdapObjectIdentifierMacro parentMacro = objectIdentifierMacros.get( nameAndSuffix[0] );
                resolveObjectIdentifierMacro( parentMacro );
                macro.setResolvedOid( parentMacro.getResolvedOid() + "." + nameAndSuffix[1] );
            }
            else
            {
                throw new ParseException( I18n.err( I18n.ERR_04257, nameAndSuffix[0] ), 0 );
            }

        }
        else
        {
            // no :suffix, 
            if ( objectIdentifierMacros.containsKey( rawOidOrNameSuffix ) )
            {
                OpenLdapObjectIdentifierMacro parentMacro = objectIdentifierMacros.get( rawOidOrNameSuffix );
                resolveObjectIdentifierMacro( parentMacro );
                macro.setResolvedOid( parentMacro.getResolvedOid() );
            }
            else
            {
                macro.setResolvedOid( rawOidOrNameSuffix );
            }
        }
    }


    /**
     * Parses an OpenLDAP schemaObject element/object.
     *
     * @param schemaObject the String image of a complete schema object
     * @throws IOException If the schemaObject can't be transformed to a byteArrayInputStream
     * @throws ParseException If the schemaObject can't be parsed
     */
    public SchemaObject parse( String schemaObject ) throws ParseException
    {
        if ( schemaObject == null || schemaObject.trim().equals( "" ) )
        {
            throw new ParseException( I18n.err( I18n.ERR_04258 ), 0 );
        }

        reset( schemaObject ); // reset and initialize the parser / lexer pair
        invokeParser( schemaObject );

        if ( !schemaDescriptions.isEmpty() )
        {
            for ( Object obj : schemaDescriptions )
            {
                if ( obj instanceof SchemaObject )
                {
                    return ( SchemaObject ) obj;
                }
            }
        }
        return null;
    }


    private void invokeParser( String subject ) throws ParseException
    {
        try
        {
            monitor.startedParse( "starting parse on:\n" + subject );
            schemaDescriptions = parser.openLdapSchema();
            afterParse();
            monitor.finishedParse( "Done parsing!" );
        }
        catch ( RecognitionException e )
        {
            String msg = "Parser failure on:\n\t" + subject;
            msg += "\nAntlr exception trace:\n" + ExceptionUtils.getFullStackTrace( e );
            throw new ParseException( msg, e.getColumn() );
        }
        catch ( TokenStreamException e2 )
        {
            String msg = "Parser failure on:\n\t" + subject;
            msg += "\nAntlr exception trace:\n" + ExceptionUtils.getFullStackTrace( e2 );
            throw new ParseException( msg, 0 );
        }
    }


    /**
     * Parses a stream of OpenLDAP schemaObject elements/objects.
     *
     * @param schemaIn a stream of schema objects
     * @throws IOException If the schemaObject can't be transformed to a byteArrayInputStream
     * @throws ParseException If the schemaObject can't be parsed
     */
    public void parse( InputStream schemaIn ) throws IOException, ParseException
    {
        InputStreamReader in = new InputStreamReader( schemaIn );
        lexer.prepareNextInput( in );
        parser.resetState();

        invokeParser( "schema input stream ==> " + schemaIn.toString() );
    }


    /**
     * Parses a file of OpenLDAP schemaObject elements/objects.
     *
     * @param schemaFile a file of schema objects
     * @throws IOException If the schemaObject can't be transformed to a byteArrayInputStream
     * @throws ParseException If the schemaObject can't be parsed
     */
    public void parse( File schemaFile ) throws IOException, ParseException
    {
        FileReader in = new FileReader( schemaFile );
        lexer.prepareNextInput( in );
        parser.resetState();

        invokeParser( "schema file ==> " + schemaFile.getAbsolutePath() );
    }


    /**
     * Checks if object identifier macros should be resolved.
     * 
     * @return true, object identifier macros should be resolved.
     */
    public boolean isResolveObjectIdentifierMacros()
    {
        return isResolveObjectIdentifierMacros;
    }


    /**
     * Sets if object identifier macros should be resolved.
     * 
     * @param isResolveObjectIdentifierMacros true if object identifier macros should be resolved
     */
    public void setResolveObjectIdentifierMacros( boolean isResolveObjectIdentifierMacros )
    {
        this.isResolveObjectIdentifierMacros = isResolveObjectIdentifierMacros;
    }

}
