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


import java.util.NoSuchElementException;

import javax.naming.NamingException;
import javax.naming.NamingEnumeration;


/**
 * An empty NamingEnumeration without any values: meaning
 * hasMore/hasMoreElements() always returns false, and next/nextElement() always
 * throws a NoSuchElementException.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision: 574314 $
 */
public class EmptyEnumeration<T> implements NamingEnumeration<T>
{

    /**
     * @see javax.naming.NamingEnumeration#close()
     */
    public void close()
    {
    }


    /**
     * Always returns false.
     * 
     * @see javax.naming.NamingEnumeration#hasMore()
     */
    public boolean hasMore() throws NamingException
    {
        return false;
    }


    /**
     * Always throws NoSuchElementException.
     * 
     * @see javax.naming.NamingEnumeration#next()
     */
    public T next() throws NamingException
    {
        throw new NoSuchElementException();
    }


    /**
     * Always return false.
     * 
     * @see java.util.Enumeration#hasMoreElements()
     */
    public boolean hasMoreElements()
    {
        return false;
    }


    /**
     * Always throws NoSuchElementException.
     * 
     * @see java.util.Enumeration#nextElement()
     */
    public T nextElement()
    {
        throw new NoSuchElementException();
    }

}
