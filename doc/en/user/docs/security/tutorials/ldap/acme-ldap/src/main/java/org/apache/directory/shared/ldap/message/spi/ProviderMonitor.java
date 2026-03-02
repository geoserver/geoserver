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

package org.apache.directory.shared.ldap.message.spi;


import java.util.Properties;


/**
 * A Provider monitor's callback interface.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 437007 $
 */
public interface ProviderMonitor
{
    /** A do nothing monitor to use if none is provided */
    public static ProviderMonitor NOOP_MONITOR = new ProviderMonitor()
    {
        public final void propsFound( final String msg, final Properties props )
        {
        }


        public final void usingDefaults( final String msg, final Properties props )
        {
        }
    };


    /**
     * Callback used to monitor the discovered properties for the provider.
     * 
     * @param msg
     *            a message about where the properties were found or null
     * @param props
     *            the properties discovered
     */
    void propsFound( String msg, Properties props );


    /**
     * Callback used to monitor if and what set of defaults are being used.
     * 
     * @param msg
     *            a descriptive message about this event or null
     * @param props
     *            the properties that constitute the defaults
     */
    void usingDefaults( String msg, Properties props );
}
