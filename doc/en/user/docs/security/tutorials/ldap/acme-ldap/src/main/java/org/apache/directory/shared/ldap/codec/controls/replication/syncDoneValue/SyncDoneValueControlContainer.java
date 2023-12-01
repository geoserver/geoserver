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
package org.apache.directory.shared.ldap.codec.controls.replication.syncDoneValue;


import org.apache.directory.shared.asn1.ber.AbstractContainer;


/**
 * 
 * ASN.1 container for SyncDoneValueControl.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class SyncDoneValueControlContainer extends AbstractContainer
{
    /** syncDoneValue*/
    private SyncDoneValueControl control;


    /**
     * 
     * Creates a new SyncDoneValueControlContainer object.
     *
     */
    public SyncDoneValueControlContainer()
    {
        super();
        stateStack = new int[1];
        grammar = SyncDoneValueControlGrammar.getInstance();
        states = SyncDoneValueControlStatesEnum.getInstance();
    }


    /**
     * @return the SyncDoneValueControlCodec object
     */
    public SyncDoneValueControl getSyncDoneValueControl()
    {
        return control;
    }


    /**
     * Set a SyncDoneValueControlCodec Object into the container. It will be completed
     * by the ldapDecoder.
     * 
     * @param control the SyncDoneValueControlCodec to set.
     */
    public void setSyncDoneValueControl( SyncDoneValueControl control )
    {
        this.control = control;
    }


    /**
     * clean the container
     */
    @Override
    public void clean()
    {
        super.clean();
        control = null;
    }

}
