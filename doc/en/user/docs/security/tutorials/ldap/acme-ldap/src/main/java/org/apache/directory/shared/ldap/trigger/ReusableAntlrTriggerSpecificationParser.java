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


import antlr.TokenStream;


/**
 * A reusable lexer class extended from antlr generated parser for an LDAP
 * Trigger Specification as defined at <a
 * href="http://docs.safehaus.org/display/APACHEDS/Grammar+for+Triggers">
 * The ABNF grammar for LDAP Triggers</a>.
 * 
 * <p>This class enables the reuse of the antlr lexer without having to recreate
 * the it every time as stated in <a
 * href="http://www.antlr.org:8080/pipermail/antlr-interest/2003-April/003631.html">
 * an ANTLR Interest Group mail</a> .
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev:$
 */
class ReusableAntlrTriggerSpecificationParser extends AntlrTriggerSpecificationParser
{
    /**
     * Creates a ReusableAntlrTriggerSpecificationParser instance.
     */
    public ReusableAntlrTriggerSpecificationParser(TokenStream lexer)
    {
        super( lexer );
    }


    /**
     * Resets the state of an antlr parser.
     */
    public void resetState()
    {
        // no set method for this protected field.
        this.traceDepth = 0;

        this.getInputState().reset();
    }
}
