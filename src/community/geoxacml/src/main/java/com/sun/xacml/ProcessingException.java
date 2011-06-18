/*
 * @(#)ProcessingException.java
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

/**
 * Runtime exception that's thrown if any unexpected error occurs. This could appear, for example,
 * if you try to match a referernced policy that can't be resolved.
 * 
 * @since 1.0
 * @author Seth Proctor
 */
public class ProcessingException extends RuntimeException {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new <code>ProcessingException</code> with no message or cause.
     */
    public ProcessingException() {

    }

    /**
     * Constructs a new <code>ProcessingException</code> with a message, but no cause. The message
     * is saved for later retrieval by the {@link java.lang#Throwable.getMessage()
     * Throwable.getMessage()} method.
     * 
     * @param message
     *            the detail message (<code>null</code> if nonexistent or unknown)
     */
    public ProcessingException(String message) {
        super(message);
    }

    /**
     * Constructs a new <code>ProcessingException</code> with a cause, but no message. The cause is
     * saved for later retrieval by the {@link java.lang#Throwable.getCause() Throwable.getCause()}
     * method.
     * 
     * @param cause
     *            the cause (<code>null</code> if nonexistent or unknown)
     */
    public ProcessingException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new <code>ProcessingException</code> with a message and a cause. The message and
     * cause are saved for later retrieval by the {@link java.lang#Throwable.getMessage()
     * Throwable.getMessage()} and {@link java.lang#Throwable.getCause() Throwable.getCause()}
     * methods.
     * 
     * @param message
     *            the detail message (<code>null</code> if nonexistent or unknown)
     * @param cause
     *            the cause (<code>null</code> if nonexistent or unknown)
     */
    public ProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

}
