/*
 * @(#)PolicyFinderResult.java
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

package com.sun.xacml.finder;

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.ctx.Status;

/**
 * This is used as the return value for the findPolicy() methods in the <code>PolicyFinder</code>.
 * It communicates either a found policy that applied to the request (eg, the target matches), an
 * Indeterminate state, or no applicable policies.
 * <p>
 * The OnlyOneApplicable combining logic is used in looking for a policy, so the result from calling
 * findPolicy can never be more than one policy.
 * 
 * @since 1.0
 * @author Seth Proctor
 */
public class PolicyFinderResult {

    // the single policy being returned
    private AbstractPolicy policy;

    // status that represents an error occurred
    private Status status;

    /**
     * Creates a result saying that no applicable policies were found.
     */
    public PolicyFinderResult() {
        policy = null;
        status = null;
    }

    /**
     * Creates a result containing a single applicable policy.
     * 
     * @param policy
     *            the applicable policy
     */
    public PolicyFinderResult(AbstractPolicy policy) {
        this.policy = policy;
        status = null;
    }

    /**
     * Create a result of Indeterminate, including Status data.
     * 
     * @param status
     *            the error information
     */
    public PolicyFinderResult(Status status) {
        policy = null;
        this.status = status;
    }

    /**
     * Returns true if the result was NotApplicable.
     * 
     * @return true if the result was NotApplicable
     */
    public boolean notApplicable() {
        return ((policy == null) && (status == null));
    }

    /**
     * Returns true if the result was Indeterminate.
     * 
     * @return true if there was an error
     */
    public boolean indeterminate() {
        return (status != null);
    }

    /**
     * Returns the found policy, or null if there was an error or no policy was found.
     * 
     * @return the applicable policy or null
     */
    public AbstractPolicy getPolicy() {
        return policy;
    }

    /**
     * Returns the status if there was an error, or null if no error occurred.
     * 
     * @return the error status data or null
     */
    public Status getStatus() {
        return status;
    }

}
