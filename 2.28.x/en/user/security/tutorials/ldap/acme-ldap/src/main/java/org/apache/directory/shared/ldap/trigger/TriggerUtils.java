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


package org.apache.directory.shared.ldap.trigger;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.util.AttributeUtils;

/**
 * A utility class for working with Triggers Execution Administrative Points
 * Trigger Execution Subentries and Trigger Specifications.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$
 */
public class TriggerUtils
{
    public static final String ADMINISTRATIVE_ROLE_ATTR = "administrativeRole";
    public static final String TRIGGER_EXECUTION_SPECIFIC_AREA_ATTR_VALUE = "triggerExecutionSpecificArea";
    public static final String TRIGGER_EXECUTION_SUBENTRY_OC = "triggerExecutionSubentry";
    public static final String ENTRY_TRIGGER_SPECIFICATION_ATTR = "entryTriggerSpecification";
    public static final String PRESCRIPTIVE_TRIGGER_SPECIFICATION_ATTR = "prescriptiveTriggerSpecification";
    
    
    public static void defineTriggerExecutionSpecificPoint( LdapContext apCtx ) throws NamingException
    {
        Attributes ap = apCtx.getAttributes( "", new String[] { ADMINISTRATIVE_ROLE_ATTR } );
        Attribute administrativeRole = ap.get( ADMINISTRATIVE_ROLE_ATTR );
        if ( administrativeRole == null || 
            !AttributeUtils.containsValueCaseIgnore( administrativeRole, TRIGGER_EXECUTION_SPECIFIC_AREA_ATTR_VALUE ) )
        {
            Attributes changes = new BasicAttributes( ADMINISTRATIVE_ROLE_ATTR, TRIGGER_EXECUTION_SPECIFIC_AREA_ATTR_VALUE, true );
            apCtx.modifyAttributes( "", DirContext.ADD_ATTRIBUTE, changes );
        }
    }
    
    
    public static void createTriggerExecutionSubentry(
        LdapContext apCtx,
        String subentryCN,
        String subtreeSpec,
        String prescriptiveTriggerSpec ) throws NamingException
    {
        Attributes subentry = new BasicAttributes( SchemaConstants.CN_AT, subentryCN, true );
        Attribute objectClass = new BasicAttribute( SchemaConstants.OBJECT_CLASS_AT );
        subentry.put( objectClass );
        objectClass.add( SchemaConstants.TOP_OC );
        objectClass.add( SchemaConstants.SUBENTRY_OC );
        objectClass.add( TRIGGER_EXECUTION_SUBENTRY_OC );
        subentry.put( SchemaConstants.SUBTREE_SPECIFICATION_AT, subtreeSpec );
        subentry.put( PRESCRIPTIVE_TRIGGER_SPECIFICATION_ATTR, prescriptiveTriggerSpec );
        apCtx.createSubcontext( "cn=" + subentryCN, subentry );
    }
    
    
    public static void loadPrescriptiveTriggerSpecification(
        LdapContext apCtx,
        String subentryCN,
        String triggerSpec ) throws NamingException
    {        
        Attributes changes = new BasicAttributes( PRESCRIPTIVE_TRIGGER_SPECIFICATION_ATTR, triggerSpec, true );
        apCtx.modifyAttributes( "cn=" + subentryCN, DirContext.ADD_ATTRIBUTE, changes );
    }
    
    
    public static void loadEntryTriggerSpecification(
        LdapContext ctx,
        String triggerSpec ) throws NamingException
    {        
        Attributes changes = new BasicAttributes( ENTRY_TRIGGER_SPECIFICATION_ATTR, triggerSpec, true );
        ctx.modifyAttributes( "", DirContext.ADD_ATTRIBUTE, changes );
    }
}
