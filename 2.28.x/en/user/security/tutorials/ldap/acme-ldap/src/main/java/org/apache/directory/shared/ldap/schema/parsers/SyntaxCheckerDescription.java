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

import org.apache.directory.shared.ldap.schema.LoadableSchemaObject;
import org.apache.directory.shared.ldap.schema.SchemaObjectType;


/**
 * An ApacheDS specific schema description for a SyntaxChecker.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SyntaxCheckerDescription extends LoadableSchemaObject
{
	/** SerialVersionUID */
	private static final long serialVersionUID = 1L;

	/**
	 * Default constructor for a SyntaxCheckerDecription
	 * @param oid The SyntaxChecker OID
	 */
	public SyntaxCheckerDescription( String oid ) 
	{
		super( SchemaObjectType.SYNTAX_CHECKER, oid );
	}

    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "SyntaxChecker description : " + getDescription();
    }
}
