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
package org.apache.directory.shared.ldap.schema.normalizers;


import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.StringValue;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.schema.Normalizer;


/**
 * 
 * Normalizer for boolean values.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class BooleanNormalizer extends Normalizer
{
    /** The serial UID */
    public static final long serialVersionUID = 1L;

    /**
     * Creates a new instance of BooleanNormalizer.
     */
    public BooleanNormalizer()
    {
        super( SchemaConstants.BOOLEAN_MATCH_MR_OID );
    }


    /**
     * {@inheritDoc}
     */
    public Value<?> normalize( Value<?> value ) throws LdapInvalidDnException
    {
        if ( value == null )
        {
            return null;
        }

        String strValue = value.getString();
        
        return new StringValue( strValue.trim().toUpperCase() );
    }

    
    /**
     * {@inheritDoc}
     */
    public String normalize( String value ) throws LdapInvalidDnException
    {
        if ( value == null )
        {
            return null;
        }

        return value.trim().toUpperCase();
    }
}
