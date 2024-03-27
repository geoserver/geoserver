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

package org.apache.directory.shared.ldap.name;


import org.apache.directory.shared.ldap.exception.LdapException;


/**
 * Normalizers of ldap name component attributes and their values.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision: 923524 $
 */
public interface NameComponentNormalizer
{
    /**
     * Checks to see if an attribute name/oid is defined.
     * 
     * @param id
     *            the name/oid of the attribute to see if it is defined
     * @return true if it is, false otherwise
     */
    boolean isDefined( String id );

    /**
     * Normalizes the attribute name/alias to use the OID for it instead.
     * 
     * @param attributeName the name or OID of the attributeType
     * @return the OID of the attributeType if it is recognized
     * @throws LdapException if the attributeName is not recognized as a valid alias
     */
    String normalizeName( String attributeName ) throws LdapException;

    /**
     * Normalizes an attribute's value given the name of the attribute - short
     * names like 'cn' as well as 'commonName' should work here.
     * 
     * @param attributeName
     *            the name of the attribute
     * @param value
     *            the value of the attribute to normalize
     * @return the normalized value
     * @throws LdapException
     *             if there is a recognition problem or a syntax issue
     */
    Object normalizeByName( String attributeName, String value ) throws LdapException;


    /**
     * Normalizes an attribute's value given the name of the attribute - short
     * names like 'cn' as well as 'commonName' should work here.
     * 
     * @param attributeName
     *            the name of the attribute
     * @param value
     *            the value of the attribute to normalize
     * @return the normalized value
     * @throws LdapException
     *             if there is a recognition problem or a syntax issue
     */
    Object normalizeByName( String attributeName, byte[] value ) throws LdapException;


    /**
     * Normalizes an attribute's value given the OID of the attribute.
     * 
     * @param attributeOid
     *            the OID of the attribute
     * @param value
     *            the value of the attribute to normalize
     * @return the normalized value
     * @throws LdapException
     *             if there is a recognition problem or a syntax issue
     */
    Object normalizeByOid( String attributeOid, String value ) throws LdapException;


    /**
     * Normalizes an attribute's value given the OID of the attribute.
     * 
     * @param attributeOid
     *            the OID of the attribute
     * @param value
     *            the value of the attribute to normalize
     * @return the normalized value
     * @throws LdapException
     *             if there is a recognition problem or a syntax issue
     */
    Object normalizeByOid( String attributeOid, byte[] value ) throws LdapException;
}
