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


import java.util.ArrayList;
import java.util.List;

import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.schema.registries.ObjectClassRegistry;
import org.apache.directory.shared.ldap.schema.registries.Registries;


/**
 * A ditContentRule specification. ditContentRules identify the content of
 * entries of a particular structural objectClass. They specify the AUXILIARY
 * objectClasses and additional attribute types permitted to appear, or excluded
 * from appearing in entries of the indicated STRUCTURAL objectClass.
 * <p>
 * According to ldapbis [MODELS]:
 * </p>
 * 
 * <pre>
 *  4.1.6. DIT Content Rules
 *  
 *    A DIT content rule is a &quot;rule governing the content of entries of a
 *    particular structural object class&quot; [X.501].
 *  
 *    For DIT entries of a particular structural object class, a DIT content
 *    rule specifies which auxiliary object classes the entries are allowed
 *    to belong to and which additional attributes (by type) are required,
 *    allowed or not allowed to appear in the entries.
 *  
 *    The list of precluded attributes cannot include any attribute listed
 *    as mandatory in rule, the structural object class, or any of the
 *    allowed auxiliary object classes.
 *  
 *    Each content rule is identified by the object identifier, as well as
 *    any short names (descriptors), of the structural object class it
 *    applies to.
 *  
 *    An entry may only belong to auxiliary object classes listed in the
 *    governing content rule.
 *  
 *    An entry must contain all attributes required by the object classes
 *    the entry belongs to as well as all attributed required by the
 *    governing content rule.
 *  
 *    An entry may contain any non-precluded attributes allowed by the
 *    object classes the entry belongs to as well as all attributes allowed
 *    by the governing content rule.
 *  
 *    An entry cannot include any attribute precluded by the governing
 *    content rule.
 *  
 *    An entry is governed by (if present and active in the subschema) the
 *    DIT content rule which applies to the structural object class of the
 *    entry (see Section 2.4.2).  If no active rule is present for the
 *    entry's structural object class, the entry's content is governed by
 *    the structural object class (and possibly other aspects of user and
 *    system schema).
 * 
 *    DIT content rule descriptions are written according to the ABNF:
 * 
 *      DITContentRuleDescription = LPAREN WSP
 *          numericoid                ; object identifier
 *          [ SP &quot;NAME&quot; SP qdescrs ]  ; short names (descriptors)
 *          [ SP &quot;DESC&quot; SP qdstring ] ; description
 *          [ SP &quot;OBSOLETE&quot; ]         ; not active
 *          [ SP &quot;AUX&quot; SP oids ]      ; auxiliary object classes
 *          [ SP &quot;MUST&quot; SP oids ]     ; attribute types
 *          [ SP &quot;MAY&quot; SP oids ]      ; attribute types
 *          [ SP &quot;NOT&quot; SP oids ]      ; attribute types
 *          extensions WSP RPAREN     ; extensions
 * 
 *    where:
 * 
 *      [numericoid] is the object identifier of the structural object class
 *          associated with this DIT content rule;
 *      NAME [qdescrs] are short names (descriptors) identifying this DIT
 *          content rule;
 *      DESC [qdstring] is a short descriptive string;
 *      OBSOLETE indicates this DIT content rule use is not active;
 *      AUX specifies a list of auxiliary object classes which entries
 *          subject to this DIT content rule may belong to;
 *      MUST, MAY, and NOT specify lists of attribute types which are
 *          required, allowed, or precluded, respectively, from appearing in
 *          entries subject to this DIT content rule; and
 *      [extensions] describe extensions.
 * </pre>
 * 
 * @see <a href="http://www.faqs.org/rfcs/rfc2252.html">RFC 2252 Section 5.4.3</a>
 * @see <a
 *      href="http://www.ietf.org/internet-drafts/draft-ietf-ldapbis-models-11.txt">ldapbis
 *      [MODELS]</a>
 * @see DescriptionUtils#getDescription(DITContentRule)
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 927122 $
 */
public class DITContentRule extends AbstractSchemaObject
{
    /** The serialVersionUID */
    public static final long serialVersionUID = 1L;

    /** The list of Auxiliary ObjectClass OIDs entries may belong to */
    private List<String> auxObjectClassOids;

    /** The list of Auxiliary ObjectClass entries may belong to */
    private List<ObjectClass> auxObjectClasses;

    /** The list of allowed AttributeType OIDs */
    private List<String> mayAttributeTypeOids;

    /** The list of allowed AttributeTypes */
    private List<AttributeType> mayAttributeTypes;

    /** The list of required AttributeType OIDs */
    private List<String> mustAttributeTypeOids;

    /** The list of required AttributeTypes */
    private List<AttributeType> mustAttributeTypes;

    /** The list of precluded AttributeType OIDs */
    private List<String> notAttributeTypeOids;

    /** The list of precluded AttributeTypes */
    private List<AttributeType> notAttributeTypes;


    /**
     * Creates a DITContentRule object using a unique OID.
     * 
     * @param oid the OID for this DITContentRule
     */
    public DITContentRule( String oid )
    {
        super( SchemaObjectType.DIT_CONTENT_RULE, oid );

        mayAttributeTypeOids = new ArrayList<String>();
        mustAttributeTypeOids = new ArrayList<String>();
        notAttributeTypeOids = new ArrayList<String>();
        auxObjectClassOids = new ArrayList<String>();

        mayAttributeTypes = new ArrayList<AttributeType>();
        mustAttributeTypes = new ArrayList<AttributeType>();
        notAttributeTypes = new ArrayList<AttributeType>();
        auxObjectClasses = new ArrayList<ObjectClass>();
    }


    /**
     * @return the auxObjectClassOids
     */
    public List<String> getAuxObjectClassOids()
    {
        return auxObjectClassOids;
    }


    /**
     * Add an Auxiliary ObjectClass Oid
     *
     * @param oid The ObjectClass oid
     */
    public void addAuxObjectClassOidOids( String oid )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( !isReadOnly )
        {
            auxObjectClassOids.add( oid );
        }
    }


    /**
     * Add an Auxiliary ObjectClass
     *
     * @param oid The ObjectClass
     */
    public void addAuxObjectClasses( ObjectClass objectClass )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( !isReadOnly )
        {
            if ( !auxObjectClassOids.contains( objectClass.getOid() ) )
            {
                auxObjectClasses.add( objectClass );
                auxObjectClassOids.add( objectClass.getOid() );
            }
        }
    }


    /**
     * @param auxObjectClassOids the auxObjectClassOids to set
     */
    public void setAuxObjectClassOids( List<String> auxObjectClassOids )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( !isReadOnly )
        {
            this.auxObjectClassOids = auxObjectClassOids;
        }
    }


    /**
     * @param auxObjectClasses the auxObjectClasses to set
     */
    public void setAuxObjectClasses( List<ObjectClass> auxObjectClasses )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( !isReadOnly )
        {
            this.auxObjectClasses = auxObjectClasses;

            // update the OIDS now
            auxObjectClassOids.clear();

            for ( ObjectClass oc : auxObjectClasses )
            {
                auxObjectClassOids.add( oc.getOid() );
            }
        }
    }


    /**
     * @return the auxObjectClasses
     */
    public List<ObjectClass> getAuxObjectClasses()
    {
        return auxObjectClasses;
    }


    /**
     * @return the mayAttributeTypeOids
     */
    public List<String> getMayAttributeTypeOids()
    {
        return mayAttributeTypeOids;
    }


    /**
     * Add an allowed AttributeType
     *
     * @param oid The attributeType oid
     */
    public void addMayAttributeTypeOids( String oid )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( !isReadOnly )
        {
            mayAttributeTypeOids.add( oid );
        }
    }


    /**
     * Add an allowed AttributeType
     *
     * @param attributeType The attributeType
     */
    public void addMayAttributeTypes( AttributeType attributeType )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( !isReadOnly )
        {
            if ( !mayAttributeTypeOids.contains( attributeType.getOid() ) )
            {
                mayAttributeTypes.add( attributeType );
                mayAttributeTypeOids.add( attributeType.getOid() );
            }
        }
    }


    /**
     * @param mayAttributeTypeOids the mayAttributeTypeOids to set
     */
    public void setMayAttributeTypeOids( List<String> mayAttributeTypeOids )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( !isReadOnly )
        {
            this.mayAttributeTypeOids = mayAttributeTypeOids;
        }
    }


    /**
     * Sets the list of allowed AttributeTypes
     *
     * @param mayAttributeTypes the list of allowed AttributeTypes
     */
    public void setMayAttributeTypes( List<AttributeType> mayAttributeTypes )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( !isReadOnly )
        {
            this.mayAttributeTypes = mayAttributeTypes;

            // update the OIDS now
            mayAttributeTypeOids.clear();

            for ( AttributeType may : mayAttributeTypes )
            {
                mayAttributeTypeOids.add( may.getOid() );
            }
        }
    }


    /**
     * @return the mayAttributeTypes
     */
    public List<AttributeType> getMayAttributeTypes()
    {
        return mayAttributeTypes;
    }


    /**
     * @return the mustAttributeTypeOids
     */
    public List<String> getMustAttributeTypeOids()
    {
        return mustAttributeTypeOids;
    }


    /**
     * Add a required AttributeType OID
     *
     * @param oid The attributeType OID
     */
    public void addMustAttributeTypeOids( String oid )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( !isReadOnly )
        {
            mustAttributeTypeOids.add( oid );
        }
    }


    /**
     * Add a required AttributeType
     *
     * @param attributeType The attributeType
     */
    public void addMustAttributeTypes( AttributeType attributeType )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( !isReadOnly )
        {
            if ( !mustAttributeTypeOids.contains( attributeType.getOid() ) )
            {
                mustAttributeTypes.add( attributeType );
                mustAttributeTypeOids.add( attributeType.getOid() );
            }
        }
    }


    /**
     * @param mustAttributeTypeOids the mustAttributeTypeOids to set
     */
    public void setMustAttributeTypeOids( List<String> mustAttributeTypeOids )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( !isReadOnly )
        {
            this.mustAttributeTypeOids = mustAttributeTypeOids;
        }
    }


    /**
     * Sets the list of required AttributeTypes
     *
     * @param mayAttributeTypes the list of required AttributeTypes
     */
    public void setMustAttributeTypes( List<AttributeType> mustAttributeTypes )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( !isReadOnly )
        {
            this.mustAttributeTypes = mustAttributeTypes;

            // update the OIDS now
            mustAttributeTypeOids.clear();

            for ( AttributeType may : mustAttributeTypes )
            {
                mustAttributeTypeOids.add( may.getOid() );
            }
        }
    }


    /**
     * @return the mustAttributeTypes
     */
    public List<AttributeType> getMustAttributeTypes()
    {
        return mustAttributeTypes;
    }


    /**
     * @return the notAttributeTypeOids
     */
    public List<String> getNotAttributeTypeOids()
    {
        return notAttributeTypeOids;
    }


    /**
     * Add a precluded AttributeType
     *
     * @param oid The attributeType oid
     */
    public void addNotAttributeTypeOids( String oid )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( !isReadOnly )
        {
            notAttributeTypeOids.add( oid );
        }
    }


    /**
     * Add a precluded AttributeType
     *
     * @param attributeType The attributeType
     */
    public void addNotAttributeTypes( AttributeType attributeType )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( !isReadOnly )
        {
            if ( !notAttributeTypeOids.contains( attributeType.getOid() ) )
            {
                notAttributeTypes.add( attributeType );
                notAttributeTypeOids.add( attributeType.getOid() );
            }
        }
    }


    /**
     * @param notAttributeTypeOids the notAttributeTypeOids to set
     */
    public void setNotAttributeTypeOids( List<String> notAttributeTypeOids )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( !isReadOnly )
        {
            this.notAttributeTypeOids = notAttributeTypeOids;
        }
    }


    /**
     * Sets the list of precluded AttributeTypes
     *
     * @param mayAttributeTypes the list of precluded AttributeTypes
     */
    public void setNotAttributeTypes( List<AttributeType> notAttributeTypes )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( !isReadOnly )
        {
            this.notAttributeTypes = notAttributeTypes;

            // update the OIDS now
            notAttributeTypeOids.clear();

            for ( AttributeType not : notAttributeTypes )
            {
                notAttributeTypeOids.add( not.getOid() );
            }
        }
    }


    /**
     * @return the notAttributeTypes
     */
    public List<AttributeType> getNotAttributeTypes()
    {
        return notAttributeTypes;
    }


    /**
     * Inject the DITContentRule into the registries, updating the references to
     * other SchemaObject
     *
     * @param registries The Registries
     * @exception If the addition failed
     */
    public void addToRegistries( Registries registries ) throws LdapException
    {
        if ( registries != null )
        {
            AttributeTypeRegistry atRegistry = registries.getAttributeTypeRegistry();
            ObjectClassRegistry ocRegistry = registries.getObjectClassRegistry();

            if ( mayAttributeTypeOids != null )
            {
                mayAttributeTypes = new ArrayList<AttributeType>( mayAttributeTypeOids.size() );

                for ( String oid : mayAttributeTypeOids )
                {
                    mayAttributeTypes.add( atRegistry.lookup( oid ) );
                }
            }

            if ( mustAttributeTypeOids != null )
            {
                mustAttributeTypes = new ArrayList<AttributeType>( mustAttributeTypeOids.size() );

                for ( String oid : mustAttributeTypeOids )
                {
                    mustAttributeTypes.add( atRegistry.lookup( oid ) );
                }
            }

            if ( notAttributeTypeOids != null )
            {
                notAttributeTypes = new ArrayList<AttributeType>( notAttributeTypeOids.size() );

                for ( String oid : notAttributeTypeOids )
                {
                    notAttributeTypes.add( atRegistry.lookup( oid ) );
                }
            }

            if ( auxObjectClassOids != null )
            {
                auxObjectClasses = new ArrayList<ObjectClass>( auxObjectClassOids.size() );

                for ( String oid : auxObjectClassOids )
                {
                    auxObjectClasses.add( ocRegistry.lookup( oid ) );
                }
            }
        }
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return objectType + " " + DescriptionUtils.getDescription( this );
    }


    /**
     * Copy a DITContentRule
     */
    public DITContentRule copy()
    {
        DITContentRule copy = new DITContentRule( oid );

        // Copy the SchemaObject common data
        copy.copy( this );

        // copy the AUX ObjectClasses OIDs
        copy.auxObjectClassOids = new ArrayList<String>();

        for ( String oid : auxObjectClassOids )
        {
            copy.auxObjectClassOids.add( oid );
        }

        // copy the AUX ObjectClasses ( will be empty )
        copy.auxObjectClasses = new ArrayList<ObjectClass>();

        // Clone the MAY AttributeTypes OIDs
        copy.mayAttributeTypeOids = new ArrayList<String>();

        for ( String oid : mayAttributeTypeOids )
        {
            copy.mayAttributeTypeOids.add( oid );
        }

        // Clone the MAY AttributeTypes ( will be empty )
        copy.mayAttributeTypes = new ArrayList<AttributeType>();

        // Clone the MUST AttributeTypes OIDs
        copy.mustAttributeTypeOids = new ArrayList<String>();

        for ( String oid : mustAttributeTypeOids )
        {
            copy.mustAttributeTypeOids.add( oid );
        }

        // Clone the MUST AttributeTypes ( will be empty )
        copy.mustAttributeTypes = new ArrayList<AttributeType>();

        // Clone the NOT AttributeTypes OIDs
        copy.notAttributeTypeOids = new ArrayList<String>();

        for ( String oid : notAttributeTypeOids )
        {
            copy.notAttributeTypeOids.add( oid );
        }

        // Clone the NOT AttributeTypes ( will be empty )
        copy.notAttributeTypes = new ArrayList<AttributeType>();

        return copy;
    }


    /**
     * @see Object#equals(Object)
     */
    public boolean equals( Object o )
    {
        if ( !super.equals( o ) )
        {
            return false;
        }

        if ( !( o instanceof DITContentRule ) )
        {
            return false;
        }

        DITContentRule that = ( DITContentRule ) o;

        // TODO : complete the check
        return true;
    }


    /**
     * {@inheritDoc}
     */
    public void clear()
    {
        // Clear the common elements
        super.clear();

        // Clear the references
        auxObjectClasses.clear();
        auxObjectClassOids.clear();
        mayAttributeTypes.clear();
        mayAttributeTypeOids.clear();
        mustAttributeTypes.clear();
        mustAttributeTypeOids.clear();
        notAttributeTypes.clear();
        notAttributeTypeOids.clear();
    }
}
