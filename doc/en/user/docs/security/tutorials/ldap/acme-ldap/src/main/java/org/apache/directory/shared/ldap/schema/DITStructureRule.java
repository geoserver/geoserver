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
import org.apache.directory.shared.ldap.NotImplementedException;


/**
 * A dITStructureRule definition. A dITStructureRules is a rule governing the
 * structure of the DIT by specifying a permitted superior to subordinate entry
 * relationship. A structure rule relates a nameForm, and therefore a STRUCTURAL
 * objectClass, to superior dITStructureRules. This permits entries of the
 * STRUCTURAL objectClass identified by the nameForm to exist in the DIT as
 * subordinates to entries governed by the indicated superior dITStructureRules.
 * Hence dITStructureRules only apply to structural object classes.
 * <p>
 * According to ldapbis [MODELS]:
 * </p>
 * 
 * <pre>
 *  DIT structure rule descriptions are written according to the ABNF:
 *  
 *    DITStructureRuleDescription = LPAREN WSP
 *        ruleid                    ; rule identifier
 *        [ SP &quot;NAME&quot; SP qdescrs ]  ; short names (descriptors)
 *        [ SP &quot;DESC&quot; SP qdstring ] ; description
 *        [ SP &quot;OBSOLETE&quot; ]         ; not active
 *        SP &quot;FORM&quot; SP oid          ; NameForm
 *        [ SP &quot;SUP&quot; ruleids ]      ; superior rules
 *        extensions WSP RPAREN     ; extensions
 * 
 *    ruleids = ruleid / ( LPAREN WSP ruleidlist WSP RPAREN )
 * 
 *    ruleidlist = ruleid *( SP ruleid )
 * 
 *    ruleid = number
 * 
 *  where:
 *    [ruleid] is the rule identifier of this DIT structure rule;
 *    NAME [qdescrs] are short names (descriptors) identifying this DIT
 *        structure rule;
 *    DESC [qdstring] is a short descriptive string;
 *    OBSOLETE indicates this DIT structure rule use is not active;
 *    FORM is specifies the name form associated with this DIT structure
 *        rule;
 *    SUP identifies superior rules (by rule id); and
 *    [extensions] describe extensions.
 *  
 *  If no superior rules are identified, the DIT structure rule applies
 *  to an autonomous administrative point (e.g. the root vertex of the
 *  subtree controlled by the subschema) [X.501].
 * </pre>
 * 
 * @see <a href="http://www.faqs.org/rfcs/rfc2252.html">RFC2252 Section 6.33</a>
 * @see <a
 *      href="http://www.ietf.org/internet-drafts/draft-ietf-ldapbis-models-11.txt">ldapbis
 *      [MODELS]</a>
 * @see DescriptionUtils#getDescription(DITStructureRule)
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 927122 $
 */
public class DITStructureRule extends AbstractSchemaObject
{
    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /** The rule ID. A DSR does not have an OID */
    private int ruleId;

    /** The associated NameForm */
    private String form;

    /** The list of superiors rules */
    private List<Integer> superRules;


    /**
     * Creates a new instance of DITStructureRule
     */
    public DITStructureRule( int ruleId )
    {
        super( SchemaObjectType.DIT_STRUCTURE_RULE, null );
        this.ruleId = ruleId;
        form = null;
        superRules = new ArrayList<Integer>();
    }


    /**
     *  @return The associated NameForm's OID
     */
    public String getForm()
    {
        return form;
    }


    /**
     * Sets the associated NameForm's OID
     *
     * @param form The NameForm's OID
     */
    public void setForm( String form )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( !isReadOnly )
        {
            this.form = form;
        }
    }


    /**
     * @return The Rule ID
     */
    public int getRuleId()
    {
        return ruleId;
    }


    /**
     * Sets the rule identifier of this DIT structure rule;
     *
     * @param ruleId the rule identifier of this DIT structure rule;
     */
    public void setRuleId( int ruleId )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( !isReadOnly )
        {
            this.ruleId = ruleId;
        }
    }


    /**
     * @return The list of superiors RuleIDs
     */
    public List<Integer> getSuperRules()
    {
        return superRules;
    }


    /**
     * Sets the list of superior RuleIds
     * 
     * @param superRules the list of superior RuleIds
     */
    public void setSuperRules( List<Integer> superRules )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        if ( !isReadOnly )
        {
            this.superRules = superRules;
        }
    }


    /**
     * Adds a new superior RuleId
     *
     * @param superRule The superior RuleID to add
     */
    public void addSuperRule( Integer superRule )
    {
        if ( locked )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04441, getName() ) );
        }
        
        superRules.add( superRule );
    }


    /**
     * The DSR does not have an OID, so throw an exception
     */
    public String getOid()
    {
        throw new NotImplementedException();
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return objectType + " " + DescriptionUtils.getDescription( this );
    }


    /**
     * Copy a DITStructureRule
     */
    public DITStructureRule copy()
    {
        DITStructureRule copy = new DITStructureRule( ruleId );

        // Copy the SchemaObject common data
        copy.copy( this );

        // Copy the Superiors rules
        copy.superRules = new ArrayList<Integer>();

        // Copy the form
        copy.form = form;

        for ( int ruleId : superRules )
        {
            copy.superRules.add( ruleId );
        }

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

        if ( !( o instanceof DITStructureRule ) )
        {
            return false;
        }

        DITStructureRule that = ( DITStructureRule ) o;

        // TODO : complete the test
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
        superRules.clear();
    }
}
