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
package org.apache.directory.shared.ldap.entry;

import java.io.Externalizable;

/**
 * An internal interface for a ModificationItem. The name has been
 * chosen so that it does not conflict with @see ModificationItem
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface Modification extends Cloneable, Externalizable
{
    /**
     *  @return the operation
     */
    ModificationOperation getOperation();


    /**
     * Store the modification operation
     *
     * @param operation The DirContext value to assign
     */
    void setOperation( int operation );


    /**
     * Store the modification operation
     *
     * @param operation The ModificationOperation value to assign
     */
    void setOperation( ModificationOperation operation );


    /**
     * @return the attribute containing the modifications
     */
    EntryAttribute getAttribute();


    /**
     * Set the attribute's modification
     *
     * @param attribute The modified attribute
     */
    void setAttribute( EntryAttribute attribute );
    
    
    /**
     * The clone operation
     *
     * @return a clone of the current modification
     */
    Modification clone();
}