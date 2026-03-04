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
package org.apache.directory.shared.ldap.schema.syntaxCheckers;

import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A SyntaxChecker which verifies that a value is a SubstringAssertion 
 * according to RFC 4517.
 * 
 * Implemented as binary right now ...
 *  
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 488616 $
 */
public class SubstringAssertionSyntaxChecker extends BinarySyntaxChecker
{
    /** A logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( SubstringAssertionSyntaxChecker.class );

    /** The serialVersionUID */
    private static final long serialVersionUID = 1L;

    /**
     * Creates an instance of SubstringAssertionSyntaxChecker
     */
    public SubstringAssertionSyntaxChecker()
    {
        super();
        setOid( SchemaConstants.SUBSTRING_ASSERTION_SYNTAX );
    }


    /**
     * {@inheritDoc}
     */
    public boolean isValidSyntax( Object value )
    {
        LOG.debug( "Syntax valid for '{}'", value );
        return true;
    }
}
