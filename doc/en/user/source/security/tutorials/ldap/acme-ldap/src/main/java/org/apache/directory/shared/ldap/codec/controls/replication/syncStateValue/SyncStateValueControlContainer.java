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
package org.apache.directory.shared.ldap.codec.controls.replication.syncStateValue;


import org.apache.directory.shared.asn1.ber.AbstractContainer;


/**
 * A container for the SyncStateValue control
 *  
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 741888 $, $Date: 2009-02-07 13:57:03 +0100 (Sat, 07 Feb 2009) $, 
 */
public class SyncStateValueControlContainer extends AbstractContainer
{
    /** SyncStateValueControl */
    private SyncStateValueControl control;


    /**
     * Creates a new SyncStateValueControlContainer object. We will store one grammar,
     * it's enough ...
     */
    public SyncStateValueControlContainer()
    {
        super();
        stateStack = new int[1];
        grammar = SyncStateValueControlGrammar.getInstance();
        states = SyncStateValueControlStatesEnum.getInstance();
    }


    /**
     * @return Returns the syncStateValue control.
     */
    public SyncStateValueControl getSyncStateValueControl()
    {
        return control;
    }


    /**
     * Set a SyncStateValueControl Object into the container. It will be completed by
     * the ldapDecoder.
     * 
     * @param control the SyncStateValueControl to set.
     */
    public void setSyncStateValueControl( SyncStateValueControl control )
    {
        this.control = control;
    }

    /**
     * Clean the container
     */
    public void clean()
    {
        super.clean();
        control = null;
    }
}
