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
package org.apache.directory.shared.ldap.schema.comparators;


import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.schema.LdapComparator;


/**
 * Compares Long keys and values within a table.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision: 437007 $
 */
public class LongComparator extends LdapComparator<Long>
{
    /**
     * Version id for serialization.
     */
    static final long serialVersionUID = 1L;
    
    /**
     * The LongComparator constructor. Its OID is the IntegerOrderingMatch matching
     * rule OID.
     */
    public LongComparator( String oid )
    {
        super( oid );
    }


    /**
     * Compare two objects.
     * 
     * @param obj1 First object
     * @param obj2 Second object
     * @return 1 if obj1 > obj2, 0 if obj1 == obj2, -1 if obj1 < obj2
     */
    public int compare( Long obj1, Long obj2 )
    {
        try
        {
            return obj1.compareTo( obj2 );
        }
        catch ( NullPointerException npe )
        {
            if ( obj1 == null )
            {
                throw new IllegalArgumentException( I18n.err( I18n.ERR_04219 ) );
            }
            else
            {
                throw new IllegalArgumentException( I18n.err( I18n.ERR_04220 ));
            }
        }
    }
}
