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
package org.apache.directory.shared.ldap.message;


import java.util.NoSuchElementException;
import javax.naming.NamingEnumeration;


/**
 * A NamingEnumeration over an array of objects.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 494173 $
 */
public class ArrayNamingEnumeration<T> implements NamingEnumeration<T>
{
    /** the objects to enumerate */
    private final T[] objects;

    /** the index pointing into the array */
    private int index = 0;


    /**
     * Creates a NamingEnumeration over an array of objects.
     * 
     * @param objects
     *            the objects to enumerate over
     */
    public ArrayNamingEnumeration( T[] objects )
    {
        this.objects = objects;
    }


    public void close()
    {
        if ( objects != null )
        {
            index = objects.length;
        }
    }


    public boolean hasMore()
    {
        if ( objects == null || objects.length == 0 )
        {
            return false;
        }

        return index < objects.length;
    }


    public T next()
    {
        if ( objects == null || objects.length == 0 || index >= objects.length )
        {
            throw new NoSuchElementException();
        }

        T retval = objects[index];
        index++;
        return retval;
    }


    public boolean hasMoreElements()
    {
        return hasMore();
    }


    public T nextElement()
    {
        return next();
    }
}
