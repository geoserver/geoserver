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


import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.entry.BinaryValue;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapInvalidDnException;
import org.apache.directory.shared.ldap.schema.Normalizer;


/**
 * A simple NameComponentNormalizer which uses the same Normalizer to always
 * normalize the value the same way regardless of the attribute the value is
 * for.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 928296 $
 */
public class SimpleNameComponentNormalizer implements NameComponentNormalizer
{
    /** the normalizer used to normalize the value every time */
    private final Normalizer normalizer;


    /**
     * Creates a new SimpleNameComponentNormalizer with the normalizer it uses
     * ever time irrespective of the attribute name or oid.
     * 
     * @param normalizer
     *            the Normalizer to use for all normalization requests
     */
    public SimpleNameComponentNormalizer( Normalizer normalizer )
    {
        this.normalizer = normalizer;
    }


    public Object normalizeByName( String name, String val ) throws LdapException
    {
        return normalizer.normalize( val );
    }


    public Object normalizeByName( String name, byte[] val ) throws LdapException
    {
        return normalizer.normalize( new BinaryValue( val ) );
    }


    public Object normalizeByOid( String oid, String val ) throws LdapException
    {
        return normalizer.normalize( val );
    }


    public Object normalizeByOid( String oid, byte[] val ) throws LdapException
    {
        return normalizer.normalize( new BinaryValue( val ) );
    }


    public boolean isDefined( String oid )
    {
        return true;
    }


    public String normalizeName( String attributeName ) throws LdapInvalidDnException
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_04216 ) );
    }
}
