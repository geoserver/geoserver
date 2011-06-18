/*
 * @(#)MatchResult.java
 *
 * Copyright 2003-2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   1. Redistribution of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *   2. Redistribution in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN")
 * AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE
 * AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE,
 * EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for use in
 * the design, construction, operation or maintenance of any nuclear facility.
 */

package com.sun.xacml;

import com.sun.xacml.ctx.Status;

/**
 * This is used as the return value for the various target matching functions. It communicates that
 * either the target matches the input request, the target doesn't match the input request, or the
 * result is Indeterminate.
 * 
 * @since 1.0
 * @author Seth Proctor
 */
public class MatchResult {

    /**
     * An integer value indicating the the target matches the request
     */
    public static final int MATCH = 0;

    /**
     * An integer value indicating that the target doesn't match the request
     */
    public static final int NO_MATCH = 1;

    /**
     * An integer value indicating the the result is Indeterminate
     */
    public static final int INDETERMINATE = 2;

    //
    private int result;

    private Status status;

    /**
     * Constructor that creates a <code>MatchResult</code> with no Status
     * 
     * @param result
     *            the applicable result
     */
    public MatchResult(int result) {
        this(result, null);
    }

    /**
     * Constructor that creates a <code>MatchResult</code>, including Status data
     * 
     * @param result
     *            the applicable result
     * @param status
     *            the error information
     * 
     * @throws IllegalArgumentException
     *             if the input result isn't a valid value
     */
    public MatchResult(int result, Status status) throws IllegalArgumentException {

        // check if input result is a valid value
        if ((result != MATCH) && (result != NO_MATCH) && (result != INDETERMINATE))
            throw new IllegalArgumentException("Input result is not a valid" + "value");

        this.result = result;
        this.status = status;
    }

    /**
     * Returns the applicable result
     * 
     * @return the applicable result
     */
    public int getResult() {
        return result;
    }

    /**
     * Returns the status if there was an error, or null if no error occurred
     * 
     * @return the error status data or null
     */
    public Status getStatus() {
        return status;
    }

}
