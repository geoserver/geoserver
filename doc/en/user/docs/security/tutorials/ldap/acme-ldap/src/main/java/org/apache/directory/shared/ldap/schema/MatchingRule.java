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
import org.apache.directory.shared.ldap.exception.LdapProtocolErrorException;
import org.apache.directory.shared.ldap.schema.comparators.ComparableComparator;
import org.apache.directory.shared.ldap.schema.normalizers.NoOpNormalizer;
import org.apache.directory.shared.ldap.schema.registries.Registries;


/**
 * A matchingRule definition. MatchingRules associate a comparator and a
 * normalizer, forming the basic tools necessary to assert actions against
 * attribute values. MatchingRules are associated with a specific Syntax for the
 * purpose of resolving a normalized form and for comparisons.
 * <p>
 * According to ldapbis [MODELS]:
 * </p>
 * 
 * <pre>
 *  4.1.3. Matching Rules
 *  
 *    Matching rules are used by servers to compare attribute values against
 *    assertion values when performing Search and Compare operations.  They
 *    are also used to identify the value to be added or deleted when
 *    modifying entries, and are used when comparing a purported
 *    distinguished name with the name of an entry.
 *  
 *    A matching rule specifies the syntax of the assertion value.
 * 
 *    Each matching rule is identified by an object identifier (OID) and,
 *    optionally, one or more short names (descriptors).
 * 
 *    Matching rule definitions are written according to the ABNF:
 * 
 *      MatchingRuleDescription = LPAREN WSP
 *          numericoid                ; object identifier
 *          [ SP &quot;NAME&quot; SP qdescrs ]  ; short names (descriptors)
 *          [ SP &quot;DESC&quot; SP qdstring ] ; description
 *          [ SP &quot;OBSOLETE&quot; ]         ; not active
 *          SP &quot;SYNTAX&quot; SP numericoid ; assertion syntax
 *          extensions WSP RPAREN     ; extensions
 * 
 *    where:
 *      [numericoid] is object identifier assigned to this matching rule;
 *      NAME [qdescrs] are short names (descriptors) identifying this
 *          matching rule;
 *      DESC [qdstring] is a short descriptive string;
 *      OBSOLETE indicates this matching rule is not active;
 *      SYNTAX identifies the assertion syntax by object identifier; and
 *      [extensions] describe extensions.
 * </pre>
 * 
 * @see <a href="http://www.faqs.org/rfcs/rfc2252.html">RFC 2252 Section 4.5</a>
 * @see <a
 *      href="http://www.ietf.org/internet-drafts/draft-ietf-ldapbis-models-11.txt">ldapbis
 *      [MODELS]</a>
 * @see DescriptionUtils#getDescription(MatchingRule)
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 927122 $
 */
public class MatchingRule extends AbstractSchemaObject
{
    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** The associated Comparator */
    protected LdapComparator<? super Object> ldapComparator;

    /** The associated Normalizer */
    protected Normalizer normalizer;

    /** The associated LdapSyntax */
    protected LdapSyntax ldapSyntax;

    /** The associated LdapSyntax OID */
    private String ldapSyntaxOid;


    /**
     * Creates a new instance of MatchingRule.
     *
     * @param oid The MatchingRule OID
     * @param registries The Registries reference
     */
    public MatchingRule( String oid )
    {
        super( SchemaObjectType.MATCHING_RULE, oid );
    }


    /**
     * Inject the MatchingRule into the registries, updating the references to
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
                // Gets the associated Comparator 
                ldapComparator = ( LdapComparator<? super Object> ) registries.getComparatorRegistry().lookup( oid );
            }
            catch ( LdapException ne )
            {
                // Default to a catch all comparator
                ldapComparator = new ComparableComparator( oid );
            }

            try
            {
                // Gets the associated Normalizer
                normalizer = registries.getNormalizerRegistry().lookup( oid );
            }
            catch ( LdapException ne )
            {
                // Default to the NoOp normalizer
                normalizer = new NoOpNormalizer( oid );
            }

            try
            {
                // Get the associated LdapSyntax
                ldapSyntax = registries.getLdapSyntaxRegistry().lookup( ldapSyntaxOid );
            }
            catch ( LdapException ne )
            {
                // The Syntax is a mandatory element, it must exist.
                throw new LdapProtocolErrorException( I18n.err( I18n.ERR_04317 ) );
            }

            /**
             * Add the MR references (using and usedBy) : 
             * MR -> C
             * MR -> N
             * MR -> S
             */
            if ( ldapComparator != null )
            {
                registries.addReference( this, ldapComparator );
            }

            if ( normalizer != null )
            {
                registries.addReference( this, normalizer );
            }

            if ( ldapSyntax != null )
            {
                registries.addReference( this, ldapSyntax );
            }

        }
    }


    /**
     * Remove the MatchingRule from the registries, updating the references to
     * other SchemaObject.
     * 
     * If one of the referenced SchemaObject does not exist (), 
     * an exception is thrown.
     *
     * @param registries The Registries
     * @exception If the MatchingRule is not valid 
     */
    public void removeFromRegistries( List<Throwable> errors, Registries registries ) throws LdapException
    {
        if ( registries != null )
        {
            /**
             * Remove the MR references (using and usedBy) : 
             * MR -> C
             * MR -> N
             * MR -> S
             */
            if ( ldapComparator != null )
            {
                registries.delReference( this, ldapComparator );
            }

            if ( ldapSyntax != null )
            {
                registries.delReference( this, ldapSyntax );
            }

            if ( normalizer != null )
            {
                registries.delReference( this, normalizer );
            }
        }
    }


    /**
     * Gets the LdapSyntax used by this MatchingRule.
     * 
     * @return the LdapSyntax of this MatchingRule
     */
    public LdapSyntax getSyntax()
    {
        return ldapSyntax;
    }


    /**
     * Gets the LdapSyntax OID used by this MatchingRule.
     * 
     * @return the LdapSyntax of this MatchingRule
     * @throws NamingException if there is a failure resolving the object
     */
    public String getSyntaxOid()
    {
        return ldapSyntaxOid;
    }


    /**
     * Sets the Syntax's OID
     *
     * @param oid The Syntax's OID
     */
    public void setSyntaxOid( String oid )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( !isReadOnly )
        {
            this.ldapSyntaxOid = oid;
        }
    }


    /**
     * Sets the Syntax
     *
     * @param oid The Syntax
     */
    public void setSyntax( LdapSyntax ldapSyntax )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( !isReadOnly )
        {
            this.ldapSyntax = ldapSyntax;
            this.ldapSyntaxOid = ldapSyntax.getOid();
        }
    }


    /**
     * Update the associated Syntax, even if the SchemaObject is readOnly
     *
     * @param oid The Syntax
     */
    public void updateSyntax( LdapSyntax ldapSyntax )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        this.ldapSyntax = ldapSyntax;
        this.ldapSyntaxOid = ldapSyntax.getOid();
    }


    /**
     * Gets the LdapComparator enabling the use of this MatchingRule for ORDERING
     * and sorted indexing.
     * 
     * @return the ordering LdapComparator
     * @throws NamingException if there is a failure resolving the object
     */
    public LdapComparator<? super Object> getLdapComparator()
    {
        return ldapComparator;
    }


    /**
     * Sets the LdapComparator
     *
     * @param oid The LdapComparator
     */
    public void setLdapComparator( LdapComparator<?> ldapComparator )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( !isReadOnly )
        {
            this.ldapComparator = ( LdapComparator<? super Object> ) ldapComparator;
        }
    }


    /**
     * Update the associated Comparator, even if the SchemaObject is readOnly
     *
     * @param oid The LdapComparator
     */
    public void updateLdapComparator( LdapComparator<?> ldapComparator )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        this.ldapComparator = ( LdapComparator<? super Object> ) ldapComparator;
    }


    /**
     * Gets the Normalizer enabling the use of this MatchingRule for EQUALITY
     * matching and indexing.
     * 
     * @return the associated normalizer
     * @throws NamingException if there is a failure resolving the object
     */
    public Normalizer getNormalizer()
    {
        return normalizer;
    }


    /**
     * Sets the Normalizer
     *
     * @param oid The Normalizer
     */
    public void setNormalizer( Normalizer normalizer )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( !isReadOnly )
        {
            this.normalizer = normalizer;
        }
    }


    /**
     * Update the associated Normalizer, even if the SchemaObject is readOnly
     *
     * @param oid The Normalizer
     */
    public void updateNormalizer( Normalizer normalizer )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        this.normalizer = normalizer;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return objectType + " " + DescriptionUtils.getDescription( this );
    }


    /**
     * Copy an MatchingRule
     */
    public MatchingRule copy()
    {
        MatchingRule copy = new MatchingRule( oid );

        // Copy the SchemaObject common data
        copy.copy( this );

        // All the references to other Registries object are set to null.
        copy.ldapComparator = null;
        copy.ldapSyntax = null;
        copy.normalizer = null;

        // Copy the syntax OID
        copy.ldapSyntaxOid = ldapSyntaxOid;

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

        if ( !( o instanceof MatchingRule ) )
        {
            return false;
        }

        MatchingRule that = ( MatchingRule ) o;

        // Check the Comparator
        if ( ldapComparator != null )
        {
            if ( !ldapComparator.equals( that.ldapComparator ) )
            {
                return false;
            }
        }
        else
        {
            if ( that.ldapComparator != null )
            {
                return false;
            }
        }

        // Check the Normalizer
        if ( normalizer != null )
        {
            if ( !normalizer.equals( that.normalizer ) )
            {
                return false;
            }
        }
        else
        {
            if ( that.normalizer != null )
            {
                return false;
            }
        }

        // Check the Syntax
        if ( !compareOid( ldapSyntaxOid, that.ldapSyntaxOid ) )
        {
            return false;
        }

        return ldapSyntax.equals( that.ldapSyntax );
    }


    /**
     * {@inheritDoc}
     */
    public void clear()
    {
        // Clear the common elements
        super.clear();

        // Clear the references
        ldapComparator = null;
        ldapSyntax = null;
        normalizer = null;
    }
}
