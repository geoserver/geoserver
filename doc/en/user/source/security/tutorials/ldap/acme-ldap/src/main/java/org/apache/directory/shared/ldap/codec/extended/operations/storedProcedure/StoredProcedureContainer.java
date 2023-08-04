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

package org.apache.directory.shared.ldap.codec.extended.operations.storedProcedure;


import org.apache.directory.shared.asn1.ber.AbstractContainer;


/**
 * A container for the StoredProcedure codec
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$, 
 */
public class StoredProcedureContainer extends AbstractContainer
{
    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** StoredProcedure */
    private StoredProcedure storedProcedure;


    // ~ Constructors
    // -------------------------------------------------------------------------------

    public StoredProcedureContainer()
    {
        super();
        stateStack = new int[1];
        grammar = StoredProcedureGrammar.getInstance();
        states = StoredProcedureStatesEnum.getInstance();
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------
    /**
     * @return Returns the ldapMessage.
     */
    public StoredProcedure getStoredProcedure()
    {
        return storedProcedure;
    }


    /**
     * Set a StoredProcedure object into the container. It will be completed by the
     * ldapDecoder.
     * 
     * @param ldapMessage
     *            The ldapMessage to set.
     */
    public void setStoredProcedure( StoredProcedure storedProcedure )
    {
        this.storedProcedure = storedProcedure;
    }


    public void clean()
    {
        super.clean();

        storedProcedure = null;
    }
}
