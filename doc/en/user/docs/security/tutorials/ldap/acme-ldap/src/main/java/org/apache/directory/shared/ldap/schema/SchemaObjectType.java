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

import org.apache.directory.shared.ldap.constants.SchemaConstants;

/**
 * The SchemaObject types
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public enum SchemaObjectType
{
    ATTRIBUTE_TYPE(0),
    COMPARATOR(1),
    DIT_CONTENT_RULE(2),
    DIT_STRUCTURE_RULE(3),
    LDAP_SYNTAX(4),
    MATCHING_RULE(5),
    MATCHING_RULE_USE(6),
    NAME_FORM(7),
    NORMALIZER(8),
    OBJECT_CLASS(9),
    SYNTAX_CHECKER(10);
    
    /** The inner value*/
    private int value;
    
    /**
     * A private constructor to associated a number to the type
     */
    private SchemaObjectType( int value )
    {
        this.value = value;
    }

    /**
     * @return The numeric value for this type
     */
    public int getValue()
    {
        return value;
    }
    
    
    /**
     * Get the RDN associated with a schemaObjectType
     *
     * @param schemaObjectType The type we want the RDN for
     * @return The associated RDN
     */
    public String getRdn()
    {
        String schemaObjectPath = null;
        
        switch ( this )
        {
            case ATTRIBUTE_TYPE :
                schemaObjectPath = SchemaConstants.ATTRIBUTES_TYPE_PATH;
                break;
                
            case COMPARATOR :
                schemaObjectPath = SchemaConstants.COMPARATORS_PATH;
                break;
                
            case DIT_CONTENT_RULE :
                schemaObjectPath = SchemaConstants.DIT_CONTENT_RULES_PATH;
                break;
                
            case DIT_STRUCTURE_RULE :
                schemaObjectPath = SchemaConstants.DIT_STRUCTURE_RULES_PATH;
                break;
                
            case LDAP_SYNTAX :
                schemaObjectPath = SchemaConstants.SYNTAXES_PATH;
                break;
                
            case MATCHING_RULE :
                schemaObjectPath = SchemaConstants.MATCHING_RULES_PATH;
                break;
                
            case MATCHING_RULE_USE :
                schemaObjectPath = SchemaConstants.MATCHING_RULE_USE_PATH;
                break;
                
            case NAME_FORM :
                schemaObjectPath = SchemaConstants.NAME_FORMS_PATH;
                break;
                
            case NORMALIZER :
                schemaObjectPath = SchemaConstants.NORMALIZERS_PATH;
                break;
                
            case OBJECT_CLASS :
                schemaObjectPath = SchemaConstants.OBJECT_CLASSES_PATH;
                break;
                
            case SYNTAX_CHECKER :
                schemaObjectPath = SchemaConstants.SYNTAX_CHECKERS_PATH;
                break;
        }
        
        return schemaObjectPath;
    }
}
