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

package org.apache.directory.shared.ldap.codec.util;


/**
 * The URI parsing and escape encoding exception.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision: 664290 $ $Date: 2002/03/14 15:14:01
 */
public class URIException extends Exception
{
    private static final long serialVersionUID = 1L;


    // ----------------------------------------------------------- constructors
    /**
     * The constructor with a reason string argument.
     * 
     * @param reason
     *            the reason
     */
    public URIException(String reason)
    {
        super( reason ); // for backward compatibility of Throwable
        this.reason = reason;
        this.reasonCode = UNKNOWN;
    }

    // -------------------------------------------------------------- constants

    /**
     * No specified reason code.
     */
    public static final int UNKNOWN = 0;

    /**
     * The URI parsing error.
     */
    // public static final int PARSING = 1;

    /**
     * The unsupported character encoding.
     */
    // public static final int UNSUPPORTED_ENCODING = 2;

    /**
     * The URI escape encoding and decoding error.
     */
    // public static final int ESCAPING = 3;

    /**
     * The DNS punycode encoding or decoding error.
     */
    // public static final int PUNYCODE = 4;
    // ------------------------------------------------------------- properties
    /**
     * The reason code.
     */
    protected int reasonCode;

    /**
     * The reason message.
     */
    protected String reason;


    // ---------------------------------------------------------------- methods

    /**
     * Get the reason code.
     * 
     * @return the reason code
     */
    public int getReasonCode()
    {
        return reasonCode;
    }
}
