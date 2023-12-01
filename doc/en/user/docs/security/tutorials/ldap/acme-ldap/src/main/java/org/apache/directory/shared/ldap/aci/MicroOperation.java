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
package org.apache.directory.shared.ldap.aci;


/**
 * An enumeration that represents all micro-operations that makes up LDAP
 * operations.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 481754 $, $Date: 2006-12-03 13:40:18 +0200 (Sun, 03 Dec 2006) $
 */
public enum MicroOperation
{
    // Permissions that may be used in conjunction with any component of
    // <tt>ProtectedItem</tt>s.
    ADD( "Add" ),

    DISCLOSE_ON_ERROR( "DiscloseOnError" ),

    READ( "Read" ),

    REMOVE( "Remove" ),

    // Permissions that may be used only in conjunction with the entry
    // component.
    BROWSE( "Browse" ),

    EXPORT( "Export" ),

    IMPORT( "Import" ),

    MODIFY( "Modify" ),

    RENAME ( "Rename" ),

    RETURN_DN( "ReturnDN" ),

    // Permissions that may be used in conjunction with any component,
    // except entry, of <tt>ProtectedItem</tt>s.
    COMPARE( "Compare" ),

    FILTER_MATCH( "FilterMatch" ),

    INVOKE( "Invoke" );

    private final String name;


    private MicroOperation(String name)
    {
        this.name = name;
    }


    /**
     * Returns the name of this micro-operation.
     */
    public String getName()
    {
        return name;
    }


    public String toString()
    {
        return name;
    }
}
