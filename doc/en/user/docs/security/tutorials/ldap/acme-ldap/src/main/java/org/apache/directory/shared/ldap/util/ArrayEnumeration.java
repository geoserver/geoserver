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
package org.apache.directory.shared.ldap.util;


import java.util.Enumeration;
import java.util.NoSuchElementException;


/**
 * An enumeration wrapper around an array.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 596943 $
 */
public class ArrayEnumeration implements Enumeration
{
    /** the index into the array */
    private int index = 0;

    /** Underlying array that is wrapped */
    private final Object[] array;


    /**
     * Constructs an enumeration by wrapping an array.
     * 
     * @param array
     *            the underlying array that is wrapped
     */
    public ArrayEnumeration(Object[] array)
    {
        if ( array != null )
        {
            this.array = new Object[ array.length ];
            System.arraycopy( array, 0, this.array, 0, array.length );
        } else {
            this.array = null;
        }
    }


    public final boolean hasMoreElements()
    {
        return array != null && array.length != 0 && index < array.length;
    }


    public Object nextElement()
    {
        if ( !hasMoreElements() )
        {
            throw new NoSuchElementException( "no more objects in array" );
        }

        return array[index++];
    }
}
