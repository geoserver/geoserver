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
package org.apache.directory.shared.ldap.codec;

import org.apache.directory.shared.ldap.message.spi.BinaryAttributeDetector;
import org.apache.directory.shared.ldap.message.spi.Provider;
import org.apache.directory.shared.ldap.message.spi.ProviderDecoder;
import org.apache.directory.shared.ldap.message.spi.ProviderEncoder;
import org.apache.directory.shared.ldap.message.spi.ProviderException;


/**
 * The specific BER provider for LDAP.
 * 
 * @author <a href="mailto:dev@directory.apache.org"> Apache Directory Project</a>
 *         $Rev: 903719 $
 */
public class LdapProvider extends Provider
{
    /**
     * Creates an instance of a LDAP BER Provider.
     */
    private LdapProvider()
    {
        super( "LDAP LDAP BER Provider", "Apache Directory Project" );
    }

    /** the singleton LdapProvider instance */
    private static LdapProvider singleton;


    /**
     * Gets a handle on the singleton LdapProvider. Only one instance should
     * have to be instantiated for the entire jvm.
     * 
     * @return the singleton SnaccProvider instance
     */
    public static synchronized Provider getProvider()
    {
        if ( singleton == null )
        {
            singleton = new LdapProvider();
        }

        return singleton;
    }


    /**
     * Gets the encoder associated with this provider.
     * 
     * @return the provider's encoder.
     * @throws org.apache.directory.shared.ldap.message.spi.ProviderException
     *             if the provider or its encoder cannot be found
     */
    public ProviderEncoder getEncoder() throws ProviderException
    {
        return new LdapEncoder( this );
    }


    /**
     * Gets the decoder associated with this provider.
     * 
     * @return the provider's decoder.
     * @throws org.apache.directory.shared.ldap.message.spi.ProviderException
     *             if the provider or its decoder cannot be found
     */
    public ProviderDecoder getDecoder( BinaryAttributeDetector binaryAttributeDetector, int maxPDUSize ) throws ProviderException
    {
        return new LdapDecoder( this, binaryAttributeDetector, maxPDUSize );
    }
}
