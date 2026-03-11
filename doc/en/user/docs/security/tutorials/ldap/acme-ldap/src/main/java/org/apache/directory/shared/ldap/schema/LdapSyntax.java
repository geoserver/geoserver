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
package org.apache.directory.shared.ldap.schema;


import java.util.List;

import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.schema.syntaxCheckers.OctetStringSyntaxChecker;


/**
 * A syntax definition. Each attribute stored in a directory has a defined
 * syntax (i.e. data type) which constrains the structure and format of its
 * values. The description of each syntax specifies how attribute or assertion
 * values conforming to the syntax are normally represented when transferred in
 * LDAP operations. This representation is referred to as the LDAP-specific
 * encoding to distinguish it from other methods of encoding attribute values.
 * <p>
 * According to ldapbis [MODELS]:
 * </p>
 * 
 * <pre>
 *  4.1.5. LDAP Syntaxes
 *  
 *    LDAP Syntaxes of (attribute and assertion) values are described in
 *    terms of ASN.1 [X.680] and, optionally, have an octet string encoding
 *    known as the LDAP-specific encoding.  Commonly, the LDAP-specific
 *    encoding is constrained to string of Universal Character Set (UCS)
 *    [ISO10646] characters in UTF-8 [UTF-8] form.
 * 
 *    Each LDAP syntax is identified by an object identifier (OID).
 * 
 *    LDAP syntax definitions are written according to the ABNF:
 * 
 *      SyntaxDescription = LPAREN WSP
 *          numericoid                ; object identifier
 *          [ SP &quot;DESC&quot; SP qdstring ] ; description
 *          extensions WSP RPAREN     ; extensions
 * 
 *    where:
 *      [numericoid] is object identifier assigned to this LDAP syntax;
 *      DESC [qdstring] is a short descriptive string; and
 *      [extensions] describe extensions.
 * </pre>
 * 
 * @see <a href="http://www.faqs.org/rfcs/rfc2252.html"> RFC2252 Section 4.3.3</a>
 * @see <a href=
 *      "http://www.ietf.org/internet-drafts/draft-ietf-ldapbis-models-09.txt">
 *      ldapbis [MODELS]</a>
 * @see DescriptionUtils#getDescription(Syntax)
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 437007 $
 */
public class LdapSyntax extends AbstractSchemaObject
{
    /** The serialVersionUID */
    public static final long serialVersionUID = 1L;

    /** the human readable flag */
    protected boolean isHumanReadable = false;

    /** The associated SyntaxChecker */
    protected SyntaxChecker syntaxChecker;


    /**
     * Creates a Syntax object using a unique OID.
     * 
     * @param oid the OID for this Syntax
     */
    public LdapSyntax( String oid )
    {
        super( SchemaObjectType.LDAP_SYNTAX, oid );
    }


    /**
     * Creates a Syntax object using a unique OID.
     * 
     * @param oid the OID for this Syntax
     */
    public LdapSyntax( String oid, String description )
    {
        super( SchemaObjectType.LDAP_SYNTAX, oid );
        this.description = description;
    }


    /**
     * Creates a Syntax object using a unique OID.
     * 
     * @param oid the OID for this Syntax
     */
    public LdapSyntax( String oid, String description, boolean isHumanReadable )
    {
        super( SchemaObjectType.LDAP_SYNTAX, oid );
        this.description = description;
        this.isHumanReadable = isHumanReadable;
    }


    /**
     * Gets whether or not the Syntax is human readable.
     * 
     * @return true if the syntax can be interpreted by humans, false otherwise
     */
    public boolean isHumanReadable()
    {
        return isHumanReadable;
    }


    /**
     * Sets the human readable flag value.
     * 
     * @param isHumanReadable the human readable flag value to set
     */
    public void setHumanReadable( boolean isHumanReadable )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( !isReadOnly )
        {
            this.isHumanReadable = isHumanReadable;
        }
    }


    /**
     * Gets the SyntaxChecker used to validate values in accordance with this
     * Syntax.
     * 
     * @return the SyntaxChecker
     */
    public SyntaxChecker getSyntaxChecker()
    {
        return syntaxChecker;
    }


    /**
     * Sets the associated SyntaxChecker
     *
     * @param syntaxChecker The associated SyntaxChecker
     */
    public void setSyntaxChecker( SyntaxChecker syntaxChecker )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( !isReadOnly )
        {
            this.syntaxChecker = syntaxChecker;
        }
    }


    /**
     * Update the associated SyntaxChecker, even if the SchemaObject is readOnly
     *
     * @param syntaxChecker The associated SyntaxChecker
     */
    public void updateSyntaxChecker( SyntaxChecker syntaxChecker )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        this.syntaxChecker = syntaxChecker;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return objectType + " " + DescriptionUtils.getDescription( this );
    }


    /**
     * Inject the Syntax into the registries, updating the references to
     * other SchemaObject
     *
     * @param registries The Registries
     * @exception If the addition failed
     */
    public void addToRegistries( List<Throwable> errors, Registries registries ) throws LdapException
    {
        if ( registries != null )
        {
            try
            {
                // Gets the associated SyntaxChecker
                syntaxChecker = registries.getSyntaxCheckerRegistry().lookup( oid );
            }
            catch ( LdapException ne )
            {
                // No SyntaxChecker ? Associate the Syntax to a catch all SyntaxChecker
                syntaxChecker = new OctetStringSyntaxChecker( oid );
            }

            // Add the references for S :
            // S -> SC
            if ( syntaxChecker != null )
            {
                registries.addReference( this, syntaxChecker );
            }
        }
    }
    
    
    /**
     * Remove the SDyntax from the registries, updating the references to
     * other SchemaObject.
     * 
     * If one of the referenced SchemaObject does not exist (), 
     * an exception is thrown.
     *
     * @param registries The Registries
     * @exception If the Syntx is not valid 
     */
    public void removeFromRegistries( List<Throwable> errors, Registries registries ) throws LdapException
    {
        if ( registries != null )
        {
            /**
             * Remove the Syntax references (using and usedBy) : 
             * S -> SC
             */
            if ( syntaxChecker != null )
            {
                registries.delReference( this, syntaxChecker );
            }
        }
    }


    /**
     * Copy a LdapSyntax
     */
    public LdapSyntax copy()
    {
        LdapSyntax copy = new LdapSyntax( oid );

        // Copy the SchemaObject common data
        copy.copy( this );

        // Copy the HR flag
        copy.isHumanReadable = isHumanReadable;

        // All the references to other Registries object are set to null.
        copy.syntaxChecker = null;

        return copy;
    }


    /**
     * @see Object#equals()
     */
    public boolean equals( Object o )
    {
        if ( !super.equals( o ) )
        {
            return false;
        }

        if ( !( o instanceof LdapSyntax ) )
        {
            return false;
        }

        LdapSyntax that = ( LdapSyntax ) o;

        // IsHR
        if ( isHumanReadable != that.isHumanReadable )
        {
            return false;
        }

        // Check the SyntaxChecker (not a equals)
        return syntaxChecker.getOid().equals( that.syntaxChecker.getOid() );
    }


    /**
     * {@inheritDoc}
     */
    public void clear()
    {
        // Clear the common elements
        super.clear();

        // Clear the references
        syntaxChecker = null;
    }
}
