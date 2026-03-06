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
package org.apache.directory.shared.ldap.message.control;


/**
 * Protocol request and response altering control interface. Any number of
 * controls may be associated with a protocol message.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 764131 $
 */
public interface Control
{
    /**
     * @return The Control's OID
     */
    String getOid();
    
    
    /**
     * Tells if the control is critical or not.
     *
     * @return <code>true/code> if the control is critical, <code>false</code> otherwise 
     */
    boolean isCritical();
    
    
    /**
     * Sets the critical flag which determines whether or not this control is
     * critical for the correct operation of a request or response message. The
     * default for this value should be false.
     * 
     * @param isCritical true if the control is critical false otherwise.
     */
    void setCritical( boolean isCritical );
    
    
    /**
     * Returns the encoded value for this Control
     *
     * @return The encoded value
     */
    byte[] getValue();
    
    
    /**
     * Stores the encoded value for this Control
     *
     * @param value The encoded value
     */
    void setValue( byte[] value );
    
    
    /**
     * Tells if the Control has a value or not
     *
     * @return <code>true</code> if th Control has a value, even containing no bytes.
     */
    boolean hasValue();
}
