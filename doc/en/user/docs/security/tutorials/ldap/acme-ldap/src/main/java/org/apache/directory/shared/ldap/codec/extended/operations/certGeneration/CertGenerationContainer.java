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
package org.apache.directory.shared.ldap.codec.extended.operations.certGeneration;

import org.apache.directory.shared.asn1.ber.AbstractContainer;

/**
 * 
 * A container for certificate generation request codec.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class CertGenerationContainer extends AbstractContainer
{
    /** CertGenerationObject */
    private CertGenerationObject certGenObj;


    /**
     * Creates a new CertGenContainer object. We will store one
     * grammar, it's enough ...
     */
    public CertGenerationContainer()
    {
        super();
        stateStack = new int[1];
        grammar = CertGenerationGrammar.getInstance();
        states = CertGenerationStatesEnum.getInstance();
    }


    /**
     * @return Returns the CertGenerationObject instance.
     */
    public CertGenerationObject getCertGenerationObject()
    {
        return certGenObj;
    }


    /**
     * Set a CertGenerationObject Object into the container. It will be completed by
     * the ldapDecoder.
     * 
     * @param certGenObj the CertGenerationObject to set.
     */
    public void setCertGenerationObject( CertGenerationObject certGenObj )
    {
        this.certGenObj = certGenObj;
    }


    /**
     * Clean the container for the next decoding.
     */
    public void clean()
    {
        super.clean();
        certGenObj = null;
    }
}
