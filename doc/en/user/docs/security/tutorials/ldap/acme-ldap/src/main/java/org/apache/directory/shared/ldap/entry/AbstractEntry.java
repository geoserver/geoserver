/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.shared.ldap.entry;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.directory.shared.ldap.name.DN;


/**
 * The Abstract class where all the DefaultClientEntry and DefaultServerEntry 
 * common fields and methods will be found.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class AbstractEntry<K> implements Entry
{
    /** The DN for this entry */
    protected DN dn;
    
    /** A map containing all the attributes for this entry */
    protected Map<K, EntryAttribute> attributes = new HashMap<K, EntryAttribute>();
    
    
    /**
     * Get this entry's DN.
     *
     * @return The entry's DN
     */
    public DN getDn()
    {
        return dn;
    }


    /**
     * Set this entry's DN.
     *
     * @param dn The DN associated with this entry
     */
    public void setDn( DN dn )
    {
        this.dn = dn;
    }
    
    
    /**
     * Remove all the attributes for this entry. The DN is not reset
     */
    public void clear()
    {
        attributes.clear();
    }
    
    
    /**
     * Returns an enumeration containing the zero or more attributes in the
     * collection. The behavior of the enumeration is not specified if the
     * attribute collection is changed.
     *
     * @return an enumeration of all contained attributes
     */
    public Iterator<EntryAttribute> iterator()
    {
        return Collections.unmodifiableMap( attributes ).values().iterator();
    }
    

    /**
     * Returns the number of attributes.
     *
     * @return the number of attributes
     */
    public int size()
    {
        return attributes.size();
    }
    
    
    /**
     * Clone the current entry
     */
    public Entry clone()
    {
        try
        {
            return (Entry)super.clone();
        }
        catch ( CloneNotSupportedException cnse )
        {
            return null;
        }
    }
}
