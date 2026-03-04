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
package org.apache.directory.shared.ldap.codec.search.controls.subentries;


import org.apache.directory.shared.asn1.ber.AbstractContainer;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 905338 $, $Date: 2010-02-01 19:07:13 +0200 (Mon, 01 Feb 2010) $, 
 */
public class SubentriesControlContainer extends AbstractContainer
{
    /** PSearchControl */
    private SubentriesControl control;


    /**
     * Creates a new SubEntryControlContainer object. 
     */
    public SubentriesControlContainer()
    {
        super();
        stateStack = new int[1];
        grammar = SubentriesControlGrammar.getInstance();
        states = SubentriesControlStatesEnum.getInstance();
    }


    /**
     * @return Returns the persistent search control.
     */
    public SubentriesControl getSubEntryControl()
    {
        return control;
    }


    /**
     * Set a SubEntryControl Object into the container. It will be completed by
     * the ldapDecoder.
     * 
     * @param control the SubEntryControl to set.
     */
    public void setSubEntryControl( SubentriesControl control )
    {
        this.control = control;
    }

    /**
     * Clean the current container
     */
    public void clean()
    {
        super.clean();
        control = null;
    }
}
