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
package org.apache.directory.shared.ldap.codec.controls;




/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class CascadeControl  extends AbstractControl
{
    /** The cascade control OID */
    public static final String CONTROL_OID = "1.3.6.1.4.1.18060.0.0.1";

    /**
     * Default constructor
     *
     */
    public CascadeControl()
    {
        super( CONTROL_OID );
        
        decoder = new CascadeControlDecoder();
    }

    
    /**
     * Returns the default control length.
     */
    public int computeLength()
    {
        // Call the super class to compute the global control length
        return super.computeLength( 0 );
    }

    
    /**
     * Return a String representing this Cascade Control.
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "    Cascade Control\n" );
        sb.append( "        oid : " ).append( getOid() ).append( '\n' );
        sb.append( "        critical : " ).append( isCritical() ).append( '\n' );
        
        return sb.toString();
    }
}
