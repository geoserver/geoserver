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
 * An enumeration that represents grants or denials of {@link MicroOperation}s.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 638218 $, $Date: 2008-03-18 07:07:20 +0200 (Tue, 18 Mar 2008) $
 */
public class GrantAndDenial
{
    // Permissions that may be used in conjunction with any component of
    // <tt>ProtectedItem</tt>s.
    /** Grant for {@link MicroOperation#ADD} */
    public static final GrantAndDenial GRANT_ADD = new GrantAndDenial( MicroOperation.ADD, 0, true );

    /** Denial for {@link MicroOperation#ADD} */
    public static final GrantAndDenial DENY_ADD = new GrantAndDenial( MicroOperation.ADD, 1, false );

    /** Grant for {@link MicroOperation#DISCLOSE_ON_ERROR} */
    public static final GrantAndDenial GRANT_DISCLOSE_ON_ERROR = new GrantAndDenial( MicroOperation.DISCLOSE_ON_ERROR,
        2, true );

    /** Denial for {@link MicroOperation#DISCLOSE_ON_ERROR} */
    public static final GrantAndDenial DENY_DISCLOSE_ON_ERROR = new GrantAndDenial( MicroOperation.DISCLOSE_ON_ERROR,
        3, false );

    /** Grant for {@link MicroOperation#READ} */
    public static final GrantAndDenial GRANT_READ = new GrantAndDenial( MicroOperation.READ, 4, true );

    /** Denial for {@link MicroOperation#READ} */
    public static final GrantAndDenial DENY_READ = new GrantAndDenial( MicroOperation.READ, 5, false );

    /** Grant for {@link MicroOperation#REMOVE} */
    public static final GrantAndDenial GRANT_REMOVE = new GrantAndDenial( MicroOperation.REMOVE, 6, true );

    /** Denial for {@link MicroOperation#REMOVE} */
    public static final GrantAndDenial DENY_REMOVE = new GrantAndDenial( MicroOperation.REMOVE, 7, false );

    // Permissions that may be used only in conjunction with the entry
    // component.
    /** Grant for {@link MicroOperation#BROWSE} */
    public static final GrantAndDenial GRANT_BROWSE = new GrantAndDenial( MicroOperation.BROWSE, 8, true );

    /** Denial for {@link MicroOperation#BROWSE} */
    public static final GrantAndDenial DENY_BROWSE = new GrantAndDenial( MicroOperation.BROWSE, 9, false );

    /** Grant for {@link MicroOperation#EXPORT} */
    public static final GrantAndDenial GRANT_EXPORT = new GrantAndDenial( MicroOperation.EXPORT, 10, true );

    /** Denial for {@link MicroOperation#EXPORT} */
    public static final GrantAndDenial DENY_EXPORT = new GrantAndDenial( MicroOperation.EXPORT, 11, false );

    /** Grant for {@link MicroOperation#IMPORT} */
    public static final GrantAndDenial GRANT_IMPORT = new GrantAndDenial( MicroOperation.IMPORT, 12, true );

    /** Denial for {@link MicroOperation#IMPORT} */
    public static final GrantAndDenial DENY_IMPORT = new GrantAndDenial( MicroOperation.IMPORT, 13, false );

    /** Grant for {@link MicroOperation#MODIFY} */
    public static final GrantAndDenial GRANT_MODIFY = new GrantAndDenial( MicroOperation.MODIFY, 14, true );

    /** Denial for {@link MicroOperation#MODIFY} */
    public static final GrantAndDenial DENY_MODIFY = new GrantAndDenial( MicroOperation.MODIFY, 15, false );

    /** Grant for {@link MicroOperation#RENAME} */
    public static final GrantAndDenial GRANT_RENAME = new GrantAndDenial( MicroOperation.RENAME, 16, true );

    /** Denial for {@link MicroOperation#RENAME} */
    public static final GrantAndDenial DENY_RENAME = new GrantAndDenial( MicroOperation.RENAME, 17, false );

    /** Grant for {@link MicroOperation#RETURN_DN} */
    public static final GrantAndDenial GRANT_RETURN_DN = new GrantAndDenial( MicroOperation.RETURN_DN, 18, true );

    /** Denial for {@link MicroOperation#RETURN_DN} */
    public static final GrantAndDenial DENY_RETURN_DN = new GrantAndDenial( MicroOperation.RETURN_DN, 19, false );

    // Permissions that may be used in conjunction with any component,
    // except entry, of <tt>ProtectedItem</tt>s.
    /** Grant for {@link MicroOperation#COMPARE} */
    public static final GrantAndDenial GRANT_COMPARE = new GrantAndDenial( MicroOperation.COMPARE, 20, true );

    /** Deny for {@link MicroOperation#COMPARE} */
    public static final GrantAndDenial DENY_COMPARE = new GrantAndDenial( MicroOperation.COMPARE, 21, false );

    /** Grant for {@link MicroOperation#FILTER_MATCH} */
    public static final GrantAndDenial GRANT_FILTER_MATCH = new GrantAndDenial( MicroOperation.FILTER_MATCH, 22, true );

    /** Denial for {@link MicroOperation#FILTER_MATCH} */
    public static final GrantAndDenial DENY_FILTER_MATCH = new GrantAndDenial( MicroOperation.FILTER_MATCH, 23, false );

    /** Grant for {@link MicroOperation#INVOKE} */
    public static final GrantAndDenial GRANT_INVOKE = new GrantAndDenial( MicroOperation.INVOKE, 24, true );

    /** Denial for {@link MicroOperation#INVOKE} */
    public static final GrantAndDenial DENY_INVOKE = new GrantAndDenial( MicroOperation.INVOKE, 25, false );

    private final MicroOperation microOperation;

    private final int code;

    private final String name;

    private final boolean grant;


    private GrantAndDenial(MicroOperation microOperation, int code, boolean grant)
    {
        this.microOperation = microOperation;
        this.code = code;
        this.name = ( grant ? "grant" : "deny" ) + microOperation.getName();
        this.grant = grant;
    }


    /**
     * Returns the {@link MicroOperation} related with this grant or denial.
     */
    public MicroOperation getMicroOperation()
    {
        return microOperation;
    }


    /**
     * Return the code number of this grant or denial.
     */
    public int getCode()
    {
        return code;
    }


    /**
     * Returns the name of this grant or denial.
     */
    public String getName()
    {
        return name;
    }


    /**
     * Returns <tt>true</tt> if and only if this is grant.
     */
    public boolean isGrant()
    {
        return grant;
    }


    public String toString()
    {
        return name;
    }
}
