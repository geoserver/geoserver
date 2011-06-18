/*
 * @(#)TopLevelPolicyException.java
 *
 * Copyright 2006 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.xacml.support.finder;

import com.sun.xacml.ctx.Status;

/**
 * This is an exception thrown by the support code when there's an error trying to resolve a
 * top-level policy
 * 
 * @since 2.0
 * @author Seth Proctor
 */
public class TopLevelPolicyException extends Exception {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    // status explaining the error
    private Status status;

    /**
     * Constructs a new <code>TopLevelPolicyException</code> with no message or cause.
     * 
     * @param status
     *            the <code>Status</code> associated with this error
     */
    public TopLevelPolicyException(Status status) {
        this.status = status;
    }

    /**
     * Constructs a new <code>TopLevelPolicyException</code> with a message, but no cause. The
     * message is saved for later retrieval by the {@link java.lang#Throwable.getMessage()
     * Throwable.getMessage()} method.
     * 
     * @param status
     *            the <code>Status</code> associated with this error
     * @param message
     *            the detail message (<code>null</code> if nonexistent or unknown)
     */
    public TopLevelPolicyException(Status status, String message) {
        super(message);

        this.status = status;
    }

    /**
     * Constructs a new <code>TopLevelPolicyException</code> with a cause, but no message. The cause
     * is saved for later retrieval by the {@link java.lang#Throwable.getCause()
     * Throwable.getCause()} method.
     * 
     * @param status
     *            the <code>Status</code> associated with this error
     * @param cause
     *            the cause (<code>null</code> if nonexistent or unknown)
     */
    public TopLevelPolicyException(Status status, Throwable cause) {
        super(cause);

        this.status = status;
    }

    /**
     * Constructs a new <code>TopLevelPolicyException</code> with a message and a cause. The message
     * and cause are saved for later retrieval by the {@link java.lang#Throwable.getMessage()
     * Throwable.getMessage()} and {@link java.lang#Throwable.getCause() Throwable.getCause()}
     * methods.
     * 
     * @param status
     *            the <code>Status</code> associated with this error
     * @param message
     *            the detail message (<code>null</code> if nonexistent or unknown)
     * @param cause
     *            the cause (<code>null</code> if nonexistent or unknown)
     */
    public TopLevelPolicyException(Status status, String message, Throwable cause) {
        super(message, cause);

        this.status = status;
    }

    /**
     * Returns the status information associated with this error.
     * 
     * @return associated status
     */
    public Status getStatus() {
        return status;
    }

}
