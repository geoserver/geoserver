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

package org.apache.directory.shared.ldap;


/**
 * This exception is thrown when a Backend operation is either temporarily
 * unsupported or perminantly unsupported as part of its implementation. Write
 * operations on a backend set to readonly throw a type of unsupported exception
 * called ReadOnlyException.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision: 437007 $
 */
public class NotImplementedException extends RuntimeException
{
    static final long serialVersionUID = -5899307402675964298L;


    /**
     * Constructs an Exception with a detailed message.
     */
    public NotImplementedException()
    {
        super( "N O T   I M P L E M E N T E D   Y E T !" );
    }


    /**
     * Constructs an Exception with a detailed message.
     * 
     * @param a_msg
     *            The message associated with the exception.
     */
    public NotImplementedException(String a_msg)
    {
        super( "N O T   I M P L E M E N T E D   Y E T !\n" + a_msg );
    }
}
